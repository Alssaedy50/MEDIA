package com.media.android.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.R;
import com.media.android.knowledge.AnswerComposer;

import java.util.Locale;

/** One chat message: a user bubble or a structured MEDIA response with actions and evidence. */
public final class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface Listener {
        void onCopy(ChatMessage message);
        void onRegenerate(ChatMessage message);
        void onShare(ChatMessage message);
        void onFeedback(ChatMessage message, int rating);
    }

    private final java.util.List<ChatMessage> messages = new java.util.ArrayList<ChatMessage>();
    private final Listener listener;

    public MessageAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setMessages(java.util.List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void append(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).id.hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? 0 : 1;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(
                viewType == 0 ? R.layout.item_message_user : R.layout.item_message_media,
                parent, false);
        return new MessageViewHolder(v, viewType == 0);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static final class MessageViewHolder extends RecyclerView.ViewHolder {
        private final boolean user;
        private final TextView text;
        private final AnswerRenderer answer;
        private final View actionsRow;
        private final View evidenceRow;
        private final TextView evidenceText;
        private final TextView confidenceText;

        MessageViewHolder(View itemView, boolean user) {
            super(itemView);
            this.user = user;
            text = itemView.findViewById(R.id.message_text);
            answer = itemView.findViewById(R.id.answer_text);
            actionsRow = itemView.findViewById(R.id.message_actions);
            evidenceRow = itemView.findViewById(R.id.evidence_row);
            evidenceText = itemView.findViewById(R.id.evidence_text);
            confidenceText = itemView.findViewById(R.id.confidence_text);
        }

        void bind(ChatMessage m, Listener listener) {
            if (user) {
                text.setText(m.content);
                return;
            }
            answer.refreshColors();
            answer.renderMarkup(m.content);

            if (actionsRow != null) {
                actionsRow.setVisibility(View.VISIBLE);
                View copy = actionsRow.findViewById(R.id.action_copy);
                View regenerate = actionsRow.findViewById(R.id.action_regenerate);
                View share = actionsRow.findViewById(R.id.action_share);
                View up = actionsRow.findViewById(R.id.action_up);
                View down = actionsRow.findViewById(R.id.action_down);
                if (copy != null) copy.setOnClickListener(v -> listener.onCopy(m));
                if (regenerate != null) regenerate.setOnClickListener(v -> listener.onRegenerate(m));
                if (share != null) share.setOnClickListener(v -> listener.onShare(m));
                if (up != null) up.setOnClickListener(v -> listener.onFeedback(m, 1));
                if (down != null) down.setOnClickListener(v -> listener.onFeedback(m, -1));
            }

            if (evidenceRow != null && confidenceText != null) {
                if (m.confidence > 0) {
                    evidenceRow.setVisibility(View.VISIBLE);
                    AnswerComposer.Confidence band = AnswerComposer.confidenceBand(m.confidence);
                    String label;
                    int color;
                    switch (band) {
                        case HIGH:
                            label = itemView.getContext().getString(R.string.confidence_high);
                            color = androidx.core.content.ContextCompat.getColor(
                                    itemView.getContext(), R.color.md_confidence_high);
                            break;
                        case MEDIUM:
                            label = itemView.getContext().getString(R.string.confidence_medium);
                            color = androidx.core.content.ContextCompat.getColor(
                                    itemView.getContext(), R.color.md_confidence_medium);
                            break;
                        default:
                            label = itemView.getContext().getString(R.string.confidence_low);
                            color = androidx.core.content.ContextCompat.getColor(
                                    itemView.getContext(), R.color.md_confidence_low);
                    }
                    confidenceText.setText(String.format(Locale.US, "%s · %.2f", label, m.confidence));
                    confidenceText.setTextColor(color);
                } else {
                    evidenceRow.setVisibility(View.GONE);
                }
                if (evidenceText != null) {
                    evidenceText.setText(m.evidenceSummary);
                    evidenceText.setVisibility(
                            m.evidenceSummary == null || m.evidenceSummary.isEmpty()
                                    ? View.GONE : View.VISIBLE);
                }
            }
        }
    }
}
