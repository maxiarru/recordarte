package com.cerus.misactividades;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;

public final class UiStyle {
    private UiStyle() {
    }

    public static void applyCard(View view, Context context, int fillColor,
                                 int borderColor, int radiusDp) {
        view.setBackground(rounded(context, fillColor, borderColor, 1, radiusDp));
        view.setElevation(dp(context, 1));
    }

    public static void applyPrimaryButton(Button button, Context context,
                                          AppThemePreferences theme) {
        applyButton(button, context, theme.primaryAction(), theme.primaryActionText(),
                theme.primaryAction(), 12);
        button.setTextSize(16);
    }

    public static void applySecondaryButton(Button button, Context context,
                                            AppThemePreferences theme) {
        applyButton(button, context, theme.secondaryAction(), theme.secondaryActionText(),
                theme.cardBorder(), 10);
        button.setTextSize(14);
    }

    public static void applyDangerButton(Button button, Context context,
                                         AppThemePreferences theme) {
        applyButton(button, context, theme.dangerAction(), theme.dangerActionText(),
                theme.dangerBorder(), 10);
        button.setTextSize(14);
    }

    public static void applyIconButton(Button button, Context context,
                                       AppThemePreferences theme) {
        applyButton(button, context, theme.secondaryAction(), theme.primaryText(),
                theme.cardBorder(), 14);
        button.setPadding(0, 0, 0, 0);
    }

    private static void applyButton(Button button, Context context, int fillColor,
                                    int textColor, int borderColor, int radiusDp) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        Drawable content = rounded(context, fillColor, borderColor, 1, radiusDp);
        int rippleColor = Color.argb(45, Color.red(textColor),
                Color.green(textColor), Color.blue(textColor));
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(rippleColor), content, null));
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(context, 10), 0, dp(context, 10), 0);
    }

    public static Drawable rounded(Context context, int fillColor, int borderColor,
                                   int borderWidthDp, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(context, radiusDp));
        if (borderWidthDp > 0) {
            background.setStroke(dp(context, borderWidthDp), borderColor);
        }
        return background;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
