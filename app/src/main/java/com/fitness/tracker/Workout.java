package com.fitness.tracker;

/**
 * Workout model — maps directly to the SQLite Workouts table.
 */
public class Workout {

    private int    id;
    private String workoutType;   // free-text entered by user
    private int    duration;      // minutes (manual or auto-calculated in Target Mode)
    private double caloriesBurned;
    private String intensity;     // "Low" | "Medium" | "High"
    private String date;          // yyyy-MM-dd

    // ── Constructors ──────────────────────────────────────────────────────

    public Workout() {}

    public Workout(String workoutType, int duration, double caloriesBurned,
                   String intensity, String date) {
        this.workoutType    = workoutType;
        this.duration       = duration;
        this.caloriesBurned = caloriesBurned;
        this.intensity      = intensity;
        this.date           = date;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public String getWorkoutType()     { return workoutType; }
    public void   setWorkoutType(String t) { this.workoutType = t; }

    public int    getDuration()        { return duration; }
    public void   setDuration(int d)   { this.duration = d; }

    public double getCaloriesBurned()  { return caloriesBurned; }
    public void   setCaloriesBurned(double c) { this.caloriesBurned = c; }

    public String getIntensity()       { return intensity; }
    public void   setIntensity(String i) { this.intensity = i; }

    public String getDate()            { return date; }
    public void   setDate(String d)    { this.date = d; }
}
