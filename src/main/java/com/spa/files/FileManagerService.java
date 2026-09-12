package com.spa.files;

import com.spa.db.DatabaseManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class FileManagerService {

    public static class FileRecord {
        public final int id;
        public final String fileName;
        public final String filePath;
        public FileRecord(int id, String fileName, String filePath) {
            this.id = id; this.fileName = fileName; this.filePath = filePath;
        }
        @Override public String toString() { return fileName; }
    }

    /** Saves plain-text content as a new file owned by the user and records its metadata. */
    public FileRecord saveTextFile(int userId, String username, String fileName, String content) throws IOException {
        String dir = DatabaseManager.userFilesDir(username);
        Path path = Path.of(dir, fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8);

        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO files_meta (user_id, file_name, file_path) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            ps.setString(3, path.toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int id = keys.next() ? keys.getInt(1) : -1;
            return new FileRecord(id, fileName, path.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String readFile(FileRecord record) throws IOException {
        return Files.readString(Path.of(record.filePath), StandardCharsets.UTF_8);
    }

    public List<FileRecord> listFiles(int userId) {
        List<FileRecord> files = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM files_meta WHERE user_id = ? ORDER BY created_at DESC")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                files.add(new FileRecord(rs.getInt("id"), rs.getString("file_name"), rs.getString("file_path")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    /**
     * Deletes a file, but ONLY after {@code confirmation} returns true.
     * This enforces the "confirmation step before any delete" requirement
     * for both GUI button clicks and voice-driven "delete this file" commands.
     */
    public boolean deleteFile(FileRecord record, BooleanSupplier confirmation) throws IOException {
        if (!confirmation.getAsBoolean()) {
            return false;
        }
        Files.deleteIfExists(Path.of(record.filePath));
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM files_meta WHERE id = ?")) {
            ps.setInt(1, record.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public FileRecord findByName(int userId, String fileName) {
        for (FileRecord f : listFiles(userId)) {
            if (f.fileName.equalsIgnoreCase(fileName)) return f;
        }
        return null;
    }
}
