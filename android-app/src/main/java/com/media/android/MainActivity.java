package com.media.android;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.media.android.data.Preferences;
import com.media.android.ui.chat.ChatViewModel;
import com.media.android.ui.common.ChatMessage;

import java.util.Locale;

/**
 * Single-activity host for the MEDIA navigation shell.
 *
 * <p>Owns the persistent offline status strip (knowledge readiness and model availability), wires
 * the Material bottom navigation to the navigation graph and exposes the shared
 * {@link ChatViewModel} so Home and Chat operate on one transcript.</p>
 */
public final class MainActivity extends AppCompatActivity {

    private TextView statusKnowledge;
    private TextView statusModel;
    private TextView statusRecords;
    private View statusDot;

    private MediaApplication app;
    private ChatViewModel chatViewModel;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the stored theme before the activity is created to avoid a flash of the wrong mode.
        Preferences.applyTheme(new Preferences(this).theme());
        super.onCreate(savedInstanceState);
        app = MediaApplication.from(this);
        setContentView(R.layout.activity_main);

        statusKnowledge = findViewById(R.id.status_knowledge);
        statusModel = findViewById(R.id.status_model);
        statusRecords = findViewById(R.id.status_records);
        statusDot = findViewById(R.id.status_dot);

        chatViewModel = new ViewModelProvider(this, new ChatViewModelFactory(app))
                .get(ChatViewModel.class);

        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host != null) {
            navController = host.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        renderStatus();
        app.ensureKnowledgeLoaded(this::renderStatus);
    }

    public ChatViewModel chatViewModel() {
        return chatViewModel;
    }

    public NavController navController() {
        return navController;
    }

    /** Called by Chat when a generation starts/stops so the strip can reflect activity. */
    public void renderStatus() {
        int size = app.repository().size();
        int failures = app.repository().loadFailures();

        if (size > 0 && failures == 0) {
            statusKnowledge.setText(R.string.status_knowledge_ready);
            statusKnowledge.setTextColor(ContextCompat.getColor(this, R.color.md_confidence_high));
        } else if (size > 0) {
            statusKnowledge.setText(R.string.status_knowledge_error);
            statusKnowledge.setTextColor(ContextCompat.getColor(this, R.color.md_confidence_medium));
        } else {
            statusKnowledge.setText(R.string.status_knowledge_loading);
            statusKnowledge.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_muted));
        }

        boolean modelReady = app.modelManager().isReady();
        statusModel.setText(modelReady
                ? R.string.status_model_ready : R.string.status_model_not_installed);
        statusModel.setTextColor(ContextCompat.getColor(this,
                modelReady ? R.color.md_confidence_high : R.color.md_on_surface_muted));

        statusRecords.setText(size > 0
                ? String.format(Locale.US, getString(R.string.status_records), size)
                : "");
        statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this,
                        failures == 0 ? R.color.md_confidence_high : R.color.md_confidence_low)));
    }

    /** Routes a question into the shared transcript and switches to the Chat destination. */
    public void askFromHome(String question) {
        chatViewModel.ask(question);
        if (navController != null) {
            navController.navigate(R.id.chatFragment);
        }
        renderStatus();
    }

    /** Brings an existing exchange back into the Chat destination. */
    public void openConversation(String question, String answer, double confidence, String evidence) {
        chatViewModel.restore(question, ChatMessage.media(answer, confidence, evidence));
        if (navController != null) {
            navController.navigate(R.id.chatFragment);
        }
    }

    private static final class ChatViewModelFactory implements ViewModelProvider.Factory {
        private final MediaApplication app;

        ChatViewModelFactory(MediaApplication app) {
            this.app = app;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ChatViewModel(app);
        }
    }
}
