package com.cerus.misactividades;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class AppBackgroundPreferences {
    public static final int LAVENDER = 0;
    public static final int LIGHT_BLUE = 1;
    public static final int MINT = 2;
    public static final int SAND = 3;
    public static final int LIGHT_GRAY = 4;

    private static final String PREFERENCES_NAME = "appearance_settings";
    private static final String BACKGROUND_KEY = "background_color";
    private static final String DARK_BACKGROUND_KEY = "dark_background_color";

    private final SharedPreferences preferences;

    public AppBackgroundPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public int getSelectedColor() {
        int value = preferences.getInt(BACKGROUND_KEY, LAVENDER);
        return value >= LAVENDER && value <= LIGHT_GRAY ? value : LAVENDER;
    }

    public int getSelectedColor(boolean darkMode) {
        if (!darkMode) {
            return getSelectedColor();
        }
        int value = preferences.getInt(DARK_BACKGROUND_KEY, LAVENDER);
        return value >= LAVENDER && value <= LIGHT_GRAY ? value : LAVENDER;
    }

    public void setSelectedColor(int selectedColor) {
        int safeValue = selectedColor >= LAVENDER && selectedColor <= LIGHT_GRAY
                ? selectedColor
                : LAVENDER;
        preferences.edit().putInt(BACKGROUND_KEY, safeValue).apply();
    }

    public void setSelectedColor(int selectedColor, boolean darkMode) {
        if (!darkMode) {
            setSelectedColor(selectedColor);
            return;
        }
        int safeValue = selectedColor >= LAVENDER && selectedColor <= LIGHT_GRAY
                ? selectedColor
                : LAVENDER;
        preferences.edit().putInt(DARK_BACKGROUND_KEY, safeValue).apply();
    }

    public int getColorValue() {
        switch (getSelectedColor()) {
            case LIGHT_BLUE:
                return Color.rgb(238, 247, 255);
            case MINT:
                return Color.rgb(239, 249, 243);
            case SAND:
                return Color.rgb(255, 248, 232);
            case LIGHT_GRAY:
                return Color.rgb(243, 244, 246);
            case LAVENDER:
            default:
                return Color.rgb(248, 247, 252);
        }
    }

    public int getColorValue(boolean darkMode) {
        if (!darkMode) {
            return getColorValue();
        }
        switch (getSelectedColor(true)) {
            case LIGHT_BLUE:
                return Color.rgb(14, 29, 43);
            case MINT:
                return Color.rgb(14, 35, 27);
            case SAND:
                return Color.rgb(40, 29, 15);
            case LIGHT_GRAY:
                return Color.rgb(28, 29, 33);
            case LAVENDER:
            default:
                return Color.rgb(30, 24, 39);
        }
    }
}
