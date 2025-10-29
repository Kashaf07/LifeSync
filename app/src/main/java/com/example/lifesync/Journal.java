package com.example.lifesync;

public class Journal {
    private int id;
    private String title;
    private String content;
    private String date;
    private boolean isFavorite;
    private boolean isPrivate;
    private boolean isDraft;

    // New fields for advanced features
    private String imageUri;
    private long reminderTime; // Stored as milliseconds since epoch
    private String audioPath;

    // Constructor for creating a new journal object
    public Journal(String title, String content, String date, boolean isFavorite, boolean isPrivate, boolean isDraft, String imageUri, long reminderTime, String audioPath) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.isFavorite = isFavorite;
        this.isPrivate = isPrivate;
        this.isDraft = isDraft;
        this.imageUri = imageUri;
        // NOTE: Possible typo in original code was here: this.reminderTime = this.reminderTime;
        // Corrected to:
        this.reminderTime = reminderTime;
        this.audioPath = audioPath;
    }

    // Constructor for reading a journal object from the database
    public Journal(int id, String title, String content, String date, boolean isFavorite, boolean isPrivate, boolean isDraft, String imageUri, long reminderTime, String audioPath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.isFavorite = isFavorite;
        this.isPrivate = isPrivate;
        this.isDraft = isDraft;
        this.imageUri = imageUri;
        this.reminderTime = reminderTime;
        this.audioPath = audioPath;
    }


    // --- Getters and Setters for all fields ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setDraft(boolean draft) {
        isDraft = draft;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }
}