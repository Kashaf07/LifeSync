// File: fragment_add_habit.java
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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

public class fragment_add_habit extends Fragment {

    private static final String TAG = "AddHabitFragment";
    private static final String ADD_HABIT_RESULT_KEY = "add_habit_result";

    private EditText etHabitName;
    private TextView tvStartDate, tvEndDate, tvEmptyState, tvSuggestionsHeader;
    private RadioGroup rgColors;
    private LinearLayout timesContainer;
    private CardView btnSave;
    private TextView btnAddTime;
    private ImageView ivBack;
    private RecyclerView rvHabitSuggestions;
    private HabitSuggestionAdapter suggestionAdapter;

    private List<String> selectedTimes = new ArrayList<>();
    private String selectedColor = "";
    private String startDate = "";
    private String endDate = "";
    private String todayDate = "";

    private static final int MAX_TIMES = 5;

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

    public fragment_add_habit() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_habit, container, false);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDate = dateFormat.format(Calendar.getInstance().getTime());
        startDate = todayDate;

        initializeViews(view);
        setupSuggestions();
        setupListeners();

        tvStartDate.setText(formatDateForDisplay(todayDate));

        if (selectedColor.isEmpty()) {
            rgColors.check(R.id.rbBlue);
            selectedColor = getSelectedColor(R.id.rbBlue);
        }

        return view;
    }

    private void initializeViews(View view) {
        ivBack = view.findViewById(R.id.ivBack);
        etHabitName = view.findViewById(R.id.etHabitName);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        rgColors = view.findViewById(R.id.rgColors);
        timesContainer = view.findViewById(R.id.timesContainer);
        btnSave = view.findViewById(R.id.btnSave);
        btnAddTime = view.findViewById(R.id.btnAddTime);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvHabitSuggestions = view.findViewById(R.id.rvHabitSuggestions);
        tvSuggestionsHeader = view.findViewById(R.id.tvSuggestionsHeader);
    }

    private void setupSuggestions() {
        Context context = getContext();
        if (context == null) return;

        suggestionAdapter = new HabitSuggestionAdapter(new ArrayList<>(), new HabitSuggestionAdapter.OnHabitClickListener() {
            @Override
            public void onHabitClick(String habitName) {
                etHabitName.setText(habitName);
                etHabitName.setSelection(habitName.length());
                rvHabitSuggestions.setVisibility(View.GONE);
                tvSuggestionsHeader.setVisibility(View.GONE);
            }
        });

        rvHabitSuggestions.setLayoutManager(new LinearLayoutManager(context));
        rvHabitSuggestions.setAdapter(suggestionAdapter);

        etHabitName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etHabitName.getText().toString().trim().isEmpty()) {
                showTopSuggestions();
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

    private void setupListeners() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        btnAddTime.setOnClickListener(v -> addTimeSlot());
        btnSave.setOnClickListener(v -> saveHabit());

        rgColors.setOnCheckedChangeListener((group, checkedId) -> {
            selectedColor = getSelectedColor(checkedId);
            updateTimesDisplay();
        });
    }

    private void showDatePicker(boolean isStartDate) {
        Context context = getContext();
        if (context == null) return;

        Calendar calendar = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (isStartDate && !startDate.isEmpty()) {
                calendar.setTime(sdf.parse(startDate));
            } else if (!isStartDate && !endDate.isEmpty()) {
                calendar.setTime(sdf.parse(endDate));
            }
        } catch (ParseException e) {
            // Use current date if parsing fails
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    if (isStartDate) {
                        if (isDateBefore(date, todayDate) && !date.equals(todayDate)) {
                            Toast.makeText(context, "Start date cannot be before today", Toast.LENGTH_LONG).show();
                            return;
                        }

                        startDate = date;
                        tvStartDate.setText(formatDateForDisplay(date));

                        if (!endDate.isEmpty() && isDateBefore(endDate, startDate)) {
                            endDate = "";
                            tvEndDate.setText("Select date (No End)");
                        }
                    } else {
                        if (!startDate.isEmpty() && isDateBefore(date, startDate)) {
                            Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_LONG).show();
                            return;
                        }

                        endDate = date;
                        tvEndDate.setText(formatDateForDisplay(date));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        } else {
            if (!startDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date startDateObj = sdf.parse(startDate);
                    if (startDateObj != null) {
                        datePickerDialog.getDatePicker().setMinDate(startDateObj.getTime());
                    }
                } catch (ParseException e) {
                    datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                }
            } else {
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }
        }

        datePickerDialog.show();
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

    private boolean isDateBefore(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);
            return d1 != null && d2 != null && d1.before(d2);
        } catch (ParseException e) {
            Log.e(TAG, "Error comparing dates: " + e.getMessage());
            return false;
        }
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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_time_picker_edit, null);

        NumberPicker hourPicker = view.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = view.findViewById(R.id.minutePicker);
        NumberPicker periodPicker = view.findViewById(R.id.periodPicker);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnDeleteTime = view.findViewById(R.id.btnDelete);
        ImageView ivClose = view.findViewById(R.id.ivClose);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        LinearLayout dialogContainer = view.findViewById(R.id.timePickerDialog);

        if (dialogContainer != null) {
            dialogContainer.setBackgroundColor(Color.WHITE);
        }

        int pickerWidth = (int) (80 * getResources().getDisplayMetrics().density);
        int pickerHeight = (int) (180 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(pickerWidth, pickerHeight);
        pickerParams.setMargins(4, 0, 4, 0);
        hourPicker.setLayoutParams(pickerParams);
        minutePicker.setLayoutParams(pickerParams);

        LinearLayout.LayoutParams periodParams = new LinearLayout.LayoutParams(pickerWidth, pickerHeight);
        periodParams.setMargins(8, 0, 4, 0);
        periodPicker.setLayoutParams(periodParams);

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setWrapSelectorWheel(true);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
        minutePicker.setWrapSelectorWheel(true);

        periodPicker.setMinValue(0);
        periodPicker.setMaxValue(1);
        periodPicker.setDisplayedValues(new String[]{"AM", "PM"});
        periodPicker.setWrapSelectorWheel(false);

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

        dialog.setOnShowListener(dialogInterface -> {
            hourPicker.postDelayed(() -> {
                setNumberPickerTextColorSafe(hourPicker);
                setNumberPickerTextColorSafe(minutePicker);
                setNumberPickerTextColorSafe(periodPicker);
            }, 100);
        });

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

    private void setNumberPickerTextColorSafe(NumberPicker numberPicker) {
        try {
            numberPicker.measure(0, 0);
            setNumberPickerTextColorRecursive(numberPicker);
            numberPicker.invalidate();
        } catch (Exception e) {
            Log.w(TAG, "Failed to set NumberPicker text color: " + e.getMessage());
        }
    }

    private void setNumberPickerTextColorRecursive(View view) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setTextColor(Color.parseColor("#2D3748"));
            editText.setTextSize(20);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setNumberPickerTextColorRecursive(viewGroup.getChildAt(i));
            }
        }
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

    private String getSelectedColor(int checkedId) {
        if (checkedId == R.id.rbBlue) {
            return "#E8F4F8";
        } else if (checkedId == R.id.rbGreen) {
            return "#C9E4DE";
        } else if (checkedId == R.id.rbPeach) {
            return "#FFE5D9";
        } else if (checkedId == R.id.rbLavender) {
            return "#E6E6FA";
        } else if (checkedId == R.id.rbSoftCoral) {
            return "#FFD4C7";
        } else if (checkedId == R.id.rbMintGreen) {
            return "#D4E8E0";
        } else if (checkedId == R.id.rbSkyBlue) {
            return "#D9E9F7";
        } else {
            return "#E8F4F8";
        }
    }

    private void saveHabit() {
        Context context = getContext();
        if (context == null) return;

        String habitName = etHabitName.getText().toString().trim();

        if (habitName.isEmpty()) {
            Toast.makeText(context, "Please enter habit name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedColor.isEmpty()) {
            Toast.makeText(context, "Please select a color", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTimes.isEmpty()) {
            Toast.makeText(context, "⏰ Please add at least one reminder", Toast.LENGTH_LONG).show();
            return;
        }

        if (startDate.isEmpty()) {
            startDate = todayDate;
        }

        String progressString = String.join(",", selectedTimes);

        Bundle result = new Bundle();
        result.putString("habit_name", habitName);
        result.putString("habit_color", selectedColor);
        result.putString("habit_start_date", startDate);
        result.putString("habit_end_date", endDate);
        result.putString("habit_progress", progressString);

        getParentFragmentManager().setFragmentResult("add_habit_result", result);

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }
}