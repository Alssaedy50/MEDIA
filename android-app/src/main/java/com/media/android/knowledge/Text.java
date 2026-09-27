package com.media.android.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic text normalisation shared by the retrieval index and the query parser.
 *
 * <p>Handles the bilingual (Arabic + Latin) medical text used across the knowledge records:
 * Latin text is lower-cased; Arabic diacritics and the common orthographic variants (alef, yeh,
 * teh marbuta, tatweel) are folded so a student query matches a record regardless of how the
 * Arabic word was typed. Keeping this in one place guarantees that documents and queries are
 * normalised identically.</p>
 */
public final class Text {

    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
            "a", "an", "the", "what", "which", "who", "where", "when", "why", "how", "is", "are",
            "was", "were", "do", "does", "did", "can", "could", "would", "should", "please",
            "explain", "describe", "tell", "me", "about", "of", "for", "to", "in", "on", "with",
            "and", "or"));

    /** Arabic function words that would otherwise dominate short Arabic queries. */
    private static final Set<String> ARABIC_STOP_WORDS = new HashSet<String>(Arrays.asList(
            "في", "من", "على", "عن", "ما", "هو", "هي", "هل", "و", "او", "ثم", "هذا", "هذه",
            "التي", "الذي", "مع", "الى", "كل", "اي", "كيف", "لماذا", "متى", "اذكر"));

    private Text() { }

    /** Normalises a string for indexing and matching. Never returns {@code null}. */
    public static String normalize(String s) {
        if (s == null) return "";
        String folded = foldArabic(s.toLowerCase(Locale.ROOT));
        return folded
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Folds Arabic orthographic variants so that common typing differences do not break matching:
     * removes diacritics/tatweel and unifies alef, yeh and teh marbuta forms.
     */
    static String foldArabic(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '\u064B' && c <= '\u065F') || c == '\u0670' || c == '\u0640'
                    || c == '\u06D6' || c == '\u06DC') {
                continue; // harakat, dagger alef, tatweel, Quranic marks
            }
            switch (c) {
                case '\u0622': // alef with madda
                case '\u0623': // alef with hamza above
                case '\u0625': // alef with hamza below
                case '\u0671': // alef wasla
                    out.append('\u0627');
                    break;
                case '\u0649': // alef maksura
                case '\u0626': // yeh with hamza
                    out.append('\u064A');
                    break;
                case '\u0629': // teh marbuta
                    out.append('\u0647');
                    break;
                case '\u0624': // waw with hamza
                    out.append('\u0648');
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    /** Lower-cased, de-duplicated, stop-word filtered tokens in order of appearance. */
    public static List<String> tokens(String s) {
        List<String> out = new ArrayList<String>();
        addTokens(out, normalize(s));
        return out;
    }

    /** Appends the tokens of an already-normalised string to {@code out}, avoiding duplicates. */
    public static void addTokens(List<String> out, String normalized) {
        Set<String> seen = new LinkedHashSet<String>(out);
        for (String token : normalized.split(" ")) {
            if (token.length() >= 2 && !isStopWord(token) && seen.add(token)) out.add(token);
        }
    }

    static boolean isStopWord(String token) {
        return STOP_WORDS.contains(token) || ARABIC_STOP_WORDS.contains(token);
    }

    /** Builds a stop-word filtered phrase from raw (already normalised) tokens. */
    static String joinTokens(String[] words) {
        StringBuilder b = new StringBuilder();
        Set<String> seen = new HashSet<String>();
        for (String word : words) {
            if (word.length() >= 2 && !isStopWord(word) && seen.add(word)) {
                if (b.length() > 0) b.append(' ');
                b.append(word);
            }
        }
        return b.toString();
    }

    /** Counts whole-word/phrase occurrences in an already normalised string. */
    public static int occurrences(String haystack, String needle) {
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

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Trims and collapses internal whitespace for display. */
    public static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    /** Renders pathway arrows consistently, e.g. {@code "A -> B"} becomes {@code "A ➜ B"}. */
    public static String arrows(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("\\s*(-{1,2}>|=>|→|➔|➜)\\s*", " \u279C ")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }
}
