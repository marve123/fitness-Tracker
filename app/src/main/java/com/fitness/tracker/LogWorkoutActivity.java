package com.fitness.tracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LogWorkoutActivity — three calorie-handling modes:
 *
 *  MODE 1 — MANUAL:
 *    User enters: workout type, duration (minutes), calories burned, intensity
 *    System: saves exactly what was entered
 *
 *  MODE 2 — AUTOMATIC:
 *    User enters: workout type, duration (minutes), intensity
 *    System: calculates calories = duration × (MET × 3.5 × weight) / 200
 *
 *  MODE 3 — TARGET:
 *    User enters: workout type, target calories, intensity
 *    System: calculates required duration = (calories × 200) / (MET × 3.5 × weight)
 *            and auto-fills the duration field
 *
 * After a successful save the timer duration is written to SharedPreferences
 * so MainActivity can start the live countdown immediately on resume.
 */
public class LogWorkoutActivity extends AppCompatActivity {

    // ── MET values ─────────────────────────────────────────────────────────
    private static final double MET_LOW    = 3.0;
    private static final double MET_MEDIUM = 5.0;
    private static final double MET_HIGH   = 8.0;

    // ── UI ─────────────────────────────────────────────────────────────────
    private TextInputEditText etWorkoutType, etDuration, etCalories;
    private TextInputLayout   tilDuration, tilCalories;
    private Spinner           spinnerIntensity;
    private RadioGroup        rgMode;
    private TextView          tvCalculatedResult;

    // ── Data ───────────────────────────────────────────────────────────────
    private WorkoutDatabaseHelper dbHelper;
    private int currentMode = MODE_MANUAL;  // default
    private boolean isUpdatingSilently = false;

    private static final int MODE_MANUAL    = R.id.rb_manual;
    private static final int MODE_AUTOMATIC = R.id.rb_automatic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_workout);

        dbHelper = new WorkoutDatabaseHelper(this);

        setupToolbar();
        bindViews();
        setupIntensitySpinner();
        setupModeToggle();
        setupLiveCalculation();
        setupSaveButton();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Log Workout");
        }
    }

    private void bindViews() {
        etWorkoutType      = findViewById(R.id.et_workout_type);
        etDuration         = findViewById(R.id.et_duration);
        etCalories         = findViewById(R.id.et_calories);
        tilDuration        = findViewById(R.id.til_duration);
        tilCalories        = findViewById(R.id.til_calories);
        spinnerIntensity   = findViewById(R.id.spinner_intensity);
        rgMode             = findViewById(R.id.rg_mode);
        tvCalculatedResult = findViewById(R.id.tv_calculated_result);
    }

    private void setupIntensitySpinner() {
        String[] levels = {"Low", "Medium", "High"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIntensity.setAdapter(adapter);
    }

    // ── Mode Toggle ────────────────────────────────────────────────────────

    /**
     * Adjusts visible fields based on the selected mode:
     *
     *  Manual:    Duration (editable) + Calories (editable)
     *  Automatic: Duration (editable) + Calories hidden (auto-preview shown)
     *  Target:    Duration (auto-filled, disabled) + Calories (editable as goal)
     */
    private void setupModeToggle() {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            currentMode = checkedId;
            applyModeLayout(checkedId);
            updateLiveCalculation(); // refresh the preview
        });
        // Default to Manual
        applyModeLayout(MODE_MANUAL);
    }

    private void applyModeLayout(int mode) {
        if (mode == MODE_MANUAL) {
            // Both fields fully editable
            tilDuration.setHint("Duration (minutes)");
            tilDuration.setVisibility(View.VISIBLE);
            etDuration.setEnabled(true);
            etDuration.setFocusableInTouchMode(true);

            tilCalories.setHint("Calories Burned (kcal)");
            tilCalories.setVisibility(View.VISIBLE);
            etCalories.setEnabled(true);
            etCalories.setFocusableInTouchMode(true);

            tvCalculatedResult.setVisibility(View.GONE);

        } else { // AUTOMATIC
            // Duration editable; calories auto-calculated
            tilDuration.setHint("Duration (minutes)");
            tilDuration.setVisibility(View.VISIBLE);
            etDuration.setEnabled(true);
            etDuration.setFocusableInTouchMode(true);

            tilCalories.setVisibility(View.GONE);
            etCalories.setText("");

            tvCalculatedResult.setVisibility(View.VISIBLE);
            tvCalculatedResult.setText("⚡ Calories will be calculated automatically");
        }
    }

    // ── Live Calculation ───────────────────────────────────────────────────

    /**
     * Attaches TextWatchers so the preview updates as the user types,
     * and also reacts to intensity spinner changes.
     */
    private void setupLiveCalculation() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                updateLiveCalculation();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etDuration.addTextChangedListener(watcher);
        etCalories.addTextChangedListener(watcher);

        spinnerIntensity.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                updateLiveCalculation();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }

    /**
     * Recalculates and updates the preview TextView whenever any input changes.
     *
     * Automatic: preview = calculated calories
     * Target:    auto-fill duration field + show in preview
     */
    private void updateLiveCalculation() {
        if (isUpdatingSilently) return;
        if (currentMode == MODE_MANUAL) return; // nothing to preview

        float weight  = getUserWeight();
        double met    = getMET();

        String dStr = etDuration.getText().toString().trim();
        if (TextUtils.isEmpty(dStr)) {
            tvCalculatedResult.setText("⚡ Enter duration to preview calories");
            return;
        }
        try {
            int duration = Integer.parseInt(dStr);
            if (duration <= 0) { tvCalculatedResult.setText("⚡ Duration must be > 0"); return; }
            double cal = calculateCalories(duration, met, weight);
            tvCalculatedResult.setText(
                    String.format("⚡ Estimated Calories: %.1f kcal", cal));
        } catch (NumberFormatException e) {
            tvCalculatedResult.setText("⚡ Enter a valid duration");
        }
    }

    // ── Save Workout ───────────────────────────────────────────────────────

    private void setupSaveButton() {
        MaterialButton btnSave = findViewById(R.id.btn_save_workout);
        btnSave.setOnClickListener(v -> saveWorkout());
    }

    private void saveWorkout() {

        // ── 1. Workout type ────────────────────────────────────────────
        String workoutType = etWorkoutType.getText().toString().trim();
        if (TextUtils.isEmpty(workoutType)) {
            etWorkoutType.setError("Enter a workout type (e.g. Running)");
            etWorkoutType.requestFocus();
            return;
        }

        // ── 2. Intensity ───────────────────────────────────────────────
        String intensity = spinnerIntensity.getSelectedItem().toString();
        double met       = getMET();
        float  weight    = getUserWeight();

        // ── 3. Mode-specific validation & value resolution ─────────────
        int    finalDuration;
        double finalCalories;

        if (currentMode == MODE_MANUAL) {

            // Duration
            String dStr = etDuration.getText().toString().trim();
            if (TextUtils.isEmpty(dStr)) {
                etDuration.setError("Enter duration in minutes");
                etDuration.requestFocus();
                return;
            }
            try {
                finalDuration = Integer.parseInt(dStr);
                if (finalDuration <= 0 || finalDuration > 600) {
                    etDuration.setError("Duration must be 1–600 minutes");
                    etDuration.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etDuration.setError("Enter a valid whole number");
                etDuration.requestFocus();
                return;
            }

            // Calories
            String cStr = etCalories.getText().toString().trim();
            if (TextUtils.isEmpty(cStr)) {
                etCalories.setError("Enter calories burned");
                etCalories.requestFocus();
                return;
            }
            try {
                finalCalories = Double.parseDouble(cStr);
                if (finalCalories <= 0 || finalCalories > 10000) {
                    etCalories.setError("Enter a value between 1 and 10,000 kcal");
                    etCalories.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etCalories.setError("Enter a valid number");
                etCalories.requestFocus();
                return;
            }

        } else { // AUTOMATIC MODE

            // Duration (user-entered)
            String dStr = etDuration.getText().toString().trim();
            if (TextUtils.isEmpty(dStr)) {
                etDuration.setError("Enter duration in minutes");
                etDuration.requestFocus();
                return;
            }
            try {
                finalDuration = Integer.parseInt(dStr);
                if (finalDuration <= 0 || finalDuration > 600) {
                    etDuration.setError("Duration must be 1–600 minutes");
                    etDuration.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etDuration.setError("Enter a valid whole number");
                etDuration.requestFocus();
                return;
            }

            // Calories auto-calculated
            finalCalories = calculateCalories(finalDuration, met, weight);
        }

        // ── 4. Build and insert the Workout ────────────────────────────
        String date    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        Workout workout = new Workout(workoutType, finalDuration,
                finalCalories, intensity, date);
        long rowId      = dbHelper.insertWorkout(workout);

        if (rowId < 0) {
            Toast.makeText(this, "Failed to save workout. Please try again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // ── 5. Hand off timer duration to MainActivity via SharedPrefs ─
        // The timer ONLY starts when Save is clicked (per spec §6)
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(SetupActivity.KEY_TIMER_ACTIVE, true)
                .putInt(SetupActivity.KEY_TIMER_MINUTES, finalDuration)
                .apply();

        Toast.makeText(this,
                String.format("Saved! %.1f kcal | %d min | Timer started ⏱",
                        finalCalories, finalDuration),
                Toast.LENGTH_LONG).show();

        finish(); // Return to MainActivity → onResume picks up the timer
    }

    // ── Calculation Helpers ────────────────────────────────────────────────

    /**
     * Calories = duration × (MET × 3.5 × weight) / 200
     */
    private double calculateCalories(int durationMin, double met, float weightKg) {
        return durationMin * (met * 3.5 * weightKg) / 200.0;
    }

    /** Returns MET value for the currently selected intensity level. */
    private double getMET() {
        String intensity = spinnerIntensity.getSelectedItem().toString();
        switch (intensity) {
            case "High":   return MET_HIGH;
            case "Medium": return MET_MEDIUM;
            default:       return MET_LOW;
        }
    }

    /** Returns user weight from SharedPreferences (defaults to 70 kg). */
    private float getUserWeight() {
        return getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                .getFloat(SetupActivity.KEY_WEIGHT, 70f);
    }
}
