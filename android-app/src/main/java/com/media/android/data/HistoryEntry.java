package com.media.android.data;

import java.util.UUID;

/** One stored question/answer exchange. */
public final class HistoryEntry {

    public final String id;
    public final String question;
    public final String answer;
    public final double confidence;
    /** {@code com.media.android.ai.orchestrator.OrchestratedResponse.State} name. */
    public final String state;
    public final String engineId;
    public final long timestamp;

    public HistoryEntry(String id, String question, String answer, double confidence,
                        String state, String engineId, long timestamp) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.question = question == null ? "" : question;
        this.answer = answer == null ? "" : answer;
        this.confidence = confidence;
        this.state = state == null ? "" : state;
        this.engineId = engineId == null ? "" : engineId;
        this.timestamp = timestamp;
    }

    public boolean isAnswered() {
        return "ANSWERED".equals(state);
    }
}
