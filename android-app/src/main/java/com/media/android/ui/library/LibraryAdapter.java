package com.media.android.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.R;
import com.media.android.knowledge.KnowledgeRecord;
import com.media.android.knowledge.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lists local knowledge records grouped visually by subject. */
final class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.Holder> {

    interface OnTopicClick {
        void onTopic(KnowledgeRecord record);
    }

    private final List<KnowledgeRecord> records = new ArrayList<KnowledgeRecord>();
    private final OnTopicClick listener;

    LibraryAdapter(OnTopicClick listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    void setRecords(List<KnowledgeRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return records.get(position).id().hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_topic, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(records.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView subject;
        private final TextView topic;
        private final TextView concept;
        private final TextView ask;

        Holder(View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.library_item_subject);
            topic = itemView.findViewById(R.id.library_item_topic);
            concept = itemView.findViewById(R.id.library_item_concept);
            ask = itemView.findViewById(R.id.library_item_ask);
        }

        void bind(KnowledgeRecord record, OnTopicClick listener) {
            String subjectValue = Text.clean(record.subject()).toUpperCase(Locale.ROOT);
            subject.setText(subjectValue.isEmpty()
                    ? Text.clean(record.domain()).toUpperCase(Locale.ROOT) : subjectValue);
            topic.setText(record.title());
            String conceptValue = Text.clean(record.concept());
            concept.setText(conceptValue);
            concept.setVisibility(conceptValue.isEmpty() ? View.GONE : View.VISIBLE);
            ask.setText(R.string.action_explain);
            itemView.setOnClickListener(v -> listener.onTopic(record));
        }
    }
}
