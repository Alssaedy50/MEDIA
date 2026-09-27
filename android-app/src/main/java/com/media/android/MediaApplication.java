package com.media.android;

import android.app.Application;
import android.content.Context;

import com.media.android.ai.engine.LocalKnowledgeEngine;
import com.media.android.ai.engine.MediaEngineRegistry;
import com.media.android.ai.orchestrator.MediaAiOrchestrator;
import com.media.android.data.HistoryStore;
import com.media.android.data.Preferences;
import com.media.android.knowledge.AssetKnowledgeSource;
import com.media.android.knowledge.KnowledgeRepository;
import com.media.android.model.ModelManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application-scoped composition root.
 *
 * <p>Owns the single instances of the knowledge layer, the engine registry, the orchestrator, the
 * model manager and the history store. Building them here (rather than per-Activity) keeps
 * startup work off the configuration-change path and gives every screen the same view of state.</p>
 */
public final class MediaApplication extends Application {

    private final AtomicBoolean enumerating = new AtomicBoolean(false);

    private KnowledgeRepository repository;
    private MediaEngineRegistry registry;
    private MediaAiOrchestrator orchestrator;
    private ModelManager modelManager;
    private HistoryStore history;
    private Preferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        // Only cheap construction happens here. The knowledge tree is enumerated lazily on a
        // background thread the first time a screen needs it, so cold start stays fast.
        history = new HistoryStore(this);
        modelManager = new ModelManager(getFilesDir());
        preferences = new Preferences(this);
        registry = buildRegistry();
        orchestrator = new MediaAiOrchestrator(registry);
    }

    public static MediaApplication from(Context context) {
        return (MediaApplication) context.getApplicationContext();
    }

    public synchronized KnowledgeRepository repository() {
        if (repository == null) {
            repository = new KnowledgeRepository(new AssetKnowledgeSource(this));
        }
        return repository;
    }

    public MediaEngineRegistry registry() {
        return registry;
    }

    public MediaAiOrchestrator orchestrator() {
        return orchestrator;
    }

    public ModelManager modelManager() {
        return modelManager;
    }

    public HistoryStore history() {
        return history;
    }

    public Preferences preferences() {
        return preferences;
    }

    /** True while the initial knowledge enumeration is running. */
    public boolean isEnumerating() {
        return enumerating.get();
    }

    /** Runs the one-time knowledge enumeration on a background thread, at most once. */
    public void ensureKnowledgeLoaded(final Runnable onReady) {
        final KnowledgeRepository repo = repository();
        if (repo.size() > 0) {
            if (onReady != null) onReady.run();
            return;
        }
        if (!enumerating.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                repo.ensureEnumerated();
                modelManager.refresh();
            } finally {
                enumerating.set(false);
            }
            if (onReady != null) onReady.run();
        }, "media-knowledge-load").start();
    }

    private MediaEngineRegistry buildRegistry() {
        MediaEngineRegistry r = new MediaEngineRegistry();
        r.register(new LocalKnowledgeEngine(repository()));
        // Documented plug-in points. These are *not* registered as engines because their runtime is
        // not bundled; the Settings screen lists them as planned so MEDIA never over-claims.
        r.registerPlanned(new MediaEngineRegistry.Planned(
                "local-gguf",
                "MEDIA Foundation (on-device)",
                "GGUF",
                "Requires a verified quantized artifact and a bundled GGUF runtime."));
        return r;
    }
}
