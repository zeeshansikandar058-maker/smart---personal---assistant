package com.spa.timetable;

import java.time.LocalTime;

public class TimetableEntry {
    private final int id;
    private final int userId;
    private final String dayOfWeek; // MONDAY, TUESDAY, ...
    private final LocalTime startTime;
    private final String title;
    private final String location;

    public TimetableEntry(int id, int userId, String dayOfWeek, LocalTime startTime, String title, String location) {
        this.id = id;
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.title = title;
        this.location = location;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
}
