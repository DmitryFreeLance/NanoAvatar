package com.example.nanoavatar.user;

import com.example.nanoavatar.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Database db;

    // ✅ Стартовый баланс
    private static final int INITIAL_BALANCE = 15;

    public UserService(Database db) {
        this.db = db;
    }

    public boolean ensureUser(long chatId, String username) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM users WHERE chat_id = ?")) {
                ps.setLong(1, chatId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (username != null) {
                            try (PreparedStatement up = conn.prepareStatement(
                                    "UPDATE users SET username = ? WHERE chat_id = ?")) {
                                up.setString(1, username);
                                up.setLong(2, chatId);
                                up.executeUpdate();
                            }
                        }
                        return false;
                    }
                }
            }

            // ✅ Было 10, стало 15
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO users(chat_id, username, balance) VALUES(?, ?, ?)")) {
                insert.setLong(1, chatId);
                insert.setString(2, username);
                insert.setInt(3, INITIAL_BALANCE);
                insert.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getUserId(long chatId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE chat_id = ?")) {
            ps.setLong(1, chatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("User not found for chatId=" + chatId);
    }

    public int getBalance(long chatId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT balance FROM users WHERE chat_id = ?")) {
            ps.setLong(1, chatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void changeBalance(long chatId, int amount, String type, String payload) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            int userId = getUserId(chatId);

            try (PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE users SET balance = balance + ? WHERE id = ?")) {
                ps1.setInt(1, amount);
                ps1.setInt(2, userId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO transactions(user_id, type, amount, payload) VALUES(?, ?, ?, ?)")) {
                ps2.setInt(1, userId);
                ps2.setString(2, type);
                ps2.setInt(3, amount);
                ps2.setString(4, payload);
                ps2.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLastBonusDate(long chatId, LocalDate date) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET last_bonus_date=? WHERE chat_id=?")) {
            ps.setString(1, date.toString());
            ps.setLong(2, chatId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public LocalDate getLastBonusDate(long chatId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_bonus_date FROM users WHERE chat_id=?")) {
            ps.setLong(1, chatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    if (v != null) return LocalDate.parse(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Long> getAllChatIds() {
        List<Long> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT chat_id FROM users")) {
            while (rs.next()) list.add(rs.getLong(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}