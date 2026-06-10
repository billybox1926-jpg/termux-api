package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.List;

/**
 * API for IME (Input Method) switching.
 * Fix for issue #284.
 * Lists and switches input methods.
 */
public class ImeSwitcherAPI {

    private static final String LOG_TAG = "ImeSwitcherAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listImes(apiReceiver, context, intent);
                break;
            case "switch":
                switchIme(apiReceiver, context, intent);
                break;
            case "current":
                currentIme(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void listImes(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) {
                    out.beginObject().name("error").value("InputMethodManager not available").endObject();
                    return;
                }

                List<InputMethodInfo> methods = imm.getEnabledInputMethodList();
                out.beginArray();
                for (InputMethodInfo info : methods) {
                    out.beginObject();
                    out.name("id").value(info.getId());
                    out.name("package").value(info.getPackageName());
                    out.name("service").value(info.getServiceName());
                    out.name("settings_activity").value(info.getSettingsActivity());
                    out.name("is_default").value(info.getId().equals(
                            android.provider.Settings.Secure.getString(
                                    context.getContentResolver(),
                                    android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)));
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    private static void switchIme(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String imeId = intent.getStringExtra("ime_id");
        if (imeId == null || imeId.isEmpty()) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'ime_id' extra"));
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) {
                    out.println("ERROR: InputMethodManager not available");
                    return;
                }
                // Note: Direct IME switching requires WRITE_SECURE_SETTINGS permission
                // or the app to be a system app. We attempt it and report the result.
                boolean success = false;
                try {
                    imm.setInputMethod(null, imeId);
                    success = true;
                } catch (SecurityException e) {
                    // Fallback: open IME settings
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                    out.println("Direct IME switch requires WRITE_SECURE_SETTINGS. Opened IME settings instead.");
                    return;
                }
                if (success) {
                    out.println("Switched to IME: " + imeId);
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    private static void currentIme(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                String current = android.provider.Settings.Secure.getString(
                        context.getContentResolver(),
                        android.provider.Settings.Secure.DEFAULT_INPUT_METHOD);
                out.beginObject();
                out.name("current_ime").value(current != null ? current : "none");
                out.endObject();
            }
        });
    }
}
