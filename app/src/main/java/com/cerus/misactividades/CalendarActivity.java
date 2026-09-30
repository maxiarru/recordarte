package com.cerus.misactividades;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends Activity {
    private static final Locale SPANISH_ARGENTINA = new Locale("es", "AR");
    private static final ZoneId BUENOS_AIRES = ZoneId.of(AppTimeZone.ID);
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH_ARGENTINA);

    private AppThemePreferences themePreferences;
    private AppBackgroundPreferences backgroundPreferences;
    private boolean darkThemeApplied;
    private List<Task> tasks = new ArrayList<>();
    private List<TaskHistoryRepository.Entry> completedEntries = new ArrayList<>();
    private List<TaskHistoryRepository.Entry> overdueEntries = new ArrayList<>();
    private LocalDate selectedDate;
    private YearMonth displayedMonth;
    private TextView monthTitle;
    private TextView selectedDateTitle;
    private LinearLayout calendarGrid;
    private LinearLayout selectedTasks;
    private LinearLayout screen;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themePreferences = new AppThemePreferences(this);
        backgroundPreferences = new AppBackgroundPreferences(this);
        darkThemeApplied = themePreferences.isDarkMode();
        if (darkThemeApplied) {
            setTheme(R.style.Theme_MisActividades_Dark);
        }
        super.onCreate(savedInstanceState);

        selectedDate = today();
        displayedMonth = YearMonth.from(selectedDate);
        loadData();
        setContentView(createScreen());
        renderCalendar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (darkThemeApplied != themePreferences.isDarkMode()) {
            recreate();
            return;
        }
        if (screen != null) {
            screen.setBackgroundColor(backgroundPreferences.getColorValue(
                    themePreferences.isDarkMode()));
            loadData();
            renderCalendar();
        }
    }

    private void loadData() {
        tasks = new TaskRepository(this).load();
        TaskHistoryRepository historyRepository = new TaskHistoryRepository(this);
        completedEntries = historyRepository.getCompletedEntries();
        overdueEntries = historyRepository.getOverdueEntries();
    }

    private ScrollView createScreen() {
        int backgroundColor = backgroundPreferences.getColorValue(
                themePreferences.isDarkMode());
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor);

        screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(16), dp(16), dp(28));
        screen.setBackgroundColor(backgroundColor);
        scrollView.addView(screen, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        addTitleRow();
        addMonthControls();
        addWeekdayHeader();

        calendarGrid = new LinearLayout(this);
        calendarGrid.setOrientation(LinearLayout.VERTICAL);
        screen.addView(calendarGrid, matchWrap());

        TextView legend = new TextView(this);
        legend.setText(R.string.calendar_legend);
        legend.setTextSize(13);
        legend.setTextColor(themePreferences.secondaryText());
        LinearLayout.LayoutParams legendParams = matchWrap();
        legendParams.setMargins(0, dp(8), 0, dp(20));
        screen.addView(legend, legendParams);

        selectedDateTitle = new TextView(this);
        selectedDateTitle.setTextSize(20);
        selectedDateTitle.setTypeface(null, Typeface.BOLD);
        selectedDateTitle.setTextColor(themePreferences.primaryText());
        LinearLayout.LayoutParams selectedTitleParams = matchWrap();
        selectedTitleParams.setMargins(0, 0, 0, dp(10));
        screen.addView(selectedDateTitle, selectedTitleParams);

        selectedTasks = new LinearLayout(this);
        selectedTasks.setOrientation(LinearLayout.VERTICAL);
        screen.addView(selectedTasks, matchWrap());
        return scrollView;
    }

    private void addTitleRow() {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText(R.string.back_icon);
        backButton.setTextSize(24);
        UiStyle.applyIconButton(backButton, this, themePreferences);
        backButton.setContentDescription(getString(R.string.back_to_tasks));
        backButton.setOnClickListener(view -> finish());
        titleRow.addView(backButton, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText(R.string.calendar_title);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(themePreferences.primaryText());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(dp(10), 0, 0, 0);
        titleRow.addView(title, titleParams);
        screen.addView(titleRow, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.calendar_subtitle);
        subtitle.setTextSize(15);
        subtitle.setTextColor(themePreferences.secondaryText());
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(12), 0, dp(16));
        screen.addView(subtitle, subtitleParams);
    }

    private void addMonthControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        Button previous = createNavigationButton(
                R.string.calendar_previous_icon,
                R.string.calendar_previous_month);
        previous.setOnClickListener(view -> changeMonth(-1));
        controls.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(46)));

        monthTitle = new TextView(this);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTextSize(20);
        monthTitle.setTypeface(null, Typeface.BOLD);
        monthTitle.setTextColor(themePreferences.primaryText());
        controls.addView(monthTitle, new LinearLayout.LayoutParams(
                0, dp(46), 1));

        Button next = createNavigationButton(
                R.string.calendar_next_icon,
                R.string.calendar_next_month);
        next.setOnClickListener(view -> changeMonth(1));
        controls.addView(next, new LinearLayout.LayoutParams(dp(48), dp(46)));

        LinearLayout.LayoutParams controlsParams = matchWrap();
        controlsParams.setMargins(0, 0, 0, dp(8));
        screen.addView(controls, controlsParams);

        Button todayButton = new Button(this);
        todayButton.setText(R.string.calendar_go_today);
        UiStyle.applySecondaryButton(todayButton, this, themePreferences);
        todayButton.setOnClickListener(view -> {
            selectedDate = today();
            displayedMonth = YearMonth.from(selectedDate);
            renderCalendar();
        });
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        todayParams.setMargins(0, 0, 0, dp(12));
        screen.addView(todayButton, todayParams);
    }

    private Button createNavigationButton(int textResource, int descriptionResource) {
        Button button = new Button(this);
        button.setText(textResource);
        button.setTextSize(24);
        button.setContentDescription(getString(descriptionResource));
        UiStyle.applyIconButton(button, this, themePreferences);
        return button;
    }

    private void addWeekdayHeader() {
        LinearLayout weekdays = new LinearLayout(this);
        weekdays.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = getResources().getStringArray(R.array.calendar_weekdays);
        for (String label : labels) {
            TextView day = new TextView(this);
            day.setText(label);
            day.setGravity(Gravity.CENTER);
            day.setTypeface(null, Typeface.BOLD);
            day.setTextSize(13);
            day.setTextColor(themePreferences.secondaryText());
            weekdays.addView(day, new LinearLayout.LayoutParams(0, dp(32), 1));
        }
        screen.addView(weekdays, matchWrap());
    }

    private void changeMonth(int amount) {
        displayedMonth = displayedMonth.plusMonths(amount);
        int day = Math.min(selectedDate.getDayOfMonth(), displayedMonth.lengthOfMonth());
        selectedDate = displayedMonth.atDay(day);
        renderCalendar();
    }

    private void renderCalendar() {
        String monthName = displayedMonth.format(MONTH_FORMATTER);
        monthTitle.setText(Character.toUpperCase(monthName.charAt(0)) + monthName.substring(1));
        calendarGrid.removeAllViews();

        int firstDayOffset = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int rowCount = (firstDayOffset + displayedMonth.lengthOfMonth() + 6) / 7;
        int dayNumber = 1;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            LinearLayout week = new LinearLayout(this);
            week.setOrientation(LinearLayout.HORIZONTAL);
            week.setBaselineAligned(false);
            for (int column = 0; column < 7; column++) {
                int cell = rowIndex * 7 + column;
                if (cell < firstDayOffset || dayNumber > displayedMonth.lengthOfMonth()) {
                    View empty = new View(this);
                    week.addView(empty, dayCellParams());
                    continue;
                }
                LocalDate date = displayedMonth.atDay(dayNumber++);
                week.addView(createDayButton(date, itemsForDate(date).size()), dayCellParams());
            }
            calendarGrid.addView(week, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));
        }
        renderSelectedTasks();
    }

    private TextView createDayButton(LocalDate date, int taskCount) {
        TextView dayCell = new TextView(this);
        dayCell.setGravity(Gravity.CENTER);
        dayCell.setIncludeFontPadding(false);
        dayCell.setLineSpacing(0, 0.92f);
        dayCell.setPadding(0, 0, 0, 0);
        dayCell.setTextSize(taskCount > 0 ? 13 : 15);
        dayCell.setText(taskCount > 0
                ? getString(R.string.calendar_day_with_tasks, date.getDayOfMonth(), taskCount)
                : String.valueOf(date.getDayOfMonth()));
        dayCell.setContentDescription(getResources().getQuantityString(
                R.plurals.calendar_day_description,
                taskCount,
                formatDate(date),
                taskCount));

        boolean selected = date.equals(selectedDate);
        boolean today = date.equals(today());
        int fillColor = themePreferences.cardBackground();
        int borderColor = taskCount > 0
                ? themePreferences.futureAccent()
                : themePreferences.cardBorder();
        int textColor = themePreferences.primaryText();
        if (today) {
            fillColor = themePreferences.todayCard();
            borderColor = themePreferences.todayAccent();
        }
        if (selected) {
            fillColor = themePreferences.primaryAction();
            borderColor = themePreferences.primaryAction();
            textColor = themePreferences.primaryActionText();
        }
        dayCell.setTextColor(textColor);
        dayCell.setBackground(UiStyle.rounded(this, fillColor, borderColor, 1, 10));
        dayCell.setClickable(true);
        dayCell.setFocusable(true);
        dayCell.setOnClickListener(view -> {
            selectedDate = date;
            renderCalendar();
            screen.post(() -> scrollView.smoothScrollTo(
                    0,
                    Math.max(0, selectedDateTitle.getTop() - dp(12))));
        });
        return dayCell;
    }

    private LinearLayout.LayoutParams dayCellParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private void renderSelectedTasks() {
        selectedDateTitle.setText(getString(
                R.string.calendar_selected_date,
                formatDate(selectedDate)));
        selectedTasks.removeAllViews();
        List<CalendarItem> items = itemsForDate(selectedDate);
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.calendar_empty_day);
            empty.setTextSize(15);
            empty.setTextColor(themePreferences.secondaryText());
            selectedTasks.addView(empty, matchWrap());
            return;
        }

        items.sort(Comparator
                .comparingLong((CalendarItem item) -> item.assignedAt)
                .thenComparing(item -> item.title, String.CASE_INSENSITIVE_ORDER));
        for (CalendarItem item : items) {
            selectedTasks.addView(createTaskCard(item), taskCardParams());
        }
    }

    private View createTaskCard(CalendarItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        int fillColor = themePreferences.cardBackground();
        int borderColor = themePreferences.cardBorder();
        int statusColor = themePreferences.futureAccent();
        if (item.status == CalendarStatus.COMPLETED
                || item.status == CalendarStatus.COMPLETED_LATE) {
            fillColor = themePreferences.completedCard();
            statusColor = themePreferences.completedText();
        } else if (item.status == CalendarStatus.OVERDUE) {
            fillColor = themePreferences.overdueCard();
            statusColor = themePreferences.overdueText();
        }
        UiStyle.applyCard(card, this, fillColor, borderColor, 14);

        TextView title = new TextView(this);
        title.setText(item.title);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(themePreferences.primaryText());
        card.addView(title, matchWrap());

        TextView status = new TextView(this);
        status.setText(statusText(item));
        status.setTextSize(14);
        status.setTextColor(statusColor);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, dp(4), 0, 0);
        card.addView(status, statusParams);

        if (item.recurrenceKnown && item.recurrence != TaskRecurrence.NONE) {
            TextView recurrence = new TextView(this);
            recurrence.setText(getString(
                    R.string.recurrence_summary,
                    recurrenceLabel(item.recurrence)));
            recurrence.setTextSize(13);
            recurrence.setTextColor(themePreferences.secondaryText());
            card.addView(recurrence, matchWrap());
        }

        if (!item.note.trim().isEmpty()) {
            TextView note = new TextView(this);
            note.setText(getString(R.string.calendar_note, item.note));
            note.setTextSize(13);
            note.setTextColor(themePreferences.secondaryText());
            NoteLinks.enableWebLinks(note, themePreferences.primaryAction());
            LinearLayout.LayoutParams noteParams = matchWrap();
            noteParams.setMargins(0, dp(6), 0, 0);
            card.addView(note, noteParams);
        }
        return card;
    }

    private String statusText(CalendarItem item) {
        String time = TaskDateFormatter.formatTime(item.assignedAt);
        switch (item.status) {
            case COMPLETED:
                return getString(R.string.calendar_task_completed, time);
            case COMPLETED_LATE:
                return getString(R.string.calendar_task_completed_late, time);
            case OVERDUE:
                return getString(R.string.calendar_task_overdue, time);
            default:
                return getString(R.string.calendar_task_scheduled, time);
        }
    }

    private String recurrenceLabel(TaskRecurrence recurrence) {
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

    private LinearLayout.LayoutParams taskCardParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private List<CalendarItem> itemsForDate(LocalDate date) {
        Map<String, CalendarItem> items = new LinkedHashMap<>();
        long now = System.currentTimeMillis();

        for (Task task : tasks) {
            if (task.getDueAt() == null || !date.equals(toLocalDate(task.getDueAt()))) {
                continue;
            }
            CalendarStatus status;
            if (task.isCompleted()) {
                status = CalendarStatus.COMPLETED;
            } else if (task.getDueAt() < now) {
                status = CalendarStatus.OVERDUE;
            } else {
                status = CalendarStatus.SCHEDULED;
            }
            CalendarItem item = new CalendarItem(
                    task.getId(),
                    task.getTitle(),
                    task.getNote(),
                    task.getDueAt(),
                    task.getRecurrence(),
                    true,
                    status);
            items.put(item.key(date), item);
        }

        for (TaskHistoryRepository.Entry entry : completedEntries) {
            long assignedAt = completedAssignedAt(entry);
            if (!date.equals(toLocalDate(assignedAt))) {
                continue;
            }
            CalendarItem item = new CalendarItem(
                    historyIdentity(entry),
                    entry.getTitle(),
                    entry.getNote(),
                    assignedAt,
                    entry.getRecurrence(),
                    entry.hasRecordedRecurrence(),
                    entry.isCompletedLate()
                            ? CalendarStatus.COMPLETED_LATE
                            : CalendarStatus.COMPLETED);
            items.putIfAbsent(item.key(date), item);
        }

        for (TaskHistoryRepository.Entry entry : overdueEntries) {
            long assignedAt = entry.getDueAt() == null
                    ? entry.getEventAt()
                    : entry.getDueAt();
            if (!date.equals(toLocalDate(assignedAt))) {
                continue;
            }
            CalendarItem item = new CalendarItem(
                    historyIdentity(entry),
                    entry.getTitle(),
                    entry.getNote(),
                    assignedAt,
                    entry.getRecurrence(),
                    entry.hasRecordedRecurrence(),
                    CalendarStatus.OVERDUE);
            items.putIfAbsent(item.key(date), item);
        }
        return new ArrayList<>(items.values());
    }

    private long completedAssignedAt(TaskHistoryRepository.Entry entry) {
        if (entry.getDueAt() != null) {
            return entry.getDueAt();
        }
        if (entry.isCompletedLate() && entry.getOriginalDueAt() != null) {
            return entry.getOriginalDueAt();
        }
        return entry.getEventAt();
    }

    private String historyIdentity(TaskHistoryRepository.Entry entry) {
        return entry.getTaskId().isEmpty() ? entry.getId() : entry.getTaskId();
    }

    private LocalDate today() {
        return Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(BUENOS_AIRES)
                .toLocalDate();
    }

    private LocalDate toLocalDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(BUENOS_AIRES).toLocalDate();
    }

    private String formatDate(LocalDate date) {
        return String.format(
                Locale.ROOT,
                "%02d/%02d/%04d",
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private enum CalendarStatus {
        SCHEDULED,
        COMPLETED,
        COMPLETED_LATE,
        OVERDUE
    }

    private static final class CalendarItem {
        private final String identity;
        private final String title;
        private final String note;
        private final long assignedAt;
        private final TaskRecurrence recurrence;
        private final boolean recurrenceKnown;
        private final CalendarStatus status;

        private CalendarItem(String identity, String title, String note, long assignedAt,
                             TaskRecurrence recurrence, boolean recurrenceKnown,
                             CalendarStatus status) {
            this.identity = identity;
            this.title = title;
            this.note = note == null ? "" : note;
            this.assignedAt = assignedAt;
            this.recurrence = recurrence;
            this.recurrenceKnown = recurrenceKnown;
            this.status = status;
        }

        private String key(LocalDate date) {
            return identity + "|" + date;
        }
    }
}
