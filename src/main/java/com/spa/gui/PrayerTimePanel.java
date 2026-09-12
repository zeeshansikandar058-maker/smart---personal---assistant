package com.spa.gui;

import com.spa.auth.User;
import com.spa.prayer.PrayerTimeService;
import com.spa.prayer.PrayerTimes;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.Map;

public class PrayerTimePanel extends JPanel {

    private final PrayerTimeService service = new PrayerTimeService();
    private final User user;
    private final JLabel statusLabel = new JLabel("Loading prayer times...");
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 2, 12, 12));

    public PrayerTimePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Prayer Times — " + user.getCity() + ", " + user.getCountry());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadTimes());
        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.CENTER);
        south.add(refresh, BorderLayout.EAST);
        remove(statusLabel);
        add(south, BorderLayout.SOUTH);

        loadTimes();
    }

    private void loadTimes() {
        statusLabel.setText("Fetching from Aladhan API...");
        gridPanel.removeAll();
        new SwingWorker<PrayerTimes, Void>() {
            @Override
            protected PrayerTimes doInBackground() throws Exception {
                return service.fetchTodayTimings(user.getCity(), user.getCountry(), 1);
            }

            @Override
            protected void done() {
                try {
                    PrayerTimes pt = get();
                    for (Map.Entry<String, LocalTime> entry : pt.all().entrySet()) {
                        gridPanel.add(boldLabel(entry.getKey()));
                        gridPanel.add(new JLabel(entry.getValue().toString()));
                    }
                    statusLabel.setText("Location: " + pt.getCity() + " • " + pt.getDate() +
                            " (auto reminders run in the background)");
                } catch (Exception ex) {
                    statusLabel.setText("Could not fetch prayer times (check internet connection): " + ex.getMessage());
                }
                gridPanel.revalidate();
                gridPanel.repaint();
            }
        }.execute();
    }

    private JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }
}
