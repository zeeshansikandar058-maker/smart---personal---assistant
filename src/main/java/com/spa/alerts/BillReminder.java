package com.spa.alerts;

import java.time.LocalDate;

public class BillReminder {
    private final int id;
    private final int userId;
    private final String title;
    private final double amount;
    private final LocalDate dueDate;
    private final String priority; // HIGH, MEDIUM, LOW
    private final boolean paid;

    public BillReminder(int id, int userId, String title, double amount, LocalDate dueDate, String priority, boolean paid) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.amount = amount;
        this.dueDate = dueDate;
        this.priority = priority;
        this.paid = paid;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public boolean isPaid() { return paid; }

    public boolean isDueOrOverdue() {
        return !paid && !dueDate.isAfter(LocalDate.now());
    }
}
