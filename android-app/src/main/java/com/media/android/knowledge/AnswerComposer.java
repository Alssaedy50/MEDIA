package com.media.android.knowledge;

import java.util.List;
import java.util.Locale;

/**
 * Composes the structured, bilingual MEDIA answer markup for a knowledge record.
 *
 * <p>The output is a small, line-oriented markup that keeps the answer layer decoupled from the UI
 * and lets a future neural engine emit the same shape. The renderer understands:</p>
 * <pre>
 *   #  Topic heading
 *   ## Section heading
 *   -  bullet item
 *   |  table row (cells separated by " | ", first row is the header)
 *   ---  divider
 *   &gt;  evidence / source item
 *   plain text  paragraph
 *   **word**  inline emphasis
 * </pre>
 *
 * <p>Sections are only emitted when the record actually carries content for them, so the answer
 * shape adapts to each record instead of forcing empty headings.</p>
 */
public final class AnswerComposer {

    /** Confidence band, used to label how strong the local evidence is. */
    public enum Confidence { HIGH, MEDIUM, LOW }

    public static Confidence confidenceBand(double score) {
        if (score >= 0.70) return Confidence.HIGH;
        if (score >= KnowledgeRepository.CONFIDENCE_THRESHOLD) return Confidence.MEDIUM;
        return Confidence.LOW;
    }

    private AnswerComposer() { }

    /** Composes the answer body for a retrieval result. */
    public static String compose(KnowledgeRepository.Result result) {
        return compose(result.record, result.score);
    }

    public static String compose(KnowledgeRecord r, double score) {
        return compose(r, score, true);
    }

    /**
     * Composes the answer body.
     *
     * @param bilingual when {@code true} section headings carry Arabic alongside the English
     *                  medical terminology; when {@code false} the Arabic section labels are
     *                  omitted (the record's own Arabic content is never translated away).
     */
    public static String compose(KnowledgeRecord r, double score, boolean bilingual) {
        StringBuilder b = new StringBuilder();

        String subject = r.subject();
        String domain = r.domain();
        String badge = subject.isEmpty() ? domain : domain + " • " + subject;

        // ---- Title block ----
        b.append("# ").append(r.title()).append('\n');
        if (!badge.isEmpty()) {
            b.append("## ").append(Text.clean(badge)).append('\n');
        }
        String concept = r.concept();
        if (!Text.isBlank(concept) && !concept.equalsIgnoreCase(r.title())) {
            b.append("> ").append(concept).append('\n');
        }
        b.append("---\n");

        // ---- Definition ----
        section(b, h(bilingual, "Definition", "التعريف"), r.contentString("definition"));

        // ---- Explanation ----
        section(b, h(bilingual, "Explanation", "الشرح"), r.contentString("explanation"));

        // ---- Mechanism / structure / function ----
        String mechanism = r.contentString("mechanism");
        String structure = r.contentString("structure");
        String function = r.contentString("function");
        if (!Text.isBlank(mechanism) || !Text.isBlank(structure) || !Text.isBlank(function)) {
            b.append("## ").append(h(bilingual, "Mechanism & Structure", "الآلية والبنية")).append('\n');
            if (!Text.isBlank(mechanism)) b.append(Text.arrows(mechanism)).append("\n\n");
            if (!Text.isBlank(structure)) b.append(h(bilingual, "Structure", "البنية")).append(": ").append(structure).append("\n\n");
            if (!Text.isBlank(function)) b.append(h(bilingual, "Function", "الوظيفة")).append(": ").append(function).append("\n\n");
        }

        // ---- Causes & effects ----
        List<String> causes = r.contentList("causes");
        List<String> effects = r.contentList("effects");
        if (!causes.isEmpty() || !effects.isEmpty()) {
            b.append("## ").append(h(bilingual, "Causes & Effects", "الأسباب والنتائج")).append('\n');
            if (!causes.isEmpty()) {
                b.append(h(bilingual, "Causes", "الأسباب")).append('\n');
                bullets(b, causes);
            }
            if (!effects.isEmpty()) {
                b.append(h(bilingual, "Effects", "النتائج")).append('\n');
                bullets(b, effects);
            }
            b.append('\n');
        }

        // ---- Clinical relevance / diagnosis / treatment ----
        String clinical = r.contentString("clinical_relevance");
        String diagnosis = r.contentString("diagnosis");
        String treatment = r.contentString("treatment");
        if (!Text.isBlank(clinical) || !Text.isBlank(diagnosis) || !Text.isBlank(treatment)) {
            b.append("## ").append(h(bilingual, "Clinical Relevance", "الأهمية السريرية")).append('\n');
            if (!Text.isBlank(clinical)) b.append(clinical).append("\n\n");
            if (!Text.isBlank(diagnosis)) b.append(h(bilingual, "Diagnosis", "التشخيص")).append(": ").append(diagnosis).append("\n\n");
            if (!Text.isBlank(treatment)) b.append(h(bilingual, "Treatment", "العلاج")).append(": ").append(treatment).append("\n\n");
        }

        // ---- High-yield key points ----
        List<String> highYield = r.contentList("high_yield");
        if (!highYield.isEmpty()) {
            b.append("## ").append(h(bilingual, "Key Points", "أهم النقاط")).append('\n');
            bullets(b, highYield);
            b.append('\n');
        }

        // ---- Terminology ----
        List<KnowledgeRecord.Term> terms = r.terminology();
        if (!terms.isEmpty()) {
            b.append("## ").append(h(bilingual, "Medical Terminology", "المصطلحات الطبية")).append('\n');
            for (KnowledgeRecord.Term t : terms) {
                b.append("- ").append(t.term);
                if (!t.arabic.isEmpty()) b.append(" | ").append(t.arabic);
                if (!t.pronunciation.isEmpty()) b.append(" | ").append(t.pronunciation);
                b.append('\n');
            }
            b.append('\n');
        }

        // ---- Evidence ----
        List<KnowledgeRecord.Source> sources = r.sources();
        if (!sources.isEmpty()) {
            b.append("## ").append(h(bilingual, "Evidence", "المصدر والدليل")).append('\n');
            for (KnowledgeRecord.Source s : sources) {
                StringBuilder line = new StringBuilder();
                line.append(s.title);
                if (!s.year.isEmpty()) line.append(" (").append(s.year).append(")");
                if (!s.sourceType.isEmpty()) line.append(" · ").append(s.sourceType);
                if (!s.location.isEmpty()) line.append(" · ").append(s.location);
                b.append("> ").append(line).append('\n');
            }
            b.append('\n');
        }

        // ---- Provenance footer ----
        b.append("---\n");
        b.append("Local evidence match · مطابقة الدليل المحلي: ")
                .append(String.format(Locale.US, "%.2f", score)).append('\n');
        b.append("Offline retrieval · استرجاع دون اتصال · No Internet required\n");
        return b.toString();
    }

    /** Section heading, optionally bilingual, so the Arabic labels can be toggled in Settings. */
    private static String h(boolean bilingual, String english, String arabic) {
        return bilingual ? english + " \u00B7 " + arabic : english;
    }

    private static void section(StringBuilder b, String heading, String body) {
        if (Text.isBlank(body) || body.trim().equals("[]")) return;
        b.append("## ").append(heading).append('\n').append(body).append("\n\n");
    }

    private static void bullets(StringBuilder b, List<String> values) {
        int limit = Math.min(values.size(), 10);
        for (int i = 0; i < limit; i++) {
            String v = values.get(i);
            if (!Text.isBlank(v)) b.append("- ").append(v).append('\n');
        }
        b.append('\n');
    }
}
