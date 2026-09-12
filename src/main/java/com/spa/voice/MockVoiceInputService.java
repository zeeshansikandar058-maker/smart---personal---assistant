package com.spa.voice;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Fallback used when no Vosk model is configured. Instead of listening to
 * the microphone, it pops a "Simulate voice command" input dialog so every
 * voice-driven feature (notes, file manager, schedule Q&A) can still be
 * demoed end-to-end. This keeps the app fully runnable without requiring a
 * ~50MB model download in environments without internet access.
 */
public class MockVoiceInputService implements VoiceInputService {

    @Override
    public void listenOnce(Consumer<String> onResult) {
        String input = JOptionPane.showInputDialog(null,
                "🎤 (Offline speech model not installed — type what you would say)\n" +
                        "Examples: \"add note buy groceries tomorrow\", \"save this file\",\n" +
                        "\"delete this file\", \"read this file\", \"what's my schedule today\"",
                "Simulated Voice Input", JOptionPane.QUESTION_MESSAGE);
        onResult.accept(input == null ? "" : input.trim().toLowerCase());
    }

    @Override
    public boolean isRealRecognition() {
        return false;
    }
}
