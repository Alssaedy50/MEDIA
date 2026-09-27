package com.media.android.ai.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the engines MEDIA can answer with.
 *
 * <p>Registering engines here is the only integration point a future on-device model needs:
 * implement {@link MediaAiEngine}, register it, and the orchestrator and UI will pick it up.
 * The registry never fabricates an engine that is not actually present.</p>
 */
public final class MediaEngineRegistry {

    /**
     * Describes an engine that exists as a plug-in point but whose runtime is not installed yet.
     * Used by the Settings → Model screen to stay truthful about what is available.
     */
    public static final class Planned {
        public final String id;
        public final String displayName;
        public final String format;
        public final String note;

        public Planned(String id, String displayName, String format, String note) {
            this.id = id;
            this.displayName = displayName;
            this.format = format;
            this.note = note;
        }
    }

    private final Map<String, MediaAiEngine> engines = new LinkedHashMap<String, MediaAiEngine>();
    private final List<Planned> planned = new ArrayList<Planned>();

    public void register(MediaAiEngine engine) {
        engines.put(engine.id(), engine);
    }

    public void registerPlanned(Planned engine) {
        planned.add(engine);
    }

    public MediaAiEngine get(String id) {
        return engines.get(id);
    }

    public Map<String, MediaAiEngine> all() {
        return Collections.unmodifiableMap(engines);
    }

    public List<Planned> planned() {
        return Collections.unmodifiableList(planned);
    }

    /** The engine that can serve a request right now, or {@code null} when none is available. */
    public MediaAiEngine active() {
        for (MediaAiEngine engine : engines.values()) {
            if (engine.isAvailable()) return engine;
        }
        return null;
    }

    public void closeAll() {
        for (MediaAiEngine engine : engines.values()) {
            try {
                engine.close();
            } catch (RuntimeException ignored) {
                // A failing engine close must not prevent the others from shutting down.
            }
        }
        engines.clear();
    }
}
