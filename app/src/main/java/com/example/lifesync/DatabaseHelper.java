package com.example.lifesync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ExpenseTracker.db";
    private static final int DATABASE_VERSION = 2; // Updated version for account support

    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COL_ID = "id";
    private static final String COL_TYPE = "type";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DATE = "date";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_ACCOUNT = "account"; // New column for account

    private String currentAccount = null; // Current selected account

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TYPE + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_ACCOUNT + " TEXT)"; // Added account column
        db.execSQL(createTable);

        // Don't insert sample data - start with empty database
        // insertSampleData(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        insertTransaction(db, "income", 15000, "Salary", "2025-10-01", "Monthly salary", "Personal");
        insertTransaction(db, "income", 8160, "Freelance", "2025-10-10", "Web project", "Personal");
        insertTransaction(db, "expense", 5000, "Rent", "2025-10-05", "House rent", "Personal");
        insertTransaction(db, "expense", 2500, "Food", "2025-10-12", "Groceries", "Personal");
        insertTransaction(db, "expense", 1470, "Transport", "2025-10-15", "Fuel and travel", "Personal");
    }

    private void insertTransaction(SQLiteDatabase db, String type, double amount,
                                   String category, String date, String description, String account) {
        ContentValues values = new ContentValues();
        values.put(COL_TYPE, type);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_ACCOUNT, account);
        db.insert(TABLE_TRANSACTIONS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add account column to existing table
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COL_ACCOUNT + " TEXT DEFAULT 'Personal'");
        }
    }

    // Set current account
    public void setCurrentAccount(String account) {
        this.currentAccount = account;
    }

    // Get current account
    public String getCurrentAccount() {
        return this.currentAccount;
    }

    public long addTransaction(String type, double amount, String category,
                               String date, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TYPE, type);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_ACCOUNT, currentAccount != null ? currentAccount : "Personal");
        return db.insert(TABLE_TRANSACTIONS, null, values);
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor;
        if (currentAccount != null && !currentAccount.isEmpty()) {
            // Filter by current account
            cursor = db.query(TABLE_TRANSACTIONS, null, COL_ACCOUNT + "=?",
                    new String[]{currentAccount}, null, null, COL_DATE + " DESC");
        } else {
            // Get all transactions if no account selected
            cursor = db.query(TABLE_TRANSACTIONS, null, null, null,
                    null, null, COL_DATE + " DESC");
        }

        if (cursor.moveToFirst()) {
            do {
                Transaction t = new Transaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION))
                );
                transactions.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Map<String, Double> getStats() {
        Map<String, Double> stats = new HashMap<>();
        double income = 0, expenses = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        String query;
        String[] args;

        if (currentAccount != null && !currentAccount.isEmpty()) {
            query = "SELECT type, SUM(amount) FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_ACCOUNT + "=? GROUP BY type";
            args = new String[]{currentAccount};
        } else {
            query = "SELECT type, SUM(amount) FROM " + TABLE_TRANSACTIONS + " GROUP BY type";
            args = null;
        }

        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(0);
                double amount = cursor.getDouble(1);
                if (type.equals("income")) income = amount;
                else if (type.equals("expense")) expenses = amount;
            } while (cursor.moveToNext());
        }
        cursor.close();

        stats.put("income", income);
        stats.put("expenses", expenses);
        stats.put("balance", income - expenses);
        return stats;
    }

    public Map<String, Double> getCategoryStats(String type) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query;
        String[] args;

        if (currentAccount != null && !currentAccount.isEmpty()) {
            query = "SELECT category, SUM(amount) FROM " + TABLE_TRANSACTIONS +
                    " WHERE type=? AND " + COL_ACCOUNT + "=? GROUP BY category";
            args = new String[]{type, currentAccount};
        } else {
            query = "SELECT category, SUM(amount) FROM " + TABLE_TRANSACTIONS +
                    " WHERE type=? GROUP BY category";
            args = new String[]{type};
        }

        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return stats;
    }

    public Map<String, Double> getDateWiseStats(String type) {
        Map<String, Double> dateStats = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query;
        String[] args;

        if (currentAccount != null && !currentAccount.isEmpty()) {
            query = "SELECT " + COL_DATE + ", SUM(" + COL_AMOUNT + ") as total " +
                    "FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TYPE + "=? AND " + COL_ACCOUNT + "=? " +
                    "GROUP BY " + COL_DATE +
                    " ORDER BY " + COL_DATE + " ASC";
            args = new String[]{type, currentAccount};
        } else {
            query = "SELECT " + COL_DATE + ", SUM(" + COL_AMOUNT + ") as total " +
                    "FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TYPE + "=? " +
                    "GROUP BY " + COL_DATE +
                    " ORDER BY " + COL_DATE + " ASC";
            args = new String[]{type};
        }

        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                dateStats.put(date, total);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return dateStats;
    }
}