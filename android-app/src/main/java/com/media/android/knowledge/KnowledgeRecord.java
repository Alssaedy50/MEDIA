package com.media.android.knowledge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Read-only view over one bundled medical knowledge record.
 *
 * <p>The underlying JSON is preserved untouched so the record always round-trips into the
 * model-ready pipeline. Accessors are tolerant: fields that are absent or of an unexpected type
 * yield an empty value rather than throwing, which keeps the offline answer layer robust against
 * partially authored records.</p>
 */
public final class KnowledgeRecord {

    private final JSONObject json;

    KnowledgeRecord(JSONObject json) {
        this.json = json;
    }

    public static KnowledgeRecord of(JSONObject json) {
        return new KnowledgeRecord(json);
    }

    public JSONObject json() { return json; }

    public String id() { return str("id"); }
    public String domain() { return str("domain"); }
    public String subject() { return str("subject"); }
    public String topic() { return str("topic"); }
    public String subtopic() { return str("subtopic"); }
    public String concept() { return str("concept"); }
    public String status() { return str("status"); }
    public String evidenceLevel() { return str("evidence_level"); }

    /** Human-readable title falling back through topic -> concept -> id. */
    public String title() {
        String t = topic();
        if (!Text.isBlank(t)) return t;
        String c = concept();
        if (!Text.isBlank(c)) return c;
        return id();
    }

    public JSONObject content() {
        Object o = json.opt("content");
        return o instanceof JSONObject ? (JSONObject) o : new JSONObject();
    }

    private String str(String key) {
        Object o = json.opt(key);
        return o == null || o == JSONObject.NULL ? "" : String.valueOf(o);
    }

    /** Reads a string field from the content object, tolerant of missing values. */
    public String contentString(String key) {
        JSONObject c = content();
        Object o = c.opt(key);
        if (o == null || o == JSONObject.NULL) return "";
        if (o instanceof String) return Text.clean((String) o);
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                String s = Text.clean(a.optString(i, ""));
                if (!s.isEmpty()) {
                    if (b.length() > 0) b.append('\n');
                    b.append(s);
                }
            }
            return b.toString();
        }
        return Text.clean(String.valueOf(o));
    }

    /**
     * Reads a string array field (e.g. {@code causes}, {@code effects}, {@code high_yield}).
     * A plain string is normalised into a single-element list.
     */
    public List<String> contentList(String key) {
        Object o = content().opt(key);
        if (o == null || o == JSONObject.NULL) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            for (int i = 0; i < a.length(); i++) {
                String s = Text.clean(a.optString(i, ""));
                if (!s.isEmpty()) out.add(s);
            }
        } else {
            String s = Text.clean(String.valueOf(o));
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** Terminology entries with the Arabic field resolved despite the two spellings in the corpus. */
    public List<Term> terminology() {
        Object o = json.opt("terminology");
        if (!(o instanceof JSONArray)) return Collections.emptyList();
        JSONArray a = (JSONArray) o;
        List<Term> out = new ArrayList<Term>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null) continue;
            String term = Text.clean(t.optString("term", ""));
            if (term.isEmpty()) continue;
            // Records use both "Arabic" and "arabic"; accept either.
            String arabic = Text.clean(t.has("Arabic") ? t.optString("Arabic", "") : t.optString("arabic", ""));
            String pronunciation = Text.clean(t.optString("pronunciation", ""));
            String notes = Text.clean(t.optString("notes", ""));
            out.add(new Term(term, arabic, pronunciation, notes));
        }
        return out;
    }

    public List<Source> sources() {
        Object o = json.opt("sources");
        if (!(o instanceof JSONArray)) return Collections.emptyList();
        JSONArray a = (JSONArray) o;
        List<Source> out = new ArrayList<Source>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject s = a.optJSONObject(i);
            if (s == null) continue;
            String title = Text.clean(s.optString("title", ""));
            if (title.isEmpty()) continue;
            String year = s.has("year") && !s.isNull("year") ? String.valueOf(s.opt("year")) : "";
            out.add(new Source(title, Text.clean(s.optString("source_type", "")),
                    Text.clean(year), Text.clean(s.optString("location", "")),
                    Text.clean(s.optString("url", ""))));
        }
        return out;
    }

    /** Raw text of the whole record, used to build the searchable index. */
    public String rawText() {
        StringBuilder b = new StringBuilder();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = json.opt(k);
            b.append(' ').append(k).append(' ').append(v == null ? "" : v.toString());
        }
        return b.toString();
    }

    public static final class Term {
        public final String term;
        public final String arabic;
        public final String pronunciation;
        public final String notes;

        Term(String term, String arabic, String pronunciation, String notes) {
            this.term = term;
            this.arabic = arabic;
            this.pronunciation = pronunciation;
            this.notes = notes;
        }
    }

    public static final class Source {
        public final String title;
        public final String sourceType;
        public final String year;
        public final String location;
        public final String url;

        Source(String title, String sourceType, String year, String location, String url) {
            this.title = title;
            this.sourceType = sourceType;
            this.year = year;
            this.location = location;
            this.url = url;
        }
    }
}
