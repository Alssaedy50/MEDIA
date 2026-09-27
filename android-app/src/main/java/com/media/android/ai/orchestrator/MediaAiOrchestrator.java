package com.media.android.ai.orchestrator;

import com.media.android.ai.GenerationOptions;
import com.media.android.ai.GenerationResult;
import com.media.android.ai.engine.MediaAiEngine;
import com.media.android.ai.engine.MediaEngineRegistry;
import com.media.android.knowledge.Text;

/**
 * The MEDIA AI orchestrator.
 *
 * <p>Single place that turns a student question into a rendered answer, following the pipeline:</p>
 * <pre>
 *   User query
 *     -&gt; intent detection
 *     -&gt; knowledge retrieval (done inside the engine for the offline engine, or via the context
 *        assembled here for a future neural engine)
 *     -&gt; engine invocation
 *     -&gt; response formatting
 *     -&gt; safety / evidence attachment
 *     -&gt; UI-ready {@link OrchestratedResponse}
 * </pre>
 *
 * <p>The orchestrator owns no medical knowledge itself. It selects a registered engine, passes a
 * grounded context and normalises the engine's outcome into a stable contract for the UI. Adding
 * the future on-device model therefore requires no UI change.</p>
 */
public final class MediaAiOrchestrator {

    private final MediaEngineRegistry registry;
    private final GenerationOptions options;

    public MediaAiOrchestrator(MediaEngineRegistry registry) {
        this(registry, GenerationOptions.defaults());
    }

    public MediaAiOrchestrator(MediaEngineRegistry registry, GenerationOptions options) {
        this.registry = registry;
        this.options = options;
    }

    public IntentDetector.Intent detectIntent(String query) {
        return IntentDetector.detect(query);
    }

    /** Runs the pipeline synchronously. Call from a background thread; the UI wraps this in a ViewModel. */
    public OrchestratedResponse answer(String query) {
        return answer(query, options);
    }

    /** Runs the pipeline with per-request options (e.g. the bilingual preference). */
    public OrchestratedResponse answer(String query, GenerationOptions requestOptions) {
        if (Text.isBlank(query)) {
            return OrchestratedResponse.error("", "Empty query");
        }
        final GenerationOptions effective = requestOptions == null ? options : requestOptions;
        MediaAiEngine engine = registry.active();
        if (engine == null) {
            return OrchestratedResponse.noEngine(
                    "Local knowledge is unavailable, so MEDIA cannot answer right now.");
        }

        IntentDetector.Intent intent = detectIntent(query);
        // Context is prefixed with the detected intent so a future neural engine can condition on it
        // while the deterministic engine can simply ignore it.
        String context = "intent=" + intent.name() + "\n";

        GenerationResult result;
        try {
            result = engine.generate(query, context, effective);
        } catch (RuntimeException e) {
            return OrchestratedResponse.error(engine.id(), "Engine failure: " + e.getMessage());
        }

        switch (result.status) {
            case ANSWERED:
                return OrchestratedResponse.answered(result.content, result.confidence,
                        result.evidenceIds, result.engineId);
            case ABSTAINED:
                return OrchestratedResponse.abstained(result.content, result.engineId);
            case UNAVAILABLE:
                return OrchestratedResponse.noEngine(result.errorMessage);
            default:
                return OrchestratedResponse.error(result.engineId, result.errorMessage);
        }
    }
}
