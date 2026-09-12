package com.spa.auth;

import com.spa.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {

    /**
     * Registers a new account. The very first account ever created becomes
     * ADMIN automatically (bootstrap); every subsequent self-registration is
     * a USER. Admins can later promote/create more admins if desired.
     */
    public Optional<String> register(String username, String password, String city, String country) {
        if (username == null || username.isBlank() || password == null || password.length() < 4) {
            return Optional.of("Username required and password must be at least 4 characters.");
        }
        Connection conn = DatabaseManager.getConnection();
        try {
            PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                return Optional.of("Username already exists.");
            }

            boolean isFirstUser = countUsers(conn) == 0;
            String role = isFirstUser ? "ADMIN" : "USER";

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash(password, salt);

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, salt, role, city, country) VALUES (?,?,?,?,?,?)");
            insert.setString(1, username);
            insert.setString(2, hash);
            insert.setString(3, salt);
            insert.setString(4, role);
            insert.setString(5, city == null || city.isBlank() ? "Karachi" : city);
            insert.setString(6, country == null || country.isBlank() ? "Pakistan" : country);
            insert.executeUpdate();
            return Optional.empty();
        } catch (SQLException e) {
            return Optional.of("Database error: " + e.getMessage());
        }
    }

    public Optional<User> login(String username, String password) {
        Connection conn = DatabaseManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();

            String hash = rs.getString("password_hash");
            String salt = rs.getString("salt");
            if (!PasswordUtil.verify(password, salt, hash)) return Optional.empty();

            User user = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    User.Role.valueOf(rs.getString("role")),
                    rs.getString("city"),
                    rs.getString("country"));
            return Optional.of(user);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<User> listAllUsers() {
        List<User> users = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY id")) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        User.Role.valueOf(rs.getString("role")),
                        rs.getString("city"),
                        rs.getString("country")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    /** Admin-only: delete a user account and cascade-delete their data. */
    public void deleteUser(int userId) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Admin-only: update a user's saved city/country for prayer-time lookups. */
    public void updateUserLocation(int userId, String city, String country) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET city = ?, country = ? WHERE id = ?")) {
            ps.setString(1, city);
            ps.setString(2, country);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int countUsers(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
