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

public final class MainActivity extends Activity {
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

        findViewById(R.id.prompt_1).setOnClickListener(v -> askPrompt("What is bone marrow?"));
        findViewById(R.id.prompt_2).setOnClickListener(v -> askPrompt("Explain erythropoiesis"));
        findViewById(R.id.prompt_3).setOnClickListener(v -> askPrompt("What causes iron deficiency anemia?"));
    }

    private void updateStatus() {
        scope.setText("Current scope: Hematology • " + knowledge.size() + " local medical records");
        if (knowledge.loadFailures() == 0 && knowledge.size() > 0) {
            status.setText("● OFFLINE  •  " + knowledge.size() + " medical records ready");
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
        answer.setText("Ask about a topic in the current offline medical library.\n\nMEDIA will only display an evidence-backed local result when the match is sufficiently strong.");
        copy.setVisibility(View.GONE);
        updateStatus();
        question.requestFocus();
    }

    private void answerQuestion() {
        String q = question.getText().toString().trim();
        if (q.isEmpty()) {
            answer.setText("Enter a medical question first.");
            copy.setVisibility(View.GONE);
            question.requestFocus();
            return;
        }

        ask.setEnabled(false);
        OfflineKnowledge.Result result = knowledge.best(q);
        ask.setEnabled(true);

        if (!knowledge.isConfident(result)) {
            answer.setText("NO SUPPORTED LOCAL MATCH\n\nI couldn't find sufficiently supported evidence in the offline Hematology library for this question.\n\nTry a more specific topic, for example:\n• bone marrow\n• erythropoiesis\n• iron deficiency anemia\n• sickle cell disease");
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
            status.setText("✓ ANSWER COPIED  •  " + knowledge.size() + " records • Offline");
        }
    }

    private String formatAnswer(JSONObject r, double score) {
        String topic = r.optString("topic", r.optString("concept", "Medical topic"));
        String concept = r.optString("concept", "");
        StringBuilder b = new StringBuilder();
        b.append(topic).append("\n");
        if (!concept.isEmpty() && !concept.equalsIgnoreCase(topic)) b.append(concept).append("\n");
        b.append("────────────────────────\n");

        JSONObject c = r.optJSONObject("content");
        if (c != null) {
            append(b, "DEFINITION", c.optString("definition", ""));
            append(b, "EXPLANATION", c.optString("explanation", ""));
            append(b, "MECHANISM", c.optString("mechanism", ""));
            append(b, "STRUCTURE", c.optString("structure", ""));
            append(b, "FUNCTION", c.optString("function", ""));
            appendArray(b, "CAUSES", c.optJSONArray("causes"));
            appendArray(b, "EFFECTS", c.optJSONArray("effects"));
            append(b, "CLINICAL RELEVANCE", c.optString("clinical_relevance", ""));
            append(b, "DIAGNOSIS", c.optString("diagnosis", ""));
            append(b, "TREATMENT", c.optString("treatment", ""));

            JSONArray high = c.optJSONArray("high_yield");
            if (high != null && high.length() > 0) {
                b.append("HIGH-YIELD\n");
                for (int i = 0; i < Math.min(high.length(), 6); i++) {
                    b.append("• ").append(high.optString(i)).append("\n");
                }
                b.append("\n");
            }
        }

        JSONArray terminology = r.optJSONArray("terminology");
        if (terminology != null && terminology.length() > 0) {
            b.append("KEY TERMINOLOGY\n");
            for (int i = 0; i < Math.min(terminology.length(), 6); i++) {
                JSONObject t = terminology.optJSONObject(i);
                if (t == null) continue;
                String term = t.optString("term", "");
                String arabic = t.optString("Arabic", "");
                if (!term.isEmpty()) {
                    b.append("• ").append(term);
                    if (!arabic.isEmpty()) b.append(" — ").append(arabic);
                    b.append("\n");
                }
            }
            b.append("\n");
        }

        JSONArray sources = r.optJSONArray("sources");
        if (sources != null && sources.length() > 0) {
            b.append("SOURCES\n");
            for (int i = 0; i < Math.min(sources.length(), 4); i++) {
                JSONObject s = sources.optJSONObject(i);
                if (s != null) {
                    String title = s.optString("title", "");
                    String year = s.optString("year", "");
                    if (!title.isEmpty()) b.append("• ").append(title);
                    if (!year.isEmpty()) b.append(" (").append(year).append(")");
                    b.append("\n");
                }
            }
            b.append("\n");
        }

        b.append("LOCAL EVIDENCE MATCH  ")
         .append(String.format(Locale.US, "%.2f", score))
         .append("\nOFFLINE RETRIEVAL  •  NO INTERNET REQUIRED");
        return b.toString();
    }

    private static void appendArray(StringBuilder b, String title, JSONArray values) {\n        if (values == null || values.length() == 0) return;\n        b.append(title).append("\\n");\n        for (int i = 0; i < Math.min(values.length(), 8); i++) {\n            String value = values.optString(i, "").trim();\n            if (!value.isEmpty()) b.append("• ").append(value).append("\\n");\n        }\n        b.append("\\n");\n    }\n\n    private static void append(StringBuilder b, String title, String text) {
        if (text != null && !text.trim().isEmpty() && !text.trim().equals("[]")) {
            b.append(title).append("\n").append(text.trim()).append("\n\n");
        }
    }
}
