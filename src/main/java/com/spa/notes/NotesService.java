package com.spa.notes;

import com.spa.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotesService {

    public void addNote(int userId, String title, String content, String category, String source) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notes (user_id, title, content, category, source) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, category == null || category.isBlank() ? "General" : category);
            ps.setString(5, source == null ? "TEXT" : source);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteNote(int noteId) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Note> allNotes(int userId) {
        return search(userId, null, null);
    }

    /** Search by free-text keyword (title/content) and/or category. Either may be null. */
    public List<Note> search(int userId, String keyword, String category) {
        List<Note> notes = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM notes WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR content LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        sql.append(" ORDER BY created_at DESC");

        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notes.add(new Note(
                        rs.getInt("id"), rs.getInt("user_id"), rs.getString("title"),
                        rs.getString("content"), rs.getString("category"),
                        rs.getString("source"), rs.getString("created_at")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }
}
