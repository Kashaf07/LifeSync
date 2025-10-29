package com.example.lifesync;

public class Habit {

    private int id;
    private String name;
    private boolean completed;
    private String color;
    private String progress;
    private String date;  // Start date
    private String endDate;  // End date

    // Default constructor
    public Habit() {
    }

    // Constructor without ID (for creating new habits)
    public Habit(String name, boolean completed, String color, String progress) {
        this.name = name;
        this.completed = completed;
        this.color = color;
        this.progress = progress;
    }

    // Constructor with ID and date (backward compatible)
    public Habit(int id, String name, boolean completed, String color, String progress, String date) {
        this.id = id;
        this.name = name;
        this.completed = completed;
        this.color = color;
        this.progress = progress;
        this.date = date;
    }

    // Full constructor with end date
    public Habit(int id, String name, boolean completed, String color, String progress, String date, String endDate) {
        this.id = id;
        this.name = name;
        this.completed = completed;
        this.color = color;
        this.progress = progress;
        this.date = date;
        this.endDate = endDate;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
