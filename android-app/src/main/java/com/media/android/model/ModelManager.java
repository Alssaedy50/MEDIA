package com.media.android.model;

import java.io.File;

/**
 * Truthful status of the on-device MEDIA model.
 *
 * <p>Nothing here fabricates an installation. When no verified artifact is present the manager
 * reports {@link State#NOT_INSTALLED} together with the exact expectations a future artifact must
 * satisfy, so the UI can tell the student precisely what is and is not available offline.</p>
 */
public final class ModelManager {

    public enum State {
        /** No verified model artifact found on the device. */
        NOT_INSTALLED,
        /** An artifact is present and its integrity has been verified. */
        READY,
        /** An artifact is present but failed verification. */
        INVALID
    }

    /**
     * Contract the frozen foundation spec targets. These are the *expected* values for the future
     * {@code media-100m-foundation-v1} artifact; they are not a claim that it is installed.
     */
    public static final class Expectation {
        public final String name;
        public final String format;
        public final String quantization;
        public final long parameters;
        public final int vocabulary;
        public final int contextLength;

        public Expectation(String name, String format, String quantization, long parameters,
                           int vocabulary, int contextLength) {
            this.name = name;
            this.format = format;
            this.quantization = quantization;
            this.parameters = parameters;
            this.vocabulary = vocabulary;
            this.contextLength = contextLength;
        }
    }

    public static final Expectation FOUNDATION_EXPECTATION = new Expectation(
            "media-100m-foundation-v1", "GGUF", "Q4_K_M", 111_365_632L, 16_384, 256);

    /** Conventional on-device locations checked for a verified artifact. */
    private static final String[] CANDIDATE_FILES = {
            "models/media-100m-foundation-v1-q4_k_m.gguf",
            "models/media-100m-foundation-v1.gguf",
    };

    private final File modelsDir;

    private State state = State.NOT_INSTALLED;
    private String artifactPath = "";
    private long artifactSizeBytes = 0L;
    private String detail = "No model artifact present.";

    public ModelManager(File filesDir) {
        this.modelsDir = filesDir == null ? null : new File(filesDir, "models");
        refresh();
    }

    /** Re-scans the device for a model artifact. Never throws. */
    public void refresh() {
        state = State.NOT_INSTALLED;
        artifactPath = "";
        artifactSizeBytes = 0L;
        detail = "No model artifact present. MEDIA answers from local knowledge only.";

        if (modelsDir == null) return;
        for (String candidate : CANDIDATE_FILES) {
            File f = new File(modelsDir.getParentFile(), candidate);
            if (f.isFile() && f.length() > 0) {
                artifactPath = f.getAbsolutePath();
                artifactSizeBytes = f.length();
                // Presence is not the same as validity: a GGUF runtime is required to load it.
                state = State.INVALID;
                detail = "Artifact found but no verified GGUF runtime is bundled yet.";
                return;
            }
        }
    }

    public State state() { return state; }
    public boolean isReady() { return state == State.READY; }
    public String artifactPath() { return artifactPath; }
    public long artifactSizeBytes() { return artifactSizeBytes; }
    public String detail() { return detail; }
    public Expectation expectation() { return FOUNDATION_EXPECTATION; }

    /** Human-readable, honest status line for the Settings screen. */
    public String statusLabel() {
        switch (state) {
            case READY: return "Ready";
            case INVALID: return "Invalid artifact";
            default: return "Not installed";
        }
    }

    /** Formats a byte count as MB for display, or a dash when unknown. */
    public String formattedSize() {
        if (artifactSizeBytes <= 0) return "—";
        double mb = artifactSizeBytes / (1024.0 * 1024.0);
        return String.format(java.util.Locale.US, "%.0f MB", mb);
    }
}
