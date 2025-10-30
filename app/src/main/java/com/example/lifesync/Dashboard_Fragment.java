package com.example.lifesync;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.example.lifesync.database.ToDoDatabaseHelper;
import com.example.lifesync.model.ToDoModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Dashboard_Fragment extends Fragment {

    // Database Helpers
    private ToDoDatabaseHelper toDoDbHelper;
    private DBHelper journalDbHelper;
    private HabitDatabaseHelper habitDbHelper;
    private DatabaseHelper expenseDbHelper;

    // Navigation cards
    private CardView cardToDo, cardJournal, cardHabits, cardExpense;
    // New Quick Action cards
    private CardView cardQuickAddTodo, cardQuickAddHabit, cardQuickAddExpense, cardQuickAddJournal;

    // Top card info TextViews
    private TextView tvTodoCount, tvJournalInfo, tvHabitInfo, tvExpenseInfo;

    // Progress section TextViews
    private TextView tvTodoProgress, tvJournalProgress, tvHabitProgress, tvExpenseProgress;
    private TextView tvTodoPercentage, tvJournalPercentage, tvHabitPercentage, tvExpensePercentage;

    // ProgressBars
    private ProgressBar progressTodo, progressJournal, progressHabit, progressExpense;

    // Header TextViews
    private TextView tvWelcomeBack;

    // ANIMATION TARGETS
    private RelativeLayout headerContainer;
    private CardView mainCardContainer;

    // 💡 ANIMATION CONTROL FLAG: Static to run only once per app launch (process lifetime)
    private static boolean hasAnimationRun = false;

    // Expense Prefs
    private static final String EXPENSE_PREFS_NAME = "ExpenseManagerPrefs";
    private static final String CURRENT_ACCOUNT_KEY = "current_account";

    public Dashboard_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize DB Helpers
        toDoDbHelper = new ToDoDatabaseHelper(requireContext());
        journalDbHelper = new DBHelper(requireContext());
        habitDbHelper = new HabitDatabaseHelper(requireContext());
        expenseDbHelper = new DatabaseHelper(requireContext());

        // Ensure Expense DB knows the current account
        SharedPreferences expensePrefs = requireActivity().getSharedPreferences(EXPENSE_PREFS_NAME, Context.MODE_PRIVATE);
        String currentAccount = expensePrefs.getString(CURRENT_ACCOUNT_KEY, "Personal");
        expenseDbHelper.setCurrentAccount(currentAccount);


        initializeViews(view);
        setClickListeners();

        // --- ANIMATION CONTROL LOGIC ---
        if (!hasAnimationRun) {
            // EXECUTE ANIMATION ONLY ON FIRST LOAD
            animateDashboardEntrance();
            animateQuickActionsBlink();
            hasAnimationRun = true; // Set static flag to true after running
        } else {
            // ENSURE VIEWS ARE VISIBLE AND IN FINAL POSITION WHEN SKIPPING ANIMATION
            if (headerContainer != null) {
                headerContainer.setAlpha(1f);
                headerContainer.setTranslationY(0f);
            }
            if (mainCardContainer != null) {
                mainCardContainer.setAlpha(1f);
                mainCardContainer.setTranslationY(0f);
            }
            // Reset all Quick Action cards to their final non-animated state
            List<CardView> cards = Arrays.asList(cardQuickAddTodo, cardQuickAddHabit, cardQuickAddExpense, cardQuickAddJournal);
            for (CardView card : cards) {
                if (card != null) {
                    card.setScaleX(1.0f);
                    card.setScaleY(1.0f);
                    card.setAlpha(1.0f);
                }
            }
        }

        return view;
    }

    private void initializeViews(View view) {
        // Initialize header TextViews
        tvWelcomeBack = view.findViewById(R.id.tvWelcomeBack);

        // Initialize animation targets
        headerContainer = view.findViewById(R.id.headerContainer);
        mainCardContainer = view.findViewById(R.id.mainCardContainer);

        // Initialize main navigation cards
        cardToDo = view.findViewById(R.id.cardToDo);
        cardJournal = view.findViewById(R.id.cardJournal);
        cardHabits = view.findViewById(R.id.cardHabits);
        cardExpense = view.findViewById(R.id.cardExpense);

        // Initialize quick action cards (NEW)
        cardQuickAddTodo = view.findViewById(R.id.cardQuickAddTodo);
        cardQuickAddHabit = view.findViewById(R.id.cardQuickAddHabit);
        cardQuickAddExpense = view.findViewById(R.id.cardQuickAddExpense);
        cardQuickAddJournal = view.findViewById(R.id.cardQuickAddJournal);


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

    /**
     * Executes the custom animation for the dashboard header and main card.
     */
    private void animateDashboardEntrance() {
        if (headerContainer != null) {
            // Animation 1: Header slides down/fades in
            headerContainer.setAlpha(0f);
            headerContainer.setTranslationY(-100f); // Start 100dp above
            headerContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }

        if (mainCardContainer != null) {
            // Animation 2: Main Card slides up/fades in with a slight delay
            mainCardContainer.setAlpha(0f);
            mainCardContainer.setTranslationY(100f); // Start 100dp below its final position

            mainCardContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(1000)
                    .setStartDelay(100)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }
    }

    /**
     * Executes a staggered pop-in animation on the four Quick Action cards.
     */
    private void animateQuickActionsBlink() {
        List<CardView> cards = Arrays.asList(cardQuickAddTodo, cardQuickAddHabit, cardQuickAddExpense, cardQuickAddJournal);
        long baseDelay = 800; // Start quick action animation after the main header/card finish initiating

        for (int i = 0; i < cards.size(); i++) {
            final CardView card = cards.get(i);
            long startDelay = baseDelay + (i * 100);

            // Set initial state (invisible/small)
            card.setScaleX(0f);
            card.setScaleY(0f);
            card.setAlpha(0f);

            // Animation sequence: Fade/Scale In -> Pop
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            fadeIn.setDuration(300);

            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(card, "scaleX", 0f, 1.1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(card, "scaleY", 0f, 1.1f);
            scaleUpX.setDuration(300);
            scaleUpY.setDuration(300);

            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(card, "scaleX", 1.1f, 1.0f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(card, "scaleY", 1.1f, 1.0f);
            scaleDownX.setDuration(200);
            scaleDownY.setDuration(200);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(scaleUpX).with(scaleUpY).with(fadeIn);
            animatorSet.play(scaleDownX).with(scaleDownY).after(scaleUpX);

            animatorSet.setInterpolator(new DecelerateInterpolator());
            animatorSet.setStartDelay(startDelay);
            animatorSet.start();
        }
    }


    private void setClickListeners() {
        // Main Navigation cards (keep as is)
        cardToDo.setOnClickListener(v -> navigateToFragment(new To_Do_Fragment()));
        cardJournal.setOnClickListener(v -> navigateToFragment(new Journal_Fragment()));
        cardHabits.setOnClickListener(v -> navigateToFragment(new Habit_Fragment()));
        cardExpense.setOnClickListener(v -> navigateToFragment(new Expense_Fragment()));

        // Quick Actions (Direct access to creation screen/dialog)

        // 1. Add To Do (Directly launch dialog)
        cardQuickAddTodo.setOnClickListener(v -> {
            To_Do_Fragment toDoFragment = new To_Do_Fragment();
            Bundle args = new Bundle();
            args.putBoolean(To_Do_Fragment.ARG_SHOW_ADD_DIALOG, true);
            toDoFragment.setArguments(args);
            navigateToFragment(toDoFragment);
        });

        // 2. Add Habit (Directly launch add screen)
        cardQuickAddHabit.setOnClickListener(v -> navigateToFragment(new fragment_add_habit()));

        // 3. Add Expense (Directly launch add screen)
        cardQuickAddExpense.setOnClickListener(v -> navigateToFragment(new add_expense()));

        // 4. New Journal (Directly launch add screen)
        cardQuickAddJournal.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("JOURNAL_ID", -1); // -1 indicates a new entry
            FragmentAddJournal addFragment = new FragmentAddJournal();
            addFragment.setArguments(args);
            navigateToFragment(addFragment);
        });
    }

    private void updateDashboardData() {
        // 1. Update To-Do data
        updateToDoStats();

        // 2. Update Journal data
        updateJournalStats();

        // 3. Update Habits data
        updateHabitStats();

        // 4. Update Expense data
        updateExpenseStats();
    }

    // ================== DATA FETCHING ==================

    private List<ToDoModel> getToDoTasks() {
        // Note: Logic copied from To_Do_Fragment's loadTasks method to get all tasks.
        List<ToDoModel> taskList = new ArrayList<>();
        // In this method, we fetch all tasks, regardless of their status (done/not done).

        try {
            android.database.Cursor cursor = toDoDbHelper.getReadableDatabase().query("tasks",
                    null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"));
                    int isDone = cursor.getInt(cursor.getColumnIndexOrThrow("isDone"));
                    taskList.add(new ToDoModel(id, title, desc, date, priority, isDone == 1));
                } while (cursor.moveToNext());
            }

            cursor.close();
            return taskList;
        } catch (Exception e) {
            Log.e("Dashboard", "Error fetching ToDo tasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int getJournalingStreak(ArrayList<Journal> allJournals) {
        // Simplified logic: Count unique days in the last 30 days that have an entry.
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Calendar thirtyDaysAgo = Calendar.getInstance();
        thirtyDaysAgo.add(Calendar.DAY_OF_YEAR, -30);

        List<String> uniqueDates = new ArrayList<>();
        for(Journal j : allJournals) {
            try {
                Date entryDate = sdf.parse(j.getDate());
                if(entryDate.after(thirtyDaysAgo.getTime())) {
                    String formattedDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(entryDate);
                    if (!uniqueDates.contains(formattedDay)) {
                        uniqueDates.add(formattedDay);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        return uniqueDates.size();
    }

    private List<Habit> getActiveHabitsToday(List<Habit> allHabits) {
        // Logic copied from Habit_Fragment's isHabitActiveOnDate logic
        List<Habit> activeHabits = new ArrayList<>();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Habit habit : allHabits) {
            // Reusing the general check method here for consistency
            if (isHabitActiveOnDate(habit, todayDate)) {
                activeHabits.add(habit);
            }
        }
        return activeHabits;
    }

    private int calculateBestStreak(List<Habit> allHabits) {
        // Note: This is an expensive operation and should ideally be cached or done in a simpler way.
        // For demonstration, we'll calculate the best perfect day streak.
        if (allHabits.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        int bestStreak = 0;
        int currentStreak = 0;

        // Start from today and go back 180 days (or until the first habit start date)
        for(int i = 0; i < 180; i++) {
            String dateStr = sdf.format(calendar.getTime());

            int activeHabitsOnDay = 0;
            int completedHabitsOnDay = 0;

            for (Habit habit : allHabits) {
                // Fix: Check if the habit's completion history includes this date
                if (habitDbHelper.getHabitCompletionDates(habit.getId()).contains(dateStr)) {
                    completedHabitsOnDay++;
                }

                // Fix: Use the new helper method
                if (isHabitActiveOnDate(habit, dateStr)) {
                    activeHabitsOnDay++;
                }
            }

            if (activeHabitsOnDay > 0 && completedHabitsOnDay == activeHabitsOnDay) {
                currentStreak++;
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak;
                }
            } else {
                currentStreak = 0;
            }

            // Move back a day
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return bestStreak;
    }

    private boolean isHabitActiveOnDate(Habit habit, String checkDate) {
        String startDate = habit.getDate();
        String endDate = habit.getEndDate();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (startDate == null || startDate.isEmpty()) {
            startDate = todayDate;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date check = sdf.parse(checkDate);

            if (start == null || check == null) {
                return false;
            }

            if (check.before(start)) {
                return false;
            }

            if (endDate != null && !endDate.isEmpty()) {
                Date end = sdf.parse(endDate);
                if (end != null && check.after(end)) {
                    return false;
                }
            }

            return true;
        } catch (ParseException e) {
            Log.e("Dashboard", "Error parsing dates in isHabitActiveOnDate: " + e.getMessage());
            return false;
        }
    }

    // ================== UI UPDATE LOGIC ==================

    private void updateToDoStats() {
        List<ToDoModel> allTasks = getToDoTasks();
        int totalTasks = allTasks.size();
        int completedTasks = 0;
        for (ToDoModel task : allTasks) {
            if (task.isDone()) completedTasks++;
        }

        if (totalTasks == 0) {
            tvTodoCount.setText("No tasks");
            tvTodoProgress.setText("Add your first task");
            tvTodoPercentage.setText("0%");
            progressTodo.setProgress(0);
        } else {
            int percentage = (int) ((completedTasks * 100.0) / totalTasks);
            tvTodoCount.setText(totalTasks + " tasks");
            tvTodoProgress.setText(completedTasks + " of " + totalTasks + " completed");
            tvTodoPercentage.setText(percentage + "%");
            progressTodo.setProgress(percentage);
        }
    }

    private void updateJournalStats() {
        ArrayList<Journal> journals = journalDbHelper.readJournals("all");
        int journalEntries = journals.size();

        int daysWithEntries = getJournalingStreak(journals);
        int targetDays = 30;
        int percentage = Math.min(100, (int) ((daysWithEntries * 100.0) / targetDays));

        tvJournalInfo.setText(journalEntries + " entries");
        tvJournalProgress.setText(daysWithEntries + " of " + targetDays + " days");
        tvJournalPercentage.setText(percentage + "%");
        progressJournal.setProgress(percentage);
    }

    private void updateHabitStats() {
        List<Habit> allHabits = habitDbHelper.getAllHabits();
        List<Habit> activeHabitsToday = getActiveHabitsToday(allHabits);

        int totalHabitsToday = activeHabitsToday.size();
        int completedHabitsToday = 0;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Habit habit : activeHabitsToday) {
            if (habitDbHelper.isHabitCompletedOnDate(habit.getId(), todayDate)) {
                completedHabitsToday++;
            }
        }

        int bestStreak = calculateBestStreak(allHabits);

        if (totalHabitsToday == 0) {
            tvHabitInfo.setText("No habits");
            tvHabitProgress.setText("Add your first habit");
            tvHabitPercentage.setText("0%");
            progressHabit.setProgress(0);
        } else {
            int percentage = (int) ((completedHabitsToday * 100.0) / totalHabitsToday);
            tvHabitInfo.setText(bestStreak + "d streak");
            tvHabitProgress.setText(completedHabitsToday + " of " + totalHabitsToday + " habits today");
            tvHabitPercentage.setText(percentage + "%");
            progressHabit.setProgress(percentage);
        }
    }

    private void updateExpenseStats() {
        Map<String, Double> stats = expenseDbHelper.getStats();
        double currentMonthExpense = stats.getOrDefault("expenses", 0.0);
        float monthlyBudget = 5000f;

        int percentage = Math.min(100, (int) ((currentMonthExpense * 100.0) / monthlyBudget));

        tvExpenseInfo.setText("₹" + String.format("%.0f", currentMonthExpense));
        tvExpenseProgress.setText("₹" + String.format("%.0f", currentMonthExpense) + " of ₹" + String.format("%.0f", monthlyBudget));
        tvExpensePercentage.setText(percentage + "%");
        progressExpense.setProgress(percentage);
    }

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
        // Load fresh data every time the dashboard is resumed
        updateToDoStats();
        updateJournalStats();
        updateHabitStats();
        updateExpenseStats();
    }
}