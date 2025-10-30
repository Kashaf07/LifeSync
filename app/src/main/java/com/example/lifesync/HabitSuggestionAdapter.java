package com.example.lifesync;

import android.graphics.Color; // Added import for styling
import android.graphics.Typeface; // Added import for styling
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class HabitSuggestionAdapter extends RecyclerView.Adapter<HabitSuggestionAdapter.HabitViewHolder> {

    private final List<String> habitList;
    private final OnHabitClickListener listener;

    // Interface to handle clicks back in the Activity/Fragment
    public interface OnHabitClickListener {
        void onHabitClick(String habitName);
    }

    public HabitSuggestionAdapter(List<String> habitList, OnHabitClickListener listener) {
        // Use a mutable list internally to allow for dynamic filtering
        this.habitList = new ArrayList<>(habitList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Simple layout: just a TextView, styled as a clickable item
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new HabitViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        String habit = habitList.get(position);
        holder.textView.setText(habit);

        // Custom styling for a better look

        // 1. Set text to BOLD
        holder.textView.setTypeface(null, Typeface.BOLD);

        // 2. Set text color
        // Note: Using explicit color int from Context for compatibility
        holder.textView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));

        // 3. Set padding
        holder.textView.setPadding(32, 20, 32, 20);

        holder.itemView.setOnClickListener(v -> listener.onHabitClick(habit));
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    // PUBLIC METHOD to update the list for filtering/top 10 display
    public void updateList(List<String> newList) {
        habitList.clear();
        habitList.addAll(newList);
        notifyDataSetChanged();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public HabitViewHolder(@NonNull TextView itemView) {
            super(itemView);
            this.textView = itemView;
        }
    }
}
