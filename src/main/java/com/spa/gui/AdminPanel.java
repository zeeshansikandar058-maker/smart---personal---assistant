package com.spa.gui;

import com.spa.auth.AuthService;
import com.spa.auth.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminPanel extends JPanel {

    private final AuthService authService = new AuthService();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Username", "Role", "City", "Country", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public AdminPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Admin — User Management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        table.removeColumn(table.getColumnModel().getColumn(4));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton editLocationBtn = new JButton("Edit Selected User's Location");
        JButton deleteBtn = new JButton("Delete Selected User");
        JButton refreshBtn = new JButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(editLocationBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        editLocationBtn.addActionListener(e -> editLocation());
        deleteBtn.addActionListener(e -> deleteUser());
        refreshBtn.addActionListener(e -> refresh());

        refresh();
    }

    private void editLocation() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int id = (int) model.getValueAt(row, 4);
        String currentCity = (String) model.getValueAt(row, 2);
        String currentCountry = (String) model.getValueAt(row, 3);

        JTextField cityField = new JTextField(currentCity);
        JTextField countryField = new JTextField(currentCountry);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("City:")); form.add(cityField);
        form.add(new JLabel("Country:")); form.add(countryField);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Location (used for prayer times)",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            authService.updateUserLocation(id, cityField.getText().trim(), countryField.getText().trim());
            refresh();
        }
    }

    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int id = (int) model.getValueAt(row, 4);
        String username = (String) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete user \"" + username + "\" and ALL their notes, files, timetable and bills?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            authService.deleteUser(id);
            refresh();
        }
    }

    public void refresh() {
        model.setRowCount(0);
        List<User> users = authService.listAllUsers();
        for (User u : users) {
            model.addRow(new Object[]{u.getUsername(), u.getRole(), u.getCity(), u.getCountry(), u.getId()});
        }
    }
}
