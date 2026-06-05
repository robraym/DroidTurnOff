package com.droid.ray.droidturnoff;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Robson on 31/07/2017.
 */

public class DroidShowDeviceAdmin {

    public static void Show(Context context) {

        ComponentName mDeviceAdminSample = new ComponentName(context, DroidScreenOffAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);

        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                mDeviceAdminSample);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_description));
        context.startActivity(intent);
    }

    public static boolean EnabledAdmin(Context context) {
        DevicePolicyManager policyManager = (DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(context,
                DroidScreenOffAdminReceiver.class);
        return policyManager.isAdminActive(adminReceiver);
    }

    public static boolean EnabledAdminAndLock(Context context) {
        DevicePolicyManager policyManager = (DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(context,
                DroidScreenOffAdminReceiver.class);
        boolean admin = policyManager.isAdminActive(adminReceiver);
        if (admin) {
            policyManager.lockNow();
        }
        //else Show(context);
        return admin;
    }
}
