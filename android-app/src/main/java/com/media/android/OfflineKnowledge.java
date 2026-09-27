package com.media.android;

import android.content.Context;
import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Iterator;

public final class OfflineKnowledge {
    public static final class Result {
        public final JSONObject record;
        public final double score;
        Result(JSONObject record, double score) { this.record = record; this.score = score; }
    }

    private final AssetManager assets;
    private final List<JSONObject> records = new ArrayList<>();

    public OfflineKnowledge(Context context) {
        assets = context.getAssets();
        loadDirectory("knowledge/hematology");
    }

    public int size() { return records.size(); }

    public List<Result> search(String query, int limit) {
        String q = normalize(query);
        if (q.isEmpty()) return Collections.emptyList();
        Set<String> qTokens = tokens(q);
        List<Result> out = new ArrayList<>();
        for (JSONObject r : records) {
            String concept = value(r, "concept");
            String topic = value(r, "topic");
            String subtopic = value(r, "subtopic");
            String domain = value(r, "domain");
            String subject = value(r, "subject");
            String searchable = normalize(concept + " " + topic + " " + subtopic + " " + domain + " " + subject + " " + contentText(r.optJSONObject("content")) + " " + terminologyText(r.optJSONArray("terminology")));
            double score = 0.0;
            if (!q.isEmpty() && searchable.contains(q)) score += 0.55;
            String ntopic = normalize(topic);
            String nconcept = normalize(concept);
            if (!q.isEmpty() && ntopic.equals(q)) score += 0.80;
            if (!q.isEmpty() && nconcept.contains(q)) score += 0.45;
            int matched = 0;
            for (String t : qTokens) if (searchable.contains(t)) matched++;
            if (!qTokens.isEmpty()) score += 0.45 * matched / (double) qTokens.size();
            if (score > 0.18) out.add(new Result(r, score));
        }
        out.sort((a,b) -> Double.compare(b.score, a.score));
        return out.subList(0, Math.min(limit, out.size()));
    }

    public Result best(String query) {
        List<Result> r = search(query, 1);
        return r.isEmpty() ? null : r.get(0);
    }

    public boolean isConfident(Result r) { return r != null && r.score >= 0.40; }

    private void loadDirectory(String path) {
        try {
            String[] names = assets.list(path);
            if (names == null) return;
            for (String name : names) {
                String child = path + "/" + name;
                String[] nested = assets.list(child);
                if (nested != null && nested.length > 0) {
                    loadDirectory(child);
                } else if (name.endsWith(".json")) {
                    try (InputStream in = assets.open(child)) {
                        records.add(new JSONObject(read(in)));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static String value(JSONObject o, String key) { return o.optString(key, ""); }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static Set<String> tokens(String s) {
        Set<String> out = new HashSet<>();
        String[] stop = {"a","an","the","what","which","who","where","when","why","how","is","are","was","were","do","does","did","can","could","would","should","please","explain","describe","tell","me","about","of","for","to","in","on","with","and","or"};
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords, stop);
        for (String t : s.split(" ")) {
            if (t.length() >= 2 && !stopWords.contains(t)) out.add(t);
        }
        return out;
    }

    private static String contentText(JSONObject o) {
        if (o == null) return "";
        StringBuilder b = new StringBuilder();
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = o.opt(k);
            b.append(' ').append(k).append(' ').append(v == null ? "" : v.toString());
        }
        return b.toString();
    }

    private static String terminologyText(JSONArray a) {
        if (a == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i=0;i<a.length();i++) b.append(' ').append(a.opt(i));
        return b.toString();
    }
}