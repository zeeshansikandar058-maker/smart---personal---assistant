package com.spa.scheduler;

import com.spa.alerts.AlertService;
import com.spa.auth.User;
import com.spa.prayer.PrayerTimeService;
import com.spa.prayer.PrayerTimes;
import com.spa.timetable.TimetableEntry;
import com.spa.timetable.TimetableService;
import com.spa.tts.TextToSpeechService;
import com.spa.util.NotificationUtil;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Single ScheduledExecutorService that ticks once a minute and checks all
 * three time-based reminder sources: prayer times, timetable entries, and
 * bill due-dates. Kept as one scheduler (rather than three) to minimize
 * background threads in the desktop app.
 */
public class ReminderScheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reminder-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final PrayerTimeService prayerTimeService;
    private final TimetableService timetableService;
    private final AlertService alertService;
    private final TextToSpeechService tts;

    private String lastAnnouncedPrayer = null;

    public ReminderScheduler(PrayerTimeService prayerTimeService, TimetableService timetableService,
                              AlertService alertService, TextToSpeechService tts) {
        this.prayerTimeService = prayerTimeService;
        this.timetableService = timetableService;
        this.alertService = alertService;
        this.tts = tts;
    }

    public void start(User user, Consumer<TimetableEntry> onTimetableDue,
                       Consumer<com.spa.alerts.BillReminder> onBillDue) {
        executor.scheduleAtFixedRate(() -> {
            try {
                checkPrayerTimes(user);
            } catch (Exception e) {
                System.err.println("Prayer time check failed: " + e.getMessage());
            }
            try {
                for (TimetableEntry entry : timetableService.dueNow(user.getId())) {
                    String msg = "Reminder: " + entry.getTitle() + " starts now" +
                            (entry.getLocation() != null && !entry.getLocation().isBlank() ? " at " + entry.getLocation() : "") + ".";
                    NotificationUtil.showNotification("Schedule Reminder", msg);
                    tts.speak(msg);
                    onTimetableDue.accept(entry);
                }
            } catch (Exception e) {
                System.err.println("Timetable check failed: " + e.getMessage());
            }
            try {
                alertService.checkAndFireDueAlerts(user.getId(), onBillDue);
            } catch (Exception e) {
                System.err.println("Bill alert check failed: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void checkPrayerTimes(User user) throws Exception {
        PrayerTimes pt = prayerTimeService.fetchTodayTimings(user.getCity(), user.getCountry(), 1);
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        for (Map.Entry<String, LocalTime> e : pt.all().entrySet()) {
            String key = pt.getDate() + "-" + e.getKey();
            if (e.getValue().equals(now) && !key.equals(lastAnnouncedPrayer)) {
                lastAnnouncedPrayer = key;
                String msg = "It is time for " + e.getKey() + " prayer.";
                NotificationUtil.showNotification("Prayer Time", msg);
                tts.speak(msg);
            }
        }
    }

    public void stop() {
        executor.shutdownNow();
    }
}
