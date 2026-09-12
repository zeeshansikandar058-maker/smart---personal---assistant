package com.spa.tts;

public interface TextToSpeechService {
    /** Speaks the given text aloud. Implementations should be non-blocking (fire-and-forget). */
    void speak(String text);
}
