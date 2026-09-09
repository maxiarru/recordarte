package com.cerus.misactividades;

import java.util.Calendar;
import java.util.List;

public final class TaskActions {
    private TaskActions() {
    }

    public static boolean setCompleted(List<Task> tasks, Task task, boolean completed) {
        task.setCompleted(completed);
        return false;
    }

    public static int compareForDisplay(Task first, Task second) {
        if (first.isCompleted() != second.isCompleted()) {
            return first.isCompleted() ? 1 : -1;
        }

        Long firstDueAt = first.getDueAt();
        Long secondDueAt = second.getDueAt();
        if (firstDueAt == null && secondDueAt != null) {
            return 1;
        }
        if (firstDueAt != null && secondDueAt == null) {
            return -1;
        }
        if (firstDueAt != null) {
            int dueComparison = Long.compare(firstDueAt, secondDueAt);
            if (dueComparison != 0) {
                return dueComparison;
            }
        }

        return String.CASE_INSENSITIVE_ORDER.compare(first.getTitle(), second.getTitle());
    }

    public static Long nextOccurrenceOnOrAfter(long dueAt, TaskRecurrence recurrence,
                                               long earliestOccurrenceAt) {
        if (recurrence == TaskRecurrence.NONE) {
            return null;
        }
        Calendar next = AppTimeZone.calendar();
        next.setTimeInMillis(dueAt);
        do {
            addRecurrence(next, recurrence);
        } while (next.getTimeInMillis() < earliestOccurrenceAt);
        return next.getTimeInMillis();
    }

    public static boolean isGeneratedOccurrenceWaitingForParentRemoval(
            List<Task> tasks, Task candidate) {
        if (candidate.isCompleted() || candidate.getDueAt() == null) {
            return false;
        }
        for (Task parent : tasks) {
            if (!parent.isCompleted()
                    || !parent.isNextOccurrenceGenerated()
                    || parent.getDueAt() == null
                    || parent.getRecurrence() == TaskRecurrence.NONE
                    || parent.getRecurrence() != candidate.getRecurrence()
                    || !parent.getTitle().equals(candidate.getTitle())) {
                continue;
            }

            Calendar generated = AppTimeZone.calendar();
            generated.setTimeInMillis(parent.getDueAt());
            addRecurrence(generated, parent.getRecurrence());
            if (generated.getTimeInMillis() == candidate.getDueAt()) {
                return true;
            }
        }
        return false;
    }

    private static void addRecurrence(Calendar next, TaskRecurrence recurrence) {
        switch (recurrence) {
            case DAILY:
                next.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case WEEKLY:
                next.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case MONTHLY:
                next.add(Calendar.MONTH, 1);
                break;
            default:
                break;
        }
    }
}
