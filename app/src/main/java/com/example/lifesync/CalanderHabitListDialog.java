package com.example.lifesync;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalanderHabitListDialog extends DialogFragment {
    private List<Habit> habits;
    private int selectedHabitId;
    private OnHabitSelectedListener listener;

    // Interface remains the same
    public interface OnHabitSelectedListener {
        void onHabitSelected(int habitId);
    }

    public CalanderHabitListDialog() {
        // Required empty public constructor
    }

    /**
     * ✅ Fixed: Factory method with correct class name and return type
     */
    public static CalanderHabitListDialog newInstance(List<Habit> habits, int selectedHabitId) {
        CalanderHabitListDialog fragment = new CalanderHabitListDialog();
        Bundle args = new Bundle();
        args.putInt("selected_habit_id", selectedHabitId);
        fragment.setArguments(args);
        fragment.setHabits(habits); // ✅ Pass list via setter
        return fragment;
    }

    public void setOnHabitSelectedListener(OnHabitSelectedListener listener) {
        this.listener = listener;
    }

    public void setHabits(List<Habit> habits) {
        this.habits = habits;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_calender_habit_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedHabitId = getArguments().getInt("selected_habit_id", -1);
        }

        RecyclerView recyclerView = view.findViewById(R.id.rvHabitList);
        TextView tvClose = view.findViewById(R.id.tvClose);

        Context context = getContext();
        if (context == null || habits == null) {
            dismiss();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new HabitListAdapter(habits, selectedHabitId, habitId -> {
            if (listener != null) {
                listener.onHabitSelected(habitId);
            }
            dismiss();
        }));

        tvClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Inner RecyclerView Adapter
     */
    private static class HabitListAdapter extends RecyclerView.Adapter<HabitListAdapter.ViewHolder> {

        private List<Habit> habits;
        private int selectedHabitId;
        private OnHabitSelectedListener listener;

        public HabitListAdapter(List<Habit> habits, int selectedHabitId, OnHabitSelectedListener listener) {
            this.habits = habits;
            this.selectedHabitId = selectedHabitId;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_habit_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Habit habit = habits.get(position);
            holder.tvHabitName.setText(habit.getName());

            try {
                holder.cardView.setCardBackgroundColor(Color.parseColor(habit.getColor()));
            } catch (Exception e) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F4F8"));
            }

            if (habit.getId() == selectedHabitId) {
                holder.ivSelected.setVisibility(View.VISIBLE);
                holder.cardView.setCardElevation(8f);
                holder.tvHabitName.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.ivSelected.setVisibility(View.GONE);
                holder.cardView.setCardElevation(3f);
                holder.tvHabitName.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            holder.cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHabitSelected(habit.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return habits.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView tvHabitName;
            ImageView ivSelected;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                tvHabitName = itemView.findViewById(R.id.tvHabitName);
                ivSelected = itemView.findViewById(R.id.ivSelected);
            }
        }
    }
}
