package com.cerus.misactividades;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class AppThemePreferences {
    private static final String PREFERENCES_NAME = "appearance_settings";
    private static final String DARK_MODE_KEY = "dark_mode";

    private final SharedPreferences preferences;

    public AppThemePreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    public boolean isDarkMode() {
        return preferences.getBoolean(DARK_MODE_KEY, false);
    }

    public void setDarkMode(boolean darkMode) {
        preferences.edit().putBoolean(DARK_MODE_KEY, darkMode).apply();
    }

    public int cardBackground() {
        return isDarkMode() ? Color.rgb(35, 32, 40) : Color.WHITE;
    }

    public int cardBorder() {
        return isDarkMode() ? Color.rgb(76, 70, 84) : Color.rgb(226, 224, 231);
    }

    public int primaryAction() {
        return isDarkMode() ? Color.rgb(255, 159, 67) : Color.rgb(8, 91, 171);
    }

    public int primaryActionText() {
        return isDarkMode() ? Color.rgb(35, 25, 18) : Color.WHITE;
    }

    public int secondaryAction() {
        return isDarkMode() ? Color.rgb(50, 46, 57) : Color.rgb(245, 247, 250);
    }

    public int secondaryActionText() {
        return isDarkMode() ? Color.rgb(235, 230, 242) : Color.rgb(43, 57, 73);
    }

    public int dangerAction() {
        return isDarkMode() ? Color.rgb(62, 39, 42) : Color.rgb(255, 246, 245);
    }

    public int dangerActionText() {
        return isDarkMode() ? Color.rgb(255, 160, 151) : Color.rgb(171, 38, 38);
    }

    public int dangerBorder() {
        return isDarkMode() ? Color.rgb(112, 62, 65) : Color.rgb(240, 207, 204);
    }

    public int completedCard() {
        return isDarkMode() ? Color.rgb(27, 48, 37) : Color.rgb(242, 250, 245);
    }

    public int completedAccent() {
        return isDarkMode() ? Color.rgb(94, 199, 135) : Color.rgb(42, 139, 82);
    }

    public int overdueCard() {
        return isDarkMode() ? Color.rgb(57, 32, 35) : Color.rgb(255, 246, 244);
    }

    public int overdueAccent() {
        return isDarkMode() ? Color.rgb(255, 126, 115) : Color.rgb(201, 49, 49);
    }

    public int todayCard() {
        return isDarkMode() ? Color.rgb(57, 42, 23) : Color.rgb(255, 249, 237);
    }

    public int todayAccent() {
        return isDarkMode() ? Color.rgb(255, 183, 77) : Color.rgb(221, 118, 12);
    }

    public int futureAccent() {
        return isDarkMode() ? Color.rgb(98, 168, 231) : Color.rgb(56, 125, 190);
    }

    public int primaryText() {
        return isDarkMode() ? Color.rgb(244, 240, 250) : Color.rgb(35, 32, 40);
    }

    public int secondaryText() {
        return isDarkMode() ? Color.rgb(202, 196, 211) : Color.rgb(90, 86, 96);
    }

    public int completedText() {
        return isDarkMode() ? Color.rgb(116, 216, 155) : Color.rgb(37, 125, 72);
    }

    public int overdueText() {
        return isDarkMode() ? Color.rgb(255, 138, 128) : Color.rgb(180, 35, 35);
    }

    public int todayText() {
        return isDarkMode() ? Color.rgb(255, 183, 77) : Color.rgb(177, 92, 0);
    }

    public int undoBackground() {
        return isDarkMode() ? Color.rgb(55, 51, 62) : Color.rgb(49, 47, 53);
    }
}
