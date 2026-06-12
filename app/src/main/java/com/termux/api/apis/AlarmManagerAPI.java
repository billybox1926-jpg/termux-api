package com.termux.api.apis;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for setting and canceling alarms via AlarmManager.
 * Fix for issue #305.
 */
public class AlarmManagerAPI {

    private static final String LOG_TAG = "AlarmManagerAPI";
    private static final String ALARM_ACTION = "com.termux.api.ALARM_TRIGGERED";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "set":
                setAlarm(apiReceiver, context, intent);
                break;
            case "cancel":
                cancelAlarm(apiReceiver, context, intent);
                break;
            case "list":
                listAlarms(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out -> out.println("Unknown action: " + action));
        }
    }

    /**
     * Set an alarm.
     * Required extras:
     *   - type: "elapsed" | "rtc" (default: "elapsed")
     *   - delay_ms: delay in milliseconds from now (default: 0 = immediate)
     *   - message: optional message to include in the alarm intent
     *   - id: optional unique ID for this alarm (default: 0)
     */
    private static void setAlarm(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am == null) {
                    out.println("ERROR: AlarmManager not available");
                    return;
                }

                String type = intent.getStringExtra("type");
                long delayMs = intent.getLongExtra("delay_ms", 0);
                String message = intent.getStringExtra("message");
                int id = intent.getIntExtra("id", 0);

                long triggerTime;
                int alarmType;
                if ("rtc".equals(type)) {
                    alarmType = AlarmManager.RTC_WAKEUP;
                    triggerTime = System.currentTimeMillis() + delayMs;
                } else {
                    alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
                    triggerTime = SystemClock.elapsedRealtime() + delayMs;
                }

                Intent alarmIntent = new Intent(ALARM_ACTION);
                alarmIntent.putExtra("message", message != null ? message : "");
                alarmIntent.putExtra("id", id);

                PendingIntent pi = PendingIntent.getBroadcast(context, id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(alarmType, triggerTime, pi);
                } else {
                    am.setExact(alarmType, triggerTime, pi);
                }

                out.println("Alarm set: id=" + id + ", trigger_in_ms=" + delayMs);
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Cancel an alarm by ID.
     * Required extra: id (the alarm ID to cancel)
     */
    private static void cancelAlarm(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am == null) {
                    out.println("ERROR: AlarmManager not available");
                    return;
                }

                int id = intent.getIntExtra("id", 0);
                Intent alarmIntent = new Intent(ALARM_ACTION);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                am.cancel(pi);
                out.println("Alarm cancelled: id=" + id);
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * List active alarms (returns info about known alarm IDs).
     * Note: Android doesn't provide a direct API to enumerate active alarms,
     * so this returns a status message.
     */
    private static void listAlarms(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("note").value("Android does not provide an API to enumerate active PendingIntents. Use unique IDs to manage alarms.");
                out.name("alarm_action").value(ALARM_ACTION);
                out.endObject();
            }
        });
    }

    /**
     * BroadcastReceiver that handles alarm triggers.
     * Register in the manifest to receive alarm broadcasts.
     */
    public static class AlarmReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "AlarmReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.logInfo(LOG_TAG, "Alarm triggered: id=" + intent.getIntExtra("id", -1)
                    + ", message=" + intent.getStringExtra("message"));
            // The alarm has been triggered. The app can handle this by
            // showing a notification or performing other actions.
        }
    }
}
