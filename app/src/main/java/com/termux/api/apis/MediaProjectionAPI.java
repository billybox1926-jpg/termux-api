package com.termux.api.apis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.JsonWriter;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for MediaProjection (screen capture) and input control.
 * Fix for issue #816.
 * Requires Android 5.0+ (API 21) for MediaProjection.
 */
public class MediaProjectionAPI {

    private static final String LOG_TAG = "MediaProjectionAPI";
    private static final int REQUEST_CODE = 9001;

    private static MediaProjection activeProjection;
    private static VirtualDisplay activeDisplay;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: MediaProjection requires Android 5.0+"));
            return;
        }

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "request":
                requestProjection(context, intent);
                break;
            case "status":
                projectionStatus(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    /**
     * Launch the system screen-capture consent dialog.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void requestProjection(Context context, Intent intent) {
        Intent i = new Intent(context, ProjectionActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        context.startActivity(i);
    }

    /**
     * Check current projection status.
     */
    private static void projectionStatus(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("active").value(activeProjection != null);
                out.name("api_level").value(Build.VERSION.SDK_INT);
                out.endObject();
            }
        });
    }

    /**
     * Activity that handles the MediaProjection permission dialog.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class ProjectionActivity extends AppCompatActivity {
        private boolean done = false;
        private MediaProjectionManager projectionManager;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (projectionManager == null) {
                done = true;
                ResultReturner.returnData(this, getIntent(), out ->
                        out.println("ERROR: MediaProjectionManager not available"));
                finishAndRemoveTask();
                return;
            }
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            done = true;
            if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
                try {
                    activeProjection = projectionManager.getMediaProjection(resultCode, data);
                    ResultReturner.returnData(this, getIntent(), out ->
                            out.println("MediaProjection granted"));
                } catch (Exception e) {
                    ResultReturner.returnData(this, getIntent(), out ->
                            out.println("ERROR: " + e.getMessage()));
                }
            } else {
                ResultReturner.returnData(this, getIntent(), out ->
                        out.println("MediaProjection denied by user"));
            }
            finishAndRemoveTask();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (!done) {
                done = true;
                try {
                    ResultReturner.returnData(this, getIntent(), out ->
                            out.println(""));
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Get the active MediaProjection instance (for internal use).
     */
    public static MediaProjection getActiveProjection() {
        return activeProjection;
    }

    /**
     * Release the active projection and virtual display.
     */
    public static void releaseProjection() {
        if (activeDisplay != null) {
            activeDisplay.release();
            activeDisplay = null;
        }
        if (activeProjection != null) {
            activeProjection.stop();
            activeProjection = null;
        }
    }
}
