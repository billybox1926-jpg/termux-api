package com.termux.api.apis;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for homescreen widget text updates.
 * Fix for issue #394.
 * Creates and updates a simple text widget on the homescreen.
 */
public class WidgetAPI {

    private static final String LOG_TAG = "WidgetAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "update":
                updateWidget(context, intent);
                break;
            case "info":
                widgetInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void updateWidget(Context context, Intent intent) {
        final String text = intent.getStringExtra("text");
        final int widgetId = intent.getIntExtra("widget_id", -1);

        Intent i = new Intent(context, WidgetConfigActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        context.startActivity(i);
    }

    private static void widgetInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                AppWidgetManager awm = AppWidgetManager.getInstance(context);
                ComponentName provider = new ComponentName(context, TextWidgetProvider.class);
                int[] ids = awm.getAppWidgetIds(provider);
                out.beginObject();
                out.name("widget_count").value(ids.length);
                out.name("widget_ids");
                out.beginArray();
                for (int id : ids) {
                    out.value(id);
                }
                out.endArray();
                out.endObject();
            } catch (Exception e) {
                out.beginObject().name("error").value(e.getMessage()).endObject();
            }
        });
    }

    /**
     * Configuration activity for the widget.
     */
    public static class WidgetConfigActivity extends AppCompatActivity {
        private boolean done = false;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            done = true;

            String text = getIntent().getStringExtra("text");
            if (text == null) text = "Termux:API";

            int widgetId = getIntent().getIntExtra("widget_id", AppWidgetManager.INVALID_APPWIDGET_ID);

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_text);
                views.setTextViewText(R.id.widget_text, text);
                AppWidgetManager.getInstance(this).updateAppWidget(widgetId, views);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                setResult(RESULT_OK, resultValue);
            }

            ResultReturner.returnData(this, getIntent(), out ->
                    out.println("Widget updated: " + text));
            finishAndRemoveTask();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (!done) {
                done = true;
                try {
                    ResultReturner.returnData(this, getIntent(), out -> out.println(""));
                } catch (Exception ignored) {}
            }
        }
    }
}
