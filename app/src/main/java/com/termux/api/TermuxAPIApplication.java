package com.termux.api;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences;


public class TermuxAPIApplication extends Application {

    public static final String LOG_TAG = "TermuxAPIApplication";

    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "AppInit");

        Context context = getApplicationContext();

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(context);

        ResultReturner.setContext(this);

        // Set log config for the app
        setLogConfig(context, true);

        // Start the KeepAliveService to create the SocketListener.
        // The service owns the socket lifecycle; Application is kept minimal.
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    public static void setLogConfig(Context context, boolean commitToFile) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_API_APP_NAME.replaceAll("[: ]", ""));

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAPIAppSharedPreferences preferences = TermuxAPIAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel(true), commitToFile);
    }

}
