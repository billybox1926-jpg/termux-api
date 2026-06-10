package com.termux.api.apis;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonWriter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for device lock and policy management.
 * Fix for issue #403.
 * Uses DevicePolicyManager for lock device functionality.
 */
public class DeviceLockAPI {

    private static final String LOG_TAG = "DeviceLockAPI";
    private static final int REQUEST_CODE = 9002;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "lock":
                lockDevice(apiReceiver, context, intent);
                break;
            case "status":
                lockStatus(apiReceiver, context, intent);
                break;
            case "enable":
                enableAdmin(context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void lockDevice(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm == null) {
                    out.println("ERROR: DevicePolicyManager not available");
                    return;
                }

                ComponentName admin = new ComponentName(context, DeviceAdminReceiver.class);
                if (!dpm.isAdminActive(admin)) {
                    out.println("ERROR: Device admin not enabled. Use 'enable' action first.");
                    return;
                }

                dpm.lockNow();
                out.println("Device locked");
            } catch (SecurityException e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    private static void lockStatus(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                out.beginObject();
                if (dpm != null) {
                    ComponentName admin = new ComponentName(context, DeviceAdminReceiver.class);
                    out.name("admin_active").value(dpm.isAdminActive(admin));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        out.name("quality").value(dpm.getStorageEncryptionStatus());
                    }
                }
                out.endObject();
            }
        });
    }

    private static void enableAdmin(Context context, Intent intent) {
        Intent i = new Intent(context, DeviceAdminRequestActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    /**
     * Minimal DeviceAdminReceiver for device lock.
     */
    public static class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
        @Override
        public void onEnabled(Context context, Intent intent) {
            Logger.logInfo(LOG_TAG, "Device admin enabled");
        }
        @Override
        public void onDisabled(Context context, Intent intent) {
            Logger.logInfo(LOG_TAG, "Device admin disabled");
        }
    }

    /**
     * Activity to request device admin activation.
     */
    public static class DeviceAdminRequestActivity extends AppCompatActivity {
        private boolean done = false;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    new ComponentName(this, DeviceAdminReceiver.class));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Termux:API needs device admin permission to lock the device.");
            try {
                startActivityForResult(intent, REQUEST_CODE);
            } catch (Exception e) {
                done = true;
                ResultReturner.returnData(this, getIntent(), out ->
                        out.println("ERROR: " + e.getMessage()));
                finishAndRemoveTask();
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            done = true;
            ResultReturner.returnData(this, getIntent(), out -> {
                if (resultCode == RESULT_OK) {
                    out.println("Device admin enabled");
                } else {
                    out.println("Device admin request denied");
                }
            });
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
