package com.media.android.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Persisted user preferences (theme + language) for the Settings screen. */
public final class Preferences {

    public enum Theme { SYSTEM, LIGHT, DARK }

    private static final String PREFS = "media_prefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_BILINGUAL = "bilingual";

    private final SharedPreferences prefs;

    public Preferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Theme theme() {
        String v = prefs.getString(KEY_THEME, Theme.SYSTEM.name());
        try {
            return Theme.valueOf(v);
        } catch (IllegalArgumentException e) {
            return Theme.SYSTEM;
        }
    }

    public void setTheme(Theme theme) {
        prefs.edit().putString(KEY_THEME, theme.name()).apply();
        applyTheme(theme);
    }

    /** Whether to show the Arabic explanation alongside English sections. */
    public boolean bilingual() {
        return prefs.getBoolean(KEY_BILINGUAL, true);
    }

    public void setBilingual(boolean value) {
        prefs.edit().putBoolean(KEY_BILINGUAL, value).apply();
    }

    /** Applies the theme preference to AppCompat. */
    public static void applyTheme(Theme theme) {
        switch (theme) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
