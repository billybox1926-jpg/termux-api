package com.termux.api.apis;

import static com.termux.api.util.JsonUtils.*;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.BuildConfig;
import com.termux.api.KeepAliveService;
import com.termux.api.SocketListener;
import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

public class ApiStatusAPI {

    private static final String LOG_TAG = "ApiStatusAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                Context appContext = context.getApplicationContext();
                PackageManager pm = appContext.getPackageManager();

                // Package identity — use BuildConfig for debug suffix awareness
                String packageName = appContext.getPackageName();
                String applicationId = BuildConfig.APPLICATION_ID;

                // Socket address the listener is actually bound to
                String socketAddress = SocketListener.LISTEN_ADDRESS;

                // Process alive — we are inside the app process, so yes
                boolean processAlive = true;

                // Socket listener state — check via KeepAliveService if possible
                // KeepAliveService is a separate component; we can't directly query it
                // from here without a binder, so we report expected state based on
                // whether the application has started (onCreate launches the service).
                // A more precise check would require IPC, but this is a status API,
                // not a health monitor.
                boolean socketListenerExpected = true;

                // Termux host package info
                String termuxHostPackage = TermuxConstants.TERMUX_PACKAGE_NAME;
                String termuxHostVersion = getPackageVersion(pm, termuxHostPackage);

                // App version
                String versionName = getAppVersion(appContext);

                // Receiver component that handles API broadcasts
                String receiverComponent = packageName + "/.TermuxApiReceiver";

                out.beginObject();
                out.name("package").value(packageName);
                out.name("application_id").value(applicationId);
                out.name("socket_address").value(socketAddress);
                out.name("process_alive").value(processAlive);
                out.name("socket_listener_expected").value(socketListenerExpected);
                out.name("termux_host_package").value(termuxHostPackage);
                if (termuxHostVersion != null) {
                    out.name("termux_host_version").value(termuxHostVersion);
                }
                out.name("version").value(versionName);
                out.name("receiver_component").value(receiverComponent);
                out.name("debug_build").value(BuildConfig.DEBUG);
                out.endObject();
            }
        });
    }

    private static String getAppVersion(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    private static String getPackageVersion(PackageManager pm, String packageName) {
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
