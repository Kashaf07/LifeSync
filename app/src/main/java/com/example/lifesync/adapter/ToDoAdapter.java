package com.example.lifesync.adapter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lifesync.R;
import com.example.lifesync.database.ToDoDatabaseHelper;
import com.example.lifesync.model.ToDoModel;

import java.util.Calendar;
import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.TaskViewHolder> {

    private final List<ToDoModel> taskList;
    private final ToDoDatabaseHelper dbHelper;
    private final OnTaskChangedListener listener;

    // Callback interface
    public interface OnTaskChangedListener {
        void onTaskChanged();
    }

    // Constructor with callback
    public ToDoAdapter(List<ToDoModel> taskList, ToDoDatabaseHelper dbHelper, OnTaskChangedListener listener) {
        this.taskList = taskList;
        this.dbHelper = dbHelper;
        this.listener = listener;
    }

    // Constructor without callback (for backward compatibility)
    public ToDoAdapter(List<ToDoModel> taskList, ToDoDatabaseHelper dbHelper) {
        this.taskList = taskList;
        this.dbHelper = dbHelper;
        this.listener = null;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ToDoModel model = taskList.get(position);

        holder.taskTitle.setText(model.getTitle());

        // Description
        if (model.getDescription() != null && !model.getDescription().isEmpty()) {
            holder.taskDesc.setVisibility(View.VISIBLE);
            holder.taskDesc.setText(model.getDescription());
        } else {
            holder.taskDesc.setVisibility(View.GONE);
        }

        // Priority indicator color
        holder.priorityIndicator.setBackgroundColor(model.getPriorityColor());

        // Date badge
        if (model.getDate() != null && !model.getDate().isEmpty() && !model.getDate().startsWith("Select")) {
            holder.deadlineContainer.setVisibility(View.VISIBLE);
            holder.taskDate.setText(model.getDate());
        } else {
            holder.deadlineContainer.setVisibility(View.GONE);
        }

        // Priority badge
        if (model.getPriority() != null && !model.getPriority().equals("None")) {
            holder.priorityBadge.setVisibility(View.VISIBLE);
            holder.priorityBadge.setText(model.getPriority());
            holder.priorityBadge.setBackgroundColor(model.getPriorityColor());
        } else {
            holder.priorityBadge.setVisibility(View.GONE);
        }

        // Custom checkbox
        boolean isDone = model.isDone();

        // Set initial image (checked or unchecked)
        holder.checkBox.setImageResource(isDone
                ? R.drawable.ic_checkbox
                : R.drawable.custom_checkbox);

        // Apply strike-through for completed tasks
        if (isDone) {
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskTitle.setAlpha(0.6f);
            holder.taskDesc.setAlpha(0.6f);
        } else {
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.taskTitle.setAlpha(1.0f);
            holder.taskDesc.setAlpha(1.0f);
        }

        // Handle toggle click on ImageView
        holder.checkBox.setOnClickListener(v -> {
            boolean newState = !model.isDone();
            model.setDone(newState);

            // Update icon immediately
            holder.checkBox.setImageResource(newState
                    ? R.drawable.ic_checkbox
                    : R.drawable.custom_checkbox);

            // Update strike-through
            if (newState) {
                holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.taskTitle.setAlpha(0.6f);
                holder.taskDesc.setAlpha(0.6f);
            } else {
                holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.taskTitle.setAlpha(1.0f);
                holder.taskDesc.setAlpha(1.0f);
            }

            // Update DB
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("isDone", newState ? 1 : 0);
            db.update("tasks", cv, "id=?", new String[]{String.valueOf(model.getId())});
            db.close();

            // Notify fragment for refresh (optional)
            if (listener != null) listener.onTaskChanged();
        });


        // Edit button click
        holder.ivMoreOptions.setOnClickListener(v -> showEditDialog(holder, model, position));
    }

    private void showEditDialog(TaskViewHolder holder, ToDoModel model, int position) {
        // Use the full add task dialog for editing
        View dialogView = LayoutInflater.from(holder.itemView.getContext())
                .inflate(R.layout.dailog_add_task, null);

        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        RadioGroup rgPriority = dialogView.findViewById(R.id.rgPriority);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        ImageView ivDescToggle = dialogView.findViewById(R.id.ivDescToggle);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTask);

        // Pre-fill data
        etTitle.setText(model.getTitle());
        etDesc.setText(model.getDescription());

        // Show description if it exists
        if (model.getDescription() != null && !model.getDescription().isEmpty()) {
            etDesc.setVisibility(View.VISIBLE);
            ivDescToggle.setAlpha(1.0f);
        }

        // Set date
        if (model.getDate() != null && !model.getDate().isEmpty()) {
            btnSelectDate.setText("📅 " + model.getDate());
        }

        // Set priority
        if (model.getPriority() != null) {
            switch (model.getPriority()) {
                case "High":
                    rgPriority.check(R.id.rbHigh);
                    break;
                case "Medium":
                    rgPriority.check(R.id.rbMedium);
                    break;
                case "Low":
                    rgPriority.check(R.id.rbLow);
                    break;
                default:
                    rgPriority.check(R.id.rbNone);
                    break;
            }
        }

        final String[] selectedDeadline = {model.getDate() != null ? model.getDate() : ""};
        final Calendar calendar = Calendar.getInstance();

        // Description toggle
        ivDescToggle.setOnClickListener(v -> {
            if (etDesc.getVisibility() == View.GONE) {
                etDesc.setVisibility(View.VISIBLE);
                ivDescToggle.setAlpha(1.0f);
            } else {
                etDesc.setVisibility(View.GONE);
                ivDescToggle.setAlpha(0.5f);
            }
        });

        // Date picker
        btnSelectDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(holder.itemView.getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDeadline[0] = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        btnSelectDate.setText("📅 " + selectedDeadline[0]);
                    }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Edit Task ✏️")
                .setView(dialogView)
                .create();

        dialog.show();

        // Update task
        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            String newDesc = etDesc.getText().toString().trim();

            if (newTitle.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(), "Title can't be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            String priority = "None";
            int selectedId = rgPriority.getCheckedRadioButtonId();
            if (selectedId == R.id.rbHigh) priority = "High";
            else if (selectedId == R.id.rbMedium) priority = "Medium";
            else if (selectedId == R.id.rbLow) priority = "Low";

            // Update database
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("title", newTitle);
            cv.put("description", newDesc);
            cv.put("date", selectedDeadline[0]);
            cv.put("priority", priority);
            db.update("tasks", cv, "id=?", new String[]{String.valueOf(model.getId())});
            db.close();

            // Notify fragment to reload tasks
            if (listener != null) {
                listener.onTaskChanged();
            }

            Toast.makeText(holder.itemView.getContext(), "Task updated! ✅", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDesc, taskDate, priorityBadge;
        ImageView checkBox;
        View priorityIndicator;
        LinearLayout deadlineContainer;
        ImageView ivMoreOptions;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDesc = itemView.findViewById(R.id.taskDesc);
            taskDate = itemView.findViewById(R.id.taskDate);
            priorityBadge = itemView.findViewById(R.id.priorityBadge);
            checkBox = itemView.findViewById(R.id.checkBox);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            deadlineContainer = itemView.findViewById(R.id.deadlineContainer);
            ivMoreOptions = itemView.findViewById(R.id.ivMoreOptions);
        }
    }
}