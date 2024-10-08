package com.cmder.floatingtimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class FloatingTimerService extends Service {

    private static final String CHANNEL_ID = "FloatingTimerServiceChannel";
    private WindowManager windowManager;
    private View floatingView;
    private TextView timerTextView;
    private Handler handler = new Handler();
    private long startTime;

    private int lastAction;
    private float initialTouchX, initialTouchY;
    private int initialX, initialY;

    private Handler longPressHandler = new Handler();
    private boolean isLongPress = false;
    private static final int LONG_PRESS_DURATION = 500; // Long press duration in ms

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        removeFloatingView();
        addFloatingView();
        startTimer();
        startForeground(1, getNotification());
        return START_STICKY;
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Timer")
                .setContentText("Floating timer is running")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating Timer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void addFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_timer, null);
        timerTextView = floatingView.findViewById(R.id.timerTextView);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastAction = event.getAction();

                        longPressHandler.postDelayed(() -> {
                            isLongPress = true;
                            Intent intent = new Intent(FloatingTimerService.this, SettingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }, LONG_PRESS_DURATION);

                        return true;
                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacksAndMessages(null);
                        if (!isLongPress) {
                            // Handle click if necessary
                        }
                        isLongPress = false;
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - initialTouchX) > 10 || Math.abs(event.getRawY() - initialTouchY) > 10) {
                            longPressHandler.removeCallbacksAndMessages(null);
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        lastAction = event.getAction();
                        return true;
                }
                return false;
            }
        });
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        handler.post(updateTimerRunnable);
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int minutes = (int) (elapsedTime / (1000 * 60));
            if (minutes >= 60) {
                int hours = minutes / 60;
                minutes = minutes % 60;
                timerTextView.setText(hours + "小时" + minutes + "分钟");
            } else if (minutes < 1){
                timerTextView.setText(elapsedTime / 1000 + "秒");
            } else {
                timerTextView.setText(minutes + "分钟");
            }
            if (minutes < 1) {
                handler.postDelayed(this, 10000);
            } else {
                handler.postDelayed(this, 60000);
            }
        }
    };

    private void removeFloatingView() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        handler.removeCallbacks(updateTimerRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatingView();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
