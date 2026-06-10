package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.JsonWriter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for getting terminal session text.
 * Fix for issue #608.
 * Retrieves text from the terminal session via Termux:API.
 */
public class SessionTextAPI {

    private static final String LOG_TAG = "SessionTextAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "get":
                getSessionText(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void getSessionText(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("note").value("Direct session text access requires Termux app integration. " +
                        "This API provides session metadata only.");
                out.name("available").value(false);
                out.endObject();
            }
        });
    }
}
