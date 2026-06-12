package com.termux.api.apis;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.List;

/**
 * API for listing and launching installed applications.
 * Fix for issue #380.
 */
public class AppManagerAPI {

    private static final String LOG_TAG = "AppManagerAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listApps(apiReceiver, context, intent);
                break;
            case "launch":
                launchApp(apiReceiver, context, intent);
                break;
            case "info":
                appInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out -> out.println("Unknown action: " + action));
        }
    }

    /**
     * List installed applications.
     * Optional extras:
     *   - type: "system" | "user" | "all" (default: "all")
     *   - json: "true" for JSON output (default: "true")
     */
    private static void listApps(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String type = intent.getStringExtra("type");
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                PackageManager pm = context.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

                String filterType = type != null ? type : "all";
                out.beginArray();
                for (ApplicationInfo app : apps) {
                    boolean isSystem = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    switch (filterType) {
                        case "system":
                            if (!isSystem) continue;
                            break;
                        case "user":
                            if (isSystem) continue;
                            break;
                        default:
                            break;
                    }
                    out.beginObject();
                    out.name("package_name").value(app.packageName);
                    out.name("app_name").value(pm.getApplicationLabel(app).toString());
                    out.name("is_system").value(isSystem);
                    out.name("enabled").value(app.enabled);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        out.name("min_sdk").value(app.minSdkVersion);
                    }
                    out.name("target_sdk").value(app.targetSdkVersion);
                    out.name("uid").value(app.uid);
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    /**
     * Launch an application by package name.
     * Required extra: package (the package name to launch)
     */
    private static void launchApp(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String packageName = intent.getStringExtra("package");
        if (packageName == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Missing 'package' extra"));
            return;
        }
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                PackageManager pm = context.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {
                    out.println("ERROR: No launch intent found for package: " + packageName);
                    return;
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                out.println("Launched: " + packageName);
            } catch (Exception e) {
                out.println("ERROR: Failed to launch " + packageName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Get detailed info about a specific app.
     * Required extra: package (the package name)
     */
    private static void appInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String packageName = intent.getStringExtra("package");
        if (packageName == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Missing 'package' extra"));
            return;
        }
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo app = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                out.beginObject();
                out.name("package_name").value(app.packageName);
                out.name("app_name").value(pm.getApplicationLabel(app).toString());
                out.name("source_dir").value(app.sourceDir);
                out.name("data_dir").value(app.dataDir);
                out.name("is_system").value((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                out.name("enabled").value(app.enabled);
                out.name("uid").value(app.uid);
                out.name("target_sdk").value(app.targetSdkVersion);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    out.name("min_sdk").value(app.minSdkVersion);
                }
                // Get launch intent categories
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    out.name("launch_activity").value(launchIntent.getComponent() != null ?
                            launchIntent.getComponent().getClassName() : "unknown");
                }
                out.endObject();
            }
        });
    }
}
