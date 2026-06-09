package com.termux.api.apis;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * API for Android Calendar content provider CRUD operations.
 * Actions: "add", "list", "delete"
 * Extras for "add": title, start_ms, end_ms, location, description, calendar_id, all_day
 * Extras for "list": limit, calendar_id
 * Extras for "delete": event_id
 */
public class CalendarAPI {

    private static final String LOG_TAG = "CalendarAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        String action = intent.getStringExtra("action");
        if (action == null) action = "list";

        switch (action) {
            case "add":
                addEvent(apiReceiver, context, intent);
                break;
            case "list":
                listEvents(apiReceiver, context, intent);
                break;
            case "delete":
                deleteEvent(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("Error: Unknown action '" + action + "'. Use 'add', 'list', or 'delete'."));
        }
    }

    // ── list calendars helper ──────────────────────────────

    static List<CalendarInfo> getCalendars(Context context) {
        List<CalendarInfo> calendars = new ArrayList<>();
        try (Cursor c = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.CALENDAR_COLOR,
                        CalendarContract.Calendars.VISIBLE
                },
                null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    CalendarInfo info = new CalendarInfo();
                    info.id = c.getLong(0);
                    info.displayName = c.getString(1);
                    info.accountName = c.getString(2);
                    info.color = c.getInt(3);
                    info.visible = c.getInt(4) != 0;
                    calendars.add(info);
                }
            }
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to query calendars", e);
        }
        return calendars;
    }

    static CalendarInfo getDefaultCalendar(Context context) {
        List<CalendarInfo> calendars = getCalendars(context);
        // Prefer the first visible calendar
        for (CalendarInfo cal : calendars) {
            if (cal.visible) return cal;
        }
        return calendars.isEmpty() ? null : calendars.get(0);
    }

    // ── add ────────────────────────────────────────────────

    private static void addEvent(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            String title = intent.getStringExtra("title");
            if (title == null || title.isEmpty()) {
                out.println("Error: Missing 'title' extra");
                return;
            }

            long startMs = intent.getLongExtra("start_ms", 0);
            long endMs = intent.getLongExtra("end_ms", 0);
            if (startMs <= 0) {
                out.println("Error: Missing or invalid 'start_ms' extra (unix epoch milliseconds)");
                return;
            }
            if (endMs <= 0) endMs = startMs + 3600000; // default 1 hour

            long calendarId = intent.getLongExtra("calendar_id", -1);
            if (calendarId < 0) {
                CalendarInfo defaultCal = getDefaultCalendar(context);
                if (defaultCal == null) {
                    out.println("Error: No calendar found. Create a calendar first.");
                    return;
                }
                calendarId = defaultCal.id;
            }

            String location = intent.getStringExtra("location");
            String description = intent.getStringExtra("description");
            boolean allDay = intent.getBooleanExtra("all_day", false);

            ContentValues event = new ContentValues();
            event.put(CalendarContract.Events.DTSTART, startMs);
            event.put(CalendarContract.Events.DTEND, endMs);
            event.put(CalendarContract.Events.TITLE, title);
            event.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            event.put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().getID());
            if (location != null) event.put(CalendarContract.Events.EVENT_LOCATION, location);
            if (description != null) event.put(CalendarContract.Events.DESCRIPTION, description);
            event.put(CalendarContract.Events.ALL_DAY, allDay ? 1 : 0);

            Uri uri = context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, event);
            if (uri != null) {
                long eventId = ContentUris.parseId(uri);
                out.println("Event created: id=" + eventId);
                out.println("Calendar: " + calendarId);
                out.println("Title: " + title);
                out.println("Start: " + startMs);
                out.println("End: " + endMs);
            } else {
                out.println("Error: Failed to create event");
            }
        });
    }

    // ── list ───────────────────────────────────────────────

    private static void listEvents(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                int limit = intent.getIntExtra("limit", 50);
                long filterCalendarId = intent.getLongExtra("calendar_id", -1);

                StringBuilder selection = new StringBuilder();
                String[] selectionArgs = null;
                if (filterCalendarId >= 0) {
                    selection.append(CalendarContract.Events.CALENDAR_ID).append(" = ?");
                    selectionArgs = new String[]{String.valueOf(filterCalendarId)};
                }

                ContentResolver cr = context.getContentResolver();
                try (Cursor c = cr.query(
                        CalendarContract.Events.CONTENT_URI,
                        new String[]{
                                CalendarContract.Events._ID,
                                CalendarContract.Events.TITLE,
                                CalendarContract.Events.DTSTART,
                                CalendarContract.Events.DTEND,
                                CalendarContract.Events.EVENT_LOCATION,
                                CalendarContract.Events.DESCRIPTION,
                                CalendarContract.Events.CALENDAR_ID,
                                CalendarContract.Events.ALL_DAY,
                                CalendarContract.Events.CALENDAR_DISPLAY_NAME
                        },
                        selection.length() > 0 ? selection.toString() : null,
                        selectionArgs,
                        CalendarContract.Events.DTSTART + " ASC LIMIT " + limit)) {

                    out.beginArray();
                    if (c != null) {
                        int idIdx = c.getColumnIndexOrThrow(CalendarContract.Events._ID);
                        int titleIdx = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE);
                        int startIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART);
                        int endIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTEND);
                        int locIdx = c.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION);
                        int descIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION);
                        int calIdIdx = c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID);
                        int allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY);
                        int calNameIdx = c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME);

                        while (c.moveToNext()) {
                            out.beginObject();
                            out.name("id").value(c.getLong(idIdx));
                            out.name("title").value(c.getString(titleIdx));
                            out.name("start_ms").value(c.getLong(startIdx));
                            out.name("end_ms").value(c.getLong(endIdx));
                            String loc = c.getString(locIdx);
                            if (loc != null) out.name("location").value(loc);
                            String desc = c.getString(descIdx);
                            if (desc != null) out.name("description").value(desc);
                            out.name("calendar_id").value(c.getLong(calIdIdx));
                            out.name("calendar_name").value(c.getString(calNameIdx));
                            out.name("all_day").value(c.getInt(allDayIdx) != 0);
                            out.endObject();
                        }
                    }
                    out.endArray();
                }
            }
        });
    }

    // ── delete ─────────────────────────────────────────────

    private static void deleteEvent(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            long eventId = intent.getLongExtra("event_id", -1);
            if (eventId < 0) {
                out.println("Error: Missing 'event_id' extra");
                return;
            }

            int deleted = context.getContentResolver().delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                    null, null);

            if (deleted > 0) {
                out.println("Event deleted: id=" + eventId);
            } else {
                out.println("Error: Event not found or delete failed (id=" + eventId + ")");
            }
        });
    }

    // ── list calendars (bonus action) ──────────────────────

    public static void listCalendars(TermuxApiReceiver apiReceiver, Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                List<CalendarInfo> calendars = getCalendars(context);
                out.beginArray();
                for (CalendarInfo cal : calendars) {
                    out.beginObject();
                    out.name("id").value(cal.id);
                    out.name("display_name").value(cal.displayName);
                    out.name("account_name").value(cal.accountName);
                    out.name("color").value(cal.color);
                    out.name("visible").value(cal.visible);
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    // ── data class ─────────────────────────────────────────

    static class CalendarInfo {
        long id;
        String displayName;
        String accountName;
        int color;
        boolean visible;
    }
}
