package com.fitness.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

/**
 * MainActivity — the main dashboard.
 *
 * Displays:
 *  - Personalised welcome message
 *  - Live workout countdown timer (starts after Save Workout is clicked)
 *  - Today's calories burned + workout count
 *  - Full workout history in a RecyclerView with delete support
 *
 * Timer lifecycle:
 *  LogWorkoutActivity writes the duration to SharedPreferences and sets
 *  a "timer_active" flag.  onResume() here picks that up and starts the
 *  CountDownTimer, which runs on the main thread until it finishes or the
 *  activity is destroyed.
 */
public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView     tvWelcome, tvTimerDisplay, tvTimerLabel;
    private TextView     tvTodayCalories, tvTodayWorkouts;
    private RecyclerView recyclerWorkouts;

    // ── State ──────────────────────────────────────────────────────────────
    private WorkoutDatabaseHelper dbHelper;
    private WorkoutAdapter        adapter;
    private CountDownTimer        countDownTimer;
    private boolean               timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new WorkoutDatabaseHelper(this);
        bindViews();
        setupLogButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setWelcomeMessage();
        refreshStats();
        checkAndStartTimer();   // picks up timer flag set by LogWorkoutActivity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }

    // ── View Setup ────────────────────────────────────────────────────────

    private void bindViews() {
        tvWelcome        = findViewById(R.id.tv_welcome);
        tvTimerDisplay   = findViewById(R.id.tv_timer_display);
        tvTimerLabel     = findViewById(R.id.tv_timer_label);
        tvTodayCalories  = findViewById(R.id.tv_today_calories);
        tvTodayWorkouts  = findViewById(R.id.tv_today_workouts);
        recyclerWorkouts = findViewById(R.id.recycler_workouts);
        recyclerWorkouts.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupLogButton() {
        MaterialButton btn = findViewById(R.id.btn_log_workout);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, LogWorkoutActivity.class)));
    }

    // ── Personalisation ───────────────────────────────────────────────────

    private void setWelcomeMessage() {
        SharedPreferences prefs = getSharedPreferences(
                SetupActivity.PREFS_NAME, MODE_PRIVATE);
        String name = prefs.getString(SetupActivity.KEY_NAME, "Athlete");
        tvWelcome.setText("Welcome, " + name + " 💪");
    }

    // ── Dashboard Refresh ─────────────────────────────────────────────────

    /**
     * Reloads today's totals and rebuilds the RecyclerView.
     * Called on every onResume so the UI is always up to date.
     */
    private void refreshStats() {
        double todayCalories = dbHelper.getTodayCalories();
        int    todayCount    = dbHelper.getTodayWorkoutCount();

        tvTodayCalories.setText(String.format("%.1f kcal", todayCalories));
        tvTodayWorkouts.setText(String.valueOf(todayCount));

        List<Workout> workouts = dbHelper.getAllWorkouts();
        adapter = new WorkoutAdapter(this, workouts, this::confirmDelete);
        recyclerWorkouts.setAdapter(adapter);
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    /**
     * Reads the SharedPreferences flag written by LogWorkoutActivity.
     * If a new timer was requested, starts the countdown.
     */
    private void checkAndStartTimer() {
        SharedPreferences prefs = getSharedPreferences(
                SetupActivity.PREFS_NAME, MODE_PRIVATE);
        boolean active = prefs.getBoolean(SetupActivity.KEY_TIMER_ACTIVE, false);

        if (active) {
            int minutes = prefs.getInt(SetupActivity.KEY_TIMER_MINUTES, 0);
            // Clear the flag so we don't restart on next resume
            prefs.edit().putBoolean(SetupActivity.KEY_TIMER_ACTIVE, false).apply();

            if (minutes > 0) {
                cancelTimer();            // cancel any previous timer first
                startWorkoutTimer(minutes);
            }
        }
    }

    /**
     * Starts a countdown timer for the given number of minutes.
     * Updates tvTimerDisplay every second.
     */
    private void startWorkoutTimer(int minutes) {
        long totalMs = minutes * 60 * 1000L;
        timerRunning = true;
        tvTimerLabel.setText("⏱ Workout in progress...");

        countDownTimer = new CountDownTimer(totalMs, 1000) {

            @Override
            public void onTick(long msRemaining) {
                long secs = msRemaining / 1000;
                long m    = secs / 60;
                long s    = secs % 60;
                tvTimerDisplay.setText(
                        String.format(Locale.getDefault(), "%02d:%02d", m, s));
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                tvTimerDisplay.setText("00:00");
                tvTimerLabel.setText("✅ Workout Complete!");
                onWorkoutTimerFinished();
            }
        }.start();
    }

    /** Stops and clears the timer without triggering the finish callback. */
    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerRunning = false;
    }

    /**
     * Called when the countdown reaches zero.
     * Vibrates the device and shows a completion dialog.
     */
    private void onWorkoutTimerFinished() {
        // Vibrate
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            long[] pattern = new long[]{0, 500, 200, 500, 200, 500};
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vib.vibrate(pattern, -1);
            }
        }

        // Alert dialog
        new AlertDialog.Builder(this)
                .setTitle("🏁 Workout Complete!")
                .setMessage("Great work! Your workout session has finished.\n\n"
                        + "Keep pushing towards your goals!")
                .setPositiveButton("Awesome!", null)
                .setCancelable(false)
                .show();

        Toast.makeText(this, "Workout Completed 🎉", Toast.LENGTH_LONG).show();
    }

    // ── Delete Confirmation ───────────────────────────────────────────────

    /**
     * Shows a confirmation AlertDialog before permanently deleting a workout.
     * Deletion only proceeds when the user confirms.
     */
    private void confirmDelete(int workoutId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to remove this workout?\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteWorkout(workoutId);
                    adapter.removeItem(position);
                    // Refresh totals after deletion
                    tvTodayCalories.setText(
                            String.format("%.1f kcal", dbHelper.getTodayCalories()));
                    tvTodayWorkouts.setText(
                            String.valueOf(dbHelper.getTodayWorkoutCount()));
                    Toast.makeText(this, "Workout deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
