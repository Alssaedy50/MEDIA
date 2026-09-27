package com.media.android.knowledge;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lazy, offline retrieval over the bundled medical knowledge records.
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li><b>Cheap startup.</b> The asset tree (the directory walk) is enumerated at construction,
 *       but record bodies are parsed on demand and cached, so the application does not hold the
 *       whole corpus in memory before the student asks anything.</li>
 *   <li><b>Query understanding.</b> Student queries are normalised, expanded through the bilingual
 *       {@link MedicalLexicon} and scored against a pre-normalised searchable view of each record.
 *       An inverted token index narrows the candidate set before scoring.</li>
 *   <li><b>Determinism.</b> Scoring is pure arithmetic over normalised strings, which makes the
 *       layer fully unit-testable and reproducible.</li>
 * </ul>
 */
public final class KnowledgeRepository {

    public static final class Result {
        public final KnowledgeRecord record;
        public final double score;

        Result(KnowledgeRecord record, double score) {
            this.record = record;
            this.score = score;
        }
    }

    /** Minimum score for a record to be considered at all. */
    public static final double MATCH_THRESHOLD = 0.18;
    /** Minimum score for an answer to be shown instead of a safe abstention. */
    public static final double CONFIDENCE_THRESHOLD = 0.40;

    private static final int CACHE_LIMIT = 64;

    private final KnowledgeSource source;
    private final List<String> paths = new ArrayList<String>();
    /** token -> candidate record indices, for candidate narrowing. */
    private final java.util.Map<String, List<Integer>> index =
            new java.util.HashMap<String, List<Integer>>();
    /** docId -> parsed, normalised view. Small LRU so hot records stay resident. */
    private final java.util.LinkedHashMap<String, DocumentText> docCache =
            new java.util.LinkedHashMap<String, DocumentText>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, DocumentText> e) {
                    return size() > CACHE_LIMIT;
                }
            };

    private int loadFailures = 0;
    private boolean enumerated = false;

    public KnowledgeRepository(KnowledgeSource source) {
        this.source = source;
    }

    /** Enumerates the knowledge tree. Safe to call repeatedly; only the first call does work. */
    public synchronized void ensureEnumerated() {
        if (enumerated) return;
        enumerated = true;
        Set<String> found = new HashSet<String>();
        for (String root : roots()) {
            collect(root, found);
        }
        paths.addAll(found);
        Collections.sort(paths);
        buildIndex();
    }

    /** The knowledge roots that make up the current MEDIA Alpha scope. */
    protected List<String> roots() {
        return Collections.singletonList("knowledge/hematology");
    }

    public int size() {
        ensureEnumerated();
        return paths.size();
    }

    public int loadFailures() {
        ensureEnumerated();
        return loadFailures;
    }

    public boolean isReady() {
        return size() > 0 && loadFailures() == 0;
    }

    /** All known record paths, sorted for a stable Library listing. */
    public List<String> recordPaths() {
        ensureEnumerated();
        return Collections.unmodifiableList(paths);
    }

    /** Records ranked for a query, highest score first. */
    public List<Result> search(String query, int limit) {
        ensureEnumerated();
        QueryPlan plan = QueryPlan.of(query);
        if (plan.isEmpty() || limit <= 0) return Collections.emptyList();

        List<Result> out = new ArrayList<Result>();
        for (int i : candidates(plan)) {
            DocumentText doc = docFor(i);
            if (doc == null) continue;
            double score = score(plan, doc);
            if (score > MATCH_THRESHOLD) out.add(new Result(doc.record, score));
        }
        Collections.sort(out, new Comparator<Result>() {
            @Override
            public int compare(Result a, Result b) {
                int byScore = Double.compare(b.score, a.score);
                if (byScore != 0) return byScore;
                return a.record.title().compareToIgnoreCase(b.record.title());
            }
        });
        return new ArrayList<Result>(out.subList(0, Math.min(limit, out.size())));
    }

    public Result best(String query) {
        List<Result> r = search(query, 1);
        return r.isEmpty() ? null : r.get(0);
    }

    public boolean isConfident(Result r) {
        return r != null && r.score >= CONFIDENCE_THRESHOLD;
    }

    /** Loads a single record by path, or {@code null} when it cannot be read/parsed. */
    public KnowledgeRecord record(String path) {
        ensureEnumerated();
        int idx = paths.indexOf(path);
        if (idx < 0) return null;
        DocumentText doc = docFor(idx);
        return doc == null ? null : doc.record;
    }

    /** Loads every record. Only used by screens that genuinely need the whole listing. */
    public List<KnowledgeRecord> allRecords() {
        ensureEnumerated();
        List<KnowledgeRecord> out = new ArrayList<KnowledgeRecord>(paths.size());
        for (int i = 0; i < paths.size(); i++) {
            DocumentText doc = docFor(i);
            if (doc != null) out.add(doc.record);
        }
        return out;
    }

    // ------------------------------------------------------------------ indexing

    private void collect(String path, Set<String> found) {
        List<String> names;
        try {
            names = source.list(path);
        } catch (IOException e) {
            loadFailures++;
            return;
        }
        if (names.isEmpty()) return;
        for (String name : names) {
            String child = path + "/" + name;
            List<String> nested;
            try {
                nested = source.list(child);
            } catch (IOException e) {
                loadFailures++;
                continue;
            }
            if (!nested.isEmpty()) {
                collect(child, found);
            } else if (name.endsWith(".json")) {
                found.add(child);
            }
        }
    }

    /**
     * Builds the inverted index from the record bodies.
     *
     * <p>Indexing needs each record's tokens, so every record is parsed once here, but the parse
     * results are deliberately not retained: the document cache stays a small LRU and is filled
     * on demand during scoring. That keeps resident memory independent of corpus size.</p>
     */
    private void buildIndex() {
        for (int i = 0; i < paths.size(); i++) {
            DocumentText doc = parse(i, false);
            if (doc == null) continue;
            for (String token : doc.tokens) {
                List<Integer> bucket = index.get(token);
                if (bucket == null) {
                    bucket = new ArrayList<Integer>(2);
                    index.put(token, bucket);
                }
                bucket.add(i);
            }
        }
    }

    private List<Integer> candidates(QueryPlan plan) {
        Set<Integer> candidate = new java.util.LinkedHashSet<Integer>();
        for (String token : plan.tokens) {
            List<Integer> bucket = index.get(token);
            if (bucket != null) candidate.addAll(bucket);
        }
        // Alias-expanded tokens widen the candidate set as well.
        for (String token : Text.tokens(plan.expanded)) {
            List<Integer> bucket = index.get(token);
            if (bucket != null) candidate.addAll(bucket);
        }
        if (candidate.isEmpty()) {
            // Fall back to scanning all records so partial matches are never lost.
            List<Integer> all = new ArrayList<Integer>(paths.size());
            for (int i = 0; i < paths.size(); i++) all.add(i);
            return all;
        }
        return new ArrayList<Integer>(candidate);
    }

    /** Returns the parsed, cached view for a record index. Not synchronized: callers are bounded. */
    private DocumentText docFor(int index) {
        return parse(index, true);
    }

    private DocumentText parse(int index, boolean cache) {
        String path = paths.get(index);
        if (cache) {
            DocumentText cached = docCache.get(path);
            if (cached != null) return cached;
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject(source.read(path));
            DocumentText doc = DocumentText.of(KnowledgeRecord.of(json), path);
            if (cache) docCache.put(path, doc);
            return doc;
        } catch (Exception e) {
            loadFailures++;
            return null;
        }
    }

    // ------------------------------------------------------------------ scoring

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
            score += 0.05 * Math.min(Text.occurrences(doc.searchable, plan.phrase), 4);
        }
        if (!plan.expanded.isEmpty() && !plan.expanded.equals(plan.phrase)) {
            if (doc.topic.equals(plan.expanded)) score += 0.80;
            if (doc.concept.contains(plan.expanded)) score += 0.30;
            if (doc.searchable.contains(plan.expanded)) score += 0.25;
        }
        if (plan.pureAbbreviation) {
            // A bare acronym should surface the record that actually defines the concept.
            String fullTerm = plan.canonical;
            score += Math.min(0.40, 0.16 * Text.occurrences(doc.searchable, fullTerm));
            score += Math.min(0.08, 0.04 * Text.occurrences(doc.searchable, plan.normalized));
            if (doc.title.contains(fullTerm)) score += 0.25;
            if (doc.concept.contains(fullTerm)) score += 0.20;
        } else {
            String fullTerm = plan.canonical;
            if (!fullTerm.isEmpty() && !fullTerm.equals(plan.phrase)) {
                score += Math.min(0.24, 0.10 * Text.occurrences(doc.searchable, fullTerm));
                if (doc.title.contains(fullTerm)) score += 0.15;
            }
        }
        return score;
    }

    /** A parsed query: canonical phrase, token set and lexicon expansion. */
    public static final class QueryPlan {
        public final String normalized;
        public final String phrase;
        public final String expanded;
        public final String canonical;
        public final List<String> tokens;
        public final boolean pureAbbreviation;
        private final boolean empty;

        private QueryPlan(String normalized, String phrase, String expanded, String canonical,
                          List<String> tokens, boolean pureAbbreviation, boolean empty) {
            this.normalized = normalized;
            this.phrase = phrase;
            this.expanded = expanded;
            this.canonical = canonical;
            this.tokens = tokens;
            this.pureAbbreviation = pureAbbreviation;
            this.empty = empty;
        }

        public static QueryPlan of(String query) {
            String normalized = Text.normalize(query);
            if (normalized.isEmpty()) {
                return new QueryPlan("", "", "", "", Collections.<String>emptyList(), false, true);
            }
            String canonical = MedicalLexicon.canonicalFor(normalized);
            boolean pure = normalized.trim().indexOf(' ') < 0 && canonical != null;

            String expanded;
            if (pure) {
                expanded = canonical;
            } else {
                expanded = MedicalLexicon.expand(normalized);
            }

            String phrase = pure ? canonical : Text.joinTokens(normalized.split(" "));
            List<String> tokens = new ArrayList<String>();
            Text.addTokens(tokens, normalized);
            if (!expanded.equals(normalized)) Text.addTokens(tokens, expanded);

            if (tokens.isEmpty() && phrase.isEmpty()) {
                return new QueryPlan(normalized, "", expanded, canonical == null ? "" : canonical,
                        tokens, false, true);
            }
            return new QueryPlan(normalized, phrase, expanded, canonical == null ? "" : canonical,
                    tokens, pure, false);
        }

        public boolean isEmpty() { return empty; }
    }

    /** Pre-normalised searchable view of a knowledge record. */
    public static final class DocumentText {
        public final KnowledgeRecord record;
        public final String path;
        public final String topic;
        public final String concept;
        public final String title;
        public final String searchable;
        public final List<String> tokens;

        DocumentText(KnowledgeRecord record, String path, String topic, String concept,
                     String title, String searchable, List<String> tokens) {
            this.record = record;
            this.path = path;
            this.topic = topic;
            this.concept = concept;
            this.title = title;
            this.searchable = searchable;
            this.tokens = tokens;
        }

        static DocumentText of(KnowledgeRecord record, String path) {
            return of(record, path, record.topic(), record.subtopic(), record.concept(),
                    record.domain(), record.subject(), contentText(record), terminologyText(record));
        }

        static DocumentText of(KnowledgeRecord record, String path, String topic, String subtopic,
                               String concept, String domain, String subject, String content,
                               String terminology) {
            String nTopic = Text.normalize(topic);
            String nConcept = Text.normalize(concept);
            String nTitle = Text.normalize(topic + " " + subtopic + " " + concept);
            String nSearchable = Text.normalize(concept + " " + topic + " " + subtopic + " "
                    + domain + " " + subject + " " + content + " " + terminology);
            List<String> tokens = Text.tokens(nTitle + " " + nSearchable);
            return new DocumentText(record, path, nTopic, nConcept, nTitle, nSearchable, tokens);
        }

        /** Simplified constructor used by pure-JVM tests that build a document from strings. */
        public static DocumentText of(String topic, String subtopic, String concept, String domain,
                                      String subject, String content, String terminology) {
            return of(null, "", topic, subtopic, concept, domain, subject, content, terminology);
        }

        private static String contentText(KnowledgeRecord record) {
            org.json.JSONObject c = record.content();
            StringBuilder b = new StringBuilder();
            java.util.Iterator<String> keys = c.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = c.opt(k);
                b.append(' ').append(k).append(' ').append(v == null ? "" : v.toString());
            }
            return b.toString();
        }

        private static String terminologyText(KnowledgeRecord record) {
            StringBuilder b = new StringBuilder();
            for (KnowledgeRecord.Term t : record.terminology()) {
                b.append(' ').append(t.term).append(' ').append(t.arabic).append(' ').append(t.notes);
            }
            return b.toString();
        }

        /** Debug helper mirroring {@link String#toLowerCase(Locale)} stability in logs. */
        @Override
        public String toString() {
            return "DocumentText(" + path + ", title=" + title + ")";
        }
    }
}
