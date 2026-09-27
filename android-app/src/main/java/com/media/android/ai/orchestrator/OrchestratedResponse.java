package com.media.android.ai.orchestrator;

import java.util.Collections;
import java.util.List;

/** Fully resolved outcome the UI renders, independent of which AI engine produced it. */
public final class OrchestratedResponse {

    public enum State {
        /** Evidence-backed answer available. */
        ANSWERED,
        /** No sufficiently strong local evidence; safe abstention. */
        ABSTAINED,
        /** No engine is available at all (for example knowledge failed to load). */
        NO_ENGINE,
        /** Unexpected failure. */
        ERROR
    }

    public final State state;
    /** Structured answer markup (see {@code AnswerComposer}). */
    public final String content;
    /** Confidence in [0,1]; 0 when not applicable. */
    public final double confidence;
    /** Knowledge record ids that support the answer. */
    public final List<String> evidenceIds;
    /** Engine that produced the answer. */
    public final String engineId;
    public final String errorMessage;

    private OrchestratedResponse(State state, String content, double confidence,
                                 List<String> evidenceIds, String engineId, String errorMessage) {
        this.state = state;
        this.content = content == null ? "" : content;
        this.confidence = confidence;
        this.evidenceIds = evidenceIds == null ? Collections.<String>emptyList() : evidenceIds;
        this.engineId = engineId == null ? "" : engineId;
        this.errorMessage = errorMessage;
    }

    static OrchestratedResponse answered(String content, double confidence,
                                         List<String> evidenceIds, String engineId) {
        return new OrchestratedResponse(State.ANSWERED, content, confidence, evidenceIds, engineId, null);
    }

    static OrchestratedResponse abstained(String content, String engineId) {
        return new OrchestratedResponse(State.ABSTAINED, content, 0.0,
                Collections.<String>emptyList(), engineId, null);
    }

    static OrchestratedResponse noEngine(String message) {
        return new OrchestratedResponse(State.NO_ENGINE, "", 0.0,
                Collections.<String>emptyList(), "", message);
    }

    static OrchestratedResponse error(String engineId, String message) {
        return new OrchestratedResponse(State.ERROR, "", 0.0,
                Collections.<String>emptyList(), engineId, message);
    }
}
