package com.example.lifesync;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentCalendar extends Fragment {

    private static final String TAG = "FragmentCalendar";

    private ImageView ivBack;
    private TextView tvAll, tvHabitList;
    private TextView tvMonth, tvYear;
    private ImageView ivPrevMonth, ivNextMonth;
    private GridLayout calendarGrid;

    private CardView cardSelectedHabit;
    private TextView tvSelectedHabitName;

    // Stats Views
    private TextView tvAverageRate;
    private TextView tvAverageLabel;
    private TextView tvBestStreak;
    private TextView tvPerfectDays;
    private TextView tvHabitsDone;
    private TextView tvDailyAverage;
    private CircularProgressView circleProgressView;

    // Toggle Buttons
    private TextView tvDaily;
    private TextView tvMonthly;
    private boolean isDailyView = true;

    // Data
    private HabitDatabaseHelper databaseHelper;
    private Calendar currentCalendar;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private List<Habit> allHabits;
    private int selectedHabitId = -1;
    private String selectedDate = "";
    private String todayDate = "";

    // Colors (Copied from Activity/Design)
    private static final int COLOR_TEXT_DEFAULT = Color.parseColor("#2D3748");
    private static final int COLOR_TEXT_DIMMED = Color.parseColor("#A0AEC0");
    private static final int COLOR_TEXT_GREY = Color.parseColor("#718096");
    private static final int COLOR_TODAY_ACCENT = Color.parseColor("#EDACAC");
    private static final int COLOR_SELECTED_ACCENT = Color.parseColor("#F5C6C6");
    private static final int COLOR_COMPLETION_BORDER = Color.parseColor("#FFD700");
    private static final int COLOR_BUTTON_SELECTED_BG = Color.parseColor("#EDACAC");
    private static final int COLOR_BUTTON_UNSELECTED_BG = Color.parseColor("#E2E8F0");


    public FragmentCalendar() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new HabitDatabaseHelper(requireContext());
        currentCalendar = Calendar.getInstance();
        todayDate = dateFormat.format(currentCalendar.getTime());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initializeViews(view);
        setupListeners();
        loadAllHabits();

        // Initial state load
        if (selectedDate.isEmpty()) {
            selectedDate = todayDate;
        }

        displayCalendar();
        calculateAndDisplayStatistics();
        updateSelectedHabitDisplay();
        updateButtonStates();
        updateToggleButtonStates();

        return view;
    }

    private void initializeViews(View view) {
        ivBack = view.findViewById(R.id.ivBack);
        tvAll = view.findViewById(R.id.tvAll);
        tvHabitList = view.findViewById(R.id.tvHabitList);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvYear = view.findViewById(R.id.tvYear);
        ivPrevMonth = view.findViewById(R.id.ivPrevMonth);
        ivNextMonth = view.findViewById(R.id.ivNextMonth);
        calendarGrid = view.findViewById(R.id.calendarGrid);

        cardSelectedHabit = view.findViewById(R.id.cardSelectedHabit);
        tvSelectedHabitName = view.findViewById(R.id.tvSelectedHabitName);

        tvAverageRate = view.findViewById(R.id.tvAverageRate);
        tvAverageLabel = view.findViewById(R.id.tvAverageLabel);
        tvBestStreak = view.findViewById(R.id.tvBestStreak);
        tvPerfectDays = view.findViewById(R.id.tvPerfectDays);
        tvHabitsDone = view.findViewById(R.id.tvHabitsDone);
        tvDailyAverage = view.findViewById(R.id.tvDailyAverage);
        circleProgressView = view.findViewById(R.id.circleProgressView); // Assumes CircularProgressView is correctly imported in XML/Manifest

        tvDaily = view.findViewById(R.id.tvDaily);
        tvMonthly = view.findViewById(R.id.tvMonthly);
    }

    private void setupListeners() {
        // Navigation Back
        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Toggle Buttons - All / Habit List
        tvAll.setOnClickListener(v -> {
            selectedHabitId = -1;
            updateButtonStates();
            displayCalendar();
            calculateAndDisplayStatistics();
            updateSelectedHabitDisplay();
        });

        tvHabitList.setOnClickListener(v -> showHabitListDialog());

        // Month Navigation
        ivPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            displayCalendar();
            calculateAndDisplayStatistics();
        });

        ivNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            displayCalendar();
            calculateAndDisplayStatistics();
        });

        // Daily / Monthly Toggle
        tvDaily.setOnClickListener(v -> {
            if (!isDailyView) {
                isDailyView = true;
                updateToggleButtonStates();
                calculateAndDisplayStatistics();
            }
        });

        tvMonthly.setOnClickListener(v -> {
            if (isDailyView) {
                isDailyView = false;
                updateToggleButtonStates();
                calculateAndDisplayStatistics();
            }
        });
    }

    private void loadAllHabits() {
        allHabits = databaseHelper.getAllHabits();
    }

    private void updateButtonStates() {
        if (selectedHabitId == -1) {
            tvAll.setBackgroundColor(COLOR_BUTTON_SELECTED_BG);
            tvAll.setTextColor(Color.WHITE);
            tvHabitList.setBackgroundColor(COLOR_BUTTON_UNSELECTED_BG);
            tvHabitList.setTextColor(COLOR_TEXT_DEFAULT);
        } else {
            tvAll.setBackgroundColor(COLOR_BUTTON_UNSELECTED_BG);
            tvAll.setTextColor(COLOR_TEXT_DEFAULT);
            tvHabitList.setBackgroundColor(COLOR_BUTTON_SELECTED_BG);
            tvHabitList.setTextColor(Color.WHITE);
        }
    }

    private void updateSelectedHabitDisplay() {
        if (selectedHabitId == -1) {
            cardSelectedHabit.setVisibility(View.GONE);
        } else {
            Habit selectedHabit = databaseHelper.getHabitById(selectedHabitId);
            if (selectedHabit != null) {
                cardSelectedHabit.setVisibility(View.VISIBLE);
                tvSelectedHabitName.setText(selectedHabit.getName());
            } else {
                cardSelectedHabit.setVisibility(View.GONE);
            }
        }
    }

    private void showHabitListDialog() {
        try {
            CalanderHabitListDialog dialog = CalanderHabitListDialog.newInstance(allHabits, selectedHabitId);

            // Set the listener before showing the dialog
            dialog.setOnHabitSelectedListener(habitId -> {
                selectedHabitId = habitId;
                updateButtonStates();
                displayCalendar();
                calculateAndDisplayStatistics();
                updateSelectedHabitDisplay();
            });

            dialog.show(getParentFragmentManager(), "HabitListDialog");

        } catch (Exception e) {
            Log.e(TAG, "Error showing habit list dialog", e);
            Toast.makeText(requireContext(), "Error loading habit list.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateToggleButtonStates() {
        if (isDailyView) {
            // Daily selected
            tvDaily.setBackgroundResource(R.drawable.toggle_selected_bg);
            tvDaily.setTextColor(Color.WHITE);

            tvMonthly.setBackgroundColor(Color.TRANSPARENT);
            tvMonthly.setTextColor(COLOR_TEXT_DEFAULT);

            tvAverageLabel.setText("Daily Average");
        } else {
            // Monthly selected
            tvMonthly.setBackgroundResource(R.drawable.toggle_selected_bg);
            tvMonthly.setTextColor(Color.WHITE);

            tvDaily.setBackgroundColor(Color.TRANSPARENT);
            tvDaily.setTextColor(COLOR_TEXT_DEFAULT);

            tvAverageLabel.setText("Monthly Rate");
        }
    }

    private void calculateAndDisplayStatistics() {
        if (allHabits == null || allHabits.isEmpty()) {
            updateUI(0, 0, 0, 0, 0);
            return;
        }

        int displayedMonth = currentCalendar.get(Calendar.MONTH);
        int displayedYear = currentCalendar.get(Calendar.YEAR);

        Calendar tempCal = Calendar.getInstance();
        tempCal.set(displayedYear, displayedMonth, 1);

        Calendar today = Calendar.getInstance();
        int totalDaysToCalculate;

        if (displayedMonth == today.get(Calendar.MONTH) && displayedYear == today.get(Calendar.YEAR)) {
            totalDaysToCalculate = today.get(Calendar.DAY_OF_MONTH);
        } else if (tempCal.after(today)) {
            updateUI(0, 0, 0, 0, 0);
            return;
        } else {
            totalDaysToCalculate = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        List<Habit> habitsToCount = new ArrayList<>();
        if (selectedHabitId == -1) {
            habitsToCount.addAll(allHabits);
        } else {
            Habit selectedHabit = databaseHelper.getHabitById(selectedHabitId);
            if (selectedHabit != null) {
                habitsToCount.add(selectedHabit);
            }
        }

        if (habitsToCount.isEmpty()) {
            updateUI(0, 0, 0, 0, 0);
            return;
        }

        int totalPossibleCompletions = 0;
        int totalCompletions = 0;
        int perfectDays = 0;
        int bestStreak = 0;
        int currentStreak = 0;

        tempCal.set(displayedYear, displayedMonth, 1);

        for (int day = 1; day <= totalDaysToCalculate; day++) {
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = dateFormat.format(tempCal.getTime());

            int activeHabitsOnThisDay = 0;
            int completedOnThisDay = 0;

            for (Habit habit : habitsToCount) {
                if (isHabitActiveOnDate(habit, dateStr, todayDate)) {
                    activeHabitsOnThisDay++;
                    if (databaseHelper.isHabitCompletedOnDate(habit.getId(), dateStr)) {
                        totalCompletions++;
                        completedOnThisDay++;
                    }
                }
            }

            totalPossibleCompletions += activeHabitsOnThisDay;

            if (activeHabitsOnThisDay > 0 && completedOnThisDay == activeHabitsOnThisDay) {
                perfectDays++;
                currentStreak++;
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak;
                }
            } else {
                currentStreak = 0;
            }
        }

        double displayValue = 0;
        double dailyAverage = 0;

        if (totalDaysToCalculate > 0) {
            dailyAverage = (double) totalCompletions / totalDaysToCalculate;
        }

        if (isDailyView) {
            // Daily view: Average habits completed per day (decimal)
            displayValue = dailyAverage;
        } else {
            // Monthly view: Percentage of days with completions
            int daysWithCompletions = 0;
            Calendar calcCal = Calendar.getInstance();
            calcCal.set(displayedYear, displayedMonth, 1);

            for (int day = 1; day <= totalDaysToCalculate; day++) {
                calcCal.set(Calendar.DAY_OF_MONTH, day);
                String dateStr = dateFormat.format(calcCal.getTime());

                boolean hadCompletion = false;
                for (Habit habit : habitsToCount) {
                    if (isHabitActiveOnDate(habit, dateStr, todayDate) &&
                            databaseHelper.isHabitCompletedOnDate(habit.getId(), dateStr)) {
                        hadCompletion = true;
                        break;
                    }
                }
                if (hadCompletion) daysWithCompletions++;
            }

            if (totalDaysToCalculate > 0) {
                displayValue = ((double) daysWithCompletions / totalDaysToCalculate) * 100.0;
            }
        }

        updateUI(displayValue, bestStreak, perfectDays, totalCompletions, dailyAverage);
    }

    private boolean isHabitActiveOnDate(Habit habit, String checkDate, String todayDate) {
        String startDate = habit.getDate();
        String endDate = habit.getEndDate();

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
            Log.e(TAG, "Error parsing dates: " + e.getMessage());
            return false;
        }
    }

    private void updateUI(double displayValue, int bestStreak, int perfectDays,
                          int habitsDone, double dailyAverage) {

        // Default Progress View expects a percentage (0-100)
        float progressPercent = 0;

        if (isDailyView) {
            // Daily view: Show average (e.g., 2.5) but scale to 10 for percentage display on circle
            double scaledValue = dailyAverage * 10.0;
            tvAverageRate.setText(String.format(Locale.getDefault(), "%.2f", dailyAverage));
            progressPercent = (float) Math.min(100, scaledValue * 10); // Scale daily avg (0-10) to 0-100%
        } else {
            // Monthly view: Show actual percentage (e.g., 46.5%)
            tvAverageRate.setText(String.format(Locale.getDefault(), "%.2f%%", displayValue));
            progressPercent = (float) Math.max(0, Math.min(100, displayValue)); // Use displayValue as percentage
        }

        if (circleProgressView != null) {
            circleProgressView.setProgress(progressPercent);
        }

        tvBestStreak.setText(String.valueOf(bestStreak));
        tvPerfectDays.setText(String.valueOf(perfectDays));
        tvHabitsDone.setText(String.valueOf(habitsDone));
        tvDailyAverage.setText(String.format(Locale.getDefault(), "%.2f", dailyAverage));
    }

    private void displayCalendar() {
        tvMonth.setText(monthFormat.format(currentCalendar.getTime()));
        tvYear.setText(yearFormat.format(currentCalendar.getTime()));
        calendarGrid.removeAllViews();

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            TextView tvDay = new TextView(requireContext());
            tvDay.setText(day);
            tvDay.setTextSize(14);
            tvDay.setTextColor(COLOR_TEXT_DIMMED);
            tvDay.setGravity(android.view.Gravity.CENTER);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 0, 0, dpToPx(8));
            tvDay.setLayoutParams(params);
            calendarGrid.addView(tvDay);
        }

        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            TextView emptyView = new TextView(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(40);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            emptyView.setLayoutParams(params);
            calendarGrid.addView(emptyView);
        }

        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = dateFormat.format(tempCal.getTime());
            boolean isToday = isSameDay(tempCal, today);
            boolean isFuture = tempCal.after(today);
            boolean isSelected = dateStr.equals(selectedDate);

            View dayCell = createDayCell(day, dateStr, isToday, isFuture, isSelected);
            calendarGrid.addView(dayCell);
        }
    }

    private View createDayCell(int day, String dateStr, boolean isToday, boolean isFuture, boolean isSelected) {
        TextView tvDay = new TextView(requireContext());
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = 0;
        layoutParams.height = dpToPx(40);
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tvDay.setLayoutParams(layoutParams);
        tvDay.setText(String.valueOf(day));
        tvDay.setTextSize(16);
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setTypeface(null, android.graphics.Typeface.BOLD);

        tvDay.setTextColor(COLOR_TEXT_DEFAULT);
        tvDay.setBackground(null);
        tvDay.setAlpha(1.0f);

        int completionStatus = !isFuture ? getCompletionStatus(dateStr) : 0;
        int sizePx = dpToPx(36);

        if (!isSelected && !isToday) {
            tvDay.setTextColor(COLOR_TEXT_GREY);
        }

        if (isFuture) {
            tvDay.setTextColor(COLOR_TEXT_DIMMED);
            tvDay.setAlpha(0.5f);
        }

        if (isSelected || isToday) {
            tvDay.setTextColor(Color.WHITE);

            GradientDrawable selectedBg = new GradientDrawable();
            selectedBg.setShape(GradientDrawable.OVAL);

            if (isToday) {
                selectedBg.setColor(COLOR_TODAY_ACCENT);
            } else {
                selectedBg.setColor(COLOR_SELECTED_ACCENT);
            }

            tvDay.setBackground(selectedBg);
            tvDay.setWidth(sizePx);
            tvDay.setHeight(sizePx);
            tvDay.setGravity(Gravity.CENTER);
        }

        if (completionStatus == 2 && hasActiveHabitsOnDate(dateStr)) {
            GradientDrawable borderBg;
            if (isSelected || isToday) {
                borderBg = new GradientDrawable();
                borderBg.setShape(GradientDrawable.OVAL);

                if (isToday) {
                    borderBg.setColor(COLOR_TODAY_ACCENT);
                } else {
                    borderBg.setColor(COLOR_SELECTED_ACCENT);
                }

                borderBg.setStroke(dpToPx(2), COLOR_COMPLETION_BORDER);
                tvDay.setBackground(borderBg);
            } else {
                borderBg = new GradientDrawable();
                borderBg.setShape(GradientDrawable.OVAL);
                borderBg.setColor(Color.TRANSPARENT);
                borderBg.setStroke(dpToPx(2), COLOR_COMPLETION_BORDER);

                tvDay.setBackground(borderBg);
                tvDay.setWidth(sizePx);
                tvDay.setHeight(sizePx);
                tvDay.setGravity(Gravity.CENTER);
                tvDay.setTextColor(COLOR_TEXT_GREY);
            }

            tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // Ensure that days with partial or full completion still show color/bolding
        if (completionStatus > 0 && !isFuture && !isSelected && !isToday) {
            tvDay.setTextColor(COLOR_TEXT_DEFAULT);
            tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
        }


        if (isFuture) {
            return tvDay;
        }

        tvDay.setOnClickListener(v -> {
            onDateClick(dateStr);
        });

        return tvDay;
    }

    private boolean hasActiveHabitsOnDate(String dateStr) {
        if (allHabits == null || allHabits.isEmpty()) {
            return false;
        }

        List<Habit> habitsToCheck = new ArrayList<>();
        if (selectedHabitId == -1) {
            habitsToCheck.addAll(allHabits);
        } else {
            Habit selectedHabit = databaseHelper.getHabitById(selectedHabitId);
            if (selectedHabit != null) {
                habitsToCheck.add(selectedHabit);
            }
        }

        for (Habit habit : habitsToCheck) {
            if (isHabitActiveOnDate(habit, dateStr, todayDate)) {
                return true;
            }
        }
        return false;
    }

    private int getCompletionStatus(String dateStr) {
        if (allHabits == null || allHabits.isEmpty()) {
            return 0;
        }

        if (selectedHabitId != -1) {
            Habit selectedHabit = databaseHelper.getHabitById(selectedHabitId);
            if (selectedHabit != null && isHabitActiveOnDate(selectedHabit, dateStr, todayDate)) {
                boolean completed = databaseHelper.isHabitCompletedOnDate(selectedHabitId, dateStr);
                return completed ? 2 : 0;
            }
            return 0;
        }

        int activeHabitsCount = 0;
        int completedCount = 0;

        for (Habit habit : allHabits) {
            if (isHabitActiveOnDate(habit, dateStr, todayDate)) {
                activeHabitsCount++;
                if (databaseHelper.isHabitCompletedOnDate(habit.getId(), dateStr)) {
                    completedCount++;
                }
            }
        }

        if (activeHabitsCount == 0) {
            return 0;
        } else if (completedCount == activeHabitsCount) {
            return 2; // Fully completed
        } else if (completedCount > 0) {
            return 1; // Partially completed
        } else {
            return 0; // Not completed
        }
    }

    private void onDateClick(String dateStr) {
        selectedDate = dateStr;
        displayCalendar();

        // Navigate to the HabitDetailsTimeliine Fragment
        try {
            String habitName = null;
            if (selectedHabitId != -1) {
                Habit selectedHabit = databaseHelper.getHabitById(selectedHabitId);
                habitName = selectedHabit != null ? selectedHabit.getName() : "Habit Details";
            }

            HabitDetailsTimeliine timelineFragment = HabitDetailsTimeliine.newInstance(dateStr, selectedHabitId, habitName);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, timelineFragment) // Use your main fragment container ID
                    .addToBackStack(null)
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Timeline Fragment: " + e.getMessage());
            Toast.makeText(requireContext(), "Error opening details for " + dateStr, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
