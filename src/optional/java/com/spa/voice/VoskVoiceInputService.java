package com.spa.voice;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * ============================================================================
 *  HOW TO ENABLE REAL OFFLINE VOICE RECOGNITION (Vosk)
 * ============================================================================
 * This file is intentionally kept OUTSIDE src/main/java so the project
 * builds and runs out-of-the-box without a ~50MB model download. To switch
 * from the demo MockVoiceInputService to real speech recognition:
 *
 *   1. Download a Vosk English model, e.g. "vosk-model-small-en-us-0.15"
 *      from https://alphacephei.com/vosk/models and unzip it somewhere,
 *      e.g. ~/models/vosk-model-small-en-us-0.15
 *   2. In pom.xml, uncomment the <dependency> block for com.alphacephei:vosk.
 *   3. Move this file into src/main/java/com/spa/voice/ (same package).
 *   4. In Main.java, set VOSK_MODEL_PATH to the folder from step 1 and
 *      change `new MockVoiceInputService()` to
 *      `new VoskVoiceInputService(VOSK_MODEL_PATH)`.
 *   5. `mvn clean package` and run — the microphone will now be used for
 *      real English speech-to-text, offline (no cloud calls).
 *
 * Urdu support: Vosk also publishes small Urdu/Hindi models; point the same
 * class at an Urdu model path to add it as a secondary recognizer (stretch
 * goal — see README "Urdu voice roadmap").
 * ============================================================================
 */
public class VoskVoiceInputService implements VoiceInputService {

    private final Model model;

    public VoskVoiceInputService(String modelPath) {
        try {
            this.model = new Model(modelPath);
        } catch (Exception e) {
            throw new RuntimeException("Could not load Vosk model at " + modelPath, e);
        }
    }

    @Override
    public void listenOnce(Consumer<String> onResult) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(format);
                mic.start();

                Recognizer recognizer = new Recognizer(model, 16000f);
                byte[] buffer = new byte[4096];
                long start = System.currentTimeMillis();
                StringBuilder finalText = new StringBuilder();

                // Listen for up to 5 seconds of speech per invocation.
                while (System.currentTimeMillis() - start < 5000) {
                    int nBytes = mic.read(buffer, 0, buffer.length);
                    if (recognizer.acceptWaveForm(buffer, nBytes)) {
                        finalText.append(recognizer.getResult());
                    }
                }
                finalText.append(recognizer.getFinalResult());
                mic.stop();
                mic.close();

                // Extract the "text" field from Vosk's JSON result manually to avoid
                // pulling in another JSON parser here.
                String raw = finalText.toString();
                String text = extractTextField(raw);
                onResult.accept(text.trim().toLowerCase());
            } catch (Exception e) {
                onResult.accept("");
            }
        }, "vosk-listener").start();
    }

    private String extractTextField(String json) {
        int idx = json.lastIndexOf("\"text\"");
        if (idx == -1) return "";
        int colon = json.indexOf(':', idx);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote == -1 || secondQuote == -1) return "";
        return json.substring(firstQuote + 1, secondQuote);
    }

    @Override
    public boolean isRealRecognition() {
        return true;
    }
}
