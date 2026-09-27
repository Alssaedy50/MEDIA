package com.media.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private OfflineKnowledge knowledge;
    private EditText question;
    private TextView answer;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        question = findViewById(R.id.question);
        answer = findViewById(R.id.answer);
        status = findViewById(R.id.status);

        knowledge = new OfflineKnowledge(this);
        status.setText("● OFFLINE • " + knowledge.size() + " medical records loaded locally");
        status.setTextColor(0xFF2E7D32);

        Button ask = findViewById(R.id.ask_button);
        ask.setOnClickListener(v -> answerQuestion());

        question.setOnEditorActionListener((v, actionId, event) -> { answerQuestion(); return true; });
    }

    private void answerQuestion() {
        String q = question.getText().toString().trim();
        if (q.isEmpty()) {
            answer.setText("Write a medical question first.");
            return;
        }

        OfflineKnowledge.Result result = knowledge.best(q);
        if (!knowledge.isConfident(result)) {
            answer.setText("I couldn't find sufficiently supported local evidence for this question.\n\nSafe abstention: no online source was requested and no unsupported medical claim was generated.");
            return;
        }

        answer.setText(formatAnswer(result.record, result.score));
    }

    private String formatAnswer(JSONObject r, double score) {
        StringBuilder b = new StringBuilder();
        b.append(r.optString("topic", r.optString("concept", "Medical topic"))).append("\n\n");

        JSONObject c = r.optJSONObject("content");
        if (c != null) {
            append(b, "Definition", c.optString("definition", ""));
            append(b, "Explanation", c.optString("explanation", ""));
            append(b, "Mechanism", c.optString("mechanism", ""));
            append(b, "Clinical relevance", c.optString("clinical_relevance", ""));
            JSONArray high = c.optJSONArray("high_yield");
            if (high != null && high.length() > 0) {
                b.append("\nHigh-yield:\n");
                for (int i=0;i<Math.min(high.length(),5);i++) b.append("• ").append(high.optString(i)).append("\n");
            }
        }

        JSONArray sources = r.optJSONArray("sources");
        if (sources != null && sources.length() > 0) {
            b.append("\nSources:\n");
            for (int i=0;i<Math.min(sources.length(),4);i++) {
                JSONObject s = sources.optJSONObject(i);
                if (s != null) b.append("• ").append(s.optString("title","")).append(" (").append(s.optString("year","")).append(")\n");
            }
        }
        b.append("\nLocal evidence match: ").append(String.format(Locale.US, "%.2f", score));
        b.append("\nMode: Offline retrieval + evidence-aware rendering");
        return b.toString();
    }

    private static void append(StringBuilder b, String title, String text) {
        if (text != null && !text.isEmpty()) b.append(title).append(":\n").append(text).append("\n\n");
    }
}