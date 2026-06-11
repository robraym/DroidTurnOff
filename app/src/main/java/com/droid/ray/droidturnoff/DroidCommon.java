package com.droid.ray.droidturnoff;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Robson on 20/08/2017.
 */



public class DroidCommon {
    public static String TAG = "DroidTurnOff";

    public static boolean AtivarBotaoFlutuante(final Context context) {
        boolean spf = false;
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            spf = sp.getBoolean("spf_botaoFlutuante", false);
        } catch (Exception ex) {
            Log.d(TAG, "AtivarBotaoFlutuante - " + ex.getMessage());
        }
        return spf;
    }

    public static void turnOffScreen(final Context context) {
        // turn off screen
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (DroidAccessibilityService.lockScreen()) {
                    return;
                }
                Intent intent = new Intent(context, DroidConfigurationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }

            if (!DroidShowDeviceAdmin.EnabledAdminAndLock(context)) {
                Intent intent = new Intent(context, DroidConfigurationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

        } catch (Exception ex) {
            Toast.makeText(context, R.string.device_admin_not_enabled,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "turnOffScreen - Erro: " + ex.getMessage());

        }
    }

    public static void stopStartService(Context context, boolean start)
    {
        Intent intentService = new Intent(context, DroidHeadService.class);

        try {
            if (!start) {
                context.stopService(intentService);
                DroidHeadService.killService = true;
                Log.d(TAG, "stopService");
            }

        } catch (Exception ex) {
            Log.d(TAG, "stopService - Erro: " + ex.getMessage());
        }
        try {
            if (start) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intentService);
                } else {
                    context.startService(intentService);
                }
                Log.d(TAG, "startService");
            }
        } catch (Exception ex) {
            Log.d(TAG, "starService - Erro: " + ex.getMessage());
        }
    }

}
