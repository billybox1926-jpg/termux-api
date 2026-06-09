package com.termux.api.apis;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.JsonWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;


public class NotificationListAPI {

    private static final String LOG_TAG = "NotificationListAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                listNotifications(context, out);
            }
        });
    }


    static void listNotifications(Context context, JsonWriter out) throws Exception {
        NotificationService notificationService = NotificationService.get();
        if (notificationService == null) {
            out.beginObject().name("error").value("Notification listener service is not connected. Please enable notification access for Termux:API in Android settings.").endObject();
            return;
        }
        StatusBarNotification[] notifications = notificationService.getActiveNotifications();
        if (notifications == null) {
            out.beginArray().endArray();
            return;
        }

        out.beginArray();
        for (StatusBarNotification n : notifications) {
            int id = n.getId();
            String key = "";
            String title = "";
            String text = "";
            CharSequence[] lines = null;
            String packageName = "";
            String tag = "";
            String group = "";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String when = dateFormat.format(new Date(n.getNotification().when));

            if (n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE) != null) {
                title = n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
            }
            if (n.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
                text = n.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
            } else if (n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT) != null) {
                text = n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
            }
            if (n.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) != null) {
                lines = n.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            }
            if (n.getTag() != null) {
                tag = n.getTag();
            }
            if (n.getNotification().getGroup() != null) {
                group = n.getNotification().getGroup();
            }
            if (n.getKey() != null) {
                key = n.getKey();
            }
            if (n.getPackageName() != null) {
                packageName = n.getPackageName();
            }
            out.beginObject()
                    .name("id").value(id)
                    .name("tag").value(tag)
                    .name("key").value(key)
                    .name("group").value(group)
                    .name("packageName").value(packageName)
                    .name("title").value(title)
                    .name("content").value(text)
                    .name("when").value(when);
            if (lines != null) {
                out.name("lines").beginArray();
                for (CharSequence line : lines) {
                    out.value(line.toString());
                }
                out.endArray();
            }
            out.endObject();
        }
        out.endArray();
    }



    public static class NotificationService extends NotificationListenerService {
        static NotificationService _this;

        public static NotificationService get() {
            return _this;
        }

        @Override
        public void onListenerConnected() {
            _this = this;
            super.onListenerConnected();
        }

        @Override
        public void onListenerDisconnected() {
            _this = null;
            super.onListenerDisconnected();
        }

        // Fix for issue #860: broadcast posted notifications so clients can listen
        @Override
        public void onNotificationPosted(StatusBarNotification sbn) {
            super.onNotificationPosted(sbn);
            broadcastNotification("posted", sbn);
        }

        // Fix for issue #860: broadcast removed notifications
        @Override
        public void onNotificationRemoved(StatusBarNotification sbn) {
            super.onNotificationRemoved(sbn);
            broadcastNotification("removed", sbn);
        }

        // Fix for issue #860: broadcast notification events via local broadcast
        private void broadcastNotification(String event, StatusBarNotification sbn) {
            try {
                Intent broadcast = new Intent("com.termux.api.notification." + event);
                broadcast.putExtra("package", sbn.getPackageName());
                broadcast.putExtra("id", sbn.getId());
                broadcast.putExtra("key", sbn.getKey());
                if (sbn.getTag() != null) broadcast.putExtra("tag", sbn.getTag());
                if (sbn.getNotification().extras != null) {
                    CharSequence title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
                    if (title != null) broadcast.putExtra("title", title.toString());
                    CharSequence text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
                    if (text != null) broadcast.putExtra("text", text.toString());
                }
                broadcast.setPackage(com.termux.shared.termux.TermuxConstants.TERMUX_API_PACKAGE_NAME);
                sendBroadcast(broadcast);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to broadcast notification event", e);
            }
        }
    }

}
