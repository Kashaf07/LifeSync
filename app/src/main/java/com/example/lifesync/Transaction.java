package com.example.lifesync;



public class Transaction {
    private int id;
    private String type;
    private double amount;
    private String category;
    private String date;
    private String description;

    public Transaction(int id, String type, double amount, String category,
                       String date, String description) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getDescription() { return description; }
}