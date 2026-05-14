package com.fitness.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * SetupActivity — shown only on first launch.
 *
 * Collects the user's name and weight, stores them in SharedPreferences,
 * then forwards permanently to MainActivity on all future launches.
 */
public class SetupActivity extends AppCompatActivity {

    // SharedPreferences keys shared across the app
    public static final String PREFS_NAME     = "FitnessPrefs";
    public static final String KEY_NAME       = "user_name";
    public static final String KEY_WEIGHT     = "user_weight";
    public static final String KEY_SETUP_DONE = "setup_done";

    // Timer handoff keys (written by LogWorkoutActivity, read by MainActivity)
    public static final String KEY_TIMER_ACTIVE  = "timer_active";
    public static final String KEY_TIMER_MINUTES = "timer_duration_minutes";

    private TextInputEditText etName, etWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip setup if already done
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SETUP_DONE, false)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_setup);

        etName   = findViewById(R.id.et_name);
        etWeight = findViewById(R.id.et_weight);
        MaterialButton btnSave = findViewById(R.id.btn_save_profile);

        btnSave.setOnClickListener(v -> saveProfile(prefs));
    }

    // ── Save Profile ──────────────────────────────────────────────────────

    private void saveProfile(SharedPreferences prefs) {
        String name      = etName.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            etName.setError("Name must be at least 2 characters");
            etName.requestFocus();
            return;
        }

        // Validate weight
        if (TextUtils.isEmpty(weightStr)) {
            etWeight.setError("Weight is required");
            etWeight.requestFocus();
            return;
        }

        float weight;
        try {
            weight = Float.parseFloat(weightStr);
        } catch (NumberFormatException e) {
            etWeight.setError("Enter a valid number");
            etWeight.requestFocus();
            return;
        }

        if (weight < 20f || weight > 500f) {
            etWeight.setError("Enter a weight between 20 and 500 kg");
            etWeight.requestFocus();
            return;
        }

        // Persist profile
        prefs.edit()
             .putString(KEY_NAME,        name)
             .putFloat(KEY_WEIGHT,       weight)
             .putBoolean(KEY_SETUP_DONE, true)
             .apply();

        Toast.makeText(this,
                "Welcome, " + name + "! Let's get moving 💪",
                Toast.LENGTH_LONG).show();

        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
