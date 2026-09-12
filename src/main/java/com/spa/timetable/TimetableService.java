package com.spa.timetable;

import com.spa.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimetableService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public void addEntry(int userId, String dayOfWeek, LocalTime startTime, String title, String location) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO timetable (user_id, day_of_week, start_time, title, location) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, dayOfWeek.toUpperCase());
            ps.setString(3, startTime.format(TIME_FMT));
            ps.setString(4, title);
            ps.setString(5, location);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteEntry(int entryId) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM timetable WHERE id = ?")) {
            ps.setInt(1, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TimetableEntry> weeklySchedule(int userId) {
        List<TimetableEntry> list = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        String sql = "SELECT * FROM timetable WHERE user_id = ? ORDER BY " +
                "CASE day_of_week " +
                "WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 " +
                "WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 " +
                "WHEN 'SUNDAY' THEN 7 END, start_time";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /** Entries scheduled for today that fall within the given minute window and haven't been notified yet. */
    public List<TimetableEntry> dueNow(int userId) {
        List<TimetableEntry> due = new ArrayList<>();
        String today = LocalDate.now().getDayOfWeek().name();
        String nowStr = LocalTime.now().withSecond(0).withNano(0).format(TIME_FMT);
        Connection conn = DatabaseManager.getConnection();
        String sql = "SELECT * FROM timetable WHERE user_id = ? AND day_of_week = ? AND start_time = ? " +
                "AND (notified_on IS NULL OR notified_on != ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, today);
            ps.setString(3, nowStr);
            ps.setString(4, LocalDate.now().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                due.add(map(rs));
            }
            // mark as notified for today so it doesn't repeat
            try (PreparedStatement mark = conn.prepareStatement(
                    "UPDATE timetable SET notified_on = ? WHERE user_id = ? AND day_of_week = ? AND start_time = ?")) {
                mark.setString(1, LocalDate.now().toString());
                mark.setInt(2, userId);
                mark.setString(3, today);
                mark.setString(4, nowStr);
                mark.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return due;
    }

    private TimetableEntry map(ResultSet rs) throws SQLException {
        return new TimetableEntry(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("day_of_week"),
                LocalTime.parse(rs.getString("start_time"), TIME_FMT),
                rs.getString("title"),
                rs.getString("location"));
    }
}
