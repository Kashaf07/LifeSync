package com.example.lifesync;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HabitDetailsTimeliine extends Fragment {

    public static final String EXTRA_DATE = "selected_date";
    public static final String EXTRA_HABIT_ID = "selected_habit_id";
    public static final String EXTRA_HABIT_NAME = "selected_habit_name";

    private TextView tvDateTitle;
    private RecyclerView rvTimelineHabits;
    private LinearLayout llEmptyStateContainer;
    private ImageView ivNoData;
    private TextView tvEmptyMessage;

    private HabitDatabaseHelper databaseHelper;
    private CalenderHabitAdapter adapter;  // ✅ Fixed spelling to match your class name
    private List<CalendarHabitItem> timelineList;

    private int selectedHabitId;
    private String selectedDate;

    public HabitDetailsTimeliine() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static HabitDetailsTimeliine newInstance(String date, int habitId, String habitName) {
        HabitDetailsTimeliine fragment = new HabitDetailsTimeliine();
        Bundle args = new Bundle();
        args.putString(EXTRA_DATE, date);
        args.putInt(EXTRA_HABIT_ID, habitId);
        args.putString(EXTRA_HABIT_NAME, habitName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedDate = getArguments().getString(EXTRA_DATE);
            selectedHabitId = getArguments().getInt(EXTRA_HABIT_ID, -1);
        }

        if (getContext() != null) {
            databaseHelper = new HabitDatabaseHelper(requireContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit_details_timeline, container, false);

        tvDateTitle = view.findViewById(R.id.tvDateTitle);
        rvTimelineHabits = view.findViewById(R.id.rvTimelineHabits);
        llEmptyStateContainer = view.findViewById(R.id.llEmptyStateContainer);
        ivNoData = view.findViewById(R.id.ivNoData);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        view.findViewById(R.id.ivBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        timelineList = new ArrayList<>();
        // ✅ Fixed to use CalenderHabitAdapter (your actual class name)
        adapter = new CalenderHabitAdapter(timelineList, selectedHabitId);
        rvTimelineHabits.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTimelineHabits.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (selectedDate != null && databaseHelper != null) {
            displayDate(selectedDate);
            loadTimelineHabits(selectedDate);
        } else {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void displayDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(inputFormat.parse(dateStr));
            tvDateTitle.setText(outputFormat.format(cal.getTime()));
        } catch (Exception e) {
            tvDateTitle.setText(dateStr);
        }
    }

    private void loadTimelineHabits(String dateStr) {
        timelineList.clear();
        List<Habit> habitsToDisplay = new ArrayList<>();
        boolean isSingleHabitView = selectedHabitId != -1;

        if (isSingleHabitView) {
            Habit habit = databaseHelper.getHabitById(selectedHabitId);
            if (habit != null) {
                habitsToDisplay.add(habit);
            }
        } else {
            habitsToDisplay.addAll(databaseHelper.getAllHabits());
        }

        // Only add COMPLETED habits to the timeline
        if (!habitsToDisplay.isEmpty()) {
            for (Habit habit : habitsToDisplay) {
                // Assuming isHabitActiveOnDate logic is complex and checking completion is sufficient here for display
                boolean isCompleted = databaseHelper.isHabitCompletedOnDate(habit.getId(), dateStr);

                if (isCompleted) {
                    timelineList.add(new CalendarHabitItem(
                            habit.getName(),
                            habit.getColor(),
                            true));
                }
            }
        }

        adapter.notifyDataSetChanged();

        // Show empty state if no completed habits
        if (timelineList.isEmpty()) {
            rvTimelineHabits.setVisibility(View.GONE);
            llEmptyStateContainer.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No Completed Habits Found for this date.");
        } else {
            rvTimelineHabits.setVisibility(View.VISIBLE);
            llEmptyStateContainer.setVisibility(View.GONE);
        }
    }
}