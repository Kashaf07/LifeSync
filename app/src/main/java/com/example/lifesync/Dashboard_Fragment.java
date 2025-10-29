package com.example.lifesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Dashboard_Fragment extends Fragment {

    // Navigation cards
    private CardView cardToDo, cardJournal, cardHabits, cardExpense;

    // Quick action buttons
    private CardView btnAddTask, btnAddExpense, btnAddJournal, btnAddHabit;

    // Top card info TextViews
    private TextView tvTodoCount, tvJournalInfo, tvHabitInfo, tvExpenseInfo;

    // Progress section TextViews
    private TextView tvTodoProgress, tvJournalProgress, tvHabitProgress, tvExpenseProgress;
    private TextView tvTodoPercentage, tvJournalPercentage, tvHabitPercentage, tvExpensePercentage;

    // ProgressBars
    private ProgressBar progressTodo, progressJournal, progressHabit, progressExpense;

    // Header TextViews
    private TextView tvUserName, tvWelcomeBack;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LifeSyncPrefs";
    private static final String KEY_USER_NAME = "user_name";

    public Dashboard_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initializeViews(view);
        setClickListeners();
        checkAndShowNameDialog();
        updateDashboardData();

        return view;
    }

    private void initializeViews(View view) {
        // Initialize header TextViews
        tvUserName = view.findViewById(R.id.tvUserName);
        tvWelcomeBack = view.findViewById(R.id.tvWelcomeBack);

        // Initialize navigation cards
        cardToDo = view.findViewById(R.id.cardToDo);
        cardJournal = view.findViewById(R.id.cardJournal);
        cardHabits = view.findViewById(R.id.cardHabits);
        cardExpense = view.findViewById(R.id.cardExpense);

        // Initialize quick action buttons
        btnAddTask = view.findViewById(R.id.btnAddTask);
        btnAddExpense = view.findViewById(R.id.btnAddExpense);
        btnAddJournal = view.findViewById(R.id.btnAddJournal);
        btnAddHabit = view.findViewById(R.id.btnAddHabit);

        // Initialize top card info TextViews
        tvTodoCount = view.findViewById(R.id.tvTodoCount);
        tvJournalInfo = view.findViewById(R.id.tvJournalInfo);
        tvHabitInfo = view.findViewById(R.id.tvHabitInfo);
        tvExpenseInfo = view.findViewById(R.id.tvExpenseInfo);

        // Initialize progress TextViews
        tvTodoProgress = view.findViewById(R.id.tvTodoProgress);
        tvJournalProgress = view.findViewById(R.id.tvJournalProgress);
        tvHabitProgress = view.findViewById(R.id.tvHabitProgress);
        tvExpenseProgress = view.findViewById(R.id.tvExpenseProgress);

        tvTodoPercentage = view.findViewById(R.id.tvTodoPercentage);
        tvJournalPercentage = view.findViewById(R.id.tvJournalPercentage);
        tvHabitPercentage = view.findViewById(R.id.tvHabitPercentage);
        tvExpensePercentage = view.findViewById(R.id.tvExpensePercentage);

        // Initialize ProgressBars
        progressTodo = view.findViewById(R.id.progressTodo);
        progressJournal = view.findViewById(R.id.progressJournal);
        progressHabit = view.findViewById(R.id.progressHabit);
        progressExpense = view.findViewById(R.id.progressExpense);
    }

    private void checkAndShowNameDialog() {
        String userName = sharedPreferences.getString(KEY_USER_NAME, "");

        if (TextUtils.isEmpty(userName)) {
            // First time user - show name input dialog
            showNameInputDialog();
        } else {
            // Returning user - display name
            updateHeaderWithName(userName);
        }
    }

    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Welcome to LifeSync!");
        builder.setMessage("What should we call you?");
        builder.setCancelable(false);

        // Create input field
        final EditText input = new EditText(requireContext());
        input.setHint("Enter your name");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                // Save name to SharedPreferences
                sharedPreferences.edit().putString(KEY_USER_NAME, name).apply();
                updateHeaderWithName(name);
                dialog.dismiss();
            } else {
                // If empty, show dialog again
                input.setError("Please enter your name");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateHeaderWithName(String name) {
        tvUserName.setText(name);
        tvUserName.setVisibility(View.VISIBLE);
        tvWelcomeBack.setVisibility(View.VISIBLE);
    }

    private void setClickListeners() {
        // Navigation cards
        cardToDo.setOnClickListener(v -> navigateToFragment(new To_Do_Fragment()));
        cardJournal.setOnClickListener(v -> navigateToFragment(new Journal_Fragment()));
        cardHabits.setOnClickListener(v -> navigateToFragment(new Habit_Fragment()));
        cardExpense.setOnClickListener(v -> navigateToFragment(new Expense_Fragment()));

        // Quick actions
        btnAddTask.setOnClickListener(v -> navigateToFragment(new To_Do_Fragment()));
        btnAddExpense.setOnClickListener(v -> navigateToFragment(new Expense_Fragment()));
        btnAddJournal.setOnClickListener(v -> navigateToFragment(new Journal_Fragment()));
        btnAddHabit.setOnClickListener(v -> navigateToFragment(new Habit_Fragment()));
    }

    private void updateDashboardData() {
        // TODO: Replace these with actual data from your database/SharedPreferences

        // Update To-Do data
        int totalTasks = getTotalTasks();
        int completedTasks = getCompletedTasks();
        updateTaskProgress(completedTasks, totalTasks);

        // Update Journal data
        int journalEntries = getJournalEntries();
        int targetDays = 30;
        updateJournalProgress(journalEntries, targetDays);

        // Update Habits data
        int completedHabits = getCompletedHabitsToday();
        int totalHabits = getTotalHabits();
        updateHabitProgress(completedHabits, totalHabits);

        // Update Expense data
        float currentExpense = getCurrentMonthExpense();
        float budget = getMonthlyBudget();
        updateExpenseProgress(currentExpense, budget);
    }

    private void updateTaskProgress(int completed, int total) {
        if (total == 0) {
            tvTodoCount.setText("No tasks");
            tvTodoProgress.setText("Add your first task");
            tvTodoPercentage.setText("0%");
            progressTodo.setProgress(0);
        } else {
            int percentage = (int) ((completed * 100.0) / total);
            tvTodoCount.setText(total + " tasks");
            tvTodoProgress.setText(completed + " of " + total + " completed");
            tvTodoPercentage.setText(percentage + "%");
            progressTodo.setProgress(percentage);
        }
    }

    private void updateJournalProgress(int entries, int target) {
        int percentage = Math.min(100, (int) ((entries * 100.0) / target));
        tvJournalInfo.setText(entries + " entries");
        tvJournalProgress.setText(entries + " of " + target + " days");
        tvJournalPercentage.setText(percentage + "%");
        progressJournal.setProgress(percentage);
    }

    private void updateHabitProgress(int completed, int total) {
        if (total == 0) {
            tvHabitInfo.setText("No habits");
            tvHabitProgress.setText("Add your first habit");
            tvHabitPercentage.setText("0%");
            progressHabit.setProgress(0);
        } else {
            int percentage = (int) ((completed * 100.0) / total);
            tvHabitInfo.setText(getHabitStreak() + "d streak");
            tvHabitProgress.setText(completed + " of " + total + " habits today");
            tvHabitPercentage.setText(percentage + "%");
            progressHabit.setProgress(percentage);
        }
    }

    private void updateExpenseProgress(float spent, float budget) {
        int percentage = Math.min(100, (int) ((spent * 100.0) / budget));
        tvExpenseInfo.setText("₹" + String.format("%.0f", spent));
        tvExpenseProgress.setText("₹" + String.format("%.0f", spent) + " of ₹" + String.format("%.0f", budget));
        tvExpensePercentage.setText(percentage + "%");
        progressExpense.setProgress(percentage);
    }

    // ============= DUMMY DATA METHODS =============

    private int getTotalTasks() { return 5; }
    private int getCompletedTasks() { return 2; }
    private int getJournalEntries() { return 15; }
    private int getCompletedHabitsToday() { return 3; }
    private int getTotalHabits() { return 4; }
    private int getHabitStreak() { return 7; }
    private float getCurrentMonthExpense() { return 1250f; }
    private float getMonthlyBudget() { return 5000f; }

    // ============= NAVIGATION HELPER =============
    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboardData();
    }
}