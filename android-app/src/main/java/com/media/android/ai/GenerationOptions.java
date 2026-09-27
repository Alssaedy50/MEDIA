package com.media.android.ai;

/**
 * Parameters controlling a single generation request.
 *
 * <p>Kept intentionally small and engine-neutral so the same call signature works for the current
 * deterministic offline knowledge engine and for a future on-device neural engine.</p>
 */
public final class GenerationOptions {

    public final int maxTokens;
    public final double temperature;
    public final boolean preferArabicExplanation;
    public final int maxEvidenceRecords;

    private GenerationOptions(int maxTokens, double temperature, boolean preferArabicExplanation,
                              int maxEvidenceRecords) {
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.preferArabicExplanation = preferArabicExplanation;
        this.maxEvidenceRecords = maxEvidenceRecords;
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions(512, 0.0, true, 3);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxTokens = 512;
        private double temperature = 0.0;
        private boolean preferArabicExplanation = true;
        private int maxEvidenceRecords = 3;

        public Builder maxTokens(int v) { this.maxTokens = v; return this; }
        public Builder temperature(double v) { this.temperature = v; return this; }
        public Builder preferArabicExplanation(boolean v) { this.preferArabicExplanation = v; return this; }
        public Builder maxEvidenceRecords(int v) { this.maxEvidenceRecords = v; return this; }

        public GenerationOptions build() {
            return new GenerationOptions(maxTokens, temperature, preferArabicExplanation,
                    maxEvidenceRecords);
        }
    }
}
