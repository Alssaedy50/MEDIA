package com.media.android.ai.orchestrator;

/**
 * Classifies what the student is asking for, so the orchestrator can tailor the response.
 *
 * <p>Intent detection is intentionally lexical and deterministic in the current Alpha. It is a
 * normalisation hint for the answer layer, never a source of medical content.</p>
 */
public final class IntentDetector {

    public enum Intent {
        /** A single medical term or acronym, e.g. "CBC". */
        TERM,
        /** A request to explain a concept. */
        EXPLAIN,
        /** A clinical vignette / case-style question. */
        CLINICAL_CASE,
        /** A study-oriented request ("study X", "high yield"). */
        STUDY,
        /** A request for a summary. */
        SUMMARY,
        /** Anything else. */
        GENERAL
    }

    private IntentDetector() { }

    public static Intent detect(String query) {
        if (query == null) return Intent.GENERAL;
        String q = com.media.android.knowledge.Text.normalize(query);

        if (containsAny(q, "clinical", "case", "vignette", "patient", "presenting", "history of",
                "diagnosis", "management", "حالة", "مريض", "سريري")) {
            return Intent.CLINICAL_CASE;
        }
        if (containsAny(q, "summarize", "summarise", "summary", "overview", "in short", "ملخص",
                "لخص", "باختصار")) {
            return Intent.SUMMARY;
        }
        if (containsAny(q, "study", "high yield", "highyield", "exam", "revision", "memorize",
                "امتحان", "مراجعة", "مذاكرة", "اهم النقاط")) {
            return Intent.STUDY;
        }
        if (containsAny(q, "explain", "what is", "what are", "define", "definition", "describe",
                "اشرح", "فسر", "ما هو", "ما هي", "عرف", "التعريف")) {
            return Intent.EXPLAIN;
        }
        if (q.indexOf(' ') < 0 && q.length() >= 2) {
            return Intent.TERM;
        }
        return Intent.GENERAL;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }
}
