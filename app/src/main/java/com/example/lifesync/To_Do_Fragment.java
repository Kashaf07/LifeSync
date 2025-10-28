package com.example.lifesync;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.lifesync.adapter.ToDoAdapter;
import com.example.lifesync.database.ToDoDatabaseHelper;
import com.example.lifesync.model.ToDoModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class To_Do_Fragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddTask;
    private ToDoAdapter adapter;
    private ToDoDatabaseHelper dbHelper;
    private List<ToDoModel> taskList;
    private List<ToDoModel> filteredTaskList;

    private TextView tvTotalTasks, tvPendingTasks, tvCompletedTasks;
    private LinearLayout weekContainer, emptyStateLayout;
    private Chip chipAll, chipPending, chipCompleted, chipHighPriority;

    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_do, container, false);

        initializeViews(view);

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Create notification channel
        NotificationHelper.createNotificationChannel(requireContext());

        setupRecyclerView();
        setupWeekView();
        setupFilterChips();
        loadTasks();

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        weekContainer = view.findViewById(R.id.weekContainer);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        chipAll = view.findViewById(R.id.chipAll);
        chipPending = view.findViewById(R.id.chipPending);
        chipCompleted = view.findViewById(R.id.chipCompleted);
        chipHighPriority = view.findViewById(R.id.chipHighPriority);

        dbHelper = new ToDoDatabaseHelper(requireContext());
        taskList = new ArrayList<>();
        filteredTaskList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ToDoAdapter(filteredTaskList, dbHelper, this::loadTasks);
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            ToDoModel taskToDelete = filteredTaskList.get(position);

            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Task?")
                    .setMessage("Are you sure you want to delete \"" + taskToDelete.getTitle() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete from database
                        AlarmScheduler.cancelTaskReminder(requireContext(), taskToDelete.getId());
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("tasks", "id=?", new String[]{String.valueOf(taskToDelete.getId())});
                        db.close();

                        loadTasks();
                        setupWeekView();
                        Toast.makeText(getContext(), "Task deleted 🗑️", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Restore the item
                        adapter.notifyItemChanged(position);
                    })
                    .setOnCancelListener(dialog -> {
                        // Restore on dismiss
                        adapter.notifyItemChanged(position);
                    })
                    .show();
        }
    };

    private int selectedDayIndex = -1; // Keep track of selected day

    private void setupWeekView() {
        weekContainer.removeAllViews();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String todayStr = dateFormat.format(new Date());

        for (int i = 0; i < 7; i++) {
            View dayView = LayoutInflater.from(getContext()).inflate(R.layout.item_week_day, weekContainer, false);

            TextView tvDay = dayView.findViewById(R.id.tvDay);
            TextView tvDate = dayView.findViewById(R.id.tvDate);
            TextView tvTaskCount = dayView.findViewById(R.id.tvTaskCount);

            String fullDateStr = dateFormat.format(calendar.getTime());
            tvDay.setText(dayFormat.format(calendar.getTime()));
            tvDate.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));

            // Count tasks for this day
            int taskCount = getTaskCountForDate(calendar.getTime());
            if (taskCount > 0) {
                tvTaskCount.setVisibility(View.VISIBLE);
                tvTaskCount.setText(String.valueOf(taskCount));
            } else {
                tvTaskCount.setVisibility(View.GONE);
            }

            // Highlight selected day
            if (i == selectedDayIndex) {
                dayView.setBackgroundResource(R.drawable.bg_day_selected);
                tvDate.setTextColor(Color.WHITE);
            } else {
                dayView.setBackgroundResource(R.drawable.bg_day_normal);
                tvDate.setTextColor(Color.BLACK);
            }

            int finalI = i;
            dayView.setOnClickListener(v -> {
                // Update selected index
                selectedDayIndex = finalI;
                setupWeekView(); // refresh to update highlight

                // Filter tasks for the clicked date
                filteredTaskList.clear();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor;

                // Order by unchecked first, then priority, then id descending
                String orderBy = "isDone ASC, " +
                        "CASE priority " +
                        "WHEN 'High' THEN 1 " +
                        "WHEN 'Medium' THEN 2 " +
                        "WHEN 'Low' THEN 3 " +
                        "ELSE 4 END ASC, " +
                        "id DESC";

                if (fullDateStr.equals(todayStr)) {
                    // Today: show all tasks
                    cursor = db.query("tasks", null, null, null, null, null, orderBy);
                } else {
                    // Other days: show only tasks with that date
                    cursor = db.query("tasks", null, "date=?", new String[]{fullDateStr}, null, null, orderBy);
                }

                if (cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                        String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                        String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                        String priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"));
                        boolean isDone = cursor.getInt(cursor.getColumnIndexOrThrow("isDone")) == 1;

                        filteredTaskList.add(new ToDoModel(id, title, desc, dateStr, priority, isDone));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                db.close();

                adapter.notifyDataSetChanged();
                updateEmptyState();

                if (filteredTaskList.isEmpty()) {
                    Toast.makeText(getContext(), "No deadlines today 📭", Toast.LENGTH_SHORT).show();
                }
            });

            weekContainer.addView(dayView);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    private int getTaskCountForDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = sdf.format(date);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tasks WHERE date=?", new String[]{dateStr});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }


    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> filterTasks("all"));
        chipPending.setOnClickListener(v -> filterTasks("pending"));
        chipCompleted.setOnClickListener(v -> filterTasks("completed"));
        chipHighPriority.setOnClickListener(v -> filterTasks("high"));
    }

    private void filterTasks(String filter) {
        currentFilter = filter;
        filteredTaskList.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        for (ToDoModel task : taskList) {
            switch (filter) {
                case "all":
                    filteredTaskList.add(task);
                    break;
                case "pending":
                    if (!task.isDone()) filteredTaskList.add(task);
                    break;
                case "completed":
                    if (task.isDone()) filteredTaskList.add(task);
                    break;
                case "high":
                    if (task.getPriority() != null && task.getPriority().equals("High")) {
                        filteredTaskList.add(task);
                    }
                    break;
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadTasks() {
        taskList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Order by priority (High > Medium > Low > None) and deadline
        String orderBy = "isDone ASC, " +
                "CASE priority " +
                "WHEN 'High' THEN 1 " +
                "WHEN 'Medium' THEN 2 " +
                "WHEN 'Low' THEN 3 " +
                "ELSE 4 END ASC, " +
                "id DESC";


        Cursor cursor = db.query("tasks", null, null, null, null, null, orderBy);

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
        db.close();

        filterTasks(currentFilter);
        updateStats();
        updateEmptyState();
    }

    private void updateStats() {
        int total = taskList.size();
        int completed = 0;
        int pending = 0;

        for (ToDoModel task : taskList) {
            if (task.isDone()) completed++;
            else pending++;
        }

        tvTotalTasks.setText(String.valueOf(total));
        tvPendingTasks.setText(String.valueOf(pending));
        tvCompletedTasks.setText(String.valueOf(completed));
    }

    private void updateEmptyState() {
        if (filteredTaskList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dailog_add_task, null);

        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        RadioGroup rgPriority = dialogView.findViewById(R.id.rgPriority);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        ImageView ivDescToggle = dialogView.findViewById(R.id.ivDescToggle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTask);

        final Calendar calendar = Calendar.getInstance();
        final String[] selectedDeadline = {""};

        // Toggle description field
        ivDescToggle.setOnClickListener(v -> {
            if (etDesc.getVisibility() == View.GONE) {
                etDesc.setVisibility(View.VISIBLE);
                ivDescToggle.setAlpha(1.0f);
            } else {
                etDesc.setVisibility(View.GONE);
                ivDescToggle.setAlpha(0.5f);
            }
        });

        // Priority selection with visual feedback
        rgPriority.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton rb = (RadioButton) child;
                    if (rb.getId() == checkedId) {
                        rb.setBackgroundResource(R.drawable.bg_priority_selected);
                        rb.setScaleX(1.05f);
                        rb.setScaleY(1.05f);
                    } else {
                        rb.setScaleX(1.0f);
                        rb.setScaleY(1.0f);
                        if (rb.getId() == R.id.rbHigh) rb.setBackgroundResource(R.drawable.bg_priority_high);
                        else if (rb.getId() == R.id.rbMedium) rb.setBackgroundResource(R.drawable.bg_priority_medium);
                        else if (rb.getId() == R.id.rbLow) rb.setBackgroundResource(R.drawable.bg_priority_low);
                        else rb.setBackgroundResource(R.drawable.bg_priority_none);
                    }
                }
            }
        });

        // Deadline picker
        btnSelectDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDeadline[0] = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        btnSelectDate.setText("📅 " + selectedDeadline[0]);
                        btnSelectDate.setBackgroundResource(R.drawable.bg_button_gradient);
                    }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        dialog.show();

        // Save task
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please enter task title! ✍️", Toast.LENGTH_SHORT).show();
                etTitle.requestFocus();
                return;
            }

            String priority = "None";
            int selectedId = rgPriority.getCheckedRadioButtonId();
            if (selectedId == R.id.rbHigh) priority = "High";
            else if (selectedId == R.id.rbMedium) priority = "Medium";
            else if (selectedId == R.id.rbLow) priority = "Low";

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("description", desc);
            values.put("date", selectedDeadline[0]);
            values.put("priority", priority);
            values.put("isDone", 0);

            long result = db.insert("tasks", null, values);
            db.close();

            if (result != -1) {
                Toast.makeText(getContext(), "Task saved successfully! ✅", Toast.LENGTH_SHORT).show();

                // Schedule reminder if date is set
                if (!selectedDeadline[0].isEmpty()) {
                    AlarmScheduler.scheduleTaskReminder(
                            requireContext(),
                            (int) result,
                            title,
                            selectedDeadline[0]
                    );
                }

                loadTasks();
                setupWeekView();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to save task ❌", Toast.LENGTH_SHORT).show();
            }
        });
    }
}