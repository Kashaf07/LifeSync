package com.example.lifesync; // Package updated to com.example.lifesync

/**
 * Data model for a single habit item displayed in the timeline/calendar view.
 */
public class CalendarHabitItem {
    private String habitName;
    private String habitColor;
    private boolean completed;

    public CalendarHabitItem(String habitName, String habitColor, boolean completed) {
        this.habitName = habitName;
        this.habitColor = habitColor;
        this.completed = completed;
    }

    /**
     * Gets the name of the habit.
     */
    public String getHabitName() {
        return habitName;
    }

    /**
     * Gets the color code (e.g., "#FFE5D9") associated with the habit.
     */
    public String getHabitColor() {
        return habitColor;
    }

    /**
     * Checks if the habit was completed for the specific date.
     */
    public boolean isCompleted() {
        return completed;
    }
}
