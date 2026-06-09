package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.view.WindowManager;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.PrintWriter;

public class SettingAPI {

    private static final String LOG_TAG = "SettingAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Error: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "get":
                getSetting(apiReceiver, context, intent);
                break;
            case "put":
                putSetting(apiReceiver, context, intent);
                break;
            // Fix for issue #425: dark mode detection
            case "dark_mode":
                getDarkMode(apiReceiver, context, intent);
                break;
            // Fix for issue #595: display info API
            case "display_info":
                getDisplayInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out -> out.println("Error: Unknown action: " + action));
        }
    }

    private static void getSetting(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            String namespace = intent.getStringExtra("namespace");
            String key = intent.getStringExtra("key");

            if (key == null || key.isEmpty()) {
                out.println("Error: Missing 'key' extra");
                return;
            }

            if (namespace == null || namespace.isEmpty()) {
                namespace = "system";
            }

            String value = null;
            try {
                switch (namespace.toLowerCase()) {
                    case "system":
                        value = Settings.System.getString(context.getContentResolver(), key);
                        break;
                    case "secure":
                        value = Settings.Secure.getString(context.getContentResolver(), key);
                        break;
                    case "global":
                        value = Settings.Global.getString(context.getContentResolver(), key);
                        break;
                    default:
                        out.println("Error: Unknown namespace '" + namespace + "'. Use: system, secure, global");
                        return;
                }
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get setting", e);
                out.println("Error: " + e.getMessage());
                return;
            }

            if (value != null) {
                out.println(value);
            } else {
                out.println("");
            }
        });
    }

    // Fix for issue #425: detect dark mode
    private static void getDarkMode(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                boolean isDarkMode = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ has system dark mode
                    int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                } else {
                    // Pre-Android 10: check if night mode is set via system settings
                    int nightMode = Settings.System.getInt(context.getContentResolver(), "ui_night_mode", 0);
                    isDarkMode = nightMode != 0;
                }
                out.name("dark_mode").value(isDarkMode);
                out.endObject();
            }
        });
    }

    // Fix for issue #595: display info (screen size, density, etc.)
    private static void getDisplayInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics metrics = new DisplayMetrics();
                if (wm != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ use WindowMetrics
                        android.view.WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
                        android.graphics.Rect bounds = windowMetrics.getBounds();
                        out.name("width_pixels").value(bounds.width());
                        out.name("height_pixels").value(bounds.height());
                        // Get density from window metrics config
                        Configuration config = context.getResources().getConfiguration();
                        out.name("density_dpi").value(config.densityDpi);
                        out.name("density").value(context.getResources().getDisplayMetrics().density);
                    } else {
                        // Pre-Android 11 use DisplayMetrics
                        wm.getDefaultDisplay().getMetrics(metrics);
                        out.name("width_pixels").value(metrics.widthPixels);
                        out.name("height_pixels").value(metrics.heightPixels);
                        out.name("density_dpi").value(metrics.densityDpi);
                        out.name("density").value(metrics.density);
                        out.name("scaled_density").value(metrics.scaledDensity);
                        out.name("xdpi").value(metrics.xdpi);
                        out.name("ydpi").value(metrics.ydpi);
                    }
                }
                out.endObject();
            }
        });
    }

    private static void putSetting(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            String namespace = intent.getStringExtra("namespace");
            String key = intent.getStringExtra("key");
            String value = intent.getStringExtra("value");

            if (key == null || key.isEmpty()) {
                out.println("Error: Missing 'key' extra");
                return;
            }
            if (value == null) {
                out.println("Error: Missing 'value' extra");
                return;
            }

            if (namespace == null || namespace.isEmpty()) {
                namespace = "system";
            }

            try {
                switch (namespace.toLowerCase()) {
                    case "system":
                        Settings.System.putString(context.getContentResolver(), key, value);
                        break;
                    case "secure":
                        Settings.Secure.putString(context.getContentResolver(), key, value);
                        break;
                    case "global":
                        Settings.Global.putString(context.getContentResolver(), key, value);
                        break;
                    default:
                        out.println("Error: Unknown namespace '" + namespace + "'. Use: system, secure, global");
                        return;
                }
                out.println("Set " + namespace + "/" + key + " = " + value);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to put setting", e);
                out.println("Error: " + e.getMessage());
            }
        });
    }
}
