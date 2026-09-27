package com.media.android.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.media.android.MainActivity;
import com.media.android.MediaApplication;
import com.media.android.R;
import com.media.android.data.HistoryEntry;

import java.util.List;

/** MEDIA home: wordmark, greeting, prominent question input, quick actions and recent questions. */
public final class HomeFragment extends Fragment {

    private EditText input;
    private MediaApplication app;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = MediaApplication.from(requireContext());

        input = view.findViewById(R.id.home_input);
        ImageButton send = view.findViewById(R.id.home_send);
        ImageButton mic = view.findViewById(R.id.home_mic);

        send.setOnClickListener(v -> submit());
        mic.setOnClickListener(v -> Toast.makeText(requireContext(),
                R.string.home_mic_planned, Toast.LENGTH_SHORT).show());

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        bindQuickAction(view, R.id.quick_explain, R.string.sample_explain);
        bindQuickAction(view, R.id.quick_case, R.string.sample_case);
        bindQuickAction(view, R.id.quick_study, R.string.sample_study);
        bindQuickAction(view, R.id.quick_term, R.string.sample_term);
        bindQuickAction(view, R.id.quick_summarize, R.string.sample_summary);

        TextView viewAll = view.findViewById(R.id.recent_view_all);
        viewAll.setOnClickListener(v -> {
            MainActivity activity = activity();
            if (activity != null && activity.navController() != null) {
                activity.navController().navigate(R.id.historyFragment);
            }
        });

        renderRecent(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) renderRecent(getView());
    }

    private void bindQuickAction(View root, int viewId, int sampleRes) {
        Chip chip = root.findViewById(viewId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            input.setText(getString(sampleRes));
            input.setSelection(input.length());
            submit();
        });
    }

    private void submit() {
        String question = input.getText().toString().trim();
        MainActivity activity = activity();
        if (activity == null) return;
        if (question.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_query, Toast.LENGTH_SHORT).show();
            input.requestFocus();
            return;
        }
        input.setText("");
        activity.askFromHome(question);
    }

    private void renderRecent(View root) {
        LinearLayout container = root.findViewById(R.id.recent_container);
        TextView empty = root.findViewById(R.id.recent_empty);
        TextView viewAll = root.findViewById(R.id.recent_view_all);
        if (container == null || app == null) return;

        List<HistoryEntry> entries = app.history().all();
        container.removeAllViews();
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        viewAll.setVisibility(entries.size() > 3 ? View.VISIBLE : View.GONE);

        int shown = Math.min(entries.size(), 3);
        for (int i = 0; i < shown; i++) {
            HistoryEntry entry = entries.get(i);
            container.addView(recentRow(container, entry));
        }
    }

    private View recentRow(LinearLayout parent, HistoryEntry entry) {
        TextView row = (TextView) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_recent_question, parent, false);
        row.setText(entry.question);
        row.setOnClickListener(v -> {
            MainActivity activity = activity();
            if (activity != null) {
                activity.openConversation(entry.question, entry.answer, entry.confidence, "");
            }
        });
        return row;
    }

    @Nullable
    private MainActivity activity() {
        return getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
    }
}
