package com.cerus.misactividades;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class ReminderNotifications {
    public static final String CHANNEL_ID = "task_reminders";

    private ReminderNotifications() {
    }

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    public static void show(Context context, Task task) {
        createChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);

        Intent openIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, task.getId().hashCode(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.getTitle())
                .setContentText(context.getString(R.string.notification_due_now))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(action(context, task.getId(), ReminderActionReceiver.ACTION_COMPLETE,
                        R.string.notification_complete))
                .addAction(action(context, task.getId(), ReminderActionReceiver.ACTION_SNOOZE,
                        R.string.notification_snooze))
                .addAction(action(context, task.getId(), ReminderActionReceiver.ACTION_TOMORROW,
                        R.string.notification_tomorrow))
                .build();
        manager.notify(task.getId().hashCode(), notification);
    }

    private static Notification.Action action(Context context, String taskId, String action,
                                               int titleResource) {
        Intent intent = new Intent(context, ReminderActionReceiver.class)
                .setAction(action)
                .setData(Uri.parse("misactividades://action/" + action + "/" + taskId))
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId + action).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(null, context.getString(titleResource), pendingIntent)
                .build();
    }

    public static void dismiss(Context context, String taskId) {
        context.getSystemService(NotificationManager.class).cancel(taskId.hashCode());
    }
}
