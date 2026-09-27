package com.media.android.ai;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of an {@link engine.MediaAiEngine} generation request.
 *
 * <p>Carries the generated content together with the provenance the UI needs to stay honest:
 * which evidence records supported it, how confident the engine is and whether the request was
 * answered or deliberately abstained from.</p>
 */
public final class GenerationResult {

    public enum Status {
        /** Answer generated from sufficiently strong local evidence. */
        ANSWERED,
        /** No sufficiently strong local evidence; MEDIA deliberately abstains. */
        ABSTAINED,
        /** The engine could not run (for example a model is declared but unavailable). */
        UNAVAILABLE,
        /** Unexpected failure inside the engine. */
        ERROR
    }

    public final Status status;
    public final String content;
    /** Confidence in [0,1]; 0 when the engine cannot estimate it. */
    public final double confidence;
    /** Human-readable evidence identifiers (knowledge record ids). */
    public final List<String> evidenceIds;
    /** The engine that produced this result, for display in the UI. */
    public final String engineId;
    /** Optional diagnostic message; null on success. */
    public final String errorMessage;

    private GenerationResult(Status status, String content, double confidence,
                             List<String> evidenceIds, String engineId, String errorMessage) {
        this.status = status;
        this.content = content == null ? "" : content;
        this.confidence = confidence;
        this.evidenceIds = evidenceIds == null ? Collections.<String>emptyList() : evidenceIds;
        this.engineId = engineId == null ? "" : engineId;
        this.errorMessage = errorMessage;
    }

    public static GenerationResult answered(String content, double confidence,
                                            List<String> evidenceIds, String engineId) {
        return new GenerationResult(Status.ANSWERED, content, confidence, evidenceIds, engineId, null);
    }

    public static GenerationResult abstained(String content, String engineId) {
        return new GenerationResult(Status.ABSTAINED, content, 0.0,
                Collections.<String>emptyList(), engineId, null);
    }

    public static GenerationResult unavailable(String engineId, String message) {
        return new GenerationResult(Status.UNAVAILABLE, "", 0.0,
                Collections.<String>emptyList(), engineId, message);
    }

    public static GenerationResult error(String engineId, String message) {
        return new GenerationResult(Status.ERROR, "", 0.0,
                Collections.<String>emptyList(), engineId, message);
    }

    public boolean isUsable() {
        return status == Status.ANSWERED || status == Status.ABSTAINED;
    }
}
