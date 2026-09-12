package com.spa.gui;

import com.spa.auth.User;
import com.spa.files.FileManagerService;
import com.spa.files.FileManagerService.FileRecord;
import com.spa.voice.VoiceInputService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class FileManagerPanel extends JPanel {

    private final FileManagerService service = new FileManagerService();
    private final User user;
    private final VoiceInputService voiceInputService;

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"File Name", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public FileManagerPanel(User user, VoiceInputService voiceInputService) {
        this.user = user;
        this.voiceInputService = voiceInputService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("File Manager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        table.removeColumn(table.getColumnModel().getColumn(1));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton saveBtn = new JButton("Save New File");
        JButton readBtn = new JButton("Read Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton voiceBtn = new JButton("🎤 Voice Command");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(saveBtn);
        buttons.add(readBtn);
        buttons.add(deleteBtn);
        buttons.add(voiceBtn);
        add(buttons, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> saveFileDialog());
        readBtn.addActionListener(e -> readSelected());
        deleteBtn.addActionListener(e -> deleteSelected(selectedRecord()));
        voiceBtn.addActionListener(e -> voiceInputService.listenOnce(this::handleVoiceCommand));

        refresh();
    }

    /** Handles voice commands such as "save this file", "delete this file", "read this file". */
    private void handleVoiceCommand(String command) {
        if (command == null || command.isBlank()) return;
        if (command.contains("save")) {
            saveFileDialog();
        } else if (command.contains("delete")) {
            deleteSelected(selectedRecordOrAskByName());
        } else if (command.contains("read")) {
            FileRecord rec = selectedRecordOrAskByName();
            if (rec != null) showFileContent(rec);
        } else {
            JOptionPane.showMessageDialog(this, "Command not recognized: \"" + command + "\"\n" +
                    "Try: \"save this file\", \"delete this file\", or \"read this file\".");
        }
    }

    private FileRecord selectedRecordOrAskByName() {
        FileRecord rec = selectedRecord();
        if (rec != null) return rec;
        String name = JOptionPane.showInputDialog(this, "Which file? (enter file name)");
        if (name == null || name.isBlank()) return null;
        FileRecord found = service.findByName(user.getId(), name.trim());
        if (found == null) {
            JOptionPane.showMessageDialog(this, "No file named \"" + name + "\" found.");
        }
        return found;
    }

    private FileRecord selectedRecord() {
        int row = table.getSelectedRow();
        if (row == -1) return null;
        int id = (int) model.getValueAt(row, 1);
        String name = (String) model.getValueAt(row, 0);
        for (FileRecord r : service.listFiles(user.getId())) {
            if (r.id == id) return r;
        }
        return null;
    }

    private void saveFileDialog() {
        JTextField nameField = new JTextField("note.txt");
        JTextArea contentArea = new JTextArea(8, 30);
        JPanel form = new JPanel(new BorderLayout(4, 4));
        JPanel top = new JPanel(new GridLayout(0, 1, 4, 4));
        top.add(new JLabel("File name:")); top.add(nameField);
        form.add(top, BorderLayout.NORTH);
        form.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, form, "Save New File",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            service.saveTextFile(user.getId(), user.getUsername(), nameField.getText().trim(), contentArea.getText());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void readSelected() {
        FileRecord rec = selectedRecord();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Select a file first.");
            return;
        }
        showFileContent(rec);
    }

    private void showFileContent(FileRecord rec) {
        try {
            String content = service.readFile(rec);
            JTextArea area = new JTextArea(content, 15, 40);
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), rec.fileName, JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Central place enforcing the required confirmation step before ANY delete (GUI or voice-triggered). */
    private void deleteSelected(FileRecord rec) {
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Select (or specify) a file first.");
            return;
        }
        try {
            boolean deleted = service.deleteFile(rec, () ->
                    JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to permanently delete \"" + rec.fileName + "\"?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                            == JOptionPane.YES_OPTION);
            if (deleted) refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        List<FileRecord> files = service.listFiles(user.getId());
        for (FileRecord f : files) {
            model.addRow(new Object[]{f.fileName, f.id});
        }
    }
}
