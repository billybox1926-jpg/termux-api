package com.termux.api.apis;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonWriter;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * API for Accessibility service integration.
 * Fix for issue #828.
 * Provides screen content reading and basic accessibility actions.
 */
public class AccessibilityAPI {

    private static final String LOG_TAG = "AccessibilityAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "info":
                accessibilityInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void accessibilityInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("note").value("Accessibility service must be enabled in Android settings. " +
                        "Use 'termux-accessibility-enable' to open settings.");
                out.endObject();
            }
        });
    }

    /**
     * AccessibilityService implementation for Termux.
     * Handles screen content queries and accessibility actions.
     */
    public static class TermuxAccessibilityService extends AccessibilityService {

        private static final String LOG_TAG = "TermuxAccessibilityService";
        private static TermuxAccessibilityService instance;

        @Override
        protected void onServiceConnected() {
            super.onServiceConnected();
            instance = this;
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.notificationTimeout = 100;
            setServiceInfo(info);
            Logger.logInfo(LOG_TAG, "Accessibility service connected");
        }

        @Override
        public void onAccessibilityEvent(AccessibilityEvent event) {
            // Events are handled passively; queries are on-demand via the API
            Logger.logDebug(LOG_TAG, "Accessibility event: " + event.getEventType());
        }

        @Override
        public void onInterrupt() {
            Logger.logInfo(LOG_TAG, "Accessibility service interrupted");
        }

        @Override
        public void onDestroy() {
            instance = null;
            super.onDestroy();
        }

        /**
         * Get the current root node of the active window.
         */
        public static AccessibilityNodeInfo getRootNode() {
            if (instance == null) return null;
            return instance.getRootInActiveWindow();
        }

        /**
         * Find nodes by text content.
         */
        public static List<AccessibilityNodeInfo> findNodesByText(String text) {
            AccessibilityNodeInfo root = getRootNode();
            if (root == null) return new ArrayList<>();
            return root.findAccessibilityNodeInfosByText(text);
        }

        /**
         * Perform a global action (back, home, recents, etc.).
         */
        public static boolean performGlobalActionStatic(int action) {
            if (instance == null) return false;
            return instance.performGlobalAction(action);
        }
    }
}
