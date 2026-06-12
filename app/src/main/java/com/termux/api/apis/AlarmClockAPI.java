package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for creating and managing system alarm clock entries.
 * Fix for issue #390.
 * Uses the AlarmClock content provider (android.permission.SET_ALARM).
 */
public class AlarmClockAPI {

    private static final String LOG_TAG = "AlarmClockAPI";

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
            case "dismiss":
                dismissAlarm(apiReceiver, context, intent);
                break;
            case "show":
                showAlarms(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out -> out.println("Unknown action: " + action));
        }
    }

    /**
     * Set an alarm in the system alarm clock app.
     * Required extras:
     *   - hour: int (0-23)
     *   - minutes: int (0-59)
     *   - message: optional label for the alarm
     *   - days: optional int[] array of Calendar.DAY_OF_WEEK values for repeating
     */
    private static void setAlarm(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                int hour = intent.getIntExtra("hour", -1);
                int minutes = intent.getIntExtra("minutes", -1);
                String message = intent.getStringExtra("message");

                if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
                    out.println("ERROR: Invalid hour/minutes. Hour must be 0-23, minutes 0-59.");
                    return;
                }

                Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour);
                alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
                if (message != null) {
                    alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, message);
                }

                // Optional: days of week for repeating
                int[] days = intent.getIntArrayExtra("days");
                if (days != null && days.length > 0) {
                    alarmIntent.putExtra(AlarmClock.EXTRA_DAYS, days);
                }

                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (alarmIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(alarmIntent);
                    out.println("Alarm set: " + hour + ":" + String.format("%02d", minutes));
                } else {
                    out.println("ERROR: No alarm clock app found on this device");
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Dismiss the currently firing alarm.
     */
    private static void dismissAlarm(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                Intent dismissIntent = new Intent(AlarmClock.ACTION_DISMISS_ALARM);
                dismissIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (dismissIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(dismissIntent);
                    out.println("Alarm dismissed");
                } else {
                    out.println("ERROR: No alarm clock app found");
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Open the system alarm clock UI to show all alarms.
     */
    private static void showAlarms(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                Intent showIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (showIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(showIntent);
                    out.println("Showing alarms");
                } else {
                    out.println("ERROR: No alarm clock app found");
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }
}
