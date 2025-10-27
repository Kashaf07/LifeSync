package com.example.lifesync.model;

public class ToDoModel {
    private int id;
    private String title;
    private String description;
    private String date;
    private String priority;
    private boolean isDone;

    public ToDoModel(int id, String title, String description, String date, String priority, boolean isDone) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.priority = priority;
        this.isDone = isDone;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getPriority() { return priority; }
    public boolean isDone() { return isDone; }

    public void setDone(boolean done) { isDone = done; }

    public int getPriorityColor() {
        if (priority == null) return 0xFFE0E0E0;
        switch (priority) {
            case "High": return 0xFFFF6B6B;
            case "Medium": return 0xFFFFA726;
            case "Low": return 0xFF4FC3F7;
            default: return 0xFFE0E0E0;
        }
    }
}