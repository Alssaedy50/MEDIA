package com.media.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * MEDIA Alpha entry screen.
 *
 * <p>Renders offline Hematology answers as bilingual, clearly labelled medical sections. When the
 * local evidence is not strong enough the screen shows a polite Arabic abstention instead of a
 * guess.</p>
 */
public final class MainActivity extends Activity {
    private static final String DIVIDER = "────────────────────────";

    private OfflineKnowledge knowledge;
    private EditText question;
    private TextView answer;
    private TextView status;
    private TextView scope;
    private Button ask;
    private Button clear;
    private Button copy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        question = findViewById(R.id.question);
        answer = findViewById(R.id.answer);
        status = findViewById(R.id.status);
        scope = findViewById(R.id.scope);
        ask = findViewById(R.id.ask_button);
        clear = findViewById(R.id.clear_button);
        copy = findViewById(R.id.copy_button);

        knowledge = new OfflineKnowledge(this);
        updateStatus();
        clearQuestion();

        ask.setOnClickListener(v -> answerQuestion());
        clear.setOnClickListener(v -> clearQuestion());
        copy.setOnClickListener(v -> copyAnswer());

        question.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                answerQuestion();
                return true;
            }
            return false;
        });

        bindChip(R.id.chip_bone_marrow, "Bone marrow");
        bindChip(R.id.chip_erythropoiesis, "Erythropoiesis");
        bindChip(R.id.chip_ida, "Iron deficiency anemia");
        bindChip(R.id.chip_cbc, "CBC");
        bindChip(R.id.chip_epo, "EPO");
        bindChip(R.id.chip_sickle, "Sickle cell disease");
    }

    private void bindChip(int viewId, String query) {
        View chip = findViewById(viewId);
        if (chip != null) chip.setOnClickListener(v -> askPrompt(query));
    }

    private void updateStatus() {
        scope.setText("نطاق المعرفة: أمراض الدم (Hematology) • " + knowledge.size() + " سجلاً طبياً محلياً");
        if (knowledge.loadFailures() == 0 && knowledge.size() > 0) {
            status.setText("● يعمل دون اتصال  •  " + knowledge.size() + " records ready");
            status.setTextColor(0xFF0B6B55);
        } else if (knowledge.size() > 0) {
            status.setText("● OFFLINE  •  " + knowledge.size() + " records loaded  •  " + knowledge.loadFailures() + " load error(s)");
            status.setTextColor(0xFF9A6700);
        } else {
            status.setText("● OFFLINE LIBRARY ERROR  •  No medical records loaded");
            status.setTextColor(0xFFB42318);
        }
    }

    private void askPrompt(String text) {
        question.setText(text);
        question.setSelection(question.length());
        answerQuestion();
    }

    private void clearQuestion() {
        question.setText("");
        answer.setText("اسأل عن موضوع في مكتبة أمراض الدم المحلية.\n"
                + "Ask about a topic in the current offline Hematology library.\n\n"
                + "MEDIA لا يعرض نتيجة إلا عند وجود دليل محلي موثوق ومطابقة كافية.");
        copy.setVisibility(View.GONE);
        updateStatus();
        question.requestFocus();
    }

    private void answerQuestion() {
        String q = question.getText().toString().trim();
        if (q.isEmpty()) {
            answer.setText("الرجاء إدخال سؤال طبي أولاً.\nEnter a medical question first.");
            copy.setVisibility(View.GONE);
            question.requestFocus();
            return;
        }

        ask.setEnabled(false);
        OfflineKnowledge.Result result = knowledge.best(q);
        ask.setEnabled(true);

        if (!knowledge.isConfident(result)) {
            answer.setText(abstention(q));
            copy.setVisibility(View.GONE);
            return;
        }

        answer.setText(formatAnswer(result.record, result.score));
        copy.setVisibility(View.VISIBLE);
    }

    private void copyAnswer() {
        String text = answer.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("MEDIA answer", text));
            status.setText("✓ تم نسخ الإجابة  •  " + knowledge.size() + " records • Offline");
        }
    }

    /** Bilingual safe abstention shown when no sufficiently supported local evidence exists. */
    private static String abstention(String query) {
        return "لا توجد أدلة محلية موثوقة كافية\n"
                + "NO SUPPORTED LOCAL MATCH\n\n"
                + "لم نعثر في وحدة أمراض الدم (Hematology) المضمّنة حالياً على دليل محلي موثوق "
                + "يجيب عن هذا السؤال بثقة كافية، ولذلك يلتزم MEDIA الصمت بدلاً من التخمين.\n\n"
                + "سؤالك: " + query + "\n\n"
                + "جرّب مصطلحاً أو اختصاراً طبياً محدّداً مثل:\n"
                + "• نخاع العظم — Bone marrow\n"
                + "• تكون الكريات الحمراء — Erythropoiesis\n"
                + "• فقر الدم بعوز الحديد — Iron deficiency anemia (IDA)\n"
                + "• تحليل الدم الشامل — CBC\n"
                + "• الإرثروبويتين — EPO\n"
                + "• مرض الخلايا المنجلية — Sickle cell disease\n\n"
                + "تعمل MEDIA دون اتصال بالإنترنت وتقتصر على سجلات وحدة أمراض الدم المُدقَّقة محلياً.";
    }

    private String formatAnswer(JSONObject r, double score) {
        String topic = r.optString("topic", r.optString("concept", "Medical topic"));
        String concept = r.optString("concept", "");
        String domain = r.optString("domain", "Hematology");
        String subject = r.optString("subject", "");
        StringBuilder b = new StringBuilder();

        // Subject & topic badge
        b.append(DIVIDER).append("\n");
        String badge = subject.isEmpty() ? domain : domain + " • " + subject;
        b.append(badge.toUpperCase(Locale.ROOT)).append("\n");
        b.append(topic).append("\n");
        b.append(DIVIDER).append("\n\n");

        // Concept name (English)
        if (!concept.isEmpty() && !concept.equalsIgnoreCase(topic)) {
            b.append("اسم المفهوم (Concept)\n");
            b.append(concept).append("\n\n");
        }

        JSONObject c = r.optJSONObject("content");
        if (c != null) {
            append(b, "📌 التعريف الطبي · Definition", c.optString("definition", ""));
            append(b, "💡 الشرح والتوضيح · Explanation", c.optString("explanation", ""));
            appendMechanism(b, c);
            appendCausesEffects(b, c);
            appendClinical(b, c);
        }

        appendTerminology(b, r.optJSONArray("terminology"));
        appendHighYield(b, c == null ? null : c.optJSONArray("high_yield"));
        appendSources(b, r.optJSONArray("sources"));

        b.append("مطابقة الدليل المحلي · Local evidence match  ")
                .append(String.format(Locale.US, "%.2f", score)).append("\n")
                .append("استرجاع دون اتصال · OFFLINE RETRIEVAL  •  NO INTERNET REQUIRED");
        return b.toString();
    }

    private static void appendMechanism(StringBuilder b, JSONObject c) {
        String mechanism = c.optString("mechanism", "");
        String structure = c.optString("structure", "");
        String function = c.optString("function", "");
        if (isBlank(mechanism) && isBlank(structure) && isBlank(function)) return;

        b.append("⚙️ الآلية والمسار · Mechanism & Pathway\n");
        if (!isBlank(mechanism)) b.append(arrows(mechanism)).append("\n\n");
        if (!isBlank(structure)) b.append("البنية (Structure): ").append(structure.trim()).append("\n\n");
        if (!isBlank(function)) b.append("الوظيفة (Function): ").append(function.trim()).append("\n\n");
    }

    private static void appendCausesEffects(StringBuilder b, JSONObject c) {
        JSONArray causes = c.optJSONArray("causes");
        JSONArray effects = c.optJSONArray("effects");
        boolean hasCauses = causes != null && causes.length() > 0;
        boolean hasEffects = effects != null && effects.length() > 0;
        if (!hasCauses && !hasEffects) return;

        b.append("⚠️ الأسباب والنتائج · Causes & Effects\n");
        if (hasCauses) {
            b.append("الأسباب (Causes):\n");
            appendBullets(b, causes);
        }
        if (hasEffects) {
            b.append("النتائج (Effects):\n");
            appendBullets(b, effects);
        }
        b.append("\n");
    }

    private static void appendClinical(StringBuilder b, JSONObject c) {
        String clinical = c.optString("clinical_relevance", "");
        String diagnosis = c.optString("diagnosis", "");
        String treatment = c.optString("treatment", "");
        if (isBlank(clinical) && isBlank(diagnosis) && isBlank(treatment)) return;

        b.append("🩺 التطبيق السريري · Clinical Relevance\n");
        if (!isBlank(clinical)) b.append(clinical.trim()).append("\n\n");
        if (!isBlank(diagnosis)) b.append("التشخيص (Diagnosis): ").append(diagnosis.trim()).append("\n\n");
        if (!isBlank(treatment)) b.append("العلاج (Treatment): ").append(treatment.trim()).append("\n\n");
    }

    private static void appendTerminology(StringBuilder b, JSONArray terminology) {
        if (terminology == null || terminology.length() == 0) return;
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < Math.min(terminology.length(), 6); i++) {
            JSONObject t = terminology.optJSONObject(i);
            if (t == null) continue;
            String term = t.optString("term", "");
            String arabic = t.optString("Arabic", "");
            if (term.isEmpty()) continue;
            body.append("• ").append(term);
            if (!arabic.isEmpty()) body.append(" — ").append(arabic);
            body.append("\n");
        }
        if (body.length() > 0) {
            b.append("المصطلحات · Key Terminology\n").append(body).append("\n");
        }
    }

    private static void appendHighYield(StringBuilder b, JSONArray high) {
        if (high == null || high.length() == 0) return;
        b.append("⭐ لؤلؤة امتحانية · High-Yield Pearls\n");
        appendBullets(b, high);
        b.append("\n");
    }

    private static void appendSources(StringBuilder b, JSONArray sources) {
        if (sources == null || sources.length() == 0) return;
        b.append("المصادر · Evidence Sources\n");
        for (int i = 0; i < Math.min(sources.length(), 4); i++) {
            JSONObject s = sources.optJSONObject(i);
            if (s == null) continue;
            String title = s.optString("title", "");
            String year = s.optString("year", "");
            String location = s.optString("location", "");
            if (title.isEmpty()) continue;
            b.append("• ").append(title);
            if (!year.isEmpty()) b.append(" (").append(year).append(")");
            if (!location.isEmpty()) b.append(" — ").append(location);
            b.append("\n");
        }
        b.append("\n");
    }

    private static void appendBullets(StringBuilder b, JSONArray values) {
        for (int i = 0; i < Math.min(values.length(), 8); i++) {
            String value = values.optString(i, "").trim();
            if (!value.isEmpty()) b.append("• ").append(value).append("\n");
        }
    }

    private static void append(StringBuilder b, String title, String text) {
        if (!isBlank(text) && !text.trim().equals("[]")) {
            b.append(title).append("\n").append(text.trim()).append("\n\n");
        }
    }

    /** Renders pathway arrows consistently, e.g. "A -> B" becomes "A ➜ B". */
    private static String arrows(String text) {
        return text.trim()
                .replaceAll("\\s*(-{1,2}>|=>|→|➔|➜)\\s*", " \u279C ")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
