package com.spa.prayer;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches daily Fajr/Dhuhr/Asr/Maghrib/Isha timings from the free Aladhan
 * API (https://aladhan.com/prayer-times-api) based on a city + country.
 * No API key is required. Results are cached in memory per (city,date) so
 * the app doesn't re-hit the network every minute when the scheduler polls.
 */
public class PrayerTimeService {

    private static final String BASE_URL = "https://api.aladhan.com/v1/timingsByCity";
    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, PrayerTimes> cache = new HashMap<>();

    /**
     * @param city    e.g. "Sahiwal"
     * @param country e.g. "Pakistan"
     * @param method  calculation method id (2 = ISNA, 1 = University of Islamic
     *                Sciences Karachi — a common default for Pakistan)
     */
    public PrayerTimes fetchTodayTimings(String city, String country, int method) throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String cacheKey = city + "|" + country + "|" + today;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        String url = BASE_URL
                + "?city=" + urlEncode(city)
                + "&country=" + urlEncode(country)
                + "&method=" + method;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Aladhan API returned HTTP " + response.statusCode());
        }

        JSONObject root = new JSONObject(response.body());
        JSONObject data = root.getJSONObject("data");
        JSONObject timings = data.getJSONObject("timings");

        PrayerTimes pt = new PrayerTimes(today, city);
        pt.put("Fajr", parseTime(timings.getString("Fajr")));
        pt.put("Dhuhr", parseTime(timings.getString("Dhuhr")));
        pt.put("Asr", parseTime(timings.getString("Asr")));
        pt.put("Maghrib", parseTime(timings.getString("Maghrib")));
        pt.put("Isha", parseTime(timings.getString("Isha")));

        cache.put(cacheKey, pt);
        return pt;
    }

    private LocalTime parseTime(String raw) {
        // Aladhan sometimes appends a timezone suffix like "05:12 (PKT)"
        String clean = raw.split(" ")[0].trim();
        return LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
