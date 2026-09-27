package com.media.android.ui.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.MainActivity;
import com.media.android.R;
import com.media.android.ui.common.ChatMessage;
import com.media.android.ui.common.MessageAdapter;

import java.util.List;

/** Chat transcript with structured MEDIA answers, evidence, actions and a keyboard-safe composer. */
public final class ChatFragment extends Fragment implements MessageAdapter.Listener {

    private RecyclerView list;
    private MessageAdapter adapter;
    private View emptyState;
    private View thinkingRow;
    private EditText input;
    private ChatViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        list = view.findViewById(R.id.chat_list);
        emptyState = view.findViewById(R.id.chat_empty);
        thinkingRow = view.findViewById(R.id.thinking_row);
        input = view.findViewById(R.id.chat_input);
        ImageButton send = view.findViewById(R.id.chat_send);
        View clear = view.findViewById(R.id.chat_clear);

        adapter = new MessageAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        list.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        send.setOnClickListener(v -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        clear.setOnClickListener(v -> viewModel().clear());

        MainActivity activity = activity();
        if (activity != null) {
            viewModel = activity.chatViewModel();
        } else {
            viewModel = new ViewModelProvider(this, new SimpleFactory(
                    com.media.android.MediaApplication.from(requireContext())))
                    .get(ChatViewModel.class);
        }

        viewModel.messages().observe(getViewLifecycleOwner(), this::render);
        viewModel.generating().observe(getViewLifecycleOwner(),
                generating -> thinkingRow.setVisibility(
                        Boolean.TRUE.equals(generating) ? View.VISIBLE : View.GONE));
    }

    private void render(List<ChatMessage> messages) {
        adapter.setMessages(messages);
        boolean hasContent = messages != null && !messages.isEmpty();
        emptyState.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        list.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        if (hasContent) {
            list.scrollToPosition(messages.size() - 1);
        }
        MainActivity activity = activity();
        if (activity != null) activity.renderStatus();
    }

    private void submit() {
        String question = input.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_query, Toast.LENGTH_SHORT).show();
            return;
        }
        input.setText("");
        viewModel().ask(question);
    }

    private ChatViewModel viewModel() {
        return viewModel;
    }

    @Nullable
    private MainActivity activity() {
        return getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
    }

    // ------------------------------------------------ MessageAdapter.Listener

    @Override
    public void onCopy(ChatMessage message) {
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("MEDIA answer", message.content));
            Toast.makeText(requireContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRegenerate(ChatMessage message) {
        String question = viewModel.lastQuestion().getValue();
        if (question != null && !question.isEmpty()) {
            viewModel.clear();
            viewModel.ask(question);
        }
    }

    @Override
    public void onShare(ChatMessage message) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message.content);
        startActivity(Intent.createChooser(share, getString(R.string.action_share)));
    }

    @Override
    public void onFeedback(ChatMessage message, int rating) {
        Toast.makeText(requireContext(),
                rating > 0 ? R.string.toast_feedback_up : R.string.toast_feedback_down,
                Toast.LENGTH_SHORT).show();
    }

    private static final class SimpleFactory implements ViewModelProvider.Factory {
        private final com.media.android.MediaApplication app;

        SimpleFactory(com.media.android.MediaApplication app) {
            this.app = app;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ChatViewModel(app);
        }
    }
}
