package com.media.android.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.media.android.BuildConfig;
import com.media.android.MediaApplication;
import com.media.android.R;
import com.media.android.ai.engine.MediaAiEngine;
import com.media.android.ai.engine.MediaEngineRegistry;
import com.media.android.data.Preferences;
import com.media.android.model.ModelManager;

import java.util.Locale;

/** Settings: honest model status, engine inventory, language, theme and about. */
public final class SettingsFragment extends Fragment {

    private MediaApplication app;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = MediaApplication.from(requireContext());

        renderModel(view);
        renderEngines(view);
        renderLanguage(view);
        renderTheme(view);
        renderAbout(view);
    }

    // ------------------------------------------------------------- model

    private void renderModel(View view) {
        ModelManager manager = app.modelManager();
        ModelManager.Expectation expected = manager.expectation();
        boolean ready = manager.isReady();

        TextView status = view.findViewById(R.id.model_status_value);
        ImageView icon = view.findViewById(R.id.model_icon);
        status.setText(manager.statusLabel());
        status.setTextColor(ContextCompat.getColor(requireContext(),
                ready ? R.color.md_confidence_high : R.color.md_on_surface_variant));
        icon.setImageResource(ready ? R.drawable.ic_model_ready : R.drawable.ic_model);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        ready ? R.color.md_confidence_high : R.color.md_on_surface_variant)));

        LinearLayout rows = view.findViewById(R.id.model_rows);
        rows.removeAllViews();
        addRow(rows, getString(R.string.settings_model),
                ready ? expected.name : getString(R.string.settings_model_not_installed));
        addRow(rows, getString(R.string.settings_format), expected.format);
        addRow(rows, getString(R.string.settings_quantization), expected.quantization);
        addRow(rows, getString(R.string.settings_size), manager.formattedSize());
        addRow(rows, getString(R.string.settings_runtime), getString(R.string.settings_runtime_local));

        TextView note = view.findViewById(R.id.model_note);
        note.setText(manager.detail() + "\n\n" + getString(R.string.settings_model_note));
    }

    private void renderEngines(View view) {
        LinearLayout container = view.findViewById(R.id.engines_container);
        container.removeAllViews();

        for (MediaAiEngine engine : app.registry().all().values()) {
            addRow(container, engine.id(),
                    engine.isAvailable() ? getString(R.string.status_model_ready)
                            : getString(R.string.status_model_not_installed));
        }
        for (MediaEngineRegistry.Planned planned : app.registry().planned()) {
            View row = inflateRow(container);
            ((TextView) row.findViewById(R.id.row_label)).setText(
                    planned.displayName + " · " + planned.format);
            TextView value = row.findViewById(R.id.row_value);
            value.setText(R.string.settings_model_not_installed);
            value.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_muted));
            container.addView(row);
        }
    }

    // ---------------------------------------------------------- language

    private void renderLanguage(View view) {
        Preferences prefs = new Preferences(requireContext());
        MaterialSwitch toggle = view.findViewById(R.id.settings_bilingual);
        toggle.setChecked(prefs.bilingual());
        toggle.setOnCheckedChangeListener((b, checked) -> prefs.setBilingual(checked));
    }

    // --------------------------------------------------------- appearance

    private void renderTheme(View view) {
        final Preferences prefs = new Preferences(requireContext());
        RadioGroup group = view.findViewById(R.id.settings_theme_group);
        switch (prefs.theme()) {
            case LIGHT: group.check(R.id.theme_light); break;
            case DARK: group.check(R.id.theme_dark); break;
            default: group.check(R.id.theme_system);
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == R.id.theme_light) {
                prefs.setTheme(Preferences.Theme.LIGHT);
            } else if (checkedId == R.id.theme_dark) {
                prefs.setTheme(Preferences.Theme.DARK);
            } else {
                prefs.setTheme(Preferences.Theme.SYSTEM);
            }
        });
    }

    // -------------------------------------------------------------- about

    private void renderAbout(View view) {
        LinearLayout rows = view.findViewById(R.id.about_rows);
        rows.removeAllViews();
        addRow(rows, getString(R.string.settings_version), BuildConfig.VERSION_NAME);
        addRow(rows, getString(R.string.settings_scope),
                String.format(Locale.US, "%s", "Hematology"));
        addRow(rows, getString(R.string.settings_internet), getString(R.string.settings_no));
    }

    // ------------------------------------------------------------ helpers

    private void addRow(LinearLayout parent, String label, String value) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.row_setting_value, parent, false);
        applyRow(row, label, value);
        parent.addView(row);
    }

    private View inflateRow(LinearLayout parent) {
        return LayoutInflater.from(requireContext())
                .inflate(R.layout.row_setting_value, parent, false);
    }

    private void applyRow(View row, String label, String value) {
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
    }
}
