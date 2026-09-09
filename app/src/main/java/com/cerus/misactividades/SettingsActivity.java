package com.cerus.misactividades;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private ScrollView scrollView;
    private LinearLayout screen;
    private Button widgetRangeButton;
    private Button themeButton;
    private Button backgroundColorButton;
    private WidgetPreferences widgetPreferences;
    private AppBackgroundPreferences backgroundPreferences;
    private AppThemePreferences themePreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themePreferences = new AppThemePreferences(this);
        if (themePreferences.isDarkMode()) {
            setTheme(R.style.Theme_MisActividades_Dark);
        }
        super.onCreate(savedInstanceState);
        widgetPreferences = new WidgetPreferences(this);
        backgroundPreferences = new AppBackgroundPreferences(this);
        setContentView(createScreen());
    }

    private ScrollView createScreen() {
        scrollView = new ScrollView(this);
        int screenColor = backgroundPreferences.getColorValue(themePreferences.isDarkMode());
        scrollView.setBackgroundColor(screenColor);

        screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(16), dp(16), dp(24));
        screen.setBackgroundColor(screenColor);
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
        backButton.setContentDescription(getString(R.string.back_to_tasks));
        backButton.setOnClickListener(view -> finish());
        titleRow.addView(backButton, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        title.setTextSize(28);
        title.setTextColor(themePreferences.primaryText());
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(dp(10), 0, 0, 0);
        titleRow.addView(title, titleParams);
        screen.addView(titleRow, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.settings_subtitle);
        subtitle.setTextSize(16);
        subtitle.setTextColor(themePreferences.secondaryText());
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(12), 0, dp(24));
        screen.addView(subtitle, subtitleParams);

        addSectionTitle(R.string.settings_widget_section);
        widgetRangeButton = new Button(this);
        UiStyle.applySecondaryButton(widgetRangeButton, this, themePreferences);
        widgetRangeButton.setTextSize(16);
        widgetRangeButton.setOnClickListener(view -> showWidgetRangeDialog());
        updateWidgetRangeButton();
        LinearLayout.LayoutParams widgetParams = settingButtonParams(24);
        screen.addView(widgetRangeButton, widgetParams);

        addSectionTitle(R.string.settings_appearance_section);
        themeButton = new Button(this);
        UiStyle.applySecondaryButton(themeButton, this, themePreferences);
        themeButton.setTextSize(16);
        themeButton.setOnClickListener(view -> showThemeDialog());
        updateThemeButton();
        LinearLayout.LayoutParams themeParams = settingButtonParams(8);
        screen.addView(themeButton, themeParams);

        backgroundColorButton = new Button(this);
        UiStyle.applySecondaryButton(backgroundColorButton, this, themePreferences);
        backgroundColorButton.setTextSize(16);
        backgroundColorButton.setOnClickListener(view -> showBackgroundColorDialog());
        updateBackgroundColorButton();
        LinearLayout.LayoutParams backgroundParams = settingButtonParams(24);
        screen.addView(backgroundColorButton, backgroundParams);

        addSectionTitle(R.string.settings_history_section);
        Button historyButton = new Button(this);
        UiStyle.applySecondaryButton(historyButton, this, themePreferences);
        historyButton.setTextSize(16);
        historyButton.setText(R.string.history_button);
        historyButton.setOnClickListener(view -> startActivity(
                new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams historyParams = settingButtonParams(8);
        screen.addView(historyButton, historyParams);

        Button monthlyCompletedButton = new Button(this);
        UiStyle.applySecondaryButton(monthlyCompletedButton, this, themePreferences);
        monthlyCompletedButton.setTextSize(16);
        monthlyCompletedButton.setText(R.string.monthly_completed_button);
        monthlyCompletedButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra(HistoryActivity.EXTRA_CURRENT_MONTH_ONLY, true);
            startActivity(intent);
        });
        LinearLayout.LayoutParams monthlyCompletedParams = settingButtonParams(24);
        screen.addView(monthlyCompletedButton, monthlyCompletedParams);

        addSectionTitle(R.string.settings_info_section);
        Button infoButton = new Button(this);
        UiStyle.applySecondaryButton(infoButton, this, themePreferences);
        infoButton.setTextSize(16);
        infoButton.setText(R.string.info_button);
        infoButton.setOnClickListener(view -> showAppInformation());
        LinearLayout.LayoutParams infoParams = settingButtonParams(0);
        screen.addView(infoButton, infoParams);

        return scrollView;
    }

    private void showAppInformation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.info_dialog_title)
                .setMessage(getString(R.string.info_dialog_message, getAppVersion()))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException exception) {
            return "—";
        }
    }

    private void addSectionTitle(int textResource) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(textResource);
        sectionTitle.setTextSize(18);
        sectionTitle.setTextColor(themePreferences.primaryText());
        sectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(dp(2), dp(2), 0, 0);
        screen.addView(sectionTitle, params);
    }

    private void showWidgetRangeDialog() {
        CharSequence[] options = {
                getString(R.string.widget_range_today_option),
                getString(R.string.widget_range_five_days_option)
        };
        int selectedOption = widgetPreferences.getRangeDays() == WidgetPreferences.RANGE_FIVE_DAYS
                ? 1
                : 0;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.widget_range_dialog_title)
                .setSingleChoiceItems(options, selectedOption, null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int checkedItem = dialog.getListView().getCheckedItemPosition();
                    widgetPreferences.setRangeDays(checkedItem == 1
                            ? WidgetPreferences.RANGE_FIVE_DAYS
                            : WidgetPreferences.RANGE_TODAY);
                    updateWidgetRangeButton();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void updateWidgetRangeButton() {
        widgetRangeButton.setText(widgetPreferences.getRangeDays()
                == WidgetPreferences.RANGE_FIVE_DAYS
                ? R.string.widget_range_five_days_button
                : R.string.widget_range_today_button);
    }

    private void showThemeDialog() {
        CharSequence[] options = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(options, themePreferences.isDarkMode() ? 1 : 0, null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    boolean darkMode = dialog.getListView().getCheckedItemPosition() == 1;
                    themePreferences.setDarkMode(darkMode);
                    TasksWidgetProvider.updateAllWidgets(this);
                    dialog.dismiss();
                    recreate();
                }));
        dialog.show();
    }

    private void updateThemeButton() {
        themeButton.setText(getString(
                R.string.theme_button,
                getString(themePreferences.isDarkMode()
                        ? R.string.theme_dark
                        : R.string.theme_light)));
    }

    private void showBackgroundColorDialog() {
        boolean darkMode = themePreferences.isDarkMode();
        int[] labels = backgroundLabels(darkMode);
        CharSequence[] options = new CharSequence[labels.length];
        for (int index = 0; index < labels.length; index++) {
            options[index] = getString(labels[index]);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.background_dialog_title)
                .setSingleChoiceItems(
                        options,
                        backgroundPreferences.getSelectedColor(darkMode),
                        null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    backgroundPreferences.setSelectedColor(
                            dialog.getListView().getCheckedItemPosition(),
                            darkMode);
                    int screenColor = backgroundPreferences.getColorValue(darkMode);
                    screen.setBackgroundColor(screenColor);
                    scrollView.setBackgroundColor(screenColor);
                    TasksWidgetProvider.updateAllWidgets(this);
                    updateBackgroundColorButton();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void updateBackgroundColorButton() {
        boolean darkMode = themePreferences.isDarkMode();
        int[] labels = backgroundLabels(darkMode);
        backgroundColorButton.setText(getString(
                darkMode ? R.string.background_button_dark : R.string.background_button,
                getString(labels[backgroundPreferences.getSelectedColor(darkMode)])));
    }

    private int[] backgroundLabels(boolean darkMode) {
        return darkMode
                ? new int[]{
                        R.string.background_dark_lavender,
                        R.string.background_dark_blue,
                        R.string.background_dark_mint,
                        R.string.background_dark_sand,
                        R.string.background_dark_gray
                }
                : new int[]{
                        R.string.background_lavender,
                        R.string.background_light_blue,
                        R.string.background_mint,
                        R.string.background_sand,
                        R.string.background_light_gray
                };
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams settingButtonParams(int bottomMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        params.setMargins(0, dp(8), 0, dp(bottomMarginDp));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
