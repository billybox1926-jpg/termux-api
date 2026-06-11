package com.termux.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.termux.shared.logger.Logger;

/**
 * Narrow exported receiver that only starts KeepAliveService.
 * This allows Termux package wrappers to prewarm the API app without
 * launching an Activity (which would steal focus).
 *
 * Intent actions:
 *   com.termux.api.action.START_SERVICE - starts KeepAliveService
 *
 * This receiver does NOT execute API commands. It only ensures the
 * KeepAliveService (and its SocketListener) are running.
 */
public class StartServiceReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "StartServiceReceiver";

    public static final String ACTION_START_SERVICE = "com.termux.api.action.START_SERVICE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "Received intent: " + intent.getAction());

        Intent serviceIntent = new Intent(context, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Logger.logDebug(LOG_TAG, "KeepAliveService start requested");
    }
}
