package com.spa.gui;

import com.spa.alerts.AlertService;
import com.spa.auth.User;
import com.spa.prayer.PrayerTimeService;
import com.spa.scheduler.ReminderScheduler;
import com.spa.timetable.TimetableService;
import com.spa.tts.SystemTextToSpeechService;
import com.spa.tts.TextToSpeechService;
import com.spa.voice.MockVoiceInputService;
import com.spa.voice.VoiceInputService;

import javax.swing.*;
import java.awt.*;

public class MainDashboardFrame extends JFrame {

    private final User user;
    private final TextToSpeechService tts = new SystemTextToSpeechService();
    private final VoiceInputService voiceInputService = new MockVoiceInputService();
    private final AlertService alertService = new AlertService(tts);
    private final ReminderScheduler scheduler = new ReminderScheduler(
            new PrayerTimeService(), new TimetableService(), alertService, tts);

    private TimetablePanel timetablePanel;
    private BillsPanel billsPanel;

    public MainDashboardFrame(User user) {
        super("Smart Personal Assistant — " + user.getUsername() + " (" + user.getRole() + ")");
        this.user = user;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        buildUI();
        wireScheduler();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Prayer Times", new PrayerTimePanel(user));

        timetablePanel = new TimetablePanel(user);
        tabs.addTab("Timetable", timetablePanel);

        tabs.addTab("Notes", new NotesPanel(user, voiceInputService));
        tabs.addTab("Files", new FileManagerPanel(user, voiceInputService));

        billsPanel = new BillsPanel(user, alertService);
        tabs.addTab("Bills / Priority Alerts", billsPanel);

        tabs.addTab("Ask Assistant", new AssistantQAPanel(user, tts, voiceInputService));

        if (user.isAdmin()) {
            tabs.addTab("Admin", new AdminPanel());
        }

        add(tabs, BorderLayout.CENTER);

        JLabel statusBar = new JLabel("  Logged in as " + user.getUsername() +
                " • " + user.getCity() + ", " + user.getCountry() +
                (voiceInputService.isRealRecognition() ? "" : "  •  Voice: DEMO MODE (see README to enable offline Vosk recognition)"));
        add(statusBar, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                scheduler.stop();
            }
        });
    }

    /** Background polling: prayer times, timetable reminders, and priority bill alerts. */
    private void wireScheduler() {
        scheduler.start(user,
                entry -> SwingUtilities.invokeLater(() -> {
                    timetablePanel.refresh();
                }),
                bill -> SwingUtilities.invokeLater(() -> {
                    billsPanel.refresh();
                    JOptionPane.showMessageDialog(this,
                            "<html><font color='red'><b>PRIORITY ALERT</b></font><br>" +
                                    bill.getTitle() + " (Rs. " + String.format("%.2f", bill.getAmount()) +
                                    ") is due on " + bill.getDueDate() + "!</html>",
                            "Priority Bill Alert", JOptionPane.WARNING_MESSAGE);
                }));
    }
}
