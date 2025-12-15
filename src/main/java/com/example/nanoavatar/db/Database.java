package com.example.nanoavatar.db;

import java.sql.*;

public class Database {
    private final String url;

    public Database(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
        init();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void init() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id INTEGER UNIQUE NOT NULL,
                    username TEXT,
                    balance INTEGER NOT NULL DEFAULT 15,
                    last_bonus_date TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    payload TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                );
            """);
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }
}