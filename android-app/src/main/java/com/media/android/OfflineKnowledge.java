package com.media.android;

import android.content.Context;
import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline retrieval over the bundled Hematology knowledge records.
 *
 * <p>The matcher is fully deterministic and dependency-free so it can be unit tested with plain
 * strings. Queries are normalized, medical abbreviations are expanded to their canonical term, and
 * multi-phrase evidence is ranked so that records describing the canonical concept outrank records
 * that only mention it in passing.</p>
 */
public final class OfflineKnowledge {
    public static final class Result {
        public final JSONObject record;
        public final double score;
        Result(JSONObject record, double score) { this.record = record; this.score = score; }
    }

    /** Minimum score for a record to be considered at all. */
    static final double MATCH_THRESHOLD = 0.18;
    /** Minimum score for the answer to be shown instead of a safe abstention. */
    static final double CONFIDENCE_THRESHOLD = 0.40;

    /** Offline medical abbreviation dictionary (Hematology focus). Never leaves the device. */
    static final Map<String, String> ABBREVIATIONS = new HashMap<String, String>();
    static {
        ABBREVIATIONS.put("cbc", "complete blood count");
        ABBREVIATIONS.put("hb", "hemoglobin");
        ABBREVIATIONS.put("hgb", "hemoglobin");
        ABBREVIATIONS.put("hct", "hematocrit");
        ABBREVIATIONS.put("epo", "erythropoietin");
        ABBREVIATIONS.put("ida", "iron deficiency anemia");
        ABBREVIATIONS.put("rbc", "red blood cell");
        ABBREVIATIONS.put("wbc", "white blood cell");
        ABBREVIATIONS.put("plt", "platelet");
        ABBREVIATIONS.put("mcv", "mean corpuscular volume");
        ABBREVIATIONS.put("mch", "mean corpuscular hemoglobin");
        ABBREVIATIONS.put("mchc", "mean corpuscular hemoglobin concentration");
        ABBREVIATIONS.put("esr", "erythrocyte sedimentation rate");
        ABBREVIATIONS.put("crp", "c reactive protein");
        ABBREVIATIONS.put("pt", "prothrombin time");
        ABBREVIATIONS.put("inr", "international normalized ratio");
        ABBREVIATIONS.put("aptt", "activated partial thromboplastin time");
        ABBREVIATIONS.put("vwd", "von willebrand disease");
        ABBREVIATIONS.put("dic", "disseminated intravascular coagulation");
        ABBREVIATIONS.put("cml", "chronic myeloid leukemia");
        ABBREVIATIONS.put("aml", "acute myeloid leukemia");
        ABBREVIATIONS.put("cll", "chronic lymphocytic leukemia");
        ABBREVIATIONS.put("mds", "myelodysplastic syndrome");
        ABBREVIATIONS.put("g6pd", "glucose 6 phosphate dehydrogenase");
    }

    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
            "a", "an", "the", "what", "which", "who", "where", "when", "why", "how", "is", "are",
            "was", "were", "do", "does", "did", "can", "could", "would", "should", "please",
            "explain", "describe", "tell", "me", "about", "of", "for", "to", "in", "on", "with",
            "and", "or"));

    private final AssetManager assets;
    private final List<JSONObject> records = new ArrayList<JSONObject>();
    private int loadFailures = 0;

    public OfflineKnowledge(Context context) {
        assets = context.getAssets();
        loadDirectory("knowledge/hematology");
    }

    public int size() { return records.size(); }
    public int loadFailures() { return loadFailures; }
    public boolean isReady() { return records.size() > 0 && loadFailures == 0; }

    public List<Result> search(String query, int limit) {
        QueryPlan plan = QueryPlan.of(query);
        if (plan.isEmpty() || limit <= 0) return Collections.emptyList();
        List<Result> out = new ArrayList<Result>();
        for (JSONObject r : records) {
            double score = score(plan, DocumentText.of(r));
            if (score > MATCH_THRESHOLD) out.add(new Result(r, score));
        }
        out.sort((a, b) -> Double.compare(b.score, a.score));
        return out.subList(0, Math.min(limit, out.size()));
    }

    public Result best(String query) {
        List<Result> r = search(query, 1);
        return r.isEmpty() ? null : r.get(0);
    }

    public boolean isConfident(Result r) { return r != null && r.score >= CONFIDENCE_THRESHOLD; }

    /** A parsed query: canonical phrase plus the token set and abbreviation expansions to score. */
    static final class QueryPlan {
        final String phrase;
        final List<String> tokens;
        final List<String[]> expansions;
        final boolean pureAbbreviation;
        final boolean empty;

        private QueryPlan(String phrase, List<String> tokens, List<String[]> expansions,
                          boolean pureAbbreviation, boolean empty) {
            this.phrase = phrase;
            this.tokens = tokens;
            this.expansions = expansions;
            this.pureAbbreviation = pureAbbreviation;
            this.empty = empty;
        }

        static QueryPlan of(String query) {
            String normalized = normalize(query);
            if (normalized.isEmpty()) {
                return new QueryPlan("", Collections.<String>emptyList(),
                        Collections.<String[]>emptyList(), false, true);
            }
            String[] raw = normalized.split(" ");
            boolean pure = raw.length == 1 && ABBREVIATIONS.containsKey(raw[0]);
            List<String[]> expansions = new ArrayList<String[]>();
            for (String token : raw) {
                String expansion = ABBREVIATIONS.get(token);
                if (expansion != null) expansions.add(new String[]{token, normalize(expansion)});
            }
            String phrase;
            List<String> tokens = new ArrayList<String>();
            if (pure) {
                phrase = normalize(ABBREVIATIONS.get(raw[0]));
                addTokens(tokens, phrase);
            } else {
                phrase = joinTokens(raw);
                addTokens(tokens, normalized);
                for (String[] expansion : expansions) addTokens(tokens, expansion[1]);
            }
            return new QueryPlan(phrase, tokens, expansions, pure, phrase.isEmpty() && tokens.isEmpty());
        }

        boolean isEmpty() { return empty; }
    }

    /** Pre-normalized searchable view of a knowledge record. */
    static final class DocumentText {
        final String topic;
        final String concept;
        final String title;
        final String searchable;

        private DocumentText(String topic, String concept, String title, String searchable) {
            this.topic = topic;
            this.concept = concept;
            this.title = title;
            this.searchable = searchable;
        }

        static DocumentText of(JSONObject r) {
            return of(value(r, "topic"), value(r, "subtopic"), value(r, "concept"),
                    value(r, "domain"), value(r, "subject"),
                    contentText(r.optJSONObject("content")), terminologyText(r.optJSONArray("terminology")));
        }

        static DocumentText of(String topic, String subtopic, String concept, String domain,
                               String subject, String content, String terminology) {
            String nTopic = normalize(topic);
            String nConcept = normalize(concept);
            String nTitle = normalize(topic + " " + subtopic + " " + concept);
            String nSearchable = normalize(concept + " " + topic + " " + subtopic + " " + domain + " "
                    + subject + " " + content + " " + terminology);
            return new DocumentText(nTopic, nConcept, nTitle, nSearchable);
        }
    }

    static double score(QueryPlan plan, DocumentText doc) {
        double score = 0.0;
        if (!plan.phrase.isEmpty()) {
            if (doc.searchable.contains(plan.phrase)) score += 0.55;
            if (doc.topic.equals(plan.phrase)) score += 0.80;
            if (doc.concept.contains(plan.phrase)) score += 0.45;
        }
        if (!plan.tokens.isEmpty()) {
            int matched = 0;
            int titleMatched = 0;
            for (String token : plan.tokens) {
                if (doc.searchable.contains(token)) matched++;
                if (doc.title.contains(token)) titleMatched++;
            }
            score += 0.45 * matched / (double) plan.tokens.size();
            score += 0.30 * titleMatched / (double) plan.tokens.size();
        }
        if (!plan.phrase.isEmpty()) {
            score += 0.05 * Math.min(occurrences(doc.searchable, plan.phrase), 4);
        }
        for (String[] expansion : plan.expansions) {
            String abbreviation = expansion[0];
            String fullTerm = expansion[1];
            if (plan.pureAbbreviation) {
                // A bare acronym should surface the record that actually defines the concept.
                score += Math.min(0.40, 0.16 * occurrences(doc.searchable, fullTerm));
                score += Math.min(0.08, 0.04 * occurrences(doc.searchable, abbreviation));
                if (doc.title.contains(fullTerm)) score += 0.25;
                if (doc.concept.contains(fullTerm)) score += 0.20;
            } else {
                score += Math.min(0.24, 0.10 * occurrences(doc.searchable, fullTerm));
                if (doc.title.contains(fullTerm)) score += 0.15;
            }
        }
        return score;
    }

    private void loadDirectory(String path) {
        try {
            String[] names = assets.list(path);
            if (names == null) {
                loadFailures++;
                return;
            }
            for (String name : names) {
                String child = path + "/" + name;
                String[] nested = assets.list(child);
                if (nested != null && nested.length > 0) {
                    loadDirectory(child);
                } else if (name.endsWith(".json")) {
                    try (InputStream in = assets.open(child)) {
                        records.add(new JSONObject(read(in)));
                    } catch (Exception ignored) {
                        loadFailures++;
                    }
                }
            }
        } catch (Exception ignored) {
            loadFailures++;
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static String value(JSONObject o, String key) { return o.optString(key, ""); }

    static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Lower-cased, de-duplicated, stop-word filtered tokens in order of appearance. */
    static List<String> tokens(String s) {
        List<String> out = new ArrayList<String>();
        addTokens(out, normalize(s));
        return out;
    }

    private static void addTokens(List<String> out, String normalized) {
        Set<String> seen = new LinkedHashSet<String>(out);
        for (String token : normalized.split(" ")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token) && seen.add(token)) out.add(token);
        }
    }

    private static String joinTokens(String[] words) {
        StringBuilder b = new StringBuilder();
        Set<String> seen = new HashSet<String>();
        for (String word : words) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word) && seen.add(word)) {
                if (b.length() > 0) b.append(' ');
                b.append(word);
            }
        }
        return b.toString();
    }

    /** Counts whole-word/phrase occurrences in an already normalized string. */
    static int occurrences(String haystack, String needle) {
        if (haystack == null || needle == null || haystack.isEmpty() || needle.isEmpty()) return 0;
        String paddedHaystack = " " + haystack + " ";
        String paddedNeedle = " " + needle + " ";
        int count = 0;
        int from = 0;
        int hit;
        while ((hit = paddedHaystack.indexOf(paddedNeedle, from)) >= 0) {
            count++;
            from = hit + 1;
        }
        return count;
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
        for (int i = 0; i < a.length(); i++) b.append(' ').append(a.opt(i));
        return b.toString();
    }
}
