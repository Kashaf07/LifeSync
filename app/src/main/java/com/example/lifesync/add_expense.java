package com.example.lifesync;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class add_expense extends Fragment {
    private DatabaseHelper dbHelper;
    private EditText etTitle, etAmount, etDate, etDescription;
    private Button btnAdd, btnBack;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String CURRENT_ACCOUNT_KEY = "current_account";
    private Calendar calendar;

    public add_expense() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        dbHelper = new DatabaseHelper(getContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        calendar = Calendar.getInstance();

        // IMPORTANT: Set current account before adding transaction
        String currentAccount = sharedPreferences.getString(CURRENT_ACCOUNT_KEY, null);
        if (currentAccount != null && !currentAccount.isEmpty()) {
            dbHelper.setCurrentAccount(currentAccount);
            android.util.Log.d("AddExpense", "Current Account: " + currentAccount);
        } else {
            android.util.Log.d("AddExpense", "No account selected!");
            Toast.makeText(getContext(), "Please create an account first", Toast.LENGTH_SHORT).show();
        }

        // Initialize views
        etTitle = view.findViewById(R.id.etTitle);
        etAmount = view.findViewById(R.id.etAmount);
        etDate = view.findViewById(R.id.etDate);
        etDescription = view.findViewById(R.id.etDescription);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnBack = view.findViewById(R.id.btnBack);

        // Set back button text color programmatically
        btnBack.setTextColor(Color.parseColor("#2E4E3F"));

        // Set current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);

        // Date picker on click
        etDate.setOnClickListener(v -> showDatePicker());

        btnAdd.setOnClickListener(v -> addExpense());

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    String formattedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    etDate.setText(formattedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void addExpense() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString();
        String date = etDate.getText().toString();
        String description = etDescription.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            etTitle.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            Toast.makeText(getContext(), "Please select date", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // Use title as category
        long result = dbHelper.addTransaction("expense", amount, title, date, description);

        android.util.Log.d("AddExpense", "Transaction added with ID: " + result);

        if (result != -1) {
            Toast.makeText(getContext(), "Expense added successfully", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        } else {
            Toast.makeText(getContext(), "Error adding expense", Toast.LENGTH_SHORT).show();
        }
    }
}
