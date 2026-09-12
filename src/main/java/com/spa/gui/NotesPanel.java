package com.spa.gui;

import com.spa.auth.User;
import com.spa.notes.Note;
import com.spa.notes.NotesService;
import com.spa.voice.VoiceInputService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class NotesPanel extends JPanel {

    private final NotesService service = new NotesService();
    private final User user;
    private final VoiceInputService voiceInputService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Title", "Category", "Source", "Created", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(16);

    public NotesPanel(User user, VoiceInputService voiceInputService) {
        this.user = user;
        this.voiceInputService = voiceInputService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Notes");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        table.removeColumn(table.getColumnModel().getColumn(4));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        JButton searchBtn = new JButton("Search");
        top.add(searchBtn);
        add(top, BorderLayout.PAGE_START);

        JButton addTextBtn = new JButton("Add Note (Text)");
        JButton addVoiceBtn = new JButton("Add Note (Voice)");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addTextBtn);
        buttons.add(addVoiceBtn);
        buttons.add(deleteBtn);
        add(buttons, BorderLayout.SOUTH);

        addTextBtn.addActionListener(e -> addNoteDialog("TEXT", null));
        addVoiceBtn.addActionListener(e -> voiceInputService.listenOnce(text -> addNoteDialog("VOICE", text)));
        deleteBtn.addActionListener(e -> deleteSelected());
        searchBtn.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());

        refresh();
    }

    private void addNoteDialog(String source, String prefill) {
        JTextField titleField = new JTextField(prefill != null && !prefill.isBlank()
                ? (prefill.length() > 30 ? prefill.substring(0, 30) : prefill) : "");
        JTextArea contentArea = new JTextArea(prefill == null ? "" : prefill, 5, 24);
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"General", "Work", "Study", "Personal", "Finance"});

        JPanel form = new JPanel(new BorderLayout(4, 4));
        JPanel labels = new JPanel(new GridLayout(0, 1, 4, 4));
        labels.add(new JLabel("Title:")); labels.add(titleField);
        labels.add(new JLabel("Category:")); labels.add(categoryBox);
        form.add(labels, BorderLayout.NORTH);
        form.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, form,
                source.equals("VOICE") ? "Confirm Voice Note" : "Add Note",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        if (titleField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.");
            return;
        }
        service.addNote(user.getId(), titleField.getText().trim(), contentArea.getText(),
                (String) categoryBox.getSelectedItem(), source);
        refresh();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int id = (int) model.getValueAt(row, 4);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this note? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            service.deleteNote(id);
            refresh();
        }
    }

    public void refresh() {
        model.setRowCount(0);
        List<Note> notes = service.search(user.getId(), searchField.getText().trim(), null);
        for (Note n : notes) {
            model.addRow(new Object[]{n.getTitle(), n.getCategory(), n.getSource(), n.getCreatedAt(), n.getId()});
        }
    }
}
