package com.cerus.misactividades;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class TasksWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        return new TasksFactory(getApplicationContext(), widgetId);
    }

    private static final class TasksFactory implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private List<Task> tasks = new ArrayList<>();
        private boolean compact;

        private TasksFactory(Context context, int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            int rangeDays = new WidgetPreferences(context).getRangeDays();
            tasks = TasksWidgetProvider.findRelevantTasks(context, rangeDays);

            Bundle options = AppWidgetManager.getInstance(context)
                    .getAppWidgetOptions(widgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110);
            compact = width < 180;
        }

        @Override
        public void onDestroy() {
            tasks.clear();
        }

        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= tasks.size()) {
                return null;
            }

            Task task = tasks.get(position);
            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_task_row);
            row.setTextViewText(
                    R.id.widget_task_text,
                    TasksWidgetProvider.formatTask(context, task, compact));
            row.setTextColor(
                    R.id.widget_task_text,
                    new AppThemePreferences(context).primaryText());

            row.setOnClickFillInIntent(R.id.widget_task_text, new Intent());
            return row;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return tasks.get(position).getId().hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
