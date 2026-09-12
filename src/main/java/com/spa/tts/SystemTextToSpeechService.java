package com.spa.tts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A zero-dependency TTS implementation that shells out to the operating
 * system's built-in speech engine:
 *   - macOS:   `say`
 *   - Windows: PowerShell + System.Speech.Synthesis
 *   - Linux:   `espeak` / `spd-say` (install via: sudo apt install espeak)
 *
 * This keeps the app runnable out-of-the-box without bundling FreeTTS's
 * (large, unmaintained) voice data. To use FreeTTS instead, implement
 * TextToSpeechService with FreeTTS's VoiceManager and swap the instance
 * created in Main.java — the rest of the app only depends on the interface.
 */
public class SystemTextToSpeechService implements TextToSpeechService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tts-worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void speak(String text) {
        executor.submit(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", text);
                } else if (os.contains("win")) {
                    String script = "Add-Type -AssemblyName System.Speech; " +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('" +
                            text.replace("'", "''") + "');";
                    pb = new ProcessBuilder("powershell", "-Command", script);
                } else {
                    pb = new ProcessBuilder("espeak", text);
                }
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                // No TTS engine available on this machine (e.g. espeak not installed) —
                // fail silently since the alert is still shown visually + as a notification.
                System.err.println("[TTS unavailable] " + text);
            }
        });
    }
}
