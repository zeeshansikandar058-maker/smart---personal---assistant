package com.spa.prayer;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Holds the five daily prayer times for a given date/location. */
public class PrayerTimes {

    private final Map<String, LocalTime> times = new LinkedHashMap<>();
    private final String date;
    private final String city;

    public PrayerTimes(String date, String city) {
        this.date = date;
        this.city = city;
    }

    public void put(String name, LocalTime time) {
        times.put(name, time);
    }

    public LocalTime get(String name) {
        return times.get(name);
    }

    public Map<String, LocalTime> all() {
        return times;
    }

    public String getDate() { return date; }
    public String getCity() { return city; }
}
