package com.cerus.misactividades;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TaskDateFormatter {
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";
    private static final String TIME_PATTERN = "HH:mm";

    private TaskDateFormatter() {
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ROOT);
        formatter.setTimeZone(AppTimeZone.get());
        return formatter.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat(TIME_PATTERN, Locale.ROOT);
        formatter.setTimeZone(AppTimeZone.get());
        return formatter.format(new Date(timestamp));
    }
}
