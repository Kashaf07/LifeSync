package com.example.lifesync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Class name changed from DatabaseHelper to HabitDatabaseHelper
public class HabitDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "HabitDatabaseHelper";
    private static final String DATABASE_NAME = "HabitTracker.db";
    private static final int DATABASE_VERSION = 3;

    // Habits table
    private static final String TABLE_HABITS = "habits";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_END_DATE = "end_date";

    // Habit completion history table
    private static final String TABLE_HABIT_HISTORY = "habit_history";
    private static final String COLUMN_HISTORY_ID = "history_id";
    private static final String COLUMN_HABIT_ID = "habit_id";
    private static final String COLUMN_COMPLETION_DATE = "completion_date";
    private static final String COLUMN_IS_COMPLETED = "is_completed";

    public HabitDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HABITS_TABLE = "CREATE TABLE " + TABLE_HABITS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_COMPLETED + " INTEGER,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_PROGRESS + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_END_DATE + " TEXT" + ")";
        db.execSQL(CREATE_HABITS_TABLE);

        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HABIT_HISTORY + "("
                + COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_HABIT_ID + " INTEGER,"
                + COLUMN_COMPLETION_DATE + " TEXT,"
                + COLUMN_IS_COMPLETED + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_HABIT_ID + ") REFERENCES " + TABLE_HABITS + "(" + COLUMN_ID + "),"
                + "UNIQUE(" + COLUMN_HABIT_ID + ", " + COLUMN_COMPLETION_DATE + ")"
                + ")";
        db.execSQL(CREATE_HISTORY_TABLE);

        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String CREATE_HISTORY_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_HABIT_HISTORY + "("
                    + COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_HABIT_ID + " INTEGER,"
                    + COLUMN_COMPLETION_DATE + " TEXT,"
                    + COLUMN_IS_COMPLETED + " INTEGER,"
                    + "FOREIGN KEY(" + COLUMN_HABIT_ID + ") REFERENCES " + TABLE_HABITS + "(" + COLUMN_ID + "),"
                    + "UNIQUE(" + COLUMN_HABIT_ID + ", " + COLUMN_COMPLETION_DATE + ")"
                    + ")";
            db.execSQL(CREATE_HISTORY_TABLE);
            Log.d(TAG, "Database upgraded to version 2 - history table added");
        }

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN " + COLUMN_END_DATE + " TEXT");
                Log.d(TAG, "Database upgraded to version 3 - end_date column added");
            } catch (Exception e) {
                Log.e(TAG, "Error adding end_date column: " + e.getMessage());
            }
        }
    }

    /**
     * Adds a new habit to the database and returns the ID of the new row.
     * The Habit object's ID is also updated internally.
     * * @param habit The Habit object to add.
     * @return The row ID of the newly inserted habit, or -1 if an error occurred.
     */
    public long addHabit(Habit habit) { // <--- MODIFIED TO RETURN long
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, habit.getName());
        values.put(COLUMN_COMPLETED, habit.isCompleted() ? 1 : 0);
        values.put(COLUMN_COLOR, habit.getColor());
        values.put(COLUMN_PROGRESS, habit.getProgress());

        // If no start date provided, use current date
        String startDate = habit.getDate();
        if (startDate == null || startDate.isEmpty()) {
            startDate = getCurrentDate();
        }
        values.put(COLUMN_DATE, startDate);
        values.put(COLUMN_END_DATE, habit.getEndDate());

        long id = db.insert(TABLE_HABITS, null, values);

        // Update the habit object with the generated ID
        if (id != -1) {
            habit.setId((int) id);
            Log.d(TAG, "Habit added with ID: " + id + " starting from: " + startDate);
        } else {
            Log.e(TAG, "Error adding habit to database.");
        }

        db.close();
        return id; // <--- Return the generated ID
    }

    public Habit getHabitById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Habit habit = null;
        try {
            // Note: Assuming Habit class is available in this package or imported
            cursor = db.query(TABLE_HABITS, new String[]{COLUMN_ID,
                            COLUMN_NAME, COLUMN_COMPLETED, COLUMN_COLOR, COLUMN_PROGRESS, COLUMN_DATE, COLUMN_END_DATE},
                    COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                habit = new Habit();
                habit.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                habit.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                habit.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1);
                habit.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR)));
                habit.setProgress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)));
                habit.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));

                int endDateIndex = cursor.getColumnIndex(COLUMN_END_DATE);
                if (endDateIndex != -1) {
                    habit.setEndDate(cursor.getString(endDateIndex));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting habit by ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return habit;
    }

    public List<Habit> getAllHabits() {
        List<Habit> habitList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HABITS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Habit habit = new Habit();
                habit.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                habit.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                habit.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1);
                habit.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR)));
                habit.setProgress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)));
                habit.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));

                int endDateIndex = cursor.getColumnIndex(COLUMN_END_DATE);
                if (endDateIndex != -1) {
                    habit.setEndDate(cursor.getString(endDateIndex));
                }

                habitList.add(habit);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        Log.d(TAG, "Retrieved " + habitList.size() + " habits");
        return habitList;
    }

    public void updateHabit(Habit habit) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, habit.getName());
            values.put(COLUMN_COMPLETED, habit.isCompleted() ? 1 : 0);
            values.put(COLUMN_COLOR, habit.getColor());
            values.put(COLUMN_PROGRESS, habit.getProgress());
            values.put(COLUMN_DATE, habit.getDate());
            values.put(COLUMN_END_DATE, habit.getEndDate());

            int rowsAffected = db.update(TABLE_HABITS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(habit.getId())});
            Log.d(TAG, "Habit updated: " + habit.getName() + " with ID: " + habit.getId() + ". Rows affected: " + rowsAffected);
        } catch (Exception e) {
            Log.e(TAG, "Error updating habit: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public void deleteHabit(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete all completion history for this habit
        int historyDeleted = db.delete(TABLE_HABIT_HISTORY, COLUMN_HABIT_ID + " = ?",
                new String[]{String.valueOf(id)});

        // Delete the habit itself
        int habitDeleted = db.delete(TABLE_HABITS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});

        db.close();
        Log.d(TAG, "Habit deleted with ID: " + id + ". History records removed: " + historyDeleted + ", Habit removed: " + habitDeleted);
    }

    public boolean updateHabitCompletionForDate(int habitId, String date, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HABIT_ID, habitId);
        values.put(COLUMN_COMPLETION_DATE, date);
        values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);

        // Using insertWithOnConflict to either insert a new record or replace an existing one
        // based on the UNIQUE constraint (habit_id, completion_date)
        long result = db.insertWithOnConflict(TABLE_HABIT_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        Log.d(TAG, "Habit ID " + habitId + " completion updated for date: " + date + ", completed: " + isCompleted + ", result: " + result);

        return result != -1;
    }

    public boolean isHabitCompletedOnDate(int habitId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_IS_COMPLETED + " FROM " + TABLE_HABIT_HISTORY +
                " WHERE " + COLUMN_HABIT_ID + " = ? AND " + COLUMN_COMPLETION_DATE + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(habitId), date});

        boolean isCompleted = false;
        if (cursor.moveToFirst()) {
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1;
        }

        cursor.close();
        db.close();
        return isCompleted;
    }

    public int getCompletedHabitsCountForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_HABIT_HISTORY +
                " WHERE " + COLUMN_COMPLETION_DATE + " = ? AND " + COLUMN_IS_COMPLETED + " = 1";

        Cursor cursor = db.rawQuery(query, new String[]{date});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    public List<String> getHabitCompletionDates(int habitId) {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_COMPLETION_DATE + " FROM " + TABLE_HABIT_HISTORY +
                " WHERE " + COLUMN_HABIT_ID + " = ? AND " + COLUMN_IS_COMPLETED + " = 1";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(habitId)});

        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPLETION_DATE)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dates;
    }

    /**
     * Clear all completion records for a specific date
     * Useful for resetting a single day's data
     */
    public void clearCompletionsForDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_HABIT_HISTORY, COLUMN_COMPLETION_DATE + " = ?", new String[]{date});
        db.close();
        Log.d(TAG, "Cleared " + deletedRows + " completions for date: " + date);
    }

    /**
     * Clear ALL completion history
     * Keeps all habits, only deletes completion records
     * Use this to start fresh from today
     */
    public void clearAllCompletionHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_HABIT_HISTORY, null, null);
        db.close();
        Log.d(TAG, "All completion history cleared. " + deletedRows + " records deleted.");
    }

    /**
     * Clear completions before a specific date
     * Useful for removing old historical data
     */
    public void clearCompletionsBeforeDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_HABIT_HISTORY, COLUMN_COMPLETION_DATE + " < ?", new String[]{date});
        db.close();
        Log.d(TAG, "Cleared " + deletedRows + " completions before date: " + date);
    }

    /**
     * Clear completions after a specific date
     * Useful for removing future/invalid data
     */
    public void clearCompletionsAfterDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_HABIT_HISTORY, COLUMN_COMPLETION_DATE + " > ?", new String[]{date});
        db.close();
        Log.d(TAG, "Cleared " + deletedRows + " completions after date: " + date);
    }

    /**
     * Clear completions for a date range
     * Useful for removing data within a specific period
     */
    public void clearCompletionsInRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_HABIT_HISTORY,
                COLUMN_COMPLETION_DATE + " >= ? AND " + COLUMN_COMPLETION_DATE + " <= ?",
                new String[]{startDate, endDate});
        db.close();
        Log.d(TAG, "Cleared " + deletedRows + " completions between " + startDate + " and " + endDate);
    }

    /**
     * Clear ALL data (habits AND completion history)
     * WARNING: This removes everything!
     * Use with extreme caution
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        int historyDeleted = db.delete(TABLE_HABIT_HISTORY, null, null);
        int habitsDeleted = db.delete(TABLE_HABITS, null, null);
        db.close();
        Log.d(TAG, "All data cleared. " + habitsDeleted + " habits and " + historyDeleted + " completion records deleted.");
    }

    /**
     * Get current date in yyyy-MM-dd format
     */
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    /**
     * Get total number of completion records
     * Useful for statistics
     */
    public int getTotalCompletionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_HABIT_HISTORY + " WHERE " + COLUMN_IS_COMPLETED + " = 1";
        Cursor cursor = db.rawQuery(query, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get completion count for a specific habit
     */
    public int getHabitCompletionCount(int habitId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_HABIT_HISTORY +
                " WHERE " + COLUMN_HABIT_ID + " = ? AND " + COLUMN_IS_COMPLETED + " = 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(habitId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}
