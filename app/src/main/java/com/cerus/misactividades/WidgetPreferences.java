package com.cerus.misactividades;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetPreferences {
    public static final int RANGE_TODAY = 1;
    public static final int RANGE_FIVE_DAYS = 5;

    private static final String PREFERENCES_NAME = "widget_settings";
    private static final String RANGE_DAYS_KEY = "range_days";

    private final Context context;
    private final SharedPreferences preferences;

    public WidgetPreferences(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public int getRangeDays() {
        int storedValue = preferences.getInt(RANGE_DAYS_KEY, RANGE_TODAY);
        return storedValue == RANGE_FIVE_DAYS ? RANGE_FIVE_DAYS : RANGE_TODAY;
    }

    public void setRangeDays(int rangeDays) {
        int safeValue = rangeDays == RANGE_FIVE_DAYS ? RANGE_FIVE_DAYS : RANGE_TODAY;
        preferences.edit().putInt(RANGE_DAYS_KEY, safeValue).apply();
        TasksWidgetProvider.updateAllWidgets(context);
    }
}
