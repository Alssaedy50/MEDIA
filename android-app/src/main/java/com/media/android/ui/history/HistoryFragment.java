package com.media.android.ui.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.MainActivity;
import com.media.android.MediaApplication;
import com.media.android.R;
import com.media.android.data.HistoryEntry;

import java.util.List;

/** Searchable, editable history of previous MEDIA questions. */
public final class HistoryFragment extends Fragment {

    private HistoryAdapter adapter;
    private TextView empty;
    private EditText search;
    private MediaApplication app;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = MediaApplication.from(requireContext());

        empty = view.findViewById(R.id.history_empty);
        search = view.findViewById(R.id.history_search);
        RecyclerView list = view.findViewById(R.id.history_list);
        TextView clear = view.findViewById(R.id.history_clear);

        adapter = new HistoryAdapter(new HistoryAdapter.Listener() {
            @Override
            public void onOpen(HistoryEntry entry) {
                MainActivity activity = activity();
                if (activity != null) {
                    activity.openConversation(entry.question, entry.answer, entry.confidence, "");
                }
            }

            @Override
            public void onDelete(HistoryEntry entry) {
                app.history().remove(entry.id);
                Toast.makeText(requireContext(), R.string.history_deleted, Toast.LENGTH_SHORT).show();
                render(search.getText().toString());
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render(s.toString()); }
        });

        clear.setOnClickListener(v -> confirmClear());
        render("");
    }

    @Override
    public void onResume() {
        super.onResume();
        render(search == null ? "" : search.getText().toString());
    }

    private void render(String query) {
        if (adapter == null || app == null) return;
        List<HistoryEntry> entries = app.history().search(query);
        adapter.setEntries(entries);
        boolean none = entries.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        empty.setText(app.history().size() == 0
                ? getString(R.string.history_empty)
                : getString(R.string.history_no_results, query));
    }

    private void confirmClear() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_clear_all)
                .setMessage(R.string.history_clear_confirm)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (d, w) -> {
                    app.history().clear();
                    render("");
                })
                .show();
    }

    @Nullable
    private MainActivity activity() {
        return getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
    }
}
