package com.termux.api;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

public class KeepAliveService extends Service {

    private static final String LOG_TAG = "KeepAliveService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "termux-keep-alive";

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

        // Start as foreground service to prevent Android from killing us
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

        // Create the socket listener if not already running.
        // This is needed because the KeepAliveService may be started from
        // TermuxApiReceiver.onReceive() and the Application.onCreate() may
        // not have run yet (e.g. process started just for the broadcast).
        SocketListener.createSocketListener(getApplication());

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
