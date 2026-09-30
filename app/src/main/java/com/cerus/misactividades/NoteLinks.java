package com.cerus.misactividades;

import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

public final class NoteLinks {
    private NoteLinks() {
    }

    public static void enableWebLinks(TextView noteView, int linkColor) {
        noteView.setLinkTextColor(linkColor);
        Linkify.addLinks(noteView, Linkify.WEB_URLS);
        noteView.setLinksClickable(true);
        noteView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
