package com.spa.alerts;

import com.spa.db.DatabaseManager;
import com.spa.tts.TextToSpeechService;
import com.spa.util.NotificationUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages bill/fee/loan reminders and fires PRIORITY alerts (spoken aloud +
 * desktop notification + red highlight callback to the GUI) when something
 * becomes due.
 */
public class AlertService {

    private final TextToSpeechService tts;

    public AlertService(TextToSpeechService tts) {
        this.tts = tts;
    }

    public void addBill(int userId, String title, double amount, LocalDate dueDate, String priority) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bills (user_id, title, amount, due_date, priority) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setDouble(3, amount);
            ps.setString(4, dueDate.toString());
            ps.setString(5, priority == null ? "HIGH" : priority);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markPaid(int billId) {
        update(billId, "UPDATE bills SET paid = 1 WHERE id = ?");
    }

    public void deleteBill(int billId) {
        update(billId, "DELETE FROM bills WHERE id = ?");
    }

    private void update(int billId, String sql) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<BillReminder> allBills(int userId) {
        List<BillReminder> bills = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM bills WHERE user_id = ? ORDER BY due_date")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bills.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return bills;
    }

    /**
     * Checks for bills due today/overdue that haven't been alerted yet, fires
     * the multi-channel priority alert for each, and invokes {@code onAlert}
     * so the GUI can highlight them in red.
     */
    public void checkAndFireDueAlerts(int userId, Consumer<BillReminder> onAlert) {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM bills WHERE user_id = ? AND paid = 0 AND alerted = 0 AND due_date <= ?")) {
            ps.setInt(1, userId);
            ps.setString(2, LocalDate.now().toString());
            ResultSet rs = ps.executeQuery();
            List<BillReminder> due = new ArrayList<>();
            while (rs.next()) {
                due.add(map(rs));
            }
            for (BillReminder bill : due) {
                fireAlert(bill);
                onAlert.accept(bill);
                markAlerted(bill.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void fireAlert(BillReminder bill) {
        String message = String.format("Priority alert: %s of amount %.2f is due on %s.",
                bill.getTitle(), bill.getAmount(), bill.getDueDate());
        NotificationUtil.showNotification("Priority Bill Alert", message);
        tts.speak(message);
    }

    private void markAlerted(int billId) {
        update(billId, "UPDATE bills SET alerted = 1 WHERE id = ?");
    }

    private BillReminder map(ResultSet rs) throws SQLException {
        return new BillReminder(
                rs.getInt("id"), rs.getInt("user_id"), rs.getString("title"),
                rs.getDouble("amount"), LocalDate.parse(rs.getString("due_date")),
                rs.getString("priority"), rs.getInt("paid") == 1);
    }
}
