package com.example.lifesync;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalenderHabitAdapter extends RecyclerView.Adapter<CalenderHabitAdapter.ViewHolder> {

    private final List<CalendarHabitItem> items;
    private final int selectedHabitId;

    // Timeline and text colors
    private static final int COLOR_TIMELINE_ACCENT = Color.parseColor("#EDACAC");
    private static final int COLOR_TEXT_DARK = Color.parseColor("#2D3748");

    public CalenderHabitAdapter(List<CalendarHabitItem> items, int selectedHabitId) {
        this.items = items;
        this.selectedHabitId = selectedHabitId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_habit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarHabitItem item = items.get(position);
        boolean isMultiHabitView = selectedHabitId == -1;

        int habitColor = Color.parseColor("#E8F4F8");
        try {
            habitColor = Color.parseColor(item.getHabitColor());
        } catch (Exception ignored) { }

        holder.tvHabitName.setText(item.getHabitName());
        holder.cardView.setCardBackgroundColor(habitColor);
        holder.tvHabitName.setTextColor(COLOR_TEXT_DARK);
        holder.tvEmoji.setText("😊");
        holder.tvEmoji.setTextColor(COLOR_TEXT_DARK);
        holder.ivCheck.setVisibility(View.GONE);

        // --- DOT Logic ---
        if (item.isCompleted()) {
            GradientDrawable accentCircle = new GradientDrawable();
            accentCircle.setShape(GradientDrawable.OVAL);
            accentCircle.setColor(COLOR_TIMELINE_ACCENT);
            holder.ivTimelineIndicator.setBackground(accentCircle);
            holder.ivTimelineIndicator.setImageResource(0);
            holder.ivTimelineIndicator.setColorFilter(null);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                holder.ivTimelineIndicator.setElevation(dpToPx(holder.itemView.getContext(), 4));
            }
            holder.ivTimelineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.ivTimelineIndicator.setVisibility(View.INVISIBLE);
        }

        // --- Line Logic ---
        if (isMultiHabitView) {
            holder.lineTop.setBackgroundColor(COLOR_TIMELINE_ACCENT);
            holder.lineBottom.setBackgroundColor(COLOR_TIMELINE_ACCENT);
            holder.timelinePointContainer.setVisibility(View.VISIBLE);

            if (position == 0) {
                holder.lineTop.setVisibility(View.INVISIBLE);
            } else {
                holder.lineTop.setVisibility(View.VISIBLE);
            }

            if (position == items.size() - 1) {
                holder.lineBottom.setVisibility(View.INVISIBLE);
            } else {
                holder.lineBottom.setVisibility(View.VISIBLE);
            }

        } else {
            holder.lineTop.setVisibility(View.GONE);
            holder.lineBottom.setVisibility(View.GONE);
            holder.timelinePointContainer.setVisibility(item.isCompleted() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvEmoji;
        TextView tvHabitName;
        ImageView ivCheck;
        FrameLayout timelinePointContainer;
        View lineTop;
        View lineBottom;
        ImageView ivTimelineIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            ivCheck = itemView.findViewById(R.id.ivCheck);
            timelinePointContainer = itemView.findViewById(R.id.timeline_point_container);
            lineTop = itemView.findViewById(R.id.lineTop);
            lineBottom = itemView.findViewById(R.id.lineBottom);
            ivTimelineIndicator = itemView.findViewById(R.id.ivTimelineIndicator);
        }
    }
}
