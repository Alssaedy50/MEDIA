package com.media.android.ui.history;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.R;
import com.media.android.data.HistoryEntry;
import com.media.android.knowledge.Text;

import java.util.ArrayList;
import java.util.List;

/** Renders stored history entries with preview, timestamp and delete affordance. */
final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    interface Listener {
        void onOpen(HistoryEntry entry);
        void onDelete(HistoryEntry entry);
    }

    private final List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
    private final Listener listener;

    HistoryAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    void setEntries(List<HistoryEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return entries.get(position).id.hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(entries.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView question;
        private final TextView preview;
        private final TextView time;
        private final ImageButton delete;

        Holder(View itemView) {
            super(itemView);
            question = itemView.findViewById(R.id.history_item_question);
            preview = itemView.findViewById(R.id.history_item_preview);
            time = itemView.findViewById(R.id.history_item_time);
            delete = itemView.findViewById(R.id.history_item_delete);
        }

        void bind(HistoryEntry entry, Listener listener) {
            question.setText(entry.question);
            preview.setText(previewOf(entry.answer));
            time.setText(DateUtils.getRelativeTimeSpanString(
                    entry.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            itemView.setOnClickListener(v -> listener.onOpen(entry));
            delete.setOnClickListener(v -> listener.onDelete(entry));
        }

        /** A short, readable preview of the structured answer (drops markup). */
        private static String previewOf(String answer) {
            if (answer == null) return "";
            StringBuilder b = new StringBuilder();
            for (String line : answer.split("\n")) {
                String clean = Text.clean(line.replace("*", ""));
                if (clean.isEmpty() || clean.startsWith("---") || clean.startsWith("─")) continue;
                if (clean.startsWith("#") || clean.equals("MEDIA")) continue;
                b.append(clean).append(' ');
                if (b.length() > 140) break;
            }
            String result = b.toString().trim();
            return result.length() > 160 ? result.substring(0, 157) + "…" : result;
        }
    }
}
