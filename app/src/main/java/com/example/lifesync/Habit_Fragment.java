package com.example.lifesync;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class Habit_Fragment extends Fragment implements HabitAdapter.OnHabitActionListener, FilterDialogFragment.FilterDialogListener {

    private static final String TAG = "Habit_Fragment";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String ADD_HABIT_REQUEST_KEY = "add_habit_result";

    private static final int COLOR_DATE_CARD_TRANSPARENT = Color.parseColor("#44FFFFFF");
    private static final int COLOR_DATE_CARD_SELECTED = Color.parseColor("#33FFFFFF");
    private static final int COLOR_DATE_CARD_PAST = Color.parseColor("#33808080");

    private RecyclerView recyclerViewHabits;
    private HabitAdapter habitAdapter;
    private List<Habit> habitList;
    private HabitDatabaseHelper databaseHelper;
    private Notificationhelperhabit notificationHelper;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabCalendar;
    private TextView tvCalendarLabel;

    // Empty State Views
    private LinearLayout llEmptyStateContainer; // Container (same as in timeline fragment)
    private ImageView ivNoData; // Using 'ivNoData' to match fragment_habit_details_timeline.xml
    private TextView tvEmptyMessage; // Message text view

    private TextView tvTodayLabel;
    private TextView tvFilterAll;
    private LinearLayout headerSection;
    private LinearLayout dateStripContainer;
    private TextView tvSwipeHint;

    private KonfettiView konfettiView;

    private String selectedDate;
    private int selectedDatePosition = -1;
    private String todayDate;

    private List<CardView> dateCards = new ArrayList<>();
    private List<String> dateStrings = new ArrayList<>();
    private int todayCardIndex = -1;

    private String currentStatusFilter = "All";
    private String currentTimeFilter = "All";

    private boolean isFirstRun = true;

    private View rootView;

    public Habit_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_habit, container, false);

        try {
            initializeViews();
            databaseHelper = new HabitDatabaseHelper(getContext());
            notificationHelper = new Notificationhelperhabit(getContext());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            todayDate = dateFormat.format(Calendar.getInstance().getTime());
            selectedDate = todayDate;

            setupFragmentResultListener();

            requestAllPermissions();
            setupHeader();
            setupDateStrip();
            setupHabitsRecyclerView();
            loadHabitsForSelectedDate();
            animateHeaderEntrance();

            if (isFirstRun) {
                animateCalendarLabel();
                isFirstRun = false;
            }

            if (fabAdd != null) {
                fabAdd.setOnClickListener(v -> showAddHabitPage());
            }

            if (fabCalendar != null) {
                fabCalendar.setOnClickListener(v -> openCalendarFragment());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error starting fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return rootView;
    }

    // --- Fragment Result Listener for AddHabitFragment ---
    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener(ADD_HABIT_REQUEST_KEY, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(ADD_HABIT_REQUEST_KEY)) {
                    String habitName = result.getString("habit_name");
                    String habitColor = result.getString("habit_color");
                    String startDateStr = result.getString("habit_start_date");
                    String endDateStr = result.getString("habit_end_date");
                    String progressString = result.getString("habit_progress");

                    if (startDateStr == null || startDateStr.isEmpty()) {
                        startDateStr = todayDate;
                    }

                    try {
                        Habit habit = new Habit(habitName, false, habitColor, progressString != null ? progressString : "");
                        habit.setDate(startDateStr);
                        habit.setEndDate(endDateStr);

                        long newHabitId = databaseHelper.addHabit(habit);
                        habit.setId((int) newHabitId);

                        if (progressString != null && !progressString.isEmpty()) {
                            notificationHelper.scheduleHabitReminders(habit.getId(), habitName, progressString);
                        }

                        loadHabitsForSelectedDate();
                        Toast.makeText(getContext(), " Habit added with reminders! 🎯", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing habit result: " + e.getMessage());
                        Toast.makeText(getContext(), "Error adding habit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setupHeader() {
        if (tvFilterAll != null) {
            tvFilterAll.setOnClickListener(v -> showFilterDialog());
        }
        if (tvTodayLabel != null) {
            tvTodayLabel.setOnClickListener(v -> jumpToToday());
        }
        updateFilterTextView();
    }

    private void animateCalendarLabel() {
        if (tvCalendarLabel != null) {
            // Reset to ensure it's visible before animating
            tvCalendarLabel.setVisibility(View.VISIBLE);
            tvCalendarLabel.setAlpha(0f);
            tvCalendarLabel.setTranslationY(dpToPx(20)); // Arbitrary starting point for slide up

            tvCalendarLabel.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Auto-hide after 3 seconds
            tvCalendarLabel.postDelayed(() -> {
                if (tvCalendarLabel.getAlpha() > 0.5f) { // Check if it's visible
                    tvCalendarLabel.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> tvCalendarLabel.setVisibility(View.INVISIBLE))
                            .start();
                }
            }, 3000);
        }
    }

    private void startKonfettiCelebration() {
        if (konfettiView == null) return;

        // Define the configuration for the confetti emitter
        EmitterConfig emitterConfig = new Emitter(50, TimeUnit.MILLISECONDS).max(50);

        // Define the shapes and colors
        final List<Shape> shapes = Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE);
        final List<Integer> colors = Arrays.asList(Color.YELLOW, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN);

        // Create the party configuration (removed .speed() method)
        Party party = new PartyFactory(emitterConfig)
                .shapes(shapes)
                .spread(360)
                .angle(90)
                .timeToLive(2000L)
                .colors(colors)
                .position(new Position.Relative(0.5, 0.2)) // Emitter starts slightly above center
                .build();

        // Launch the confetti
        konfettiView.start(party);
    }

    private void initializeViews() {
        recyclerViewHabits = rootView.findViewById(R.id.recyclerViewHabits);
        fabAdd = rootView.findViewById(R.id.fabAdd);
        fabCalendar = rootView.findViewById(R.id.fabCalendar);
        tvCalendarLabel = rootView.findViewById(R.id.tvCalendarLabel);
        tvTodayLabel = rootView.findViewById(R.id.tvTodayLabel);
        tvFilterAll = rootView.findViewById(R.id.tvFilterAll);
        headerSection = rootView.findViewById(R.id.headerSection);
        dateStripContainer = rootView.findViewById(R.id.dateStripContainer);
        konfettiView = rootView.findViewById(R.id.konfetti_view);
        tvSwipeHint = rootView.findViewById(R.id.tvSwipeHint);

        // Initialize Empty State views (assuming you add these to fragment_habit.xml)
        // FIX: R.id.ivNoHabits is likely R.id.ivNoData based on fragment_habit_details_timeline.xml
        llEmptyStateContainer = rootView.findViewById(R.id.llEmptyStateContainer);
        ivNoData = rootView.findViewById(R.id.ivNoData);
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHabitsForSelectedDate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "✅ Exact alarm permission is active");
            } else {
                Log.d(TAG, "⚠️ Exact alarm permission still not granted");
            }
        }
    }

    private void setupHabitsRecyclerView() {
        habitList = new ArrayList<>();
        habitAdapter = new HabitAdapter(habitList, getContext(), this);
        recyclerViewHabits.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewHabits.setAdapter(habitAdapter);

        // Attach ItemTouchHelper for swipe-to-delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewHabits);
    }

    // --- ItemTouchHelper Implementation (Swipe-to-Delete) ---
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                habitAdapter.notifyItemChanged(position);
                return;
            }

            Habit habitToDelete = habitList.get(position);

            if (isDateBefore(selectedDate, todayDate)) {
                Toast.makeText(getContext(), "❌ Cannot delete habits from past dates.", Toast.LENGTH_LONG).show();
                habitAdapter.notifyItemChanged(position);
                return;
            }

            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Habit?")
                    .setMessage("Are you sure you want to delete \"" + habitToDelete.getName() + "\"? This action will cancel all its future reminders.")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        // 1. Cancel reminders
                        notificationHelper.cancelHabitReminders(habitToDelete.getId(), habitToDelete.getProgress());

                        // 2. Delete from database
                        databaseHelper.deleteHabit(habitToDelete.getId());

                        // 3. Update UI list
                        habitList.remove(position);
                        habitAdapter.notifyItemRemoved(position);
                        loadHabitsForSelectedDate(); // Reload to ensure correct sorting/filtering after deletion

                        Toast.makeText(getContext(), "Habit deleted! 🗑️", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Restore the item
                        habitAdapter.notifyItemChanged(position);
                    })
                    .setOnCancelListener(dialog -> {
                        // Restore on dismiss
                        habitAdapter.notifyItemChanged(position);
                    })
                    .show();
        }
    };
    // --------------------------------------------------------

    // --- Navigation Implementations ---
    private void openCalendarFragment() {
        try {
            FragmentCalendar calendarFragment = new FragmentCalendar();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, calendarFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Calendar Fragment: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening Calendar screen.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddHabitPage() {
        try {
            fragment_add_habit addHabitFragment = new fragment_add_habit();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, addHabitFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Add Habit Fragment: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening Add Habit screen.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilterDialog() {
        try {
            FragmentManager fm = getParentFragmentManager();
            FilterDialogFragment filterDialog = new FilterDialogFragment();

            Bundle args = new Bundle();
            args.putString("status", currentStatusFilter);
            args.putString("time", currentTimeFilter);
            filterDialog.setArguments(args);

            filterDialog.setTargetFragment(this, 0);

            filterDialog.show(fm, "FilterDialog");
        } catch (Exception e) {
            Log.e(TAG, "Error showing Filter Dialog: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening filter options.", Toast.LENGTH_SHORT).show();
        }
    }

    // Implementing FilterDialogListener method
    @Override
    public void onFilterSelected(String status, String time) {
        currentStatusFilter = status;
        currentTimeFilter = time;

        updateFilterTextView();
        loadHabitsForSelectedDate();
    }

    // --- HabitAdapter.OnHabitActionListener Implementation ---
    @Override
    public void onHabitMarkComplete(Habit habit, int position) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            if (isDateBefore(selectedDate, todayDate)) {
                Toast.makeText(getContext(), "❌ Cannot modify habits for past dates. Past records are view-only.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTime(dateFormat.parse(selectedDate));
            Calendar today = Calendar.getInstance();

            if (selectedCal.after(today)) {
                Toast.makeText(getContext(), "❌ Cannot complete habits for future dates", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean newStatus = !habit.isCompleted();
            habit.setCompleted(newStatus);

            databaseHelper.updateHabitCompletionForDate(habit.getId(), selectedDate, newStatus);

            int newPosition;
            if (newStatus) {
                newPosition = habitList.size();
                for (int i = habitList.size() - 1; i >= 0; i--) {
                    if (!habitList.get(i).isCompleted()) {
                        newPosition = i + 1;
                        break;
                    }
                }
                if (position < newPosition) {
                    newPosition--;
                }
            } else {
                newPosition = 0;
            }

            if (position != newPosition) {
                habitList.remove(position);
                habitAdapter.notifyItemRemoved(position);
                if (newPosition > habitList.size()) {
                    newPosition = habitList.size();
                }
                habitList.add(newPosition, habit);
                habitAdapter.notifyItemInserted(newPosition);
                recyclerViewHabits.smoothScrollToPosition(newPosition);
            } else {
                habitAdapter.notifyItemChanged(position);
            }

            if (newStatus) {
                startKonfettiCelebration();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onHabitMarkComplete: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error updating habit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onHabitEditClick(Habit habit, int position) {
        if (!selectedDate.equals(todayDate)) {
            if (isDateBefore(selectedDate, todayDate)) {
                Toast.makeText(getContext(), "Cannot edit habits for past dates", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putInt("habit_id", habit.getId());

            Fragment editFragment = new EditHabitFragment();
            editFragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Edit Habit Fragment: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening Edit Habit screen.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- All other helper methods (including missing ones) ---

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionDialog();
                } else {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                }
            } else {
                Log.d(TAG, "✅ Notification permission already granted");
                checkAndRequestExactAlarmPermission();
            }
        } else {
            Log.d(TAG, "✅ No need to request notification permission (Android < 13)");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkAndRequestExactAlarmPermission();
            }
        }
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("🔔 Enable Notifications")
                .setMessage("Habit Tracker needs notification permission to send you reminders for your habits at the right time. Stay on track with timely alerts! 🎯")
                .setPositiveButton("Allow", (dialog, which) -> {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    Toast.makeText(getContext(),
                            "You can enable notifications later in Settings",
                            Toast.LENGTH_LONG).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        checkAndRequestExactAlarmPermission();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(getContext())
                        .setTitle("⚠️ CRITICAL: Enable Exact Alarms")
                        .setMessage("🚨 YOUR REMINDERS WILL NOT WORK WITHOUT THIS!\n\n" +
                                "❌ Without permission:\n" +
                                "• NO notifications\n" +
                                "• OR 15+ minute delays\n\n" +
                                "✅ With permission:\n" +
                                "• Perfect timing\n" +
                                "• Daily repetition\n" +
                                "• Zero delays\n\n" +
                                "👉 Please tap 'Open Settings' and enable 'Alarms & reminders'")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(intent);
                                Toast.makeText(getContext(),
                                        "👉 Enable 'Alarms & reminders' toggle for Habit Tracker",
                                        Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Error opening settings: " + e.getMessage());
                                Toast.makeText(getContext(),
                                        "Go to: Settings > Apps > Habit Tracker > Enable 'Alarms & reminders'",
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Log.d(TAG, "✅ Exact alarm permission GRANTED - notifications will work perfectly!");

                new Thread(() -> {
                    try {
                        notificationHelper.rescheduleAllReminders(databaseHelper);
                        // Removed Toast as requested
                    } catch (Exception e) {
                        Log.e(TAG, "Error rescheduling: " + e.getMessage());
                    }
                }).start();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "✅ Notifications enabled! You'll receive habit reminders.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "✅ Notification permission granted");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkAndRequestExactAlarmPermission();
                }
            } else {
                Toast.makeText(getContext(), "⚠️ Notifications disabled. Enable in Settings to get reminders.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "❌ Notification permission denied");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkAndRequestExactAlarmPermission();
                }
            }
        }
    }

    private void animateHeaderEntrance() {
        if (headerSection != null) {
            headerSection.setAlpha(0f);
            headerSection.setTranslationY(-30f);
            headerSection.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void updateFilterTextView() {
        String filterText = "All ▼";

        if (!currentStatusFilter.equals("All") && !currentTimeFilter.equals("All")) {
            filterText = currentStatusFilter + " & " + currentTimeFilter + " ▼";
        } else if (!currentStatusFilter.equals("All")) {
            filterText = currentStatusFilter + " ▼";
        } else if (!currentTimeFilter.equals("All")) {
            filterText = currentTimeFilter + " ▼";
        }

        if (tvFilterAll != null) {
            tvFilterAll.setText(filterText);
        }
    }

    private void updateDateTextColors(TextView tvDate, TextView tvDay, String dateString,
                                      boolean isToday, boolean isPast) {
        boolean isSelected = dateString.equals(selectedDate);
        if (isToday) {
            tvDate.setTextColor(Color.BLACK);
            tvDay.setTextColor(Color.BLACK);
        } else if (isSelected) {
            tvDate.setTextColor(Color.WHITE);
            tvDay.setTextColor(Color.WHITE);
        } else if (isPast) {
            tvDate.setTextColor(Color.parseColor("#B0B0B0"));
            tvDay.setTextColor(Color.parseColor("#B0B0B0"));
        } else {
            tvDate.setTextColor(Color.WHITE);
            tvDay.setTextColor(Color.WHITE);
        }
    }

    private void updateDateCardColors(CardView dateCard, String dateString,
                                      boolean isToday, boolean isPast) {
        boolean isSelected = dateString.equals(selectedDate);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(dpToPx(16));

        if (isToday) {
            gradientDrawable.setColor(Color.WHITE);
            gradientDrawable.setStroke(0, Color.TRANSPARENT);
            dateCard.setCardBackgroundColor(Color.WHITE);
            dateCard.setCardElevation(dpToPx(3));
        } else if (isSelected) {
            gradientDrawable.setColor(COLOR_DATE_CARD_SELECTED);
            gradientDrawable.setStroke(dpToPx(2), Color.BLACK);
            dateCard.setBackground(gradientDrawable);
            dateCard.setCardBackgroundColor(Color.TRANSPARENT);
            dateCard.setCardElevation(dpToPx(0));
        } else if (isPast) {
            gradientDrawable.setColor(COLOR_DATE_CARD_PAST);
            gradientDrawable.setStroke(0, Color.TRANSPARENT);
            dateCard.setBackground(gradientDrawable);
            dateCard.setCardBackgroundColor(Color.TRANSPARENT);
            dateCard.setCardElevation(dpToPx(0));
        } else {
            gradientDrawable.setColor(COLOR_DATE_CARD_TRANSPARENT);
            gradientDrawable.setStroke(0, Color.TRANSPARENT);
            dateCard.setBackground(gradientDrawable);
            dateCard.setCardBackgroundColor(Color.TRANSPARENT);
            dateCard.setCardElevation(dpToPx(0));
        }
    }

    private void updateAllDateCards() {
        for (int i = 0; i < dateCards.size() && i < dateStrings.size(); i++) {
            CardView card = dateCards.get(i);
            String dateStr = dateStrings.get(i); // Correct variable name is dateStr
            boolean isToday = dateStr.equals(todayDate);
            boolean isPast = isDateBefore(dateStr, todayDate);

            updateDateCardColors(card, dateStr, isToday, isPast);
            LinearLayout innerLayout = (LinearLayout) card.getChildAt(0);
            TextView tvDate = (TextView) innerLayout.getChildAt(0);
            TextView tvDay = (TextView) innerLayout.getChildAt(1);
            // FIX: Using the correct variable 'dateStr'
            updateDateTextColors(tvDate, tvDay, dateStr, isToday, isPast);
        }
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private CardView createDateCard(final String dateString, final boolean isToday,
                                    final boolean isFuture, final boolean isPast,
                                    final int position, SimpleDateFormat dayFormat,
                                    SimpleDateFormat dateFormat, SimpleDateFormat fullDateFormat,
                                    Calendar calendar) {

        CardView dateCard = new CardView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(55), dpToPx(75));
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        dateCard.setLayoutParams(params);
        dateCard.setRadius(dpToPx(16));
        dateCard.setCardElevation(dpToPx(0));
        dateCard.setPreventCornerOverlap(false);
        dateCard.setUseCompatPadding(true);

        updateDateCardColors(dateCard, dateString, isToday, isPast);

        LinearLayout innerLayout = new LinearLayout(getContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setGravity(android.view.Gravity.CENTER);
        innerLayout.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

        TextView tvDate = new TextView(getContext());
        tvDate.setText(dateFormat.format(calendar.getTime()));
        tvDate.setTextSize(18);
        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDay = new TextView(getContext());
        tvDay.setText(dayFormat.format(calendar.getTime()));
        tvDay.setTextSize(11);

        updateDateTextColors(tvDate, tvDay, dateString, isToday, isPast);

        innerLayout.addView(tvDate);
        innerLayout.addView(tvDay);
        dateCard.addView(innerLayout);

        dateCard.setOnClickListener(v -> {
            selectedDate = dateString;
            selectedDatePosition = position;
            updateAllDateCards();
            loadHabitsForSelectedDate();

            if (isToday) {
                tvTodayLabel.setText("Today");
            } else {
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fullDateFormat.parse(dateString));
                    tvTodayLabel.setText(displayFormat.format(cal.getTime()));
                } catch (Exception e) {
                    tvTodayLabel.setText(dateString);
                }
            }
        });
        return dateCard;
    }

    private void setupDateStrip() {
        if (dateStripContainer == null) return;

        dateStripContainer.removeAllViews();
        dateCards.clear();
        dateStrings.clear();

        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        calendar.add(Calendar.MONTH, -3);
        todayCardIndex = -1;
        int totalDays = 180;

        for (int i = 0; i < totalDays; i++) {
            final String dateString = fullDateFormat.format(calendar.getTime());
            final int position = i;
            final boolean isToday = dateString.equals(todayDate);
            final boolean isFuture = calendar.after(today);
            final boolean isPast = calendar.before(today) && !isToday;

            dateStrings.add(dateString);
            if (isToday) todayCardIndex = i;

            CardView dateCard = createDateCard(dateString, isToday, isFuture, isPast, position,
                    dayFormat, dateFormat, fullDateFormat, calendar);

            dateCards.add(dateCard);
            dateStripContainer.addView(dateCard);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (todayCardIndex != -1) {
            dateStripContainer.post(() -> {
                HorizontalScrollView scrollView = rootView.findViewById(R.id.dateScrollView);
                if (scrollView != null && dateStripContainer.getChildAt(todayCardIndex) != null) {
                    View targetCard = dateStripContainer.getChildAt(todayCardIndex);
                    int scrollX = targetCard.getLeft() - (scrollView.getWidth() / 2) + (targetCard.getWidth() / 2);
                    scrollView.smoothScrollTo(scrollX, 0);
                }
            });
        }
    }

    private void jumpToToday() {
        selectedDate = todayDate;
        selectedDatePosition = todayCardIndex;
        updateAllDateCards();
        loadHabitsForSelectedDate();

        if (tvTodayLabel != null) {
            tvTodayLabel.setText("Today");
        }

        if (todayCardIndex != -1) {
            HorizontalScrollView scrollView = rootView.findViewById(R.id.dateScrollView);
            if (scrollView != null && dateStripContainer.getChildAt(todayCardIndex) != null) {
                View targetCard = dateStripContainer.getChildAt(todayCardIndex);
                int scrollX = targetCard.getLeft() - (scrollView.getWidth() / 2) + (targetCard.getWidth() / 2);
                scrollView.smoothScrollTo(scrollX, 0);
            }
        }
    }

    private void loadHabitsForSelectedDate() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewHabits.getLayoutManager();
        int scrollPosition = layoutManager != null ? layoutManager.findFirstVisibleItemPosition() : 0;
        View firstVisibleView = layoutManager != null ? layoutManager.findViewByPosition(scrollPosition) : null;
        int offsetTop = (firstVisibleView == null) ? 0 : firstVisibleView.getTop();

        habitList.clear();

        List<Habit> allHabits = databaseHelper.getAllHabits();
        List<Habit> filteredHabits = new ArrayList<>();

        if (allHabits != null) {
            for (Habit habit : allHabits) {
                if (!isHabitActiveOnDate(habit, selectedDate)) {
                    continue;
                }

                boolean isCompletedOnDate = databaseHelper.isHabitCompletedOnDate(habit.getId(), selectedDate);
                boolean matchesStatus = checkStatusFilter(isCompletedOnDate);
                boolean matchesTime = checkTimeFilter(habit);

                if (matchesStatus && matchesTime) {
                    habit.setCompleted(isCompletedOnDate);
                    filteredHabits.add(habit);
                }
            }
        }

        Collections.sort(filteredHabits, new Comparator<Habit>() {
            @Override
            public int compare(Habit h1, Habit h2) {
                return Boolean.compare(h1.isCompleted(), h2.isCompleted());
            }
        });

        habitList.addAll(filteredHabits);
        habitAdapter.notifyDataSetChanged();

        // --- FIX 3: Empty State Logic (matching timeline design) ---
        if (habitList.isEmpty()) {
            recyclerViewHabits.setVisibility(View.GONE);
            llEmptyStateContainer.setVisibility(View.VISIBLE);
            tvSwipeHint.setVisibility(View.GONE);

            // Set the image and message text
            // Note: ivNoData should be linked to an image resource (like R.drawable.nodata)
            // if it exists in your project.
            // ivNoData.setImageResource(R.drawable.nodata);
            tvEmptyMessage.setText("No Habit Data");

            if (allHabits.isEmpty()) {
                tvEmptyMessage.setText("No active habits found. Tap '+' to create your first habit!");
            } else if (!selectedDate.equals(todayDate)) {
                tvEmptyMessage.setText("No active habits on " + selectedDate + ".");
            } else if (!currentStatusFilter.equals("All") || !currentTimeFilter.equals("All")) {
                tvEmptyMessage.setText("No habits match the current filters.");
            } else {
                tvEmptyMessage.setText("No Habit Data");
            }

        } else {
            recyclerViewHabits.setVisibility(View.VISIBLE);
            llEmptyStateContainer.setVisibility(View.GONE);
            tvSwipeHint.setVisibility(View.VISIBLE);
        }
        // --- END FIX 3 ---


        if (layoutManager != null && scrollPosition >= 0 && scrollPosition < habitList.size()) {
            layoutManager.scrollToPositionWithOffset(scrollPosition, offsetTop);
        }
    }

    private boolean isHabitActiveOnDate(Habit habit, String checkDate) {
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

    private boolean checkStatusFilter(boolean isCompleted) {
        if (currentStatusFilter.equals("All")) return true;
        if (currentStatusFilter.equals("Pending")) return !isCompleted;
        if (currentStatusFilter.equals("Complete")) return isCompleted;
        return false;
    }

    private boolean checkTimeFilter(Habit habit) {
        if (currentTimeFilter.equals("All")) {
            return true;
        }

        String progress = habit.getProgress();
        if (progress == null || progress.trim().isEmpty()) {
            return false;
        }

        String[] times = progress.split(",");

        for (String time : times) {
            time = time.trim();
            if (time.isEmpty()) continue;

            try {
                String[] parts = time.split(" ");
                if (parts.length != 2) continue;

                String[] timeParts = parts[0].split(":");
                if (timeParts.length != 2) continue;

                int hour = Integer.parseInt(timeParts[0]);
                String period = parts[1].toUpperCase();

                int hour24 = convertTo24Hour(hour, period);

                switch (currentTimeFilter) {
                    case "Morning":
                        if (hour24 >= 5 && hour24 < 12) return true;
                        break;
                    case "Afternoon":
                        if (hour24 >= 12 && hour24 < 17) return true;
                        break;
                    case "Evening":
                        if (hour24 >= 17 && hour24 < 22) return true;
                        break;
                    case "Night":
                        if (hour24 >= 22 || hour24 < 5) return true;
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing time: " + time, e);
            }
        }

        return false;
    }

    private int convertTo24Hour(int hour, String period) {
        if (period.equals("AM")) {
            if (hour == 12) return 0; // 12 AM is 0 hour
            return hour;
        } else {
            if (hour == 12) return 12; // 12 PM is 12 hour
            return hour + 12;
        }
    }
}