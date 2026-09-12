package com.spa.gui;

import com.spa.auth.User;
import com.spa.timetable.TimetableEntry;
import com.spa.timetable.TimetableService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.util.List;

public class TimetablePanel extends JPanel {

    private final TimetableService service = new TimetableService();
    private final User user;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Day", "Time", "Title", "Location", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public TimetablePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Weekly Timetable");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        table.removeColumn(table.getColumnModel().getColumn(4)); // hide raw ID column visually
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Entry");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addBtn);
        buttons.add(deleteBtn);
        add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> showAddDialog());
        deleteBtn.addActionListener(e -> deleteSelected());

        refresh();
    }

    private void showAddDialog() {
        JComboBox<String> dayBox = new JComboBox<>(new String[]{
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"});
        JTextField timeField = new JTextField("09:00");
        JTextField titleField = new JTextField();
        JTextField locationField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Day:")); form.add(dayBox);
        form.add(new JLabel("Time (HH:mm, 24h):")); form.add(timeField);
        form.add(new JLabel("Title (subject/meeting):")); form.add(titleField);
        form.add(new JLabel("Location (optional):")); form.add(locationField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Timetable Entry",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalTime time = LocalTime.parse(timeField.getText().trim());
            if (titleField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Title cannot be empty.");
                return;
            }
            service.addEntry(user.getId(), (String) dayBox.getSelectedItem(), time,
                    titleField.getText().trim(), locationField.getText().trim());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid time format. Use HH:mm, e.g. 14:30.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int id = (int) model.getValueAt(row, 4);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this timetable entry?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            service.deleteEntry(id);
            refresh();
        }
    }

    public void refresh() {
        model.setRowCount(0);
        List<TimetableEntry> entries = service.weeklySchedule(user.getId());
        for (TimetableEntry entry : entries) {
            model.addRow(new Object[]{
                    entry.getDayOfWeek(), entry.getStartTime(), entry.getTitle(),
                    entry.getLocation() == null ? "" : entry.getLocation(), entry.getId()});
        }
    }
}
