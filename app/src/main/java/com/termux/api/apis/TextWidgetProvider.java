package com.termux.api.apis;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.termux.api.R;
import com.termux.shared.logger.Logger;

/**
 * Simple text widget provider for homescreen.
 */
public class TextWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = "TextWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_text);
            views.setTextViewText(R.id.widget_text, "Termux:API");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Logger.logInfo(LOG_TAG, "Widget enabled");
    }

    @Override
    public void onDisabled(Context context) {
        Logger.logInfo(LOG_TAG, "Widget disabled");
    }
}
