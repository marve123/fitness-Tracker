package com.fitness.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * WorkoutDatabaseHelper manages the FitnessTrackerDB SQLite database.
 *
 * Responsibilities:
 *  - Create / upgrade the Workouts table
 *  - Insert, retrieve, and delete workout records
 *  - Aggregate today's calorie total and workout count
 */
public class WorkoutDatabaseHelper extends SQLiteOpenHelper {

    // ── Constants ──────────────────────────────────────────────────────────
    private static final String DB_NAME       = "FitnessTrackerDB";
    private static final int    DB_VERSION    = 1;
    private static final String TABLE         = "Workouts";

    // Column names (match the spec exactly)
    public static final String COL_ID         = "id";
    public static final String COL_TYPE       = "workoutType";
    public static final String COL_DURATION   = "duration";
    public static final String COL_CALORIES   = "caloriesBurned";
    public static final String COL_INTENSITY  = "intensity";
    public static final String COL_DATE       = "date";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ("
            + COL_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_TYPE      + " TEXT NOT NULL, "
            + COL_DURATION  + " INTEGER NOT NULL, "
            + COL_CALORIES  + " REAL NOT NULL, "
            + COL_INTENSITY + " TEXT NOT NULL, "
            + COL_DATE      + " TEXT NOT NULL)";

    // ── Constructor ────────────────────────────────────────────────────────
    public WorkoutDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ── INSERT ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new workout record.
     * @return row ID on success, -1 on failure.
     */
    public long insertWorkout(Workout w) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TYPE,      w.getWorkoutType());
        cv.put(COL_DURATION,  w.getDuration());
        cv.put(COL_CALORIES,  w.getCaloriesBurned());
        cv.put(COL_INTENSITY, w.getIntensity());
        cv.put(COL_DATE,      w.getDate());
        long result = db.insert(TABLE, null, cv);
        db.close();
        return result;
    }

    // ── RETRIEVE ───────────────────────────────────────────────────────────

    /**
     * Returns all workouts ordered newest-first.
     */
    public List<Workout> getAllWorkouts() {
        List<Workout> list = new ArrayList<>();
        SQLiteDatabase db  = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null,
                COL_ID + " DESC");

        if (c.moveToFirst()) {
            do {
                Workout w = new Workout();
                w.setId(c.getInt(c.getColumnIndexOrThrow(COL_ID)));
                w.setWorkoutType(c.getString(c.getColumnIndexOrThrow(COL_TYPE)));
                w.setDuration(c.getInt(c.getColumnIndexOrThrow(COL_DURATION)));
                w.setCaloriesBurned(c.getDouble(c.getColumnIndexOrThrow(COL_CALORIES)));
                w.setIntensity(c.getString(c.getColumnIndexOrThrow(COL_INTENSITY)));
                w.setDate(c.getString(c.getColumnIndexOrThrow(COL_DATE)));
                list.add(w);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    /**
     * Deletes a workout by primary key.
     */
    public void deleteWorkout(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ── AGGREGATIONS ───────────────────────────────────────────────────────

    /**
     * Total calories burned today using SQL SUM aggregation.
     */
    public double getTodayCalories() {
        String today = getToday();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_CALORIES + ") FROM " + TABLE
                + " WHERE " + COL_DATE + "=?",
                new String[]{today});
        double total = 0;
        if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    /**
     * Number of workouts logged today.
     */
    public int getTodayWorkoutCount() {
        String today = getToday();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE
                + " WHERE " + COL_DATE + "=?",
                new String[]{today});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }
}
