package com.cerus.misactividades;

import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Comparator;

public final class NoteLinks {
    private NoteLinks() {
    }

    public static void enableWebLinks(TextView noteView, int linkColor) {
        noteView.setLinkTextColor(linkColor);
        Linkify.addLinks(noteView, Linkify.WEB_URLS);
        noteView.setLinksClickable(true);
        noteView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void bindEditorPreview(EditText editor, TextView preview, int linkColor) {
        preview.setLinkTextColor(linkColor);
        preview.setLinksClickable(true);
        preview.setMovementMethod(LinkMovementMethod.getInstance());
        updateEditorPreview(preview, editor.getText());
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                updateEditorPreview(preview, text);
            }
        });
    }

    private static void updateEditorPreview(TextView preview, CharSequence note) {
        SpannableString linkedNote = new SpannableString(note == null ? "" : note);
        Linkify.addLinks(linkedNote, Linkify.WEB_URLS);
        URLSpan[] links = linkedNote.getSpans(0, linkedNote.length(), URLSpan.class);
        if (links.length == 0) {
            preview.setText("");
            preview.setVisibility(View.GONE);
            return;
        }

        Arrays.sort(links, Comparator.comparingInt(linkedNote::getSpanStart));
        SpannableStringBuilder summary = new SpannableStringBuilder(
                preview.getContext().getString(R.string.note_links_title));
        for (URLSpan link : links) {
            int sourceStart = linkedNote.getSpanStart(link);
            int sourceEnd = linkedNote.getSpanEnd(link);
            int targetStart = summary.length();
            summary.append('\n');
            targetStart++;
            summary.append(linkedNote, sourceStart, sourceEnd);
            summary.setSpan(
                    new URLSpan(link.getURL()),
                    targetStart,
                    summary.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        preview.setText(summary);
        preview.setVisibility(View.VISIBLE);
    }
}
