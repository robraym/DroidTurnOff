package com.droid.ray.droidturnoff;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DroidConfigurationActivity extends Activity {
    private static final String PREF_FLOATING_BUTTON = "spf_botaoFlutuante";
    private static final String PREF_LEGACY_SHORTCUT_REQUESTED = "legacy_shortcut_requested";
    private static final String PREF_PENDING_OVERLAY_REQUEST = "pending_overlay_request";
    private static final String SHORTCUT_ID_TURN_OFF = "turn_off_screen";
    private static final int REQUEST_POST_NOTIFICATIONS = 1201;

    private static final int COLOR_BACKGROUND = Color.rgb(0, 0, 0);
    private static final int COLOR_GROUP = Color.rgb(28, 29, 33);
    private static final int COLOR_GROUP_PRESSED = Color.rgb(34, 38, 50);
    private static final int COLOR_PRIMARY_TEXT = Color.rgb(248, 248, 250);
    private static final int COLOR_SECONDARY_TEXT = Color.rgb(180, 182, 190);
    private static final int COLOR_BLUE = Color.rgb(66, 133, 244);
    private static final int COLOR_BLUE_TRACK = Color.rgb(25, 78, 163);
    private static final int COLOR_DIALOG = Color.rgb(31, 32, 36);
    private static final int COLOR_ICON_FLOATING = Color.rgb(66, 133, 244);
    private static final int COLOR_ICON_ACCESSIBILITY = Color.rgb(28, 177, 116);

    private Context context;
    private SharedPreferences preferences;
    private Switch floatingSwitch;
    private TextView floatingSummary;
    private TextView accessibilitySummary;
    private boolean accessibilityGateVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);

        buildSettingsScreen();

        if (!shouldUseAccessibilityLock() && !DroidShowDeviceAdmin.EnabledAdmin(this)) {
            showDeviceAdminDialog();
        }

        Log.d(DroidCommon.TAG, "onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean shouldShowAccessibilityGate = shouldShowAccessibilityGate();
        if (accessibilityGateVisible != shouldShowAccessibilityGate) {
            buildSettingsScreen();
        }

        if (shouldShowAccessibilityGate) {
            DroidCommon.stopStartService(context, false);
            refreshAccessibilitySummary();
            return;
        }

        handlePendingOverlayRequest();
        refreshFloatingSummary();
        refreshAccessibilitySummary();

        if (DroidCommon.AtivarBotaoFlutuante(context) && canDrawOverlays()) {
            DroidCommon.stopStartService(context, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean floatingEnabled = DroidCommon.AtivarBotaoFlutuante(context);
        DroidCommon.stopStartService(context, floatingEnabled && canDrawOverlays());
        Log.d(DroidCommon.TAG, "onDestroy");
    }

    private void buildSettingsScreen() {
        floatingSwitch = null;
        floatingSummary = null;
        accessibilitySummary = null;
        accessibilityGateVisible = shouldShowAccessibilityGate();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(34), dp(18), dp(30));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(R.string.configuracoes_titulo);
        title.setTextColor(COLOR_PRIMARY_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.START);
        title.setPadding(dp(34), 0, 0, dp(6));
        setTextSize(title, 33);
        content.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.configuracoes_subtitulo);
        subtitle.setTextColor(COLOR_SECONDARY_TEXT);
        subtitle.setPadding(dp(34), 0, dp(18), dp(22));
        subtitle.setLineSpacing(dp(1), 1.0f);
        setTextSize(subtitle, 15);
        content.addView(subtitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView section = createSection(getString(R.string.txt_tela_Config));
        content.addView(section);

        if (shouldUseAccessibilityLock()) {
            content.addView(createAccessibilityGroup());
            if (accessibilityGateVisible) {
                setContentView(scrollView);
                refreshAccessibilitySummary();
                return;
            }
        }

        LinearLayout group = createGroup();
        floatingSwitch = createSwitch();
        floatingSwitch.setChecked(DroidCommon.AtivarBotaoFlutuante(context));
        floatingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyFloatingButtonPreference(isChecked);
            }
        });

        View row = createSwitchRow(
                getString(R.string.txt_titulo),
                "",
                R.drawable.ic_oneui_floating_button,
                COLOR_ICON_FLOATING,
                floatingSwitch);
        floatingSummary = (TextView) row.findViewWithTag("summary");
        group.addView(row);
        content.addView(group);

        setContentView(scrollView);
        refreshFloatingSummary();
        refreshAccessibilitySummary();
    }

    private LinearLayout createAccessibilityGroup() {
        LinearLayout lockGroup = createGroup();
        View accessibilityRow = createNavigationRow(
                getString(R.string.accessibility_permission_title),
                "",
                R.drawable.ic_oneui_floating_button,
                COLOR_ICON_ACCESSIBILITY,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openAccessibilitySettings();
                    }
                });
        accessibilitySummary = (TextView) accessibilityRow.findViewWithTag("summary");
        lockGroup.addView(accessibilityRow);
        return lockGroup;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
    }

    private void applyFloatingButtonPreference(boolean enabled) {
        if (enabled) {
            if (canDrawOverlays()) {
                preferences.edit()
                        .putBoolean(PREF_FLOATING_BUTTON, true)
                        .putBoolean(PREF_PENDING_OVERLAY_REQUEST, false)
                        .apply();
                createRemoveShortcut(true);
                requestNotificationPermissionIfNeeded();
                DroidCommon.stopStartService(context, true);
            } else {
                preferences.edit()
                        .putBoolean(PREF_FLOATING_BUTTON, false)
                        .putBoolean(PREF_PENDING_OVERLAY_REQUEST, true)
                        .apply();
                setFloatingSwitchChecked(false);
                DroidCommon.stopStartService(context, false);
                openOverlaySettings();
                refreshFloatingSummary();
                return;
            }
        } else {
            preferences.edit()
                    .putBoolean(PREF_FLOATING_BUTTON, false)
                    .putBoolean(PREF_PENDING_OVERLAY_REQUEST, false)
                    .apply();
            DroidCommon.stopStartService(context, false);
            createRemoveShortcut(false);
        }

        refreshFloatingSummary();
    }

    private void handlePendingOverlayRequest() {
        if (!preferences.getBoolean(PREF_PENDING_OVERLAY_REQUEST, false)) {
            return;
        }

        boolean granted = canDrawOverlays();
        preferences.edit()
                .putBoolean(PREF_FLOATING_BUTTON, granted)
                .putBoolean(PREF_PENDING_OVERLAY_REQUEST, false)
                .apply();
        setFloatingSwitchChecked(granted);

        if (granted) {
            createRemoveShortcut(true);
            requestNotificationPermissionIfNeeded();
            DroidCommon.stopStartService(context, true);
        } else {
            DroidCommon.stopStartService(context, false);
        }
    }

    private void setFloatingSwitchChecked(boolean checked) {
        if (floatingSwitch == null) {
            return;
        }

        floatingSwitch.setOnCheckedChangeListener(null);
        floatingSwitch.setChecked(checked);
        floatingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyFloatingButtonPreference(isChecked);
            }
        });
    }

    private void createRemoveShortcut(boolean remove) {
        Intent shortcutIntent = new Intent(getApplicationContext(), DroidTurnOffActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        if (!remove && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                if (isPinnedShortcutCreated(shortcutManager)) {
                    return;
                }

                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID_TURN_OFF)
                        .setShortLabel("Desligar")
                        .setLongLabel("Desligar a tela")
                        .setIcon(Icon.createWithResource(this, R.mipmap.button))
                        .setIntent(shortcutIntent)
                        .build();
                shortcutManager.requestPinShortcut(shortcut, null);
                Toast.makeText(this, R.string.shortcut_request_sent, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!remove && preferences.getBoolean(PREF_LEGACY_SHORTCUT_REQUESTED, false)) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Desligar");
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.button));
        intent.setAction(remove
                ? "com.android.launcher.action.UNINSTALL_SHORTCUT"
                : "com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(intent);

        if (!remove) {
            preferences.edit().putBoolean(PREF_LEGACY_SHORTCUT_REQUESTED, true).apply();
        }
    }

    private boolean isPinnedShortcutCreated(ShortcutManager shortcutManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }

        for (ShortcutInfo shortcut : shortcutManager.getPinnedShortcuts()) {
            if (SHORTCUT_ID_TURN_OFF.equals(shortcut.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private boolean shouldUseAccessibilityLock() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    private boolean shouldShowAccessibilityGate() {
        return shouldUseAccessibilityLock() && !DroidAccessibilityService.isEnabled(this);
    }

    private void showDeviceAdmin() {
        try {
            DroidShowDeviceAdmin.Show(this);
        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "ShowDeviceAdmin - Erro: " + ex.getMessage());
        }
    }

    private void showDeviceAdminDialog() {
        final Dialog dialog = new Dialog(this);
        LinearLayout container = createDialogContainer();
        container.addView(createDialogTitle(getString(R.string.app_name)));

        TextView message = createDialogMessage(getString(R.string.messageAdmin));
        message.setPadding(0, dp(8), 0, dp(18));
        container.addView(message);

        LinearLayout actions = createDialogActions();
        TextView cancel = createDialogButton(getString(R.string.cancel));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();
            }
        });
        actions.addView(cancel);

        TextView ok = createDialogButton(getString(R.string.ok));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showDeviceAdmin();
            }
        });
        actions.addView(ok);

        container.addView(actions);
        showStyledDialog(dialog, container);
    }

    private void showOverlayPermissionDialog() {
        final Dialog dialog = new Dialog(this);
        LinearLayout container = createDialogContainer();
        container.addView(createDialogTitle(getString(R.string.overlay_permission_title)));

        TextView message = createDialogMessage(getString(R.string.overlay_permission_message));
        message.setPadding(0, dp(8), 0, dp(18));
        container.addView(message);

        LinearLayout actions = createDialogActions();
        TextView cancel = createDialogButton(getString(R.string.cancel));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        actions.addView(cancel);

        TextView settings = createDialogButton(getString(R.string.open_settings));
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openOverlaySettings();
            }
        });
        actions.addView(settings);

        container.addView(actions);
        showStyledDialog(dialog, container);
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void refreshFloatingSummary() {
        if (floatingSummary == null) {
            return;
        }

        boolean enabled = DroidCommon.AtivarBotaoFlutuante(context);
        String state = getString(enabled ? R.string.floating_button_enabled : R.string.floating_button_disabled);
        floatingSummary.setText(state + " - " + getString(R.string.spf_usar_botao_flutuante));
    }

    private void refreshAccessibilitySummary() {
        if (accessibilitySummary == null) {
            return;
        }

        boolean enabled = DroidAccessibilityService.isEnabled(this);
        accessibilitySummary.setText(enabled
                ? getString(R.string.accessibility_enabled)
                : getString(R.string.accessibility_disabled));
    }

    private TextView createSection(String title) {
        TextView section = new TextView(this);
        section.setText(title);
        section.setTextColor(COLOR_BLUE);
        section.setTypeface(Typeface.DEFAULT);
        section.setAllCaps(false);
        section.setPadding(dp(4), dp(18), 0, dp(10));
        setTextSize(section, 15);
        return section;
    }

    private LinearLayout createGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(createRoundedBackground(COLOR_GROUP, 32));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(14), 0, 0);
        group.setLayoutParams(params);
        return group;
    }

    private View createSwitchRow(String title, String summary, int iconResId, int iconColor, final Switch rowSwitch) {
        LinearLayout row = createBaseRow();
        addRowIcon(row, iconResId, iconColor);

        LinearLayout texts = createTextsWithValue(title, summary);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(rowSwitch);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rowSwitch.setChecked(!rowSwitch.isChecked());
            }
        });
        return row;
    }

    private View createNavigationRow(String title, String summary, int iconResId, int iconColor, View.OnClickListener listener) {
        LinearLayout row = createBaseRow();
        addRowIcon(row, iconResId, iconColor);

        LinearLayout texts = createTextsWithValue(title, summary);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.rgb(176, 179, 188));
        arrow.setGravity(Gravity.CENTER);
        setTextSize(arrow, 22);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT));

        row.setOnClickListener(listener);
        return row;
    }

    private LinearLayout createBaseRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(78));
        row.setPadding(dp(12), dp(14), dp(18), dp(14));
        row.setBackground(createPressedBackground());
        return row;
    }

    private void addRowIcon(LinearLayout row, int iconResId, int iconColor) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        icon.setBackground(createCircleBackground(iconColor));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
        params.setMargins(0, 0, dp(22), 0);
        row.addView(icon, params);
    }

    private LinearLayout createTextsWithValue(String title, String summary) {
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(14), 0);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(COLOR_PRIMARY_TEXT);
        titleView.setTypeface(Typeface.DEFAULT);
        titleView.setSingleLine(false);
        titleView.setLineSpacing(dp(1), 1.0f);
        setTextSize(titleView, 18);
        texts.addView(titleView);

        TextView summaryView = new TextView(this);
        summaryView.setTag("summary");
        summaryView.setText(summary);
        summaryView.setTextColor(COLOR_SECONDARY_TEXT);
        summaryView.setSingleLine(false);
        summaryView.setMaxLines(3);
        summaryView.setEllipsize(TextUtils.TruncateAt.END);
        summaryView.setPadding(0, dp(3), 0, 0);
        summaryView.setLineSpacing(dp(1), 1.0f);
        setTextSize(summaryView, 15);
        texts.addView(summaryView);

        return texts;
    }

    private Switch createSwitch() {
        Switch itemSwitch = new Switch(this);
        styleSwitch(itemSwitch);
        return itemSwitch;
    }

    private void styleSwitch(Switch itemSwitch) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_enabled},
                new int[]{}
        };

        itemSwitch.setThumbTintList(new ColorStateList(states, new int[]{
                COLOR_BLUE,
                Color.rgb(120, 123, 132),
                Color.rgb(232, 234, 237)
        }));
        itemSwitch.setTrackTintList(new ColorStateList(states, new int[]{
                COLOR_BLUE_TRACK,
                Color.rgb(50, 52, 58),
                Color.rgb(88, 91, 99)
        }));
    }

    private LinearLayout createDialogContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(22), dp(24), dp(16));
        container.setBackground(createRoundedBackground(COLOR_DIALOG, 28));
        return container;
    }

    private TextView createDialogTitle(String title) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(COLOR_PRIMARY_TEXT);
        titleView.setTypeface(Typeface.DEFAULT);
        titleView.setLineSpacing(dp(1), 1.0f);
        setTextSize(titleView, 22);
        return titleView;
    }

    private TextView createDialogMessage(String message) {
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextColor(COLOR_SECONDARY_TEXT);
        messageView.setLineSpacing(dp(1), 1.0f);
        setTextSize(messageView, 15);
        return messageView;
    }

    private LinearLayout createDialogActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(8), 0, 0);
        return actions;
    }

    private TextView createDialogButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(COLOR_BLUE);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(true);
        setTextSize(button, 15);
        button.setPadding(dp(18), dp(10), dp(4), dp(10));
        return button;
    }

    private void showStyledDialog(Dialog dialog, View contentView) {
        dialog.setContentView(contentView);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(shownWindow.getAttributes());
            params.width = getResources().getDisplayMetrics().widthPixels - dp(56);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.62f;
            shownWindow.setAttributes(params);
            shownWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private GradientDrawable createRoundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable createCircleBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private StateListDrawable createPressedBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, createRoundedBackground(COLOR_GROUP_PRESSED, 32));
        drawable.addState(new int[]{}, createRoundedBackground(COLOR_GROUP, 32));
        return drawable;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }

    private void setTextSize(TextView textView, int sp) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }
}
