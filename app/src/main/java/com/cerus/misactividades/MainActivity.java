package com.cerus.misactividades;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private TaskRepository repository;
    private List<Task> tasks = new ArrayList<>();
    private LinearLayout screen;
    private EditText taskInput;
    private LinearLayout taskList;
    private TextView emptyMessage;
    private TextView taskSectionTitle;
    private Button allTasksFilterButton;
    private Button todayTasksFilterButton;
    private Button otherTasksFilterButton;
    private Button dueDateButton;
    private Button clearDateButton;
    private Button recurrenceButton;
    private AppBackgroundPreferences backgroundPreferences;
    private AppThemePreferences themePreferences;
    private boolean darkThemeApplied;
    private LinearLayout undoBar;
    private TextView undoMessage;
    private final Handler undoHandler = new Handler(Looper.getMainLooper());
    private Task pendingDeletedTask;
    private int pendingDeletedIndex = -1;
    private Runnable hideUndoRunnable;
    private Runnable completedTasksCleanupRunnable;
    private Runnable completedTasksLockRunnable;
    private Long selectedDueAt;
    private TaskRecurrence selectedRecurrence = TaskRecurrence.NONE;
    private TaskFilter selectedTaskFilter = TaskFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themePreferences = new AppThemePreferences(this);
        darkThemeApplied = themePreferences.isDarkMode();
        if (darkThemeApplied) {
            setTheme(R.style.Theme_MisActividades_Dark);
        }
        super.onCreate(savedInstanceState);

        repository = new TaskRepository(this);
        backgroundPreferences = new AppBackgroundPreferences(this);
        tasks = repository.load();
        ReminderNotifications.createChannel(this);
        requestNotificationPermission();
        ReminderScheduler.syncAll(this, tasks);
        setContentView(createScreen());
        renderTasks();
        TasksWidgetProvider.updateAllWidgets(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (darkThemeApplied != themePreferences.isDarkMode()) {
            recreate();
            return;
        }
        if (screen != null && backgroundPreferences != null) {
            screen.setBackgroundColor(backgroundPreferences.getColorValue(
                    themePreferences.isDarkMode()));
            tasks = repository.load();
            renderTasks();
            scheduleCompletedTasksCleanup();
            scheduleCompletedTaskLocks();
        }
    }

    private View createScreen() {
        screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(16), dp(16), dp(12));
        screen.setBackgroundColor(backgroundPreferences.getColorValue(
                themePreferences.isDarkMode()));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView wordmark = new ImageView(this);
        wordmark.setImageResource(R.drawable.recordarte_wordmark_dark_safe);
        wordmark.setAdjustViewBounds(true);
        wordmark.setScaleType(ImageView.ScaleType.FIT_START);
        wordmark.setContentDescription(getString(R.string.app_name));
        titleRow.addView(wordmark, new LinearLayout.LayoutParams(0, dp(60), 1));

        Button calendarButton = new Button(this);
        calendarButton.setText(R.string.calendar_icon);
        calendarButton.setTextSize(23);
        UiStyle.applyIconButton(calendarButton, this, themePreferences);
        calendarButton.setContentDescription(getString(R.string.open_calendar));
        calendarButton.setOnClickListener(view -> startActivity(
                new Intent(this, CalendarActivity.class)));
        LinearLayout.LayoutParams calendarButtonParams = new LinearLayout.LayoutParams(
                dp(52), dp(48));
        calendarButtonParams.setMargins(0, 0, dp(6), 0);
        titleRow.addView(calendarButton, calendarButtonParams);

        Button settingsButton = new Button(this);
        settingsButton.setText(R.string.settings_icon);
        settingsButton.setTextSize(24);
        UiStyle.applyIconButton(settingsButton, this, themePreferences);
        settingsButton.setContentDescription(getString(R.string.open_settings));
        settingsButton.setOnClickListener(view -> startActivity(
                new Intent(this, SettingsActivity.class)));
        titleRow.addView(settingsButton, new LinearLayout.LayoutParams(dp(52), dp(48)));
        screen.addView(titleRow, matchWrap());

        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(14), dp(10), dp(14), dp(14));
        UiStyle.applyCard(formCard, this, themePreferences.cardBackground(),
                themePreferences.cardBorder(), 16);

        TextView formTitle = new TextView(this);
        formTitle.setText(R.string.new_task_section);
        formTitle.setTextSize(16);
        formTitle.setTextColor(themePreferences.primaryText());
        formTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        formCard.addView(formTitle, matchWrap());

        taskInput = new EditText(this);
        taskInput.setHint(R.string.task_hint);
        taskInput.setSingleLine(true);
        taskInput.setTextSize(17);
        taskInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        taskInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        taskInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTask();
                return true;
            }
            return false;
        });
        formCard.addView(taskInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        TextView dueSectionLabel = new TextView(this);
        dueSectionLabel.setText(R.string.due_date_section);
        dueSectionLabel.setTextSize(14);
        dueSectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        dueSectionLabel.setTextColor(themePreferences.secondaryText());
        LinearLayout.LayoutParams dueSectionParams = matchWrap();
        dueSectionParams.setMargins(0, dp(4), 0, 0);
        formCard.addView(dueSectionLabel, dueSectionParams);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        dueDateButton = new Button(this);
        dueDateButton.setText(R.string.no_due_date_selected);
        UiStyle.applySecondaryButton(dueDateButton, this, themePreferences);
        dueDateButton.setOnClickListener(view -> chooseDueDate());
        dateRow.addView(dueDateButton, new LinearLayout.LayoutParams(0, dp(46), 1));

        clearDateButton = new Button(this);
        clearDateButton.setText(R.string.set_no_due_date);
        UiStyle.applyDangerButton(clearDateButton, this, themePreferences);
        clearDateButton.setVisibility(View.GONE);
        clearDateButton.setOnClickListener(view -> {
            selectedDueAt = null;
            selectedRecurrence = TaskRecurrence.NONE;
            updateDateSelection();
            updateRecurrenceButton();
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(82), dp(46));
        clearParams.setMargins(dp(8), 0, 0, 0);
        dateRow.addView(clearDateButton, clearParams);
        LinearLayout.LayoutParams dateRowParams = matchWrap();
        dateRowParams.setMargins(0, dp(6), 0, 0);
        formCard.addView(dateRow, dateRowParams);

        recurrenceButton = new Button(this);
        UiStyle.applySecondaryButton(recurrenceButton, this, themePreferences);
        recurrenceButton.setTextSize(14);
        recurrenceButton.setOnClickListener(view -> showRecurrenceDialog(
                selectedRecurrence,
                recurrence -> {
                    selectedRecurrence = recurrence;
                    updateRecurrenceButton();
                }));
        updateRecurrenceButton();
        recurrenceButton.setEnabled(selectedDueAt != null);
        recurrenceButton.setAlpha(selectedDueAt == null ? 0.5f : 1f);
        LinearLayout.LayoutParams recurrenceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        recurrenceParams.setMargins(0, dp(6), 0, 0);
        formCard.addView(recurrenceButton, recurrenceParams);

        Button addButton = new Button(this);
        addButton.setText(R.string.add_task);
        UiStyle.applyPrimaryButton(addButton, this, themePreferences);
        addButton.setOnClickListener(view -> addTask());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        addParams.setMargins(0, dp(8), 0, 0);
        formCard.addView(addButton, addParams);

        LinearLayout.LayoutParams formParams = matchWrap();
        formParams.setMargins(0, dp(10), 0, dp(12));
        screen.addView(formCard, formParams);

        taskSectionTitle = new TextView(this);
        taskSectionTitle.setTextSize(17);
        taskSectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        taskSectionTitle.setTextColor(themePreferences.primaryText());
        screen.addView(taskSectionTitle, matchWrap());

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        allTasksFilterButton = createTaskFilterButton(TaskFilter.ALL);
        filterRow.addView(allTasksFilterButton, filterButtonParams(true));

        todayTasksFilterButton = createTaskFilterButton(TaskFilter.TODAY);
        filterRow.addView(todayTasksFilterButton, filterButtonParams(false));

        otherTasksFilterButton = createTaskFilterButton(TaskFilter.OTHER);
        filterRow.addView(otherTasksFilterButton, filterButtonParams(false));

        LinearLayout.LayoutParams filterRowParams = matchWrap();
        filterRowParams.setMargins(0, dp(6), 0, 0);
        screen.addView(filterRow, filterRowParams);

        emptyMessage = new TextView(this);
        emptyMessage.setText(R.string.empty_tasks);
        emptyMessage.setTextSize(17);
        emptyMessage.setTextColor(themePreferences.secondaryText());
        emptyMessage.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        emptyParams.setMargins(0, dp(18), 0, 0);
        screen.addView(emptyMessage, emptyParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        taskList = new LinearLayout(this);
        taskList.setOrientation(LinearLayout.VERTICAL);
        taskList.setPadding(0, dp(8), 0, 0);
        scrollView.addView(taskList, matchWrap());
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        undoBar = new LinearLayout(this);
        undoBar.setOrientation(LinearLayout.HORIZONTAL);
        undoBar.setGravity(Gravity.CENTER_VERTICAL);
        undoBar.setPadding(dp(14), dp(6), dp(6), dp(6));
        UiStyle.applyCard(undoBar, this, themePreferences.undoBackground(),
                themePreferences.undoBackground(), 12);
        undoBar.setVisibility(View.GONE);

        undoMessage = new TextView(this);
        undoMessage.setTextColor(Color.WHITE);
        undoMessage.setTextSize(15);
        undoBar.addView(undoMessage, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button undoButton = new Button(this);
        undoButton.setText(R.string.undo_delete);
        UiStyle.applySecondaryButton(undoButton, this, themePreferences);
        undoButton.setTextSize(15);
        undoButton.setOnClickListener(view -> undoLastDeletion());
        undoBar.addView(undoButton, new LinearLayout.LayoutParams(dp(112), dp(48)));

        LinearLayout.LayoutParams undoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
        undoParams.setMargins(0, dp(8), 0, 0);
        screen.addView(undoBar, undoParams);

        return screen;
    }

    private void chooseDueDate() {
        taskInput.clearFocus();
        hideKeyboard();
        Calendar selected = AppTimeZone.calendar();
        if (selectedDueAt != null) {
            selected.setTimeInMillis(selectedDueAt);
        } else {
            selected.add(Calendar.HOUR_OF_DAY, 1);
        }

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> chooseDueTime(year, month, day, selected),
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private void chooseDueTime(int year, int month, int day, Calendar previousSelection) {
        TimePickerDialog timeDialog = new TimePickerDialog(
                this,
                (timePicker, hour, minute) -> {
                    Calendar dueDate = AppTimeZone.calendar();
                    dueDate.set(year, month, day, hour, minute, 0);
                    dueDate.set(Calendar.MILLISECOND, 0);
                    selectedDueAt = dueDate.getTimeInMillis();
                    updateDateSelection();
                },
                previousSelection.get(Calendar.HOUR_OF_DAY),
                previousSelection.get(Calendar.MINUTE),
                true);
        timeDialog.show();
    }

    private void updateDateSelection() {
        if (selectedDueAt == null) {
            dueDateButton.setText(R.string.no_due_date_selected);
            clearDateButton.setVisibility(View.GONE);
            if (recurrenceButton != null) {
                recurrenceButton.setEnabled(false);
                recurrenceButton.setAlpha(0.5f);
            }
            return;
        }

        dueDateButton.setText(getString(
                R.string.selected_due_date,
                TaskDateFormatter.formatDateTime(selectedDueAt)));
        clearDateButton.setVisibility(View.VISIBLE);
        if (recurrenceButton != null) {
            recurrenceButton.setEnabled(true);
            recurrenceButton.setAlpha(1f);
        }
    }

    private void updateRecurrenceButton() {
        recurrenceButton.setText(getString(
                R.string.recurrence_button,
                getRecurrenceLabel(selectedRecurrence)));
    }

    private String getRecurrenceLabel(TaskRecurrence recurrence) {
        switch (recurrence) {
            case DAILY:
                return getString(R.string.recurrence_daily);
            case WEEKLY:
                return getString(R.string.recurrence_weekly);
            case MONTHLY:
                return getString(R.string.recurrence_monthly);
            default:
                return getString(R.string.recurrence_none);
        }
    }

    private void showRecurrenceDialog(TaskRecurrence current,
                                      RecurrenceSelectionListener listener) {
        TaskRecurrence[] values = {
                TaskRecurrence.NONE,
                TaskRecurrence.DAILY,
                TaskRecurrence.WEEKLY,
                TaskRecurrence.MONTHLY
        };
        CharSequence[] labels = new CharSequence[values.length];
        int selected = 0;
        for (int index = 0; index < values.length; index++) {
            labels[index] = getRecurrenceLabel(values[index]);
            if (values[index] == current) {
                selected = index;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.recurrence_dialog_title)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    listener.onSelected(values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addTask() {
        String title = taskInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.task_required, Toast.LENGTH_SHORT).show();
            return;
        }

        TaskRecurrence recurrence = selectedDueAt == null
                ? TaskRecurrence.NONE
                : selectedRecurrence;
        tasks.add(new Task(title, selectedDueAt, recurrence));
        repository.save(tasks);
        taskInput.setText("");
        selectedDueAt = null;
        selectedRecurrence = TaskRecurrence.NONE;
        updateDateSelection();
        updateRecurrenceButton();
        taskInput.clearFocus();
        hideKeyboard();
        renderTasks();
    }

    private void renderTasks() {
        taskList.removeAllViews();
        List<Task> displayableTasks = new ArrayList<>();
        int todayTasks = 0;
        int otherTasks = 0;
        List<Task> orderedTasks = new ArrayList<>(tasks);
        orderedTasks.sort(TaskActions::compareForDisplay);
        for (Task task : orderedTasks) {
            if (TaskActions.isGeneratedOccurrenceWaitingForParentRemoval(tasks, task)) {
                continue;
            }
            displayableTasks.add(task);
            if (isTodayTask(task)) {
                todayTasks++;
            } else {
                otherTasks++;
            }
        }

        int visibleTasks = 0;
        for (Task task : displayableTasks) {
            if (!matchesSelectedFilter(task)) {
                continue;
            }
            taskList.addView(createTaskRow(task));
            visibleTasks++;
        }

        taskSectionTitle.setText(getString(
                R.string.task_list_section,
                displayableTasks.size()));
        updateTaskFilterButtons(displayableTasks.size(), todayTasks, otherTasks);
        if (selectedTaskFilter == TaskFilter.TODAY) {
            emptyMessage.setText(R.string.empty_tasks_today);
        } else if (selectedTaskFilter == TaskFilter.OTHER) {
            emptyMessage.setText(R.string.empty_tasks_other);
        } else {
            emptyMessage.setText(R.string.empty_tasks);
        }
        emptyMessage.setVisibility(visibleTasks == 0 ? View.VISIBLE : View.GONE);
    }

    private Button createTaskFilterButton(TaskFilter filter) {
        Button button = new Button(this);
        button.setOnClickListener(view -> {
            selectedTaskFilter = filter;
            renderTasks();
        });
        return button;
    }

    private void updateTaskFilterButtons(int allTasks, int todayTasks, int otherTasks) {
        allTasksFilterButton.setText(getString(R.string.filter_all_tasks, allTasks));
        todayTasksFilterButton.setText(getString(R.string.filter_today_tasks, todayTasks));
        otherTasksFilterButton.setText(getString(R.string.filter_other_tasks, otherTasks));
        styleTaskFilterButton(allTasksFilterButton, selectedTaskFilter == TaskFilter.ALL);
        styleTaskFilterButton(todayTasksFilterButton, selectedTaskFilter == TaskFilter.TODAY);
        styleTaskFilterButton(otherTasksFilterButton, selectedTaskFilter == TaskFilter.OTHER);
    }

    private void styleTaskFilterButton(Button button, boolean selected) {
        if (selected) {
            UiStyle.applyPrimaryButton(button, this, themePreferences);
        } else {
            UiStyle.applySecondaryButton(button, this, themePreferences);
        }
        button.setTextSize(13);
    }

    private boolean matchesSelectedFilter(Task task) {
        if (selectedTaskFilter == TaskFilter.ALL) {
            return true;
        }
        boolean today = isTodayTask(task);
        return selectedTaskFilter == TaskFilter.TODAY ? today : !today;
    }

    private boolean isTodayTask(Task task) {
        return task.getDueAt() != null && AppTimeZone.isToday(task.getDueAt());
    }

    private LinearLayout.LayoutParams filterButtonParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
        if (!first) {
            params.setMargins(dp(6), 0, 0, 0);
        }
        return params;
    }

    private View createTaskRow(Task task) {
        long now = System.currentTimeMillis();
        boolean canCorrectCompletion = task.canCorrectCompletion(now);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        int cardColor = themePreferences.cardBackground();
        int accentColor = themePreferences.futureAccent();
        if (task.isCompleted()) {
            cardColor = themePreferences.completedCard();
            accentColor = themePreferences.completedAccent();
        } else if (task.getDueAt() != null && task.getDueAt() < now) {
            cardColor = themePreferences.overdueCard();
            accentColor = themePreferences.overdueAccent();
        } else if (task.getDueAt() != null && AppTimeZone.isToday(task.getDueAt())) {
            cardColor = themePreferences.todayCard();
            accentColor = themePreferences.todayAccent();
        }
        UiStyle.applyCard(row, this, cardColor, themePreferences.cardBorder(), 14);

        View statusAccent = new View(this);
        statusAccent.setBackground(UiStyle.rounded(
                this, accentColor, accentColor, 0, 4));
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(
                dp(5), LinearLayout.LayoutParams.MATCH_PARENT);
        accentParams.setMargins(dp(5), dp(7), 0, dp(7));
        row.addView(statusAccent, accentParams);

        LinearLayout taskInfo = new LinearLayout(this);
        taskInfo.setOrientation(LinearLayout.VERTICAL);
        taskInfo.setPadding(dp(6), dp(6), dp(10), dp(10));

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(task.getTitle());
        checkBox.setTextSize(17);
        checkBox.setTextColor(themePreferences.primaryText());
        checkBox.setChecked(task.isCompleted());
        updateCompletedStyle(checkBox, task.isCompleted());
        checkBox.setEnabled(!task.isCompleted() || canCorrectCompletion);
        checkBox.setClickable(!task.isCompleted() || canCorrectCompletion);
        if (!task.isCompleted() || canCorrectCompletion) {
            checkBox.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked == task.isCompleted()) {
                    return;
                }
                TaskActions.setCompleted(tasks, task, isChecked);
                if (isChecked) {
                    ReminderNotifications.dismiss(this, task.getId());
                } else {
                    new TaskHistoryRepository(this).removeCompletedEntry(task.getId());
                }
                repository.save(tasks);
                renderTasks();
                scheduleCompletedTasksCleanup();
                scheduleCompletedTaskLocks();
            });
        }
        taskInfo.addView(checkBox, matchWrap());

        if (task.getDueAt() == null) {
            TextView noDueDate = new TextView(this);
            noDueDate.setText(R.string.task_no_due_date);
            noDueDate.setTextColor(themePreferences.futureAccent());
            noDueDate.setTextSize(14);
            noDueDate.setPadding(dp(42), 0, 0, dp(3));
            taskInfo.addView(noDueDate, matchWrap());
        }

        if (task.isCompleted() || task.getDueAt() != null) {
            TextView dueDate = new TextView(this);
            if (task.isCompleted()) {
                dueDate.setText(canCorrectCompletion
                        ? R.string.task_completed_correction_window
                        : R.string.task_completed_until_end_of_day);
                dueDate.setTextColor(themePreferences.completedText());
            } else {
                dueDate.setText(formatDueDate(task.getDueAt()));
                dueDate.setTextColor(dueDateColor(task.getDueAt()));
            }
            dueDate.setTextSize(14);
            dueDate.setPadding(dp(42), 0, 0, dp(3));
            taskInfo.addView(dueDate, matchWrap());
        }

        if (task.getRecurrence() != TaskRecurrence.NONE) {
            TextView recurrence = new TextView(this);
            recurrence.setText(getString(
                    R.string.recurrence_summary,
                    getRecurrenceLabel(task.getRecurrence())));
            recurrence.setTextSize(13);
            recurrence.setPadding(dp(42), 0, 0, dp(3));
            recurrence.setTextColor(themePreferences.secondaryText());
            taskInfo.addView(recurrence, matchWrap());
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(42), dp(6), 0, 0);

        if (task.isCompleted()) {
            Button noteButton = createNoteButton(task);
            actions.addView(noteButton, actionParams(true));

            TextView locked = new TextView(this);
            locked.setText(canCorrectCompletion
                    ? R.string.completed_task_correctable
                    : R.string.completed_task_locked);
            locked.setTextSize(13);
            locked.setGravity(Gravity.CENTER);
            locked.setTextColor(themePreferences.secondaryText());
            LinearLayout.LayoutParams lockedParams = new LinearLayout.LayoutParams(
                    0, dp(40), 2);
            lockedParams.setMargins(dp(6), 0, 0, 0);
            actions.addView(locked, lockedParams);
        } else {
            Button editButton = new Button(this);
            editButton.setText(R.string.edit_task);
            UiStyle.applySecondaryButton(editButton, this, themePreferences);
            editButton.setContentDescription(getString(R.string.edit_task_description, task.getTitle()));
            editButton.setOnClickListener(view -> showEditTaskDialog(task));
            actions.addView(editButton, actionParams(true));

            actions.addView(createNoteButton(task), actionParams(false));

            Button deleteButton = new Button(this);
            deleteButton.setText(R.string.delete_task);
            UiStyle.applyDangerButton(deleteButton, this, themePreferences);
            deleteButton.setContentDescription(getString(R.string.delete_task_description, task.getTitle()));
            deleteButton.setOnClickListener(view -> deleteTaskWithUndo(task));
            actions.addView(deleteButton, actionParams(false));
        }
        taskInfo.addView(actions, matchWrap());
        row.addView(taskInfo, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);
        return row;
    }

    private Button createNoteButton(Task task) {
        Button noteButton = new Button(this);
        noteButton.setText(task.hasNote() ? R.string.note_button_has_note : R.string.note_button);
        UiStyle.applySecondaryButton(noteButton, this, themePreferences);
        noteButton.setContentDescription(getString(
                task.hasNote()
                        ? R.string.open_note_description
                        : R.string.add_note_description,
                task.getTitle()));
        noteButton.setOnClickListener(view -> showNoteDialog(task));
        return noteButton;
    }

    private LinearLayout.LayoutParams actionParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
        if (!first) {
            params.setMargins(dp(6), 0, 0, 0);
        }
        return params;
    }

    private void showNoteDialog(Task task) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), 0);

        EditText noteInput = new EditText(this);
        noteInput.setText(task.getNote());
        noteInput.setHint(R.string.note_hint);
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        noteInput.setMinLines(6);
        noteInput.setMaxLines(12);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        NoteLinks.enableWebLinks(noteInput, themePreferences.primaryAction());
        content.addView(noteInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.note_dialog_title, task.getTitle()))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete_note, null)
                .setPositiveButton(R.string.save_note, null)
                .create();

        dialog.setOnShowListener(unused -> {
            Button deleteNoteButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            deleteNoteButton.setEnabled(task.hasNote());
            deleteNoteButton.setOnClickListener(view -> {
                task.setNote("");
                repository.save(tasks);
                renderTasks();
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                task.setNote(noteInput.getText().toString().trim());
                repository.save(tasks);
                renderTasks();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void scheduleCompletedTasksCleanup() {
        if (completedTasksCleanupRunnable != null) {
            undoHandler.removeCallbacks(completedTasksCleanupRunnable);
        }

        Calendar tomorrow = AppTimeZone.calendar();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 1);
        tomorrow.set(Calendar.MILLISECOND, 0);

        completedTasksCleanupRunnable = () -> {
            tasks = repository.load();
            renderTasks();
            TasksWidgetProvider.updateAllWidgets(this);
            scheduleCompletedTasksCleanup();
        };
        undoHandler.postDelayed(
                completedTasksCleanupRunnable,
                Math.max(1000L, tomorrow.getTimeInMillis() - System.currentTimeMillis()));
    }

    private void scheduleCompletedTaskLocks() {
        if (completedTasksLockRunnable != null) {
            undoHandler.removeCallbacks(completedTasksLockRunnable);
        }

        long now = System.currentTimeMillis();
        long nextLockAt = Long.MAX_VALUE;
        for (Task task : tasks) {
            Long completedAt = task.getCompletedAt();
            if (!task.isCompleted() || completedAt == null) {
                continue;
            }
            long lockAt = completedAt + Task.COMPLETION_CORRECTION_WINDOW_MILLIS;
            if (lockAt > now && lockAt < nextLockAt) {
                nextLockAt = lockAt;
            }
        }

        if (nextLockAt == Long.MAX_VALUE) {
            completedTasksLockRunnable = null;
            return;
        }

        completedTasksLockRunnable = () -> {
            renderTasks();
            scheduleCompletedTaskLocks();
        };
        undoHandler.postDelayed(completedTasksLockRunnable, Math.max(250L, nextLockAt - now));
    }

    private void deleteTaskWithUndo(Task task) {
        clearPendingDeletion();
        pendingDeletedTask = task;
        pendingDeletedIndex = tasks.indexOf(task);

        ReminderScheduler.cancel(this, task.getId());
        tasks.remove(task);
        repository.save(tasks);
        renderTasks();

        undoMessage.setText(getString(R.string.task_deleted_message, task.getTitle()));
        undoBar.setVisibility(View.VISIBLE);
        hideUndoRunnable = this::clearPendingDeletion;
        undoHandler.postDelayed(hideUndoRunnable, 8000L);
    }

    private void undoLastDeletion() {
        if (pendingDeletedTask == null) {
            return;
        }
        if (hideUndoRunnable != null) {
            undoHandler.removeCallbacks(hideUndoRunnable);
        }

        int restoredIndex = Math.max(0, Math.min(pendingDeletedIndex, tasks.size()));
        tasks.add(restoredIndex, pendingDeletedTask);
        repository.save(tasks);
        renderTasks();
        clearPendingDeletion();
    }

    private void clearPendingDeletion() {
        if (hideUndoRunnable != null) {
            undoHandler.removeCallbacks(hideUndoRunnable);
        }
        hideUndoRunnable = null;
        pendingDeletedTask = null;
        pendingDeletedIndex = -1;
        if (undoBar != null) {
            undoBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (hideUndoRunnable != null) {
            undoHandler.removeCallbacks(hideUndoRunnable);
        }
        if (completedTasksCleanupRunnable != null) {
            undoHandler.removeCallbacks(completedTasksCleanupRunnable);
        }
        if (completedTasksLockRunnable != null) {
            undoHandler.removeCallbacks(completedTasksLockRunnable);
        }
        super.onDestroy();
    }

    private void showEditTaskDialog(Task task) {
        final Long[] editedDueAt = {task.getDueAt()};
        final TaskRecurrence[] editedRecurrence = {task.getRecurrence()};

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), 0);

        EditText titleInput = new EditText(this);
        titleInput.setText(task.getTitle());
        titleInput.setSelectAllOnFocus(true);
        titleInput.setSingleLine(true);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        content.addView(titleInput, matchWrap());

        Button dateButton = new Button(this);
        UiStyle.applySecondaryButton(dateButton, this, themePreferences);
        LinearLayout.LayoutParams dateParams = matchWrap();
        dateParams.setMargins(0, dp(8), 0, 0);
        content.addView(dateButton, dateParams);

        Button noDueDateButton = new Button(this);
        LinearLayout.LayoutParams noDueDateParams = matchWrap();
        noDueDateParams.setMargins(0, dp(6), 0, 0);
        content.addView(noDueDateButton, noDueDateParams);

        LinearLayout quickDates = new LinearLayout(this);
        quickDates.setOrientation(LinearLayout.HORIZONTAL);

        Button todayButton = new Button(this);
        todayButton.setText(R.string.quick_today);
        UiStyle.applySecondaryButton(todayButton, this, themePreferences);
        quickDates.addView(todayButton, new LinearLayout.LayoutParams(0, dp(46), 1));

        Button tomorrowButton = new Button(this);
        tomorrowButton.setText(R.string.quick_tomorrow);
        UiStyle.applySecondaryButton(tomorrowButton, this, themePreferences);
        LinearLayout.LayoutParams tomorrowParams = new LinearLayout.LayoutParams(
                0, dp(46), 1);
        tomorrowParams.setMargins(dp(6), 0, 0, 0);
        quickDates.addView(tomorrowButton, tomorrowParams);

        content.addView(quickDates, matchWrap());

        Button editRecurrenceButton = new Button(this);
        UiStyle.applySecondaryButton(editRecurrenceButton, this, themePreferences);
        editRecurrenceButton.setOnClickListener(view -> showRecurrenceDialog(
                editedRecurrence[0],
                selected -> {
                    editedRecurrence[0] = selected;
                    editRecurrenceButton.setText(getString(
                            R.string.recurrence_button,
                            getRecurrenceLabel(selected)));
                }));
        content.addView(editRecurrenceButton, matchWrap());

        updateEditDueControls(
                dateButton,
                noDueDateButton,
                editRecurrenceButton,
                editedDueAt[0],
                editedRecurrence[0]);
        dateButton.setOnClickListener(view -> chooseEditDueDate(editedDueAt[0], selected -> {
            editedDueAt[0] = selected;
            updateEditDueControls(
                    dateButton,
                    noDueDateButton,
                    editRecurrenceButton,
                    editedDueAt[0],
                    editedRecurrence[0]);
        }));
        noDueDateButton.setOnClickListener(view -> {
            editedDueAt[0] = null;
            editedRecurrence[0] = TaskRecurrence.NONE;
            updateEditDueControls(
                    dateButton,
                    noDueDateButton,
                    editRecurrenceButton,
                    null,
                    editedRecurrence[0]);
        });
        todayButton.setOnClickListener(view -> {
            editedDueAt[0] = quickDueDate(0, editedDueAt[0]);
            updateEditDueControls(
                    dateButton,
                    noDueDateButton,
                    editRecurrenceButton,
                    editedDueAt[0],
                    editedRecurrence[0]);
        });
        tomorrowButton.setOnClickListener(view -> {
            editedDueAt[0] = quickDueDate(1, editedDueAt[0]);
            updateEditDueControls(
                    dateButton,
                    noDueDateButton,
                    editRecurrenceButton,
                    editedDueAt[0],
                    editedRecurrence[0]);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_task_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String editedTitle = titleInput.getText().toString().trim();
                    if (editedTitle.isEmpty()) {
                        titleInput.setError(getString(R.string.task_required));
                        return;
                    }
                    ReminderScheduler.cancel(this, task.getId());
                    task.setTitle(editedTitle);
                    task.setDueAt(editedDueAt[0]);
                    task.setRecurrence(editedDueAt[0] == null
                            ? TaskRecurrence.NONE
                            : editedRecurrence[0]);
                    repository.save(tasks);
                    renderTasks();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void updateEditDateButton(Button button, Long dueAt) {
        button.setText(dueAt == null
                ? getString(R.string.choose_due_date)
                : getString(R.string.selected_due_date, TaskDateFormatter.formatDateTime(dueAt)));
    }

    private void updateEditDueControls(Button dateButton, Button noDueDateButton,
                                       Button recurrenceButton, Long dueAt,
                                       TaskRecurrence recurrence) {
        updateEditDateButton(dateButton, dueAt);
        recurrenceButton.setText(getString(
                R.string.recurrence_button,
                getRecurrenceLabel(recurrence)));
        recurrenceButton.setEnabled(dueAt != null);
        recurrenceButton.setAlpha(dueAt == null ? 0.5f : 1f);
        noDueDateButton.setText(dueAt == null
                ? R.string.no_due_date_active
                : R.string.set_no_due_date_long);
        if (dueAt == null) {
            UiStyle.applyPrimaryButton(noDueDateButton, this, themePreferences);
        } else {
            UiStyle.applySecondaryButton(noDueDateButton, this, themePreferences);
        }
    }

    private long quickDueDate(int daysFromToday, Long previousDueAt) {
        Calendar result = AppTimeZone.calendar();
        Calendar timeSource = AppTimeZone.calendar();
        if (previousDueAt != null) {
            timeSource.setTimeInMillis(previousDueAt);
        } else {
            timeSource.add(Calendar.HOUR_OF_DAY, 1);
        }
        result.add(Calendar.DAY_OF_MONTH, daysFromToday);
        result.set(Calendar.HOUR_OF_DAY, timeSource.get(Calendar.HOUR_OF_DAY));
        result.set(Calendar.MINUTE, timeSource.get(Calendar.MINUTE));
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        if (daysFromToday == 0 && result.getTimeInMillis() <= System.currentTimeMillis()) {
            result = AppTimeZone.calendar();
            result.add(Calendar.HOUR_OF_DAY, 1);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MILLISECOND, 0);
        }
        return result.getTimeInMillis();
    }

    private void chooseEditDueDate(Long currentDueAt, DueDateSelectionListener listener) {
        Calendar selected = AppTimeZone.calendar();
        if (currentDueAt != null) {
            selected.setTimeInMillis(currentDueAt);
        } else {
            selected.add(Calendar.HOUR_OF_DAY, 1);
        }
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    TimePickerDialog timeDialog = new TimePickerDialog(
                            this,
                            (timePicker, hour, minute) -> {
                                Calendar due = AppTimeZone.calendar();
                                due.set(year, month, day, hour, minute, 0);
                                due.set(Calendar.MILLISECOND, 0);
                                listener.onSelected(due.getTimeInMillis());
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            true);
                    timeDialog.show();
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private String formatDueDate(long dueAt) {
        String formatted = TaskDateFormatter.formatDateTime(dueAt);
        if (dueAt < System.currentTimeMillis()) {
            return getString(R.string.due_overdue, formatted);
        }
        if (AppTimeZone.isToday(dueAt)) {
            return getString(R.string.due_today, formatted);
        }
        return getString(R.string.due_future, formatted);
    }

    private int dueDateColor(long dueAt) {
        if (dueAt < System.currentTimeMillis()) {
            return themePreferences.overdueText();
        }
        if (AppTimeZone.isToday(dueAt)) {
            return themePreferences.todayText();
        }
        return themePreferences.secondaryText();
    }

    private void updateCompletedStyle(TextView view, boolean completed) {
        view.setPaintFlags(completed
                ? view.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : view.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        view.setAlpha(completed ? 0.6f : 1f);
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(taskInput.getWindowToken(), 0);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private interface RecurrenceSelectionListener {
        void onSelected(TaskRecurrence recurrence);
    }

    private interface DueDateSelectionListener {
        void onSelected(long dueAt);
    }

    private enum TaskFilter {
        ALL,
        TODAY,
        OTHER
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
