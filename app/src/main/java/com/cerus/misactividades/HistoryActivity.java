package com.cerus.misactividades;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends Activity {
    public static final String EXTRA_CURRENT_MONTH_ONLY = "current_month_only";

    private LinearLayout screen;
    private LinearLayout historyContent;
    private AppThemePreferences themePreferences;
    private TaskHistoryRepository historyRepository;
    private final Set<String> expandedGroups = new HashSet<>();
    private List<TaskHistoryRepository.Entry> completedEntries = new ArrayList<>();
    private List<TaskHistoryRepository.Entry> overdueEntries = new ArrayList<>();
    private boolean currentMonthOnly;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themePreferences = new AppThemePreferences(this);
        if (themePreferences.isDarkMode()) {
            setTheme(R.style.Theme_MisActividades_Dark);
        }
        super.onCreate(savedInstanceState);
        historyRepository = new TaskHistoryRepository(this);
        currentMonthOnly = getIntent().getBooleanExtra(EXTRA_CURRENT_MONTH_ONLY, false);
        setContentView(createScreen());
    }

    private ScrollView createScreen() {
        int backgroundColor = new AppBackgroundPreferences(this).getColorValue(
                themePreferences.isDarkMode());
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(backgroundColor);

        screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(16), dp(16), dp(24));
        screen.setBackgroundColor(backgroundColor);
        scrollView.addView(screen, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText(R.string.back_icon);
        backButton.setTextSize(24);
        UiStyle.applyIconButton(backButton, this, themePreferences);
        backButton.setContentDescription(getString(R.string.back_to_settings));
        backButton.setOnClickListener(view -> finish());
        titleRow.addView(backButton, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText(currentMonthOnly
                ? R.string.monthly_completed_title
                : R.string.history_title);
        title.setTextSize(28);
        title.setTextColor(themePreferences.primaryText());
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(dp(10), 0, 0, 0);
        titleRow.addView(title, titleParams);
        screen.addView(titleRow, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText(currentMonthOnly
                ? R.string.monthly_completed_subtitle
                : R.string.history_subtitle);
        subtitle.setTextSize(16);
        subtitle.setTextColor(themePreferences.secondaryText());
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(12), 0, dp(14));
        screen.addView(subtitle, subtitleParams);

        EditText searchInput = new EditText(this);
        searchInput.setHint(R.string.history_search_hint);
        searchInput.setContentDescription(getString(R.string.history_search_description));
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setTextColor(themePreferences.primaryText());
        searchInput.setHintTextColor(themePreferences.secondaryText());
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        searchInput.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0);
        searchInput.setCompoundDrawablePadding(dp(10));
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        UiStyle.applyCard(searchInput, this, themePreferences.cardBackground(),
                themePreferences.cardBorder(), 12);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        searchParams.setMargins(0, 0, 0, dp(10));
        screen.addView(searchInput, searchParams);

        Button clearHistoryButton = new Button(this);
        clearHistoryButton.setText(currentMonthOnly
                ? R.string.history_clear_month_button
                : R.string.history_clear_button);
        UiStyle.applyDangerButton(clearHistoryButton, this, themePreferences);
        clearHistoryButton.setOnClickListener(view -> {
            if (currentMonthOnly) {
                confirmClearHistory(true);
            } else {
                showClearHistoryOptions();
            }
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        clearParams.setMargins(0, 0, 0, dp(20));
        screen.addView(clearHistoryButton, clearParams);

        historyContent = new LinearLayout(this);
        historyContent.setOrientation(LinearLayout.VERTICAL);
        screen.addView(historyContent, matchWrap());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                searchQuery = normalizeSearch(text.toString());
                renderHistory();
            }
        });

        loadEntries();
        renderHistory();
        return scrollView;
    }

    private void loadEntries() {
        completedEntries = historyRepository.getCompletedEntries();
        if (currentMonthOnly) {
            completedEntries.removeIf(entry -> !AppTimeZone.isCurrentMonth(entry.getEventAt()));
        }
        overdueEntries = historyRepository.getOverdueEntries();
        overdueEntries.removeIf(entry -> entry.getEventAt()
                >= AppTimeZone.startOfTodayMillis());
    }

    private void refreshHistory() {
        loadEntries();
        renderHistory();
    }

    private void renderHistory() {
        historyContent.removeAllViews();
        List<TaskHistoryRepository.Entry> visibleCompleted = filterEntries(completedEntries);
        List<TaskHistoryRepository.Entry> visibleOverdue = filterEntries(overdueEntries);

        if (currentMonthOnly) {
            addGroupedSection(
                    "monthly",
                    getString(R.string.monthly_completed_section, visibleCompleted.size()),
                    visibleCompleted,
                    R.string.history_completed_at,
                    themePreferences.completedText(),
                    false);
            return;
        }

        addGroupedSection(
                "overdue",
                getString(R.string.history_overdue_section, visibleOverdue.size()),
                visibleOverdue,
                R.string.history_overdue_at,
                themePreferences.overdueText(),
                true);
        addGroupedSection(
                "completed",
                getString(R.string.history_completed_section, visibleCompleted.size()),
                visibleCompleted,
                R.string.history_completed_at,
                themePreferences.completedText(),
                false);
    }

    private List<TaskHistoryRepository.Entry> filterEntries(
            List<TaskHistoryRepository.Entry> entries) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(entries);
        }
        List<TaskHistoryRepository.Entry> filtered = new ArrayList<>();
        for (TaskHistoryRepository.Entry entry : entries) {
            if (normalizeSearch(entry.getTitle()).contains(searchQuery)
                    || normalizeSearch(entry.getNote()).contains(searchQuery)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private String normalizeSearch(String value) {
        String normalized = Normalizer.normalize(
                value == null ? "" : value,
                Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void addGroupedSection(String sectionKey, String sectionLabel,
                                   List<TaskHistoryRepository.Entry> entries,
                                   int eventLabel, int eventColor,
                                   boolean overdueSection) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(sectionLabel);
        sectionTitle.setTextSize(20);
        sectionTitle.setTextColor(themePreferences.primaryText());
        sectionTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.setMargins(0, 0, 0, dp(8));
        historyContent.addView(sectionTitle, sectionParams);

        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(searchQuery.isEmpty()
                    ? R.string.history_empty
                    : R.string.history_search_empty);
            empty.setTextSize(15);
            empty.setTextColor(themePreferences.secondaryText());
            LinearLayout.LayoutParams emptyParams = matchWrap();
            emptyParams.setMargins(0, 0, 0, dp(24));
            historyContent.addView(empty, emptyParams);
            return;
        }

        addFrequencyGroup(sectionKey, "daily", R.string.history_frequency_daily,
                entriesForRecurrence(entries, TaskRecurrence.DAILY, true),
                eventLabel, eventColor, overdueSection);
        addFrequencyGroup(sectionKey, "weekly", R.string.history_frequency_weekly,
                entriesForRecurrence(entries, TaskRecurrence.WEEKLY, true),
                eventLabel, eventColor, overdueSection);
        addFrequencyGroup(sectionKey, "monthly", R.string.history_frequency_monthly,
                entriesForRecurrence(entries, TaskRecurrence.MONTHLY, true),
                eventLabel, eventColor, overdueSection);
        addFrequencyGroup(sectionKey, "single", R.string.history_frequency_single,
                entriesForRecurrence(entries, TaskRecurrence.NONE, true),
                eventLabel, eventColor, overdueSection);
        addFrequencyGroup(sectionKey, "unknown", R.string.history_frequency_unknown,
                entriesForRecurrence(entries, TaskRecurrence.NONE, false),
                eventLabel, eventColor, overdueSection);

        TextView spacer = new TextView(this);
        historyContent.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
    }

    private List<TaskHistoryRepository.Entry> entriesForRecurrence(
            List<TaskHistoryRepository.Entry> entries,
            TaskRecurrence recurrence,
            boolean recurrenceKnown) {
        List<TaskHistoryRepository.Entry> grouped = new ArrayList<>();
        for (TaskHistoryRepository.Entry entry : entries) {
            if (entry.hasRecordedRecurrence() == recurrenceKnown
                    && (!recurrenceKnown || entry.getRecurrence() == recurrence)) {
                grouped.add(entry);
            }
        }
        return grouped;
    }

    private void addFrequencyGroup(String sectionKey, String frequencyKey,
                                   int frequencyLabel,
                                   List<TaskHistoryRepository.Entry> entries,
                                   int eventLabel, int eventColor,
                                   boolean overdueSection) {
        if (entries.isEmpty()) {
            return;
        }

        String groupKey = sectionKey + "|" + frequencyKey;
        String label = getString(frequencyLabel);
        boolean searchActive = !searchQuery.isEmpty();
        boolean expanded = searchActive || expandedGroups.contains(groupKey);

        Button groupButton = new Button(this);
        groupButton.setText(getString(
                expanded
                        ? R.string.history_frequency_group_expanded
                        : R.string.history_frequency_group_collapsed,
                label,
                entries.size()));
        groupButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        UiStyle.applySecondaryButton(groupButton, this, themePreferences);
        groupButton.setContentDescription(getString(
                expanded
                        ? R.string.history_collapse_group_description
                        : R.string.history_expand_group_description,
                label,
                entries.size()));
        if (!searchActive) {
            groupButton.setOnClickListener(view -> {
                if (expandedGroups.contains(groupKey)) {
                    expandedGroups.remove(groupKey);
                } else {
                    expandedGroups.add(groupKey);
                }
                renderHistory();
            });
        }
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        groupParams.setMargins(0, 0, 0, dp(8));
        historyContent.addView(groupButton, groupParams);

        if (!expanded) {
            return;
        }
        for (TaskHistoryRepository.Entry entry : entries) {
            addEntryCard(entry, eventLabel, eventColor, overdueSection);
        }
    }

    private void addEntryCard(TaskHistoryRepository.Entry entry,
                              int eventLabel, int eventColor,
                              boolean overdueSection) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        UiStyle.applyCard(card, this, themePreferences.cardBackground(),
                themePreferences.cardBorder(), 14);

        TextView taskTitle = new TextView(this);
        taskTitle.setText(entry.getTitle());
        taskTitle.setTextSize(17);
        taskTitle.setTypeface(null, Typeface.BOLD);
        taskTitle.setTextColor(themePreferences.primaryText());
        card.addView(taskTitle, matchWrap());

        TextView event = new TextView(this);
        event.setText(entry.isCompletedLate()
                ? getString(
                        R.string.history_completed_late_at,
                        TaskDateFormatter.formatDateTime(entry.getEventAt()))
                : getString(
                        eventLabel,
                        TaskDateFormatter.formatDateTime(entry.getEventAt())));
        event.setTextSize(14);
        event.setTextColor(eventColor);
        LinearLayout.LayoutParams eventParams = matchWrap();
        eventParams.setMargins(0, dp(4), 0, 0);
        card.addView(event, eventParams);

        if (entry.isCompletedLate() && entry.getOriginalDueAt() != null) {
            TextView originalDue = new TextView(this);
            originalDue.setText(getString(
                    R.string.history_original_due_at,
                    TaskDateFormatter.formatDateTime(entry.getOriginalDueAt())));
            originalDue.setTextSize(13);
            originalDue.setTextColor(themePreferences.secondaryText());
            LinearLayout.LayoutParams originalDueParams = matchWrap();
            originalDueParams.setMargins(0, dp(3), 0, 0);
            card.addView(originalDue, originalDueParams);
        }

        if (overdueSection) {
            addOverdueActions(card, entry);
        } else if (entry.hasNote()) {
            card.addView(createReadOnlyNoteButton(entry), fullWidthActionParams());
        }

        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(dp(8), 0, dp(8), dp(10));
        historyContent.addView(card, cardParams);
    }

    private void addOverdueActions(LinearLayout card, TaskHistoryRepository.Entry entry) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button noteButton = new Button(this);
        noteButton.setText(entry.hasNote()
                ? R.string.note_button_has_note
                : R.string.note_button);
        UiStyle.applySecondaryButton(noteButton, this, themePreferences);
        noteButton.setContentDescription(getString(
                R.string.history_edit_note_description,
                entry.getTitle()));
        noteButton.setOnClickListener(view -> showOverdueNoteDialog(entry));
        actions.addView(noteButton, horizontalActionParams(true));

        Button completeButton = new Button(this);
        completeButton.setText(R.string.history_complete_late);
        UiStyle.applyPrimaryButton(completeButton, this, themePreferences);
        completeButton.setTextSize(14);
        completeButton.setContentDescription(getString(
                R.string.history_complete_late_description,
                entry.getTitle()));
        completeButton.setOnClickListener(view -> confirmLateCompletion(entry));
        actions.addView(completeButton, horizontalActionParams(false));

        card.addView(actions, fullWidthActionParams());
    }

    private Button createReadOnlyNoteButton(TaskHistoryRepository.Entry entry) {
        Button noteButton = new Button(this);
        noteButton.setText(R.string.history_view_note);
        UiStyle.applySecondaryButton(noteButton, this, themePreferences);
        noteButton.setContentDescription(getString(
                R.string.history_view_note_description,
                entry.getTitle()));
        noteButton.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(getString(R.string.note_dialog_title, entry.getTitle()))
                .setMessage(entry.getNote())
                .setPositiveButton(R.string.close, null)
                .show());
        return noteButton;
    }

    private void showOverdueNoteDialog(TaskHistoryRepository.Entry entry) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), 0);

        EditText noteInput = new EditText(this);
        noteInput.setText(entry.getNote());
        noteInput.setHint(R.string.note_hint);
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        noteInput.setMinLines(5);
        noteInput.setMaxLines(10);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        content.addView(noteInput, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.note_dialog_title, entry.getTitle()))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete_note, null)
                .setPositiveButton(R.string.save_note, null)
                .create();
        dialog.setOnShowListener(unused -> {
            Button deleteButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            deleteButton.setEnabled(entry.hasNote());
            deleteButton.setOnClickListener(view -> {
                historyRepository.updateOverdueNote(entry.getId(), "");
                dialog.dismiss();
                refreshHistory();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                historyRepository.updateOverdueNote(
                        entry.getId(),
                        noteInput.getText().toString().trim());
                dialog.dismiss();
                refreshHistory();
            });
        });
        dialog.show();
    }

    private void confirmLateCompletion(TaskHistoryRepository.Entry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_complete_late_title)
                .setMessage(R.string.history_complete_late_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.history_complete_late_confirm, (dialog, which) -> {
                    if (historyRepository.markOverdueAsCompleted(
                            entry.getId(),
                            System.currentTimeMillis())) {
                        Toast.makeText(
                                this,
                                R.string.history_completed_late_confirmation,
                                Toast.LENGTH_SHORT).show();
                        refreshHistory();
                    }
                })
                .show();
    }

    private void showClearHistoryOptions() {
        String[] options = {
                getString(R.string.history_clear_month_option),
                getString(R.string.history_clear_all_option)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_clear_title)
                .setItems(options, (dialog, which) -> confirmClearHistory(which == 0))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearHistory(boolean currentMonth) {
        new AlertDialog.Builder(this)
                .setTitle(currentMonth
                        ? R.string.history_clear_month_confirm_title
                        : R.string.history_clear_all_confirm_title)
                .setMessage(currentMonth
                        ? R.string.history_clear_month_confirm_message
                        : R.string.history_clear_all_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.history_clear_confirm, (dialog, which) -> {
                    int removed = currentMonth
                            ? historyRepository.clearCurrentMonthHistory()
                            : historyRepository.clearAllHistory();
                    Toast.makeText(
                            this,
                            getResources().getQuantityString(
                                    R.plurals.history_clear_done,
                                    removed,
                                    removed),
                            Toast.LENGTH_SHORT).show();
                    refreshHistory();
                })
                .show();
    }

    private LinearLayout.LayoutParams fullWidthActionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        params.setMargins(0, dp(8), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams horizontalActionParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        if (!first) {
            params.setMargins(dp(8), 0, 0, 0);
        }
        return params;
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
