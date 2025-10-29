package com.example.lifesync;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class EditHabitFragment extends Fragment {

    private static final String TAG = "EditHabitFragment";
    private static final int MAX_TIMES = 5;

    private EditText etHabitName;
    private TextView tvStartDate, tvEndDate, tvEmptyState, btnAddTime, tvSuggestionsHeader;
    private RadioGroup rgColors;
    private CardView btnSave, btnDeleteContainer;
    private ImageView ivBack;
    private LinearLayout timesContainer;
    private RecyclerView rvHabitSuggestions;
    private HabitSuggestionAdapter suggestionAdapter;

    private HabitDatabaseHelper databaseHelper;
    private Notificationhelperhabit notificationHelper;

    private int habitId = -1;
    private List<String> selectedTimes = new ArrayList<>();
    private String selectedColor = "#E8F4F8";
    private String startDateStr = "";
    private String endDateStr = "";
    private String todayDate;

    private List<String> allHabitSuggestions = Arrays.asList(
            "Drink Water", "Exercise", "Journal", "Read", "Walk", "Yoga", "Run", "Cycling",
            "Play Sports", "Pray", "Get 7+ Hours Sleep", "Eat Healthy Breakfast",
            "Meditate 10 Minutes", "Floss Teeth", "Make Your Bed", "Take Daily Vitamins",
            "Stretch for 10 Min", "Cook a Homemade Meal", "Limit Screen Time Before Bed",
            "Clean & Organize 15 Min", "Avoid Snooze Button", "Limit Sugar Intake",
            "Do a Digital Detox", "Deep Breathing Exercises", "Plan Your Top 3 Tasks",
            "Work on a Side Project", "Learn a New Skill/Hobby", "Call a Friend/Family",
            "Give a Genuine Compliment", "Practice Daily Gratitude", "Write Thank You Note",
            "Set Time for Deep Work", "Avoid Social Media in AM", "Listen to Educational Content",
            "Review Daily Finances", "Visualize Goals", "Creative Activity (Draw/Music)",
            "Read News/Current Events", "Tidy Up Workspace", "Wash Dishes Immediately",
            "De-clutter Hotspot Area", "Go to Bed at Set Time", "Prep Meal for Tomorrow",
            "Help Someone Else", "Do a Brain Dump", "Take Cold Shower", "Maintain Good Posture",
            "Review Your Goals", "Say No to Unnecessary Commitments", "Update Calendar/Planner"
    );

    public EditHabitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            habitId = getArguments().getInt("habit_id", -1);
        }
        databaseHelper = new HabitDatabaseHelper(requireContext());
        notificationHelper = new Notificationhelperhabit(requireContext());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDate = dateFormat.format(Calendar.getInstance().getTime());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_habit, container, false);

        initializeViews(view);
        setupSuggestions();
        setupColorSelector();
        setupDatePickers();
        setupButtons();

        if (habitId != -1) {
            loadHabitData(habitId);
        } else {
            Toast.makeText(requireContext(), "Error: Habit ID missing.", Toast.LENGTH_LONG).show();
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }

        return view;
    }

    private void initializeViews(View view) {
        etHabitName = view.findViewById(R.id.etHabitName);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        rgColors = view.findViewById(R.id.rgColors);
        btnSave = view.findViewById(R.id.btnSave);
        btnDeleteContainer = view.findViewById(R.id.btnDeleteContainer);
        ivBack = view.findViewById(R.id.ivBack);

        // Required views for reminders and suggestions
        timesContainer = view.findViewById(R.id.timesContainer);
        btnAddTime = view.findViewById(R.id.btnAddTime);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvHabitSuggestions = view.findViewById(R.id.rvHabitSuggestions);
        tvSuggestionsHeader = view.findViewById(R.id.tvSuggestionsHeader);
    }

    private void setupSuggestions() {
        Context context = getContext();
        if (context == null) return;

        suggestionAdapter = new HabitSuggestionAdapter(new ArrayList<>(), habitName -> {
            etHabitName.setText(habitName);
            etHabitName.setSelection(habitName.length());
            rvHabitSuggestions.setVisibility(View.GONE);
            tvSuggestionsHeader.setVisibility(View.GONE);
        });

        rvHabitSuggestions.setLayoutManager(new LinearLayoutManager(context));
        rvHabitSuggestions.setAdapter(suggestionAdapter);

        // ISSUE FIX 3: Show suggestions when focusing or clearing text
        etHabitName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etHabitName.getText().toString().trim().isEmpty()) {
                showTopSuggestions();
            } else if (!hasFocus) {
                rvHabitSuggestions.setVisibility(View.GONE);
                tvSuggestionsHeader.setVisibility(View.GONE);
            }
        });

        etHabitName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSuggestions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showTopSuggestions() {
        List<String> topSuggestions = allHabitSuggestions.subList(0, Math.min(10, allHabitSuggestions.size()));
        suggestionAdapter.updateList(topSuggestions);
        rvHabitSuggestions.setVisibility(View.VISIBLE);
        tvSuggestionsHeader.setVisibility(View.VISIBLE);
    }

    private void filterSuggestions(String query) {
        if (query.trim().isEmpty()) {
            showTopSuggestions();
            return;
        }

        List<String> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (String habit : allHabitSuggestions) {
            if (habit.toLowerCase().contains(lowerQuery)) {
                filteredList.add(habit);
            }
        }

        if (filteredList.isEmpty()) {
            rvHabitSuggestions.setVisibility(View.GONE);
            tvSuggestionsHeader.setVisibility(View.GONE);
        } else {
            List<String> topSuggestions = filteredList.subList(0, Math.min(10, filteredList.size()));
            suggestionAdapter.updateList(topSuggestions);
            rvHabitSuggestions.setVisibility(View.VISIBLE);
            tvSuggestionsHeader.setVisibility(View.VISIBLE);
        }
    }

    private void loadHabitData(int id) {
        Habit habit = databaseHelper.getHabitById(id);
        if (habit != null) {
            etHabitName.setText(habit.getName());
            selectedColor = habit.getColor();

            // ISSUE FIX 2: Correctly load dates and display
            startDateStr = habit.getDate() != null && !habit.getDate().isEmpty() ? habit.getDate() : todayDate;
            tvStartDate.setText(formatDateForDisplay(startDateStr));

            endDateStr = habit.getEndDate() != null ? habit.getEndDate() : "";
            if (!endDateStr.isEmpty()) {
                tvEndDate.setText(formatDateForDisplay(endDateStr));
            } else {
                tvEndDate.setText("Select date (No End)");
            }

            setInitialColor(selectedColor);

            // ISSUE FIX 1: Load and display reminder times
            selectedTimes.clear();
            String progress = habit.getProgress();
            if (progress != null && !progress.trim().isEmpty()) {
                // Split and trim each time, then add
                String[] times = progress.split(",");
                for (String time : times) {
                    selectedTimes.add(time.trim());
                }
            }
            updateTimesDisplay();

        } else {
            Toast.makeText(requireContext(), "Habit not found.", Toast.LENGTH_LONG).show();
        }
    }

    private void setInitialColor(String color) {
        int checkedId = R.id.rbBlue; // Default
        if ("#C9E4DE".equals(color)) checkedId = R.id.rbGreen;
        else if ("#FFE5D9".equals(color)) checkedId = R.id.rbPeach;
        else if ("#E6E6FA".equals(color)) checkedId = R.id.rbLavender;
        else if ("#FFD4C7".equals(color)) checkedId = R.id.rbSoftCoral;
        else if ("#D4E8E0".equals(color)) checkedId = R.id.rbMintGreen;
        else if ("#D9E9F7".equals(color)) checkedId = R.id.rbSkyBlue;

        if (rgColors != null) {
            rgColors.check(checkedId);
        }
    }

    private void setupColorSelector() {
        rgColors.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBlue) selectedColor = "#E8F4F8";
            else if (checkedId == R.id.rbGreen) selectedColor = "#C9E4DE";
            else if (checkedId == R.id.rbPeach) selectedColor = "#FFE5D9";
            else if (checkedId == R.id.rbLavender) selectedColor = "#E6E6FA";
            else if (checkedId == R.id.rbSoftCoral) selectedColor = "#FFD4C7";
            else if (checkedId == R.id.rbMintGreen) selectedColor = "#D4E8E0";
            else if (checkedId == R.id.rbSkyBlue) selectedColor = "#D9E9F7";
        });
    }

    private void setupDatePickers() {
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        final Calendar calendar = Calendar.getInstance();

        // Use current stored date for picker if available
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (isStartDate && !startDateStr.isEmpty()) {
                calendar.setTime(sdf.parse(startDateStr));
            } else if (!isStartDate && !endDateStr.isEmpty()) {
                calendar.setTime(sdf.parse(endDateStr));
            }
        } catch (ParseException e) {
            // Fallback to today
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (DatePicker view, int y, int m, int d) -> {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);

                    if (isStartDate) {
                        // ISSUE FIX 2: Validate against today
                        if (isDateBefore(dateStr, todayDate) && !dateStr.equals(todayDate)) {
                            Toast.makeText(requireContext(), "Start date cannot be before today", Toast.LENGTH_LONG).show();
                            return;
                        }

                        startDateStr = dateStr;
                        tvStartDate.setText(formatDateForDisplay(dateStr));

                        // Auto-correct end date if it precedes new start date
                        if (!endDateStr.isEmpty() && isDateBefore(endDateStr, startDateStr)) {
                            endDateStr = "";
                            tvEndDate.setText("Select date (No End)");
                        }
                    } else {
                        // Validate end date against start date
                        if (!startDateStr.isEmpty() && isDateBefore(dateStr, startDateStr)) {
                            Toast.makeText(requireContext(), "End date cannot be before start date", Toast.LENGTH_LONG).show();
                            return;
                        }
                        endDateStr = dateStr;
                        tvEndDate.setText(formatDateForDisplay(dateStr));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // ISSUE FIX 2: Enforce constraints on the picker itself
        if (isStartDate) {
            // Cannot select a past date
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        } else {
            // Cannot select a date before the start date
            if (!startDateStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date startDateObj = sdf.parse(startDateStr);
                    if (startDateObj != null) {
                        datePicker.getDatePicker().setMinDate(startDateObj.getTime());
                    }
                } catch (ParseException e) {
                    datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                }
            } else {
                // If start date is somehow unset, restrict to today
                datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }
        }

        datePicker.show();
    }

    private boolean isDateBefore(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);
            return d1 != null && d2 != null && d1.before(d2);
        } catch (ParseException e) {
            return false;
        }
    }

    private String formatDateForDisplay(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage());
        }
        return dateStr;
    }

    private void setupButtons() {
        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnAddTime.setOnClickListener(v -> addTimeSlot());
        btnSave.setOnClickListener(v -> updateHabit());
        btnDeleteContainer.setOnClickListener(v -> deleteHabitConfirmation());
    }

    private void addTimeSlot() {
        Context context = getContext();
        if (context == null) return;

        if (selectedTimes.size() >= MAX_TIMES) {
            Toast.makeText(context, "Maximum 5 times allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        showCustomTimePickerDialog(-1);
    }

    private void showCustomTimePickerDialog(int editIndex) {
        Context context = getContext();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = getLayoutInflater().inflate(R.layout.dialog_time_picker_edit, null);

        NumberPicker hourPicker = view.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = view.findViewById(R.id.minutePicker);
        NumberPicker periodPicker = view.findViewById(R.id.periodPicker);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnDeleteTime = view.findViewById(R.id.btnDelete);
        ImageView ivClose = view.findViewById(R.id.ivClose);
        TextView tvTitle = view.findViewById(R.id.tvTitle);

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
        minutePicker.setWrapSelectorWheel(true);

        periodPicker.setMinValue(0);
        periodPicker.setMaxValue(1);
        periodPicker.setDisplayedValues(new String[]{"AM", "PM"});

        if (editIndex >= 0) {
            tvTitle.setText("Edit Time");
            btnDeleteTime.setVisibility(View.VISIBLE);
            String existingTime = selectedTimes.get(editIndex);
            String[] parts = existingTime.split(" ");
            String[] timeParts = parts[0].split(":");

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            String periodStr = parts[1].toUpperCase();

            hourPicker.setValue(hour);
            minutePicker.setValue(minute);
            periodPicker.setValue(periodStr.equals("AM") ? 0 : 1);
        } else {
            tvTitle.setText("Select Time");
            btnDeleteTime.setVisibility(View.GONE);

            Calendar now = Calendar.getInstance();
            int currentHour12 = now.get(Calendar.HOUR);
            if (currentHour12 == 0) currentHour12 = 12;

            hourPicker.setValue(currentHour12);
            minutePicker.setValue(now.get(Calendar.MINUTE));
            periodPicker.setValue(now.get(Calendar.AM_PM));
        }

        AlertDialog dialog = builder.setView(view).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int period = periodPicker.getValue();

            String time = String.format(Locale.getDefault(), "%02d:%02d %s",
                    hour, minute, period == 0 ? "AM" : "PM");

            if (editIndex < 0 && selectedTimes.contains(time)) {
                Toast.makeText(context, "This reminder time is already added.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editIndex >= 0) {
                selectedTimes.set(editIndex, time);
            } else {
                selectedTimes.add(time);
            }
            updateTimesDisplay();
            dialog.dismiss();
        });

        btnDeleteTime.setOnClickListener(v -> {
            if (editIndex >= 0) {
                selectedTimes.remove(editIndex);
                updateTimesDisplay();
            }
            dialog.dismiss();
        });

        ivClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateTimesDisplay() {
        Context context = getContext();
        if (context == null) return;

        timesContainer.removeAllViews();
        Collections.sort(selectedTimes);

        if (selectedTimes.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        LinearLayout chipRow = new LinearLayout(context);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 12);
        chipRow.setLayoutParams(rowParams);

        for (int i = 0; i < selectedTimes.size(); i++) {
            String time = selectedTimes.get(i);
            final int index = i;

            CardView timeBadge = new CardView(context);
            timeBadge.setRadius(20);
            timeBadge.setCardElevation(4);

            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.setMargins(0, 0, 16, 8);

            timeBadge.setLayoutParams(badgeParams);
            // Use the same accent color for consistency
            timeBadge.setCardBackgroundColor(Color.parseColor("#EDACAC"));

            TextView tvTime = new TextView(context);
            tvTime.setText(time);
            tvTime.setTextSize(16);
            tvTime.setTextColor(Color.parseColor("#2D3748"));
            tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTime.setPadding(30, 18, 30, 18);

            timeBadge.addView(tvTime);
            timeBadge.setOnClickListener(v -> showCustomTimePickerDialog(index));

            if (i < MAX_TIMES) {
                chipRow.addView(timeBadge);
            }
        }

        timesContainer.addView(chipRow);
    }

    private void updateHabit() {
        String habitName = etHabitName.getText().toString().trim();

        if (habitName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter habit name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedColor.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a color", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTimes.isEmpty()) {
            Toast.makeText(requireContext(), "⏰ Please add at least one reminder", Toast.LENGTH_LONG).show();
            return;
        }

        String progressString = String.join(",", selectedTimes);

        // 1. Cancel old reminders
        Habit oldHabit = databaseHelper.getHabitById(habitId);
        if (oldHabit != null && oldHabit.getProgress() != null) {
            notificationHelper.cancelHabitReminders(habitId, oldHabit.getProgress());
        }

        // 2. Update DB
        Habit habit = new Habit(habitId, habitName, false, selectedColor, progressString, startDateStr, endDateStr);
        databaseHelper.updateHabit(habit);

        // 3. Schedule new reminders
        if (!progressString.isEmpty()) {
            notificationHelper.scheduleHabitReminders(habitId, habitName, progressString);
        }

        Toast.makeText(requireContext(), "Habit updated! ✅", Toast.LENGTH_SHORT).show();

        // Navigate back
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void deleteHabitConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete this habit? This cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> deleteHabit())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteHabit() {
        // Cancel reminders
        Habit habit = databaseHelper.getHabitById(habitId);
        if (habit != null && habit.getProgress() != null) {
            notificationHelper.cancelHabitReminders(habitId, habit.getProgress());
        }

        // Delete from DB
        databaseHelper.deleteHabit(habitId);

        Toast.makeText(requireContext(), "Habit deleted! 🗑️", Toast.LENGTH_SHORT).show();

        // Navigate back
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }
}