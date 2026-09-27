package com.media.android.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.media.android.MediaApplication;
import com.media.android.ai.GenerationOptions;
import com.media.android.ai.engine.MediaAiEngine;
import com.media.android.ai.orchestrator.MediaAiOrchestrator;
import com.media.android.ai.orchestrator.OrchestratedResponse;
import com.media.android.data.HistoryEntry;
import com.media.android.ui.common.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the MEDIA conversation.
 *
 * <p>Lives at the activity scope so the Home and Chat destinations share one transcript. All
 * generation happens on {@link com.media.android.ai.engine.MediaAiEngine#BACKGROUND}, never on the
 * UI thread, so scrolling and input stay responsive while an answer is produced.</p>
 */
public final class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<ChatMessage>> messages =
            new MutableLiveData<List<ChatMessage>>(new ArrayList<ChatMessage>());
    private final MutableLiveData<Boolean> generating = new MutableLiveData<Boolean>(false);
    private final MutableLiveData<String> lastQuestion = new MutableLiveData<String>("");

    private final MediaAiOrchestrator orchestrator;
    private final MediaApplication app;

    public ChatViewModel(MediaApplication app) {
        this.app = app;
        this.orchestrator = app.orchestrator();
    }

    public LiveData<List<ChatMessage>> messages() { return messages; }
    public LiveData<Boolean> generating() { return generating; }
    public LiveData<String> lastQuestion() { return lastQuestion; }

    public boolean isEmpty() {
        List<ChatMessage> current = messages.getValue();
        return current == null || current.isEmpty();
    }

    /** Submits a question and appends the MEDIA answer when it is ready. */
    public void ask(final String rawQuestion) {
        final String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isEmpty() || Boolean.TRUE.equals(generating.getValue())) return;

        lastQuestion.setValue(question);
        final List<ChatMessage> transcript = new ArrayList<ChatMessage>(nonNull());
        transcript.add(ChatMessage.user(question));
        messages.setValue(transcript);
        generating.setValue(true);

        MediaAiEngine.BACKGROUND.execute(() -> {
            GenerationOptions options = GenerationOptions.builder()
                    .preferArabicExplanation(app == null || app.preferences().bilingual())
                    .build();
            final OrchestratedResponse response = orchestrator.answer(question, options);
            final String evidence = evidenceSummary(response);
            final ChatMessage reply = ChatMessage.media(
                    textFor(response), response.confidence, evidence);
            List<ChatMessage> next = new ArrayList<ChatMessage>(transcript);
            next.add(reply);
            messages.postValue(next);
            generating.postValue(false);

            if (app != null) {
                app.history().add(new HistoryEntry(
                        null, question, reply.content, response.confidence,
                        response.state.name(), response.engineId, System.currentTimeMillis()));
            }
        });
    }

    public void clear() {
        messages.setValue(new ArrayList<ChatMessage>());
        lastQuestion.setValue("");
    }

    private List<ChatMessage> nonNull() {
        List<ChatMessage> current = messages.getValue();
        return current == null ? new ArrayList<ChatMessage>() : current;
    }

    /** Appends a transcript state without regeneration (used when reopening from History). */
    public void restore(String question, ChatMessage reply) {
        List<ChatMessage> next = new ArrayList<ChatMessage>();
        next.add(ChatMessage.user(question));
        next.add(reply);
        messages.setValue(next);
    }

    private static String textFor(OrchestratedResponse response) {
        switch (response.state) {
            case ANSWERED:
            case ABSTAINED:
                return response.content;
            case NO_ENGINE:
                return "NO LOCAL ENGINE · لا يوجد محرك محلي\n---\n"
                        + (response.errorMessage == null ? "" : response.errorMessage);
            default:
                return "INTERNAL ERROR · خطأ داخلي\n---\n"
                        + (response.errorMessage == null ? "" : response.errorMessage);
        }
    }

    private static String evidenceSummary(OrchestratedResponse response) {
        if (response.evidenceIds.isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        for (String id : response.evidenceIds) {
            if (b.length() > 0) b.append("\n");
            b.append(id);
        }
        return b.toString();
    }
}
