package com.cerus.misactividades;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMIND = "com.cerus.misactividades.REMIND";
    public static final String EXTRA_TASK_ID = "task_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId == null) {
            return;
        }
        List<Task> tasks = new TaskRepository(context).load();
        for (Task task : tasks) {
            if (taskId.equals(task.getId()) && !task.isCompleted()) {
                ReminderNotifications.show(context, task);
                return;
            }
        }
    }
}
