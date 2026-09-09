package com.cerus.misactividades;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TaskHistoryRepository {
    private static final String TAG = "TaskHistory";
    private static final String PREFERENCES_NAME = "recordarte_history";
    private static final String COMPLETED_KEY = "completed_tasks";
    private static final String OVERDUE_KEY = "overdue_tasks";
    private static final int MAX_ENTRIES_PER_LOG = 500;

    private final SharedPreferences preferences;

    public TaskHistoryRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    public synchronized void recordTaskStates(List<Task> tasks) {
        JSONArray completedEntries = readArray(COMPLETED_KEY);
        JSONArray overdueEntries = readArray(OVERDUE_KEY);
        long now = System.currentTimeMillis();
        long startOfToday = AppTimeZone.startOfTodayMillis();
        boolean completedChanged = false;
        boolean overdueChanged = removeOverdueEntriesAlreadyCompleted(
                completedEntries,
                overdueEntries);

        for (Task task : tasks) {
            Long completedAt = task.getCompletedAt();
            if (task.isCompleted()) {
                long eventAt = completedAt == null ? now : completedAt;
                completedChanged |= appendIfMissing(
                        completedEntries,
                        "completed|" + task.getId(),
                        task.getId(),
                        task.getTitle(),
                        task.getNote(),
                        task.getDueAt(),
                        task.getRecurrence(),
                        eventAt,
                        now);
                overdueChanged |= removeEntriesForTaskId(overdueEntries, task.getId());
            } else if (task.getDueAt() != null && task.getDueAt() < startOfToday) {
                long dueAt = task.getDueAt();
                overdueChanged |= appendIfMissing(
                        overdueEntries,
                        "overdue|" + task.getId() + "|" + dueAt,
                        task.getId(),
                        task.getTitle(),
                        task.getNote(),
                        dueAt,
                        task.getRecurrence(),
                        dueAt,
                        now);
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        if (completedChanged) {
            trimOldest(completedEntries);
            editor.putString(COMPLETED_KEY, completedEntries.toString());
        }
        if (overdueChanged) {
            trimOldest(overdueEntries);
            editor.putString(OVERDUE_KEY, overdueEntries.toString());
        }
        if (completedChanged || overdueChanged) {
            editor.apply();
        }
    }

    public void recordStoredTasks(String storedTasks) {
        List<Task> tasks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(storedTasks);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                tasks.add(new Task(
                        item.getString("id"),
                        item.getString("title"),
                        item.optString("note", ""),
                        item.optBoolean("completed", false),
                        item.has("dueAt") && !item.isNull("dueAt")
                                ? item.getLong("dueAt")
                                : null,
                        item.has("completedAt") && !item.isNull("completedAt")
                                ? item.getLong("completedAt")
                                : null,
                        TaskRecurrence.fromStoredValue(item.optString("recurrence", null)),
                        item.optBoolean("nextOccurrenceGenerated", false)));
            }
            recordTaskStates(tasks);
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudieron registrar las tareas guardadas", exception);
        }
    }

    public synchronized List<Entry> getCompletedEntries() {
        repairDuplicateStates();
        return readEntries(COMPLETED_KEY);
    }

    public synchronized List<Entry> getOverdueEntries() {
        repairDuplicateStates();
        return readEntries(OVERDUE_KEY);
    }

    public synchronized int clearCurrentMonthHistory() {
        JSONArray completedEntries = readArray(COMPLETED_KEY);
        JSONArray overdueEntries = readArray(OVERDUE_KEY);
        int removed = removeCurrentMonthEntries(completedEntries)
                + removeCurrentMonthEntries(overdueEntries);
        if (removed > 0) {
            preferences.edit()
                    .putString(COMPLETED_KEY, completedEntries.toString())
                    .putString(OVERDUE_KEY, overdueEntries.toString())
                    .apply();
        }
        return removed;
    }

    public synchronized int clearAllHistory() {
        int removed = readArray(COMPLETED_KEY).length() + readArray(OVERDUE_KEY).length();
        preferences.edit()
                .remove(COMPLETED_KEY)
                .remove(OVERDUE_KEY)
                .apply();
        return removed;
    }

    public synchronized boolean updateOverdueNote(String entryId, String note) {
        JSONArray overdueEntries = readArray(OVERDUE_KEY);
        for (int index = 0; index < overdueEntries.length(); index++) {
            JSONObject entry = overdueEntries.optJSONObject(index);
            if (entry == null || !entryId.equals(entry.optString("id"))) {
                continue;
            }
            try {
                entry.put("note", note == null ? "" : note);
                preferences.edit()
                        .putString(OVERDUE_KEY, overdueEntries.toString())
                        .apply();
                return true;
            } catch (JSONException exception) {
                Log.e(TAG, "No se pudo actualizar la nota vencida", exception);
                return false;
            }
        }
        return false;
    }

    public synchronized boolean markOverdueAsCompleted(String entryId, long completedAt) {
        JSONArray overdueEntries = readArray(OVERDUE_KEY);
        JSONArray completedEntries = readArray(COMPLETED_KEY);
        for (int index = 0; index < overdueEntries.length(); index++) {
            JSONObject overdue = overdueEntries.optJSONObject(index);
            if (overdue == null || !entryId.equals(overdue.optString("id"))) {
                continue;
            }
            try {
                long originalDueAt = overdue.getLong("eventAt");
                String completedId = "completed-late|" + entryId;
                if (!containsEntry(completedEntries, completedId)) {
                    JSONObject completed = new JSONObject();
                    completed.put("id", completedId);
                    completed.put("taskId", overdue.optString("taskId", entryId));
                    completed.put("title", overdue.getString("title"));
                    completed.put("note", overdue.optString("note", ""));
                    completed.put("dueAt", originalDueAt);
                    if (overdue.has("recurrence") && !overdue.isNull("recurrence")) {
                        completed.put("recurrence", overdue.getString("recurrence"));
                    }
                    completed.put("eventAt", completedAt);
                    completed.put("recordedAt", completedAt);
                    completed.put("completedLate", true);
                    completed.put("originalDueAt", originalDueAt);
                    completedEntries.put(completed);
                }
                overdueEntries.remove(index);
                trimOldest(completedEntries);
                preferences.edit()
                        .putString(COMPLETED_KEY, completedEntries.toString())
                        .putString(OVERDUE_KEY, overdueEntries.toString())
                        .apply();
                return true;
            } catch (JSONException exception) {
                Log.e(TAG, "No se pudo completar la tarea vencida", exception);
                return false;
            }
        }
        return false;
    }

    public synchronized void removeCompletedEntry(String taskId) {
        JSONArray completedEntries = readArray(COMPLETED_KEY);
        boolean changed = false;
        for (int index = completedEntries.length() - 1; index >= 0; index--) {
            JSONObject entry = completedEntries.optJSONObject(index);
            if (entry != null && taskId.equals(entry.optString("taskId"))) {
                completedEntries.remove(index);
                changed = true;
            }
        }
        if (changed) {
            preferences.edit().putString(COMPLETED_KEY, completedEntries.toString()).apply();
        }
    }

    private void repairDuplicateStates() {
        JSONArray completedEntries = readArray(COMPLETED_KEY);
        JSONArray overdueEntries = readArray(OVERDUE_KEY);
        if (removeOverdueEntriesAlreadyCompleted(completedEntries, overdueEntries)) {
            preferences.edit().putString(OVERDUE_KEY, overdueEntries.toString()).apply();
        }
    }

    private boolean removeOverdueEntriesAlreadyCompleted(
            JSONArray completedEntries,
            JSONArray overdueEntries) {
        Set<String> completedTaskIds = new HashSet<>();
        for (int index = 0; index < completedEntries.length(); index++) {
            JSONObject entry = completedEntries.optJSONObject(index);
            if (entry == null) {
                continue;
            }
            String taskId = entry.optString("taskId", "");
            if (!taskId.isEmpty()) {
                completedTaskIds.add(taskId);
            }
        }

        boolean changed = false;
        for (int index = overdueEntries.length() - 1; index >= 0; index--) {
            JSONObject entry = overdueEntries.optJSONObject(index);
            if (entry != null
                    && completedTaskIds.contains(entry.optString("taskId", ""))) {
                overdueEntries.remove(index);
                changed = true;
            }
        }
        return changed;
    }

    private boolean removeEntriesForTaskId(JSONArray entries, String taskId) {
        boolean changed = false;
        for (int index = entries.length() - 1; index >= 0; index--) {
            JSONObject entry = entries.optJSONObject(index);
            if (entry != null && taskId.equals(entry.optString("taskId", ""))) {
                entries.remove(index);
                changed = true;
            }
        }
        return changed;
    }

    private int removeCurrentMonthEntries(JSONArray entries) {
        int removed = 0;
        for (int index = entries.length() - 1; index >= 0; index--) {
            JSONObject entry = entries.optJSONObject(index);
            if (entry != null
                    && AppTimeZone.isCurrentMonth(entry.optLong("eventAt", Long.MIN_VALUE))) {
                entries.remove(index);
                removed++;
            }
        }
        return removed;
    }

    private JSONArray readArray(String key) {
        try {
            return new JSONArray(preferences.getString(key, "[]"));
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudo leer el historial", exception);
            return new JSONArray();
        }
    }

    private List<Entry> readEntries(String key) {
        JSONArray array = readArray(key);
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            try {
                JSONObject item = array.getJSONObject(index);
                String recurrenceValue = item.optString("recurrence", "");
                boolean recurrenceKnown = isKnownRecurrence(recurrenceValue);
                entries.add(new Entry(
                        item.optString("id", ""),
                        item.optString("taskId", ""),
                        item.getString("title"),
                        item.optString("note", ""),
                        item.getLong("eventAt"),
                        item.has("dueAt") && !item.isNull("dueAt")
                                ? item.getLong("dueAt")
                                : null,
                        recurrenceKnown
                                ? TaskRecurrence.fromStoredValue(recurrenceValue)
                                : TaskRecurrence.NONE,
                        recurrenceKnown,
                        item.optBoolean("completedLate", false),
                        item.has("originalDueAt") && !item.isNull("originalDueAt")
                                ? item.getLong("originalDueAt")
                                : null));
            } catch (JSONException exception) {
                Log.e(TAG, "No se pudo leer una entrada del historial", exception);
            }
        }
        entries.sort(Comparator.comparingLong(Entry::getEventAt).reversed());
        return entries;
    }

    private boolean appendIfMissing(JSONArray array, String id, String taskId,
                                    String title, String note, Long dueAt,
                                    TaskRecurrence recurrence,
                                    long eventAt, long recordedAt) {
        for (int index = 0; index < array.length(); index++) {
            JSONObject existing = array.optJSONObject(index);
            if (existing != null && id.equals(existing.optString("id"))) {
                try {
                    String safeNote = note == null ? "" : note;
                    String recurrenceValue = recurrence == null
                            ? TaskRecurrence.NONE.name()
                            : recurrence.name();
                    boolean changed = !title.equals(existing.optString("title"))
                            || !safeNote.equals(existing.optString("note", ""))
                            || !sameNullableLong(dueAt, existing, "dueAt")
                            || !recurrenceValue.equals(existing.optString("recurrence", ""));
                    if (changed) {
                        existing.put("title", title);
                        existing.put("note", safeNote);
                        existing.put("dueAt", dueAt == null ? JSONObject.NULL : dueAt);
                        existing.put("recurrence", recurrenceValue);
                    }
                    return changed;
                } catch (JSONException exception) {
                    Log.e(TAG, "No se pudo actualizar una entrada del historial", exception);
                    return false;
                }
            }
        }

        JSONObject item = new JSONObject();
        try {
            item.put("id", id);
            item.put("taskId", taskId);
            item.put("title", title);
            item.put("note", note == null ? "" : note);
            item.put("dueAt", dueAt == null ? JSONObject.NULL : dueAt);
            item.put("recurrence", recurrence == null
                    ? TaskRecurrence.NONE.name()
                    : recurrence.name());
            item.put("eventAt", eventAt);
            item.put("recordedAt", recordedAt);
            array.put(item);
            return true;
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudo agregar una entrada al historial", exception);
            return false;
        }
    }

    private boolean containsEntry(JSONArray array, String id) {
        for (int index = 0; index < array.length(); index++) {
            JSONObject entry = array.optJSONObject(index);
            if (entry != null && id.equals(entry.optString("id"))) {
                return true;
            }
        }
        return false;
    }

    private boolean sameNullableLong(Long value, JSONObject object, String key) {
        if (value == null) {
            return !object.has(key) || object.isNull(key);
        }
        return object.has(key) && !object.isNull(key)
                && value == object.optLong(key);
    }

    private boolean isKnownRecurrence(String value) {
        for (TaskRecurrence recurrence : TaskRecurrence.values()) {
            if (recurrence.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void trimOldest(JSONArray array) {
        while (array.length() > MAX_ENTRIES_PER_LOG) {
            array.remove(0);
        }
    }

    public static final class Entry {
        private final String id;
        private final String taskId;
        private final String title;
        private final String note;
        private final long eventAt;
        private final Long dueAt;
        private final TaskRecurrence recurrence;
        private final boolean recurrenceKnown;
        private final boolean completedLate;
        private final Long originalDueAt;

        private Entry(String id, String taskId, String title, String note, long eventAt,
                      Long dueAt,
                      TaskRecurrence recurrence, boolean recurrenceKnown,
                      boolean completedLate, Long originalDueAt) {
            this.id = id;
            this.taskId = taskId;
            this.title = title;
            this.note = note == null ? "" : note;
            this.eventAt = eventAt;
            this.dueAt = dueAt;
            this.recurrence = recurrence;
            this.recurrenceKnown = recurrenceKnown;
            this.completedLate = completedLate;
            this.originalDueAt = originalDueAt;
        }

        public String getId() {
            return id;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getTitle() {
            return title;
        }

        public String getNote() {
            return note;
        }

        public boolean hasNote() {
            return !note.trim().isEmpty();
        }

        public long getEventAt() {
            return eventAt;
        }

        public Long getDueAt() {
            return dueAt;
        }

        public TaskRecurrence getRecurrence() {
            return recurrence;
        }

        public boolean hasRecordedRecurrence() {
            return recurrenceKnown;
        }

        public boolean isCompletedLate() {
            return completedLate;
        }

        public Long getOriginalDueAt() {
            return originalDueAt;
        }
    }
}
