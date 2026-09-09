package com.cerus.misactividades;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReminderScheduler {
    private static final String PREFERENCES_NAME = "reminder_schedule";
    private static final String SCHEDULED_IDS_KEY = "scheduled_task_ids";

    private ReminderScheduler() {
    }

    public static void syncAll(Context context, List<Task> tasks) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences preferences = applicationContext.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> previouslyScheduled = new HashSet<>(
                preferences.getStringSet(SCHEDULED_IDS_KEY, new HashSet<>()));
        Set<String> currentlyScheduled = new HashSet<>();
        long now = System.currentTimeMillis();

        for (Task task : tasks) {
            if (!task.isCompleted() && task.getDueAt() != null && task.getDueAt() > now) {
                schedule(applicationContext, task.getId(), task.getDueAt());
                currentlyScheduled.add(task.getId());
            }
        }

        for (String taskId : previouslyScheduled) {
            if (!currentlyScheduled.contains(taskId)) {
                cancel(applicationContext, taskId);
            }
        }
        preferences.edit().putStringSet(SCHEDULED_IDS_KEY, currentlyScheduled).apply();
    }

    public static void schedule(Context context, String taskId, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = reminderPendingIntent(context, taskId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void scheduleSnooze(Context context, String taskId, long triggerAtMillis) {
        schedule(context, taskId, triggerAtMillis);
    }

    public static void cancel(Context context, String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(reminderPendingIntent(context, taskId));
    }

    private static PendingIntent reminderPendingIntent(Context context, String taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ReminderReceiver.ACTION_REMIND)
                .setData(Uri.parse("misactividades://reminder/" + taskId))
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
