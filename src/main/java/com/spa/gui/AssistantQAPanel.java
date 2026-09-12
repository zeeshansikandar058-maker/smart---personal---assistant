package com.spa.gui;

import com.spa.auth.User;
import com.spa.prayer.PrayerTimeService;
import com.spa.prayer.PrayerTimes;
import com.spa.timetable.TimetableEntry;
import com.spa.timetable.TimetableService;
import com.spa.tts.TextToSpeechService;
import com.spa.voice.VoiceInputService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * STRETCH GOAL: a small fixed-intent Q&A assistant. It recognizes a handful
 * of canned question patterns (not a general LLM) — this is intentionally
 * simple/rule-based per the project's "documented as future scope" allowance
 * for anything beyond a basic conversational layer.
 */
public class AssistantQAPanel extends JPanel {

    private final User user;
    private final TimetableService timetableService = new TimetableService();
    private final PrayerTimeService prayerTimeService = new PrayerTimeService();
    private final TextToSpeechService tts;
    private final VoiceInputService voiceInputService;

    private final JTextArea conversation = new JTextArea();
    private final JTextField inputField = new JTextField();

    public AssistantQAPanel(User user, TextToSpeechService tts, VoiceInputService voiceInputService) {
        this.user = user;
        this.tts = tts;
        this.voiceInputService = voiceInputService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Ask Your Assistant");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setText("Try asking:\n" +
                "  • \"what's my schedule today?\"\n" +
                "  • \"what's my next prayer time?\"\n" +
                "  • \"do I have any bills due?\"\n\n");
        add(new JScrollPane(conversation), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        JButton askBtn = new JButton("Ask");
        JButton voiceBtn = new JButton("🎤");
        JPanel right = new JPanel(new FlowLayout());
        right.add(askBtn);
        right.add(voiceBtn);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        askBtn.addActionListener(e -> ask(inputField.getText()));
        inputField.addActionListener(e -> ask(inputField.getText()));
        voiceBtn.addActionListener(e -> voiceInputService.listenOnce(this::ask));
    }

    private void ask(String question) {
        if (question == null || question.isBlank()) return;
        String q = question.toLowerCase().trim();
        String answer = answerFor(q);
        conversation.append("You: " + question + "\n");
        conversation.append("Assistant: " + answer + "\n\n");
        inputField.setText("");
        tts.speak(answer);
    }

    private String answerFor(String q) {
        try {
            if (q.contains("schedule") || q.contains("timetable") || q.contains("class")) {
                return todaysScheduleAnswer();
            }
            if (q.contains("prayer")) {
                return nextPrayerAnswer();
            }
            if (q.contains("bill") || q.contains("fee") || q.contains("due")) {
                return "Check the Bills tab for full details — any overdue items are highlighted in red there.";
            }
            return "I can currently answer questions about your schedule, prayer times, and bills. " +
                    "More general conversation is a documented future goal for this assistant.";
        } catch (Exception e) {
            return "Sorry, I couldn't fetch that right now (" + e.getMessage() + ").";
        }
    }

    private String todaysScheduleAnswer() {
        String today = LocalDate.now().getDayOfWeek().name();
        List<TimetableEntry> all = timetableService.weeklySchedule(user.getId());
        StringBuilder sb = new StringBuilder();
        for (TimetableEntry e : all) {
            if (e.getDayOfWeek().equalsIgnoreCase(today)) {
                sb.append(e.getStartTime()).append(" - ").append(e.getTitle()).append("; ");
            }
        }
        return sb.length() == 0 ? "You have nothing scheduled today." : "Today: " + sb;
    }

    private String nextPrayerAnswer() throws Exception {
        PrayerTimes pt = prayerTimeService.fetchTodayTimings(user.getCity(), user.getCountry(), 1);
        LocalTime now = LocalTime.now();
        String next = null;
        LocalTime nextTime = null;
        for (Map.Entry<String, LocalTime> entry : pt.all().entrySet()) {
            if (entry.getValue().isAfter(now) && (nextTime == null || entry.getValue().isBefore(nextTime))) {
                next = entry.getKey();
                nextTime = entry.getValue();
            }
        }
        if (next == null) {
            return "All of today's prayers have passed; the next one is Fajr tomorrow.";
        }
        return "The next prayer is " + next + " at " + nextTime + ".";
    }
}
