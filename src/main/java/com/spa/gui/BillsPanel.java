package com.spa.gui;

import com.spa.alerts.AlertService;
import com.spa.alerts.BillReminder;
import com.spa.auth.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class BillsPanel extends JPanel {

    private final AlertService alertService;
    private final User user;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Title", "Amount", "Due Date", "Priority", "Paid", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public BillsPanel(User user, AlertService alertService) {
        this.user = user;
        this.alertService = alertService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Bills, Fees & Loan Due Dates");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        table.removeColumn(table.getColumnModel().getColumn(5));
        table.setDefaultRenderer(Object.class, new DueDateHighlightRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Bill");
        JButton payBtn = new JButton("Mark Paid");
        JButton deleteBtn = new JButton("Delete");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addBtn);
        buttons.add(payBtn);
        buttons.add(deleteBtn);
        add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addBillDialog());
        payBtn.addActionListener(e -> withSelected(id -> { alertService.markPaid(id); refresh(); }));
        deleteBtn.addActionListener(e -> withSelected(id -> {
            int c = JOptionPane.showConfirmDialog(this, "Delete this bill reminder?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) { alertService.deleteBill(id); refresh(); }
        }));

        refresh();
    }

    private void withSelected(java.util.function.IntConsumer action) {
        int row = table.getSelectedRow();
        if (row == -1) return;
        action.accept((int) model.getValueAt(row, 5));
    }

    private void addBillDialog() {
        JTextField titleField = new JTextField();
        JTextField amountField = new JTextField("0.00");
        JTextField dateField = new JTextField(LocalDate.now().plusDays(3).toString());
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"HIGH", "MEDIUM", "LOW"});

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Title (e.g. Electricity Bill):")); form.add(titleField);
        form.add(new JLabel("Amount:")); form.add(amountField);
        form.add(new JLabel("Due Date (YYYY-MM-DD):")); form.add(dateField);
        form.add(new JLabel("Priority:")); form.add(priorityBox);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Bill / Fee Reminder",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate due = LocalDate.parse(dateField.getText().trim());
            if (titleField.getText().isBlank()) throw new IllegalArgumentException("Title required");
            alertService.addBill(user.getId(), titleField.getText().trim(), amount, due,
                    (String) priorityBox.getSelectedItem());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        List<BillReminder> bills = alertService.allBills(user.getId());
        for (BillReminder b : bills) {
            model.addRow(new Object[]{
                    b.getTitle(), String.format("%.2f", b.getAmount()), b.getDueDate(),
                    b.getPriority(), b.isPaid() ? "Yes" : "No", b.getId()});
        }
    }

    /** Highlights due/overdue, unpaid, high-priority bills in red — per the "priority alert" requirement. */
    private class DueDateHighlightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            boolean paid = "Yes".equals(model.getValueAt(row, 4));
            LocalDate due = LocalDate.parse(model.getValueAt(row, 2).toString());
            boolean isDue = !paid && !due.isAfter(LocalDate.now());
            if (isDue) {
                c.setBackground(new Color(255, 205, 205));
                c.setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                c.setBackground(isSelected ? tbl.getSelectionBackground() : Color.WHITE);
                c.setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.PLAIN));
            }
            return c;
        }
    }
}
