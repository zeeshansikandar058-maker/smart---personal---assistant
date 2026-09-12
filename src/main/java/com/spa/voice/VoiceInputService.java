package com.spa.voice;

import java.util.function.Consumer;

/**
 * Abstraction over speech-to-text so the rest of the app never depends on
 * Vosk directly. Swap the implementation returned by VoiceInputFactory to
 * go from the demo mock to real offline recognition.
 */
public interface VoiceInputService {

    /**
     * Starts listening for a single voice command asynchronously.
     * When recognition finishes, {@code onResult} is called on the EDT-safe
     * caller thread with the recognized text (lower-cased, trimmed), e.g.
     * "add note buy groceries", "save this file", "delete this file",
     * "read this file", "what's my schedule today".
     */
    void listenOnce(Consumer<String> onResult);

    /** Whether this implementation is backed by a real offline model. */
    boolean isRealRecognition();
}
