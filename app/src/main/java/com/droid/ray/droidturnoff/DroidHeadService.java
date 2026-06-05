package com.droid.ray.droidturnoff;



import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class DroidHeadService extends Service {
    private static final String CHANNEL_ID = "floating_button";
    private static final int NOTIFICATION_ID = 10;

    private WindowManager windowManager;
    private ImageView chatHead;
    //private TextView txtHead;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private Context context;
    private View.OnTouchListener onTouchListener;
    public static boolean killService = false;

    public enum EnumStateButton {
        CLOSE,
        VIEW
    }

    private EnumStateButton StateButton;

    WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        Log.d(DroidCommon.TAG, "DroidHeadService - onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        killService = false;
        startAsForegroundService();
        InicializarVariavel();
        InicializarAcao();
        AtualizarPosicao();
        Log.d(DroidCommon.TAG, "DroidHeadService - onCreate");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //call widget update methods/services/broadcasts
        Log.d(DroidCommon.TAG, "onTouch - Neworientation: " + newConfig.orientation);
        //GravarPosicaoAtual();
        AtualizarPosicao();
    }

    private void Vibrar(int valor) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(valor);
        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "Vibrar: " + ex.getMessage());
        }
    }

    private void AtualizarPosicao() {
        try {
            if (DroidPreferences.GetInteger(context, "orientationActual") == 2 || getResources().getConfiguration().orientation == 2) {
                params.x = DroidPreferences.GetInteger(context, "params.y");
                params.y = DroidPreferences.GetInteger(context, "params.x");
            } else {
                params.x = DroidPreferences.GetInteger(context, "params.x");
                params.y = DroidPreferences.GetInteger(context, "params.y");
            }

            windowManager.updateViewLayout(chatHead, params);
            //windowManager.updateViewLayout(txtHead, params);

        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "InicializarVariavel: " + ex.getMessage());
        }

        Log.d(DroidCommon.TAG, "onTouch - x: " + DroidPreferences.GetInteger(context, "params.x"));
        Log.d(DroidCommon.TAG, "onTouch - y: " + DroidPreferences.GetInteger(context, "params.y"));
    }

    private void GravarPosicaoAtual() {
        try {
            DroidPreferences.SetInteger(context, "params.x", params.x);
            DroidPreferences.SetInteger(context, "params.y", params.y);
            DroidPreferences.SetInteger(context, "orientationActual", getResources().getConfiguration().orientation);
        } catch (Exception ex) {
        }
    }

    private void InicializarVariavel() {
        context = getBaseContext();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        onTouchListener = new TouchListener();
        params = createLayoutParams();

        chatHead = new ImageView(context);
        chatHead.setImageResource(R.mipmap.stoprec);
        //txtHead = new TextView(context);
        //txtHead.setTextSize(20);
        //txtHead.setText("100");

        StateButton = EnumStateButton.VIEW;
        params.gravity = Gravity.CENTER;
        if (!canDrawOverlays()) {
            Log.d(DroidCommon.TAG, "Overlay permission not granted");
            killService = true;
            stopSelf();
            return;
        }
        windowManager.addView(chatHead, params);
        //windowManager.addView(txtHead, params);

    }

    private WindowManager.LayoutParams createLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void startAsForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.foreground_service_channel),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, DroidConfigurationActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.foreground_service_title))
                .setContentText(getString(R.string.foreground_service_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.rgb(66, 133, 244));
        }
        return builder.build();
    }

    private void InicializarAcao() {
        //txtHead.setOnTouchListener(onTouchListener);

        try {
            DroidPreferences.SetInteger(context, "show", 1);
            chatHead.setOnTouchListener(onTouchListener);
        } catch (Exception ex) {
        }
    }


    public class TouchListener implements View.OnTouchListener {

        private GestureDetector gestureDetector = new GestureDetector(DroidHeadService.this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (StateButton == EnumStateButton.VIEW) {
                    chatHead.setImageResource(R.mipmap.closerec);
                    StateButton = EnumStateButton.CLOSE;
                } else {
                    chatHead.setImageResource(R.mipmap.stoprec);
                    StateButton = EnumStateButton.VIEW;
                }
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (StateButton == EnumStateButton.VIEW) {
                   DroidCommon.turnOffScreen(context);
                } else {
                    Vibrar(100);

                    try {
                        killService = true;
                        stopSelf();
                    }
                    catch (Exception ex)
                    {
                        Log.d(DroidCommon.TAG, "stopSelf: " + ex.getMessage());
                    }


                }
                return super.onSingleTapConfirmed(e);
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    Integer totalMoveX = (int) (event.getRawX() - initialTouchX);
                    params.x = initialX + totalMoveX;
                    Integer totalMoveY = (int) (event.getRawY() - initialTouchY);
                    params.y = initialY + totalMoveY;
                    windowManager.updateViewLayout(chatHead, params);
                  //  windowManager.updateViewLayout(txtHead, params);
                    GravarPosicaoAtual();
                    return true;
            }

            return true;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatHead != null && windowManager != null) {
            try {
                windowManager.removeView(chatHead);
            } catch (Exception ex) {
                Log.d(DroidCommon.TAG, "removeView: " + ex.getMessage());
            }
        }
        //if (txtHead != null) windowManager.removeView(txtHead);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        if (!killService) {
            Intent broadcastIntent = new Intent("com.droid.ray.droidturnoff.ACTION_RESTART_SERVICE");
            broadcastIntent.setPackage(getPackageName());
            sendBroadcast(broadcastIntent);
            Log.d(DroidCommon.TAG, "DroidHeadService - onDestroy");
        }
        DroidPreferences.SetInteger(context, "show", 0);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onUnbind");
        return super.onUnbind(intent);

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(DroidCommon.TAG, "DroidHeadService - onTaskRemoved");

        if (!killService) {
            Intent broadcastIntent = new Intent("com.droid.ray.droidturnoff.ACTION_RESTART_SERVICE");
            broadcastIntent.setPackage(getPackageName());
            sendBroadcast(broadcastIntent);
            Log.d(DroidCommon.TAG, "DroidHeadService - onDestroy");
        }
    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        Log.d(DroidCommon.TAG, "DroidHeadService - onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onRebind");
        super.onRebind(intent);
    }


}





