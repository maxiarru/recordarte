package com.cerus.misactividades;

public enum TaskRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    public static TaskRecurrence fromStoredValue(String value) {
        if (value == null) {
            return NONE;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
