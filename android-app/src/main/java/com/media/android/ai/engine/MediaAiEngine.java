package com.media.android.ai.engine;

import com.media.android.ai.GenerationOptions;
import com.media.android.ai.GenerationResult;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Engine-neutral boundary between the MEDIA AI orchestrator and whatever produces an answer.
 *
 * <p>This is the seam that keeps the UI independent of any particular inference implementation.
 * Today it is implemented by {@link LocalKnowledgeEngine} (deterministic offline retrieval).
 * A future on-device neural engine ({@code LocalGgufEngine}, {@code HybridMediaEngine}) can be
 * plugged in by implementing this interface and registering it in {@link MediaEngineRegistry},
 * without touching the screens.</p>
 */
public interface MediaAiEngine {

    /** Stable identifier shown in the UI and used to select the engine. */
    String id();

    /** True when the engine is ready to serve requests right now. */
    boolean isAvailable();

    /**
     * Generates an answer for {@code prompt}, optionally grounded in {@code context} assembled by
     * the orchestrator from the local knowledge layer.
     */
    GenerationResult generate(String prompt, String context, GenerationOptions options);

    /** Cooperative shutdown for engines that own resources (a model, a native runtime, ...). */
    void close();

    /** Shared single-thread executor so engines can run off the UI thread without extra deps. */
    Executor BACKGROUND = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "media-engine");
        t.setDaemon(true);
        return t;
    });
}
