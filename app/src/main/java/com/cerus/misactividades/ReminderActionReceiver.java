package com.cerus.misactividades;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class ReminderActionReceiver extends BroadcastReceiver {
    public static final String ACTION_COMPLETE = "com.cerus.misactividades.COMPLETE";
    public static final String ACTION_SNOOZE = "com.cerus.misactividades.SNOOZE";
    public static final String ACTION_TOMORROW = "com.cerus.misactividades.TOMORROW";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderReceiver.EXTRA_TASK_ID);
        if (taskId == null) {
            return;
        }

        TaskRepository repository = new TaskRepository(context);
        List<Task> tasks = repository.load();
        Task selectedTask = null;
        for (Task task : tasks) {
            if (taskId.equals(task.getId())) {
                selectedTask = task;
                break;
            }
        }
        if (selectedTask == null) {
            ReminderNotifications.dismiss(context, taskId);
            return;
        }

        String action = intent.getAction();
        if (ACTION_COMPLETE.equals(action)) {
            TaskActions.setCompleted(tasks, selectedTask, true);
            repository.save(tasks);
        } else if (ACTION_SNOOZE.equals(action)) {
            selectedTask.setDueAt(System.currentTimeMillis() + 15 * 60 * 1000L);
            repository.save(tasks);
        } else if (ACTION_TOMORROW.equals(action)) {
            Calendar timeSource = AppTimeZone.calendar();
            if (selectedTask.getDueAt() != null) {
                timeSource.setTimeInMillis(selectedTask.getDueAt());
            }
            Calendar due = AppTimeZone.calendar();
            due.add(Calendar.DAY_OF_MONTH, 1);
            due.set(Calendar.HOUR_OF_DAY, timeSource.get(Calendar.HOUR_OF_DAY));
            due.set(Calendar.MINUTE, timeSource.get(Calendar.MINUTE));
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            selectedTask.setDueAt(due.getTimeInMillis());
            repository.save(tasks);
        }
        ReminderNotifications.dismiss(context, taskId);
    }
}
