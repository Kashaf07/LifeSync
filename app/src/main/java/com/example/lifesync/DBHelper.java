package com.example.lifesync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Journal.db";
    // Increment version if schema changes (like adding columns)
    private static final int DATABASE_VERSION = 6; // <-- INCREMENTED VERSION

    // Journal Table and Columns
    private static final String TABLE_JOURNAL = "journal";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_IS_FAVORITE = "is_favorite"; // 0 or 1
    private static final String COLUMN_IS_PRIVATE = "is_private";   // 0 or 1
    private static final String COLUMN_IS_DRAFT = "is_draft";     // 0 or 1
    private static final String COLUMN_IMAGE_URI = "image_uri";   // String URI
    private static final String COLUMN_REMINDER_TIME = "reminder_time"; // long (milliseconds)
    private static final String COLUMN_AUDIO_PATH = "audio_path"; // <-- ADDED

    private static final String TAG = "DBHelper"; // For logging

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables...");
        // SQL query to create the journal table with all columns
        String CREATE_JOURNAL_TABLE = "CREATE TABLE " + TABLE_JOURNAL + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_CONTENT + " TEXT," // Stores HTML content
                + COLUMN_DATE + " TEXT,"    // Stores formatted date string
                + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"
                + COLUMN_IS_PRIVATE + " INTEGER DEFAULT 0,"
                + COLUMN_IS_DRAFT + " INTEGER DEFAULT 0,"
                + COLUMN_IMAGE_URI + " TEXT," // Can be null
                + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0," // Store as long (milliseconds)
                + COLUMN_AUDIO_PATH + " TEXT" // <-- ADDED
                + ")";
        db.execSQL(CREATE_JOURNAL_TABLE);
        Log.i(TAG, "Table '" + TABLE_JOURNAL + "' created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Handle database upgrades incrementally
        if (oldVersion < 5) {
            // Add columns introduced in version 5 if upgrading from an older version
            try {
                Log.d(TAG, "Attempting to add column: " + COLUMN_IMAGE_URI);
                // Check if column exists before adding (safer)
                if (!isColumnExists(db, TABLE_JOURNAL, COLUMN_IMAGE_URI)) {
                    db.execSQL("ALTER TABLE " + TABLE_JOURNAL + " ADD COLUMN " + COLUMN_IMAGE_URI + " TEXT;");
                    Log.i(TAG, "Added column: " + COLUMN_IMAGE_URI);
                } else {
                    Log.w(TAG, "Column " + COLUMN_IMAGE_URI + " already exists.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding column " + COLUMN_IMAGE_URI, e);
            }
            try {
                Log.d(TAG, "Attempting to add column: " + COLUMN_REMINDER_TIME);
                if (!isColumnExists(db, TABLE_JOURNAL, COLUMN_REMINDER_TIME)) {
                    db.execSQL("ALTER TABLE " + TABLE_JOURNAL + " ADD COLUMN " + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Added column: " + COLUMN_REMINDER_TIME);
                } else {
                    Log.w(TAG, "Column " + COLUMN_REMINDER_TIME + " already exists.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding column " + COLUMN_REMINDER_TIME, e);
            }
        }

        // --- ADDED THIS BLOCK FOR VERSION 6 ---
        if (oldVersion < 6) {
            try {
                Log.d(TAG, "Attempting to add column: " + COLUMN_AUDIO_PATH);
                if (!isColumnExists(db, TABLE_JOURNAL, COLUMN_AUDIO_PATH)) {
                    db.execSQL("ALTER TABLE " + TABLE_JOURNAL + " ADD COLUMN " + COLUMN_AUDIO_PATH + " TEXT;");
                    Log.i(TAG, "Added column: " + COLUMN_AUDIO_PATH);
                } else {
                    Log.w(TAG, "Column " + COLUMN_AUDIO_PATH + " already exists.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding column " + COLUMN_AUDIO_PATH, e);
            }
        }
        // Add more 'if (oldVersion < X)' blocks for future upgrades
    }

    // Helper to check if a column exists
    private boolean isColumnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            // Querying with LIMIT 1 is efficient
            cursor = db.query(tableName, null, null, null, null, null, null, "1");
            return cursor != null && cursor.getColumnIndex(columnName) != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if column " + columnName + " exists in " + tableName, e);
            return false; // Assume it doesn't exist if error occurs
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    // --- CRUD Operations ---

    /** Adds a new journal entry to the database. Returns the new row ID, or -1 if failed. */
    public long addJournal(Journal journal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = journalToContentValues(journal); // Use helper
        long id = -1;
        try {
            id = db.insert(TABLE_JOURNAL, null, values);
            if (id != -1) {
                Log.d(TAG, "Successfully added journal with ID: " + id + ", ReminderTime: " + journal.getReminderTime());
            } else {
                Log.e(TAG, "Failed to add journal. Title: " + journal.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting journal: " + journal.getTitle(), e);
        } finally {
            db.close();
        }
        return id;
    }

    /** Retrieves a single journal entry by its ID. Returns null if not found. */
    public Journal getJournal(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Journal journal = null;
        try {
            cursor = db.query(TABLE_JOURNAL, null, // null selects all columns
                    COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                    null, null, null, "1"); // LIMIT 1
            if (cursor != null && cursor.moveToFirst()) {
                journal = cursorToJournal(cursor); // Use helper method
                Log.d(TAG, "Successfully retrieved journal ID: " + id);
            } else {
                Log.w(TAG, "Journal with ID: " + id + " not found.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting journal with ID: " + id, e);
        } finally {
            if (cursor != null) cursor.close();
            // db.close(); // Don't close readable db if reused quickly? Best practice is often debated. Closing for safety.
            db.close();
        }
        return journal;
    }

    /** Reads journal entries based on a filter ("all", "drafts", "favorites", "private"). */
    public ArrayList<Journal> readJournals(String filter) {
        ArrayList<Journal> journalArrayList = new ArrayList<>();
        String selection = null;
        String[] selectionArgs = null;

        // Build the WHERE clause based on the filter
        switch (filter) {
            case "drafts":
                selection = COLUMN_IS_DRAFT + " = ?";
                selectionArgs = new String[]{"1"};
                break;
            case "favorites":
                selection = COLUMN_IS_FAVORITE + " = ? AND " + COLUMN_IS_DRAFT + " = ? AND " + COLUMN_IS_PRIVATE + " = ?";
                selectionArgs = new String[]{"1", "0", "0"};
                break;
            case "private":
                selection = COLUMN_IS_PRIVATE + " = ? AND " + COLUMN_IS_DRAFT + " = ?";
                selectionArgs = new String[]{"1", "0"};
                break;
            default: // "all" visible journals
                selection = COLUMN_IS_DRAFT + " = ? AND " + COLUMN_IS_PRIVATE + " = ?";
                selectionArgs = new String[]{"0", "0"};
                break;
        }

        // Order by ID descending (newest first)
        String orderBy = COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            Log.d(TAG, "Reading journals with filter: " + filter + ", Selection: " + selection);
            cursor = db.query(TABLE_JOURNAL, null, selection, selectionArgs, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Journal journal = cursorToJournal(cursor);
                    if (journal != null) { // Add only if parsing was successful
                        journalArrayList.add(journal);
                    }
                } while (cursor.moveToNext());
                Log.d(TAG, "Read " + journalArrayList.size() + " journals for filter: " + filter);
            } else {
                Log.d(TAG, "No journals found for filter: " + filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading journals with filter: " + filter, e);
        } finally {
            if (cursor != null) cursor.close();
            db.close(); // Close db after query
        }
        return journalArrayList;
    }

    /** Retrieves all journals with a reminder time set in the future. */
    public ArrayList<Journal> getAllFutureReminders() {
        ArrayList<Journal> journalArrayList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        // Selection criteria: reminder time is greater than 0 AND greater than current time
        String selection = COLUMN_REMINDER_TIME + " > ?";
        String[] selectionArgs = new String[]{String.valueOf(currentTime)};
        String orderBy = COLUMN_REMINDER_TIME + " ASC"; // Order by soonest first (optional)

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Log.d(TAG, "Querying future reminders (current time: " + currentTime + ")");
        try {
            cursor = db.query(TABLE_JOURNAL, null, selection, selectionArgs, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Journal journal = cursorToJournal(cursor);
                    if (journal != null) {
                        journalArrayList.add(journal);
                    }
                } while (cursor.moveToNext());
            }
            Log.d(TAG, "Found " + journalArrayList.size() + " future reminders.");
        } catch (Exception e) {
            Log.e(TAG, "Error reading future reminders", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close(); // Close db after query
        }
        return journalArrayList;
    }

    /** Updates an existing journal entry. Returns the number of rows affected (should be 1 or 0). */
    public int updateJournal(Journal journal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = journalToContentValues(journal); // Use helper
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_JOURNAL, values,
                    COLUMN_ID + " = ?", new String[]{String.valueOf(journal.getId())});
            if (rowsAffected > 0) {
                Log.d(TAG, "Successfully updated journal ID: " + journal.getId() + ", New ReminderTime: " + journal.getReminderTime());
            } else {
                Log.w(TAG, "Failed to update journal ID: " + journal.getId() + ". Journal not found?");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating journal ID: " + journal.getId(), e);
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    /** Deletes a journal entry by its ID. */
    public void deleteJournal(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(TABLE_JOURNAL, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            if (rowsDeleted > 0) {
                Log.d(TAG, "Successfully deleted journal ID: " + id);
            } else {
                Log.w(TAG, "Failed to delete journal ID: " + id + ". Journal not found?");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting journal ID: " + id, e);
        } finally {
            db.close();
        }

        // Note: Canceling the alarm should ideally happen *before* deleting,
        // using the journal details fetched before deletion, or rely on BootReceiver
        // not finding it next time. For explicit cancellation, call AlarmReceiver.scheduleReminder
        // with context and time 0 *before* calling db.delete.
    }

    // --- Helper Methods ---

    /** Creates ContentValues from a Journal object for insertion/update. */
    private ContentValues journalToContentValues(Journal journal) {
        ContentValues values = new ContentValues();
        // ID is not included here as it's auto-increment or used in WHERE clause
        values.put(COLUMN_TITLE, journal.getTitle());
        values.put(COLUMN_CONTENT, journal.getContent());
        values.put(COLUMN_DATE, journal.getDate());
        values.put(COLUMN_IS_FAVORITE, journal.isFavorite() ? 1 : 0);
        values.put(COLUMN_IS_PRIVATE, journal.isPrivate() ? 1 : 0);
        values.put(COLUMN_IS_DRAFT, journal.isDraft() ? 1 : 0);
        values.put(COLUMN_IMAGE_URI, journal.getImageUri());
        values.put(COLUMN_REMINDER_TIME, journal.getReminderTime());
        values.put(COLUMN_AUDIO_PATH, journal.getAudioPath()); // <-- ADDED
        return values;
    }

    /** Creates a Journal object from a database cursor. Handles potential errors. */
    private Journal cursorToJournal(Cursor cursor) {
        try {
            // Use getColumnIndexOrThrow for robustness against schema changes
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
            boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1;
            boolean isPrivate = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PRIVATE)) == 1;
            boolean isDraft = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DRAFT)) == 1;
            String imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI));
            long reminderTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME));
            String audioPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUDIO_PATH)); // <-- ADDED

            return new Journal(id, title, content, date, isFavorite, isPrivate, isDraft, imageUri, reminderTime, audioPath); // <-- UPDATED

        } catch (IllegalArgumentException e) {
            // This usually means a column name was incorrect or missing
            Log.e(TAG, "Error reading column from cursor. Schema mismatch?", e);
            return null; // Return null if data is corrupt or schema is wrong
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error creating Journal from cursor", e);
            return null;
        }
    }
}