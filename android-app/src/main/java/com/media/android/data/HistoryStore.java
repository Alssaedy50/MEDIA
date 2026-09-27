package com.media.android.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Durable, offline history of MEDIA conversations.
 *
 * <p>Backed by {@link SharedPreferences} as a bounded JSON array so it needs no database
 * dependency and survives application restarts. The in-memory list is the read path used by the
 * History screen, keeping list rendering free of disk access.</p>
 */
public final class HistoryStore {

    private static final String PREFS = "media_history";
    private static final String KEY_ENTRIES = "entries";
    private static final int MAX_ENTRIES = 200;

    private final SharedPreferences prefs;
    private final List<HistoryEntry> cache = new ArrayList<HistoryEntry>();
    private boolean loaded = false;

    public HistoryStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        String raw = prefs.getString(KEY_ENTRIES, null);
        if (raw == null) return;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                cache.add(new HistoryEntry(
                        o.optString("id", ""),
                        o.optString("question", ""),
                        o.optString("answer", ""),
                        o.optDouble("confidence", 0.0),
                        o.optString("state", ""),
                        o.optString("engineId", ""),
                        o.optLong("timestamp", 0L)));
            }
        } catch (Exception ignored) {
            // A corrupt store is treated as empty rather than crashing the History screen.
        }
    }

    /** Most recent first. */
    public synchronized List<HistoryEntry> all() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<HistoryEntry>(cache));
    }

    public synchronized int size() {
        ensureLoaded();
        return cache.size();
    }

    public synchronized void add(HistoryEntry entry) {
        ensureLoaded();
        if (!cache.isEmpty() && cache.get(0).question.equalsIgnoreCase(entry.question)) {
            cache.remove(0); // collapse immediate repeats
        }
        cache.add(0, entry);
        while (cache.size() > MAX_ENTRIES) cache.remove(cache.size() - 1);
        persist();
    }

    /** Case-insensitive substring search over the stored questions. */
    public synchronized List<HistoryEntry> search(String query) {
        ensureLoaded();
        if (query == null || query.trim().isEmpty()) return all();
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        List<HistoryEntry> out = new ArrayList<HistoryEntry>();
        for (HistoryEntry e : cache) {
            if (e.question.toLowerCase(java.util.Locale.ROOT).contains(needle)) out.add(e);
        }
        return out;
    }

    public synchronized void remove(String id) {
        ensureLoaded();
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id)) {
                cache.remove(i);
                persist();
                return;
            }
        }
    }

    public synchronized void clear() {
        ensureLoaded();
        cache.clear();
        persist();
    }

    private void persist() {
        JSONArray arr = new JSONArray();
        for (HistoryEntry e : cache) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", e.id);
                o.put("question", e.question);
                o.put("answer", e.answer);
                o.put("confidence", e.confidence);
                o.put("state", e.state);
                o.put("engineId", e.engineId);
                o.put("timestamp", e.timestamp);
            } catch (Exception ignored) {
                continue;
            }
            arr.put(o);
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply();
    }
}
