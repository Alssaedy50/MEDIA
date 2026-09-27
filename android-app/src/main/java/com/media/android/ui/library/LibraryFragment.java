package com.media.android.ui.library;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.media.android.MainActivity;
import com.media.android.MediaApplication;
import com.media.android.R;
import com.media.android.knowledge.KnowledgeRecord;
import com.media.android.knowledge.KnowledgeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Browse the bundled local knowledge.
 *
 * <p>Records are grouped by subject and listed lazily; tapping an entry asks MEDIA to explain that
 * topic in the Chat destination, so the Library is a real entry point rather than decoration.</p>
 */
public final class LibraryFragment extends Fragment {

    private LibraryAdapter adapter;
    private TextView empty;
    private TextView scope;
    private List<KnowledgeRecord> all = new ArrayList<KnowledgeRecord>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MediaApplication app = MediaApplication.from(requireContext());
        KnowledgeRepository repository = app.repository();

        scope = view.findViewById(R.id.library_scope);
        empty = view.findViewById(R.id.library_empty);
        RecyclerView list = view.findViewById(R.id.library_list);
        EditText search = view.findViewById(R.id.library_search);

        adapter = new LibraryAdapter(this::askTopic);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
        });

        scope.setText(getString(R.string.status_knowledge_loading));
        app.ensureKnowledgeLoaded(() -> {
            if (!isAdded()) return;
            all = repository.allRecords();
            scope.setText(getString(R.string.library_scope, "Hematology", all.size()));
            filter(search.getText().toString());
        });
    }

    private void filter(String query) {
        if (adapter == null) return;
        String needle = com.media.android.knowledge.Text.normalize(query);
        List<KnowledgeRecord> filtered = new ArrayList<KnowledgeRecord>();
        for (KnowledgeRecord r : all) {
            if (needle.isEmpty()
                    || com.media.android.knowledge.Text.normalize(r.title()).contains(needle)
                    || com.media.android.knowledge.Text.normalize(r.concept()).contains(needle)
                    || com.media.android.knowledge.Text.normalize(r.subject()).contains(needle)) {
                filtered.add(r);
            }
        }
        adapter.setRecords(filtered);
        boolean none = filtered.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        empty.setText(all.isEmpty()
                ? getString(R.string.library_empty)
                : getString(R.string.library_no_results, query));
    }

    private void askTopic(KnowledgeRecord record) {
        MainActivity activity = getActivity() instanceof MainActivity
                ? (MainActivity) getActivity() : null;
        if (activity == null) return;
        String question = record.title();
        activity.askFromHome(question);
    }
}
