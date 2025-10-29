package com.example.lifesync;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habitList;
    private Context context;
    private OnHabitClickListener clickListener;

    // ✅ NEW INTERFACE - Used by Habit_Fragment
    public interface OnHabitClickListener {
        void onEditClick(Habit habit);
        void onDeleteClick(Habit habit);
    }

    // ✅ CONSTRUCTOR for Habit_Fragment
    public HabitAdapter(List<Habit> habitList, Context context, OnHabitClickListener clickListener) {
        this.habitList = habitList;
        this.context = context;
        this.clickListener = clickListener;
    }

    // OLD INTERFACE - Keep for backward compatibility if used elsewhere
    public interface OnHabitActionListener {
        void onHabitMarkComplete(Habit habit, int position);
        void onHabitEditClick(Habit habit, int position);
    }

    // OLD CONSTRUCTOR - Keep for backward compatibility
    private OnHabitActionListener actionListener;

    public HabitAdapter(List<Habit> habitList, Context context, OnHabitActionListener listener) {
        this.habitList = habitList;
        this.context = context;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);

        holder.tvHabitName.setText(habit.getName());

        // Card styling based on completion status
        if (habit.isCompleted()) {
            try {
                holder.habitCard.setCardBackgroundColor(Color.parseColor(habit.getColor()));
            } catch (Exception e) {
                holder.habitCard.setCardBackgroundColor(Color.parseColor("#A7C7E7"));
            }
            holder.tvEmojiIcon.setText("😊");
        } else {
            holder.habitCard.setCardBackgroundColor(Color.WHITE);
            holder.tvEmojiIcon.setText("😞");
        }

        holder.tvHabitName.setTextColor(Color.BLACK);
        holder.tvHabitName.setTypeface(null, android.graphics.Typeface.BOLD);

        // Show date range if available
        String dateRangeText = getDateRangeText(habit);
        if (dateRangeText != null && !dateRangeText.isEmpty()) {
            holder.tvHabitProgress.setVisibility(View.VISIBLE);
            holder.tvHabitProgress.setText(dateRangeText);
            holder.tvHabitProgress.setTextColor(Color.parseColor("#666666"));
            holder.tvHabitProgress.setTextSize(12);
        } else {
            holder.tvHabitProgress.setVisibility(View.GONE);
        }

        // ✅ Handle clicks based on which listener is set
        if (clickListener != null) {
            // For Habit_Fragment - card deletes, pencil edits
            holder.habitCard.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    clickListener.onDeleteClick(habit);
                }
            });

            holder.ivEditPencil.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    clickListener.onEditClick(habit);
                }
            });
        } else if (actionListener != null) {
            // For other fragments using OnHabitActionListener
            holder.habitCard.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    actionListener.onHabitMarkComplete(habit, adapterPosition);
                }
            });

            holder.ivEditPencil.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    actionListener.onHabitEditClick(habit, adapterPosition);
                }
            });
        }
    }

    private String getDateRangeText(Habit habit) {
        String startDate = habit.getDate();
        String endDate = habit.getEndDate();

        if (startDate == null || startDate.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            Date start = inputFormat.parse(startDate);
            if (start != null) {
                String formattedStart = outputFormat.format(start);

                if (endDate != null && !endDate.isEmpty()) {
                    Date end = inputFormat.parse(endDate);
                    if (end != null) {
                        String formattedEnd = outputFormat.format(end);
                        return "📅 " + formattedStart + " - " + formattedEnd;
                    }
                }

                return "📅 From " + formattedStart;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        CardView habitCard;
        TextView tvEmojiIcon;
        TextView tvHabitName;
        TextView tvHabitProgress;
        ImageView ivEditPencil;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            habitCard = itemView.findViewById(R.id.habitCard);
            tvEmojiIcon = itemView.findViewById(R.id.tvEmojiIcon);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvHabitProgress = itemView.findViewById(R.id.tvHabitProgress);
            ivEditPencil = itemView.findViewById(R.id.ivEditPencil);
        }
    }
}