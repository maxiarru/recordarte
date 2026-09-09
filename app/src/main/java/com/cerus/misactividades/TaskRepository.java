package com.cerus.misactividades;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String PREFERENCES_NAME = "mis_actividades";
    private static final String TASKS_KEY = "tasks";
    private static final String BUENOS_AIRES_MIGRATION_KEY = "buenos_aires_timezone_migrated";

    private final Context context;
    private final SharedPreferences preferences;

    public TaskRepository(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public List<Task> load() {
        List<Task> tasks = new ArrayList<>();
        String migratedTasks = migrateDueDatesToBuenosAires();
        new TaskHistoryRepository(context).recordStoredTasks(migratedTasks);
        String storedTasks = cleanupFinishedDays(migratedTasks);

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
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudieron leer las actividades guardadas", exception);
        }

        ReminderScheduler.syncAll(context, tasks);
        return tasks;
    }

    private String cleanupFinishedDays(String storedTasks) {
        try {
            JSONArray storedArray = new JSONArray(storedTasks);
            JSONArray activeArray = new JSONArray();
            long now = System.currentTimeMillis();
            long startOfToday = AppTimeZone.startOfTodayMillis();
            boolean changed = false;

            for (int index = 0; index < storedArray.length(); index++) {
                JSONObject item = storedArray.getJSONObject(index);
                if (item.optBoolean("completed", false)) {
                    if (!item.has("completedAt") || item.isNull("completedAt")) {
                        item.put("completedAt", now);
                        changed = true;
                    }
                    if (item.getLong("completedAt") < startOfToday) {
                        JSONObject nextOccurrence = createNextOccurrence(
                                item,
                                startOfToday);
                        if (nextOccurrence != null) {
                            activeArray.put(nextOccurrence);
                        }
                        changed = true;
                        continue;
                    }
                } else if (item.has("dueAt")
                        && !item.isNull("dueAt")
                        && item.getLong("dueAt") < startOfToday) {
                    JSONObject nextOccurrence = createNextOccurrence(
                            item,
                            startOfToday);
                    if (nextOccurrence != null) {
                        activeArray.put(nextOccurrence);
                    }
                    changed = true;
                    continue;
                }
                activeArray.put(item);
            }

            if (!changed) {
                return storedTasks;
            }

            String cleanedTasks = activeArray.toString();
            preferences.edit().putString(TASKS_KEY, cleanedTasks).apply();
            return cleanedTasks;
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudieron cerrar las actividades de días anteriores", exception);
            return storedTasks;
        }
    }

    private JSONObject createNextOccurrence(JSONObject previousOccurrence,
                                            long earliestOccurrenceAt)
            throws JSONException {
        if (previousOccurrence.optBoolean("nextOccurrenceGenerated", false)
                || !previousOccurrence.has("dueAt")
                || previousOccurrence.isNull("dueAt")) {
            return null;
        }

        TaskRecurrence recurrence = TaskRecurrence.fromStoredValue(
                previousOccurrence.optString("recurrence", null));
        Long nextDueAt = TaskActions.nextOccurrenceOnOrAfter(
                previousOccurrence.getLong("dueAt"),
                recurrence,
                earliestOccurrenceAt);
        if (nextDueAt == null) {
            return null;
        }

        JSONObject next = new JSONObject();
        next.put("id", UUID.randomUUID().toString());
        next.put("title", previousOccurrence.getString("title"));
        next.put("note", "");
        next.put("completed", false);
        next.put("dueAt", nextDueAt);
        next.put("completedAt", JSONObject.NULL);
        next.put("recurrence", recurrence.name());
        next.put("nextOccurrenceGenerated", false);
        return next;
    }

    private String migrateDueDatesToBuenosAires() {
        String storedTasks = preferences.getString(TASKS_KEY, "[]");
        if (preferences.getBoolean(BUENOS_AIRES_MIGRATION_KEY, false)) {
            return storedTasks;
        }

        TimeZone sourceTimeZone = TimeZone.getDefault();
        TimeZone targetTimeZone = AppTimeZone.get();
        if (sourceTimeZone.getID().equals(targetTimeZone.getID())) {
            preferences.edit().putBoolean(BUENOS_AIRES_MIGRATION_KEY, true).apply();
            return storedTasks;
        }

        try {
            JSONArray array = new JSONArray(storedTasks);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                if (!item.has("dueAt") || item.isNull("dueAt")) {
                    continue;
                }

                Calendar source = Calendar.getInstance(sourceTimeZone);
                source.setTimeInMillis(item.getLong("dueAt"));

                Calendar target = Calendar.getInstance(targetTimeZone);
                target.clear();
                target.set(
                        source.get(Calendar.YEAR),
                        source.get(Calendar.MONTH),
                        source.get(Calendar.DAY_OF_MONTH),
                        source.get(Calendar.HOUR_OF_DAY),
                        source.get(Calendar.MINUTE),
                        source.get(Calendar.SECOND));
                target.set(Calendar.MILLISECOND, source.get(Calendar.MILLISECOND));
                item.put("dueAt", target.getTimeInMillis());
            }

            String migratedTasks = array.toString();
            preferences.edit()
                    .putString(TASKS_KEY, migratedTasks)
                    .putBoolean(BUENOS_AIRES_MIGRATION_KEY, true)
                    .apply();
            return migratedTasks;
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudieron migrar las fechas al horario de Buenos Aires", exception);
            return storedTasks;
        }
    }

    public void save(List<Task> tasks) {
        JSONArray array = new JSONArray();

        try {
            new TaskHistoryRepository(context).recordTaskStates(tasks);
            for (Task task : tasks) {
                JSONObject item = new JSONObject();
                item.put("id", task.getId());
                item.put("title", task.getTitle());
                item.put("note", task.getNote());
                item.put("completed", task.isCompleted());
                item.put("dueAt", task.getDueAt() == null ? JSONObject.NULL : task.getDueAt());
                item.put("completedAt",
                        task.getCompletedAt() == null ? JSONObject.NULL : task.getCompletedAt());
                item.put("recurrence", task.getRecurrence().name());
                item.put("nextOccurrenceGenerated", task.isNextOccurrenceGenerated());
                array.put(item);
            }

            preferences.edit().putString(TASKS_KEY, array.toString()).apply();
            ReminderScheduler.syncAll(context, tasks);
            TasksWidgetProvider.updateAllWidgets(context);
        } catch (JSONException exception) {
            Log.e(TAG, "No se pudieron guardar las actividades", exception);
        }
    }
}
