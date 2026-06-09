package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

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
