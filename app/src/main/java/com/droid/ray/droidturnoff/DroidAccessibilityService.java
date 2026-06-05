package com.droid.ray.droidturnoff;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class DroidAccessibilityService extends AccessibilityService {
    private static DroidAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(DroidCommon.TAG, "DroidAccessibilityService - onServiceConnected");
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public static boolean lockScreen() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && instance != null
                && instance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    public static boolean isEnabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }

        ComponentName serviceName = new ComponentName(context, DroidAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabledService = ComponentName.unflattenFromString(splitter.next());
            if (serviceName.equals(enabledService)) {
                return true;
            }
        }
        return false;
    }
}
