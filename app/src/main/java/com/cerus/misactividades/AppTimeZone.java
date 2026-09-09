package com.cerus.misactividades;

import java.util.Calendar;
import java.util.TimeZone;

public final class AppTimeZone {
    public static final String ID = "America/Argentina/Buenos_Aires";

    private AppTimeZone() {
    }

    public static TimeZone get() {
        return TimeZone.getTimeZone(ID);
    }

    public static Calendar calendar() {
        return Calendar.getInstance(get());
    }

    public static boolean isToday(long timestamp) {
        Calendar today = calendar();
        Calendar date = calendar();
        date.setTimeInMillis(timestamp);
        return today.get(Calendar.ERA) == date.get(Calendar.ERA)
                && today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isCurrentMonth(long timestamp) {
        Calendar today = calendar();
        Calendar date = calendar();
        date.setTimeInMillis(timestamp);
        return today.get(Calendar.ERA) == date.get(Calendar.ERA)
                && today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == date.get(Calendar.MONTH);
    }

    public static long startOfTodayMillis() {
        Calendar today = calendar();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis();
    }
}
