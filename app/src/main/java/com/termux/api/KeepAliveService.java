package com.termux.api;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

public class KeepAliveService extends Service {

    private static final String LOG_TAG = "KeepAliveService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "termux-keep-alive";

    private SocketListener socketListener;

    public class LocalBinder extends Binder {
        public KeepAliveService getService() {
            return KeepAliveService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Termux Keep Alive", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("Keeps Termux:API running in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Start as foreground service to prevent Android from killing the process.
        // A foreground service gives the process the highest app-side importance
        // without requiring a visible activity.
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Termux:API")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Termux:API")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
        }
        startForeground(NOTIFICATION_ID, notification);

        // Create and start the socket listener on a background thread.
        // The listener is owned by this service, so it lives as long as the
        // foreground service keeps the process alive.
        ensureSocketListenerStarted();

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public synchronized void ensureSocketListenerStarted() {
        if (socketListener != null && socketListener.isRunning()) return;
        socketListener = new SocketListener(getApplication());
        socketListener.start();
        Logger.logDebug(LOG_TAG, "SocketListener started");
    }

    public boolean isSocketListenerRunning() {
        return socketListener != null && socketListener.isRunning();
    }

    @Override
    public void onDestroy() {
        if (socketListener != null) {
            socketListener.stop();
            socketListener = null;
        }
        super.onDestroy();
    }
}
