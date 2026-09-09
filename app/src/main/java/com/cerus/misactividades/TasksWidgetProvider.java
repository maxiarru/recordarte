package com.cerus.misactividades;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TasksWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            updateWidget(context, manager, widgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int widgetId, Bundle newOptions) {
        updateWidget(context, manager, widgetId);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TasksWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(provider);
        for (int widgetId : widgetIds) {
            updateWidget(context, manager, widgetId);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        manager.updateAppWidget(widgetId, createViews(context, widgetId));
        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_task_list);
    }

    private static RemoteViews createViews(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tasks_widget);
        AppThemePreferences theme = new AppThemePreferences(context);
        views.setInt(
                R.id.widget_container,
                "setBackgroundResource",
                theme.isDarkMode()
                        ? darkWidgetBackground(context)
                        : R.drawable.widget_background);
        views.setTextColor(R.id.widget_title, theme.secondaryText());
        views.setTextColor(R.id.widget_empty, theme.secondaryText());

        int rangeDays = new WidgetPreferences(context).getRangeDays();
        views.setTextViewText(
                R.id.widget_title,
                context.getString(rangeDays == WidgetPreferences.RANGE_FIVE_DAYS
                        ? R.string.widget_mode_five_days
                        : R.string.widget_mode_today));
        views.setTextViewText(
                R.id.widget_empty,
                context.getString(rangeDays == WidgetPreferences.RANGE_FIVE_DAYS
                        ? R.string.widget_empty_five_days
                        : R.string.widget_empty_today));

        Intent serviceIntent = new Intent(context, TasksWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_task_list, serviceIntent);
        views.setEmptyView(R.id.widget_task_list, R.id.widget_empty);

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent taskPendingIntent = PendingIntent.getActivity(
                context,
                widgetId + 10_000,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent);
        views.setPendingIntentTemplate(R.id.widget_task_list, taskPendingIntent);

        return views;
    }

    private static int darkWidgetBackground(Context context) {
        switch (new AppBackgroundPreferences(context).getSelectedColor(true)) {
            case AppBackgroundPreferences.LIGHT_BLUE:
                return R.drawable.widget_background_dark_blue;
            case AppBackgroundPreferences.MINT:
                return R.drawable.widget_background_dark_mint;
            case AppBackgroundPreferences.SAND:
                return R.drawable.widget_background_dark_sand;
            case AppBackgroundPreferences.LIGHT_GRAY:
                return R.drawable.widget_background_dark_gray;
            case AppBackgroundPreferences.LAVENDER:
            default:
                return R.drawable.widget_background_dark;
        }
    }

    static List<Task> findRelevantTasks(Context context, int rangeDays) {
        long now = System.currentTimeMillis();
        Calendar start = AppTimeZone.calendar();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, rangeDays);

        long startMillis = start.getTimeInMillis();
        long endMillis = end.getTimeInMillis();
        List<Task> relevant = new ArrayList<>();

        List<Task> allTasks = new TaskRepository(context).load();
        for (Task task : allTasks) {
            Long dueAt = task.getDueAt();
            if (!task.isCompleted()
                    && !TaskActions.isGeneratedOccurrenceWaitingForParentRemoval(allTasks, task)
                    && dueAt != null
                    && (dueAt < now || (dueAt >= startMillis && dueAt < endMillis))) {
                relevant.add(task);
            }
        }

        relevant.sort(TaskActions::compareForDisplay);
        return relevant;
    }

    static String formatTask(Context context, Task task, boolean compact) {
        long dueAt = task.getDueAt();
        boolean overdue = dueAt < System.currentTimeMillis();
        if (compact) {
            return context.getString(
                    overdue
                            ? R.string.widget_overdue_task_compact
                            : R.string.widget_task_compact,
                    task.getTitle());
        }

        String formatted = TaskDateFormatter.formatDateTime(dueAt);
        if (overdue) {
            return context.getString(
                    R.string.widget_overdue_task_line,
                    task.getTitle(),
                    formatted);
        }

        String when;
        if (AppTimeZone.isToday(dueAt)) {
            when = context.getString(R.string.widget_today, formatted);
        } else {
            when = context.getString(R.string.widget_upcoming, formatted);
        }
        return context.getString(R.string.widget_task_line, task.getTitle(), when);
    }
}
