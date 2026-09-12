package com.spa.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central access point for the SQLite database. One connection is kept open
 * for the lifetime of the application (SQLite is file-based and does not
 * benefit from pooling the way a networked DB would).
 */
public final class DatabaseManager {

    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".smart-assistant";
    private static final String DB_FILE = DB_DIR + File.separator + "assistant.db";

    private static Connection connection;

    private DatabaseManager() {
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                new File(DB_DIR).mkdirs();
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
                initSchema();
            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException("Failed to initialize SQLite database", e);
            }
        }
        return connection;
    }

    /** Returns the folder where per-user files saved via the File Manager live. */
    public static String userFilesDir(String username) {
        String dir = DB_DIR + File.separator + "files" + File.separator + username;
        new File(dir).mkdirs();
        return dir;
    }

    private static void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('ADMIN','USER')),
                    city TEXT DEFAULT 'Karachi',
                    country TEXT DEFAULT 'Pakistan',
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS timetable (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    day_of_week TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    title TEXT NOT NULL,
                    location TEXT,
                    notified_on TEXT,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT,
                    category TEXT DEFAULT 'General',
                    source TEXT DEFAULT 'TEXT',
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS files_meta (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    file_name TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    amount REAL,
                    due_date TEXT NOT NULL,
                    priority TEXT DEFAULT 'HIGH',
                    paid INTEGER DEFAULT 0,
                    alerted INTEGER DEFAULT 0,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
            """);
        }
    }
}
