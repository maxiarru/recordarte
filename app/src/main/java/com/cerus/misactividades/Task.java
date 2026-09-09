package com.cerus.misactividades;

import java.util.UUID;

public class Task {
    public static final long COMPLETION_CORRECTION_WINDOW_MILLIS = 5L * 60L * 1000L;

    private final String id;
    private String title;
    private String note;
    private boolean completed;
    private Long dueAt;
    private Long completedAt;
    private TaskRecurrence recurrence;
    private boolean nextOccurrenceGenerated;

    public Task(String title) {
        this(title, null);
    }

    public Task(String title, Long dueAt) {
        this(title, dueAt, TaskRecurrence.NONE);
    }

    public Task(String title, Long dueAt, TaskRecurrence recurrence) {
        this(UUID.randomUUID().toString(), title, "", false, dueAt, null, recurrence, false);
    }

    public Task(String id, String title, boolean completed, Long dueAt, Long completedAt,
                TaskRecurrence recurrence, boolean nextOccurrenceGenerated) {
        this(id, title, "", completed, dueAt, completedAt, recurrence,
                nextOccurrenceGenerated);
    }

    public Task(String id, String title, String note, boolean completed, Long dueAt,
                Long completedAt, TaskRecurrence recurrence,
                boolean nextOccurrenceGenerated) {
        this.id = id;
        this.title = title;
        this.note = note == null ? "" : note;
        this.completed = completed;
        this.dueAt = dueAt;
        this.completedAt = completedAt;
        this.recurrence = recurrence == null ? TaskRecurrence.NONE : recurrence;
        this.nextOccurrenceGenerated = nextOccurrenceGenerated;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note;
    }

    public boolean hasNote() {
        return !note.trim().isEmpty();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        if (completed && !this.completed) {
            completedAt = System.currentTimeMillis();
        } else if (!completed) {
            completedAt = null;
        }
        this.completed = completed;
    }

    public Long getDueAt() {
        return dueAt;
    }

    public void setDueAt(Long dueAt) {
        this.dueAt = dueAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public boolean canCorrectCompletion(long now) {
        return completed
                && completedAt != null
                && now < completedAt + COMPLETION_CORRECTION_WINDOW_MILLIS;
    }

    public TaskRecurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(TaskRecurrence recurrence) {
        this.recurrence = recurrence == null ? TaskRecurrence.NONE : recurrence;
    }

    public boolean isNextOccurrenceGenerated() {
        return nextOccurrenceGenerated;
    }

    public void setNextOccurrenceGenerated(boolean nextOccurrenceGenerated) {
        this.nextOccurrenceGenerated = nextOccurrenceGenerated;
    }
}
