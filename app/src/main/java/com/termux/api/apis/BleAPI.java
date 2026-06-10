package com.termux.api.apis;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * API for Bluetooth Low Energy (BLE) scanning.
 * Fix for issue #713.
 * Requires android.permission.BLUETOOTH_SCAN (Android 12+) or
 * android.permission.BLUETOOTH + BLUETOOTH_ADMIN (older).
 */
public class BleAPI {

    private static final String LOG_TAG = "BleAPI";
    private static final long DEFAULT_SCAN_TIMEOUT_MS = 10000;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: BLE scanning requires Android 5.0+"));
            return;
        }

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "scan":
                scanBle(apiReceiver, context, intent);
                break;
            case "info":
                bleInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void scanBle(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final long timeoutMs = intent.getLongExtra("timeout_ms", DEFAULT_SCAN_TIMEOUT_MS);
        final String filterUuid = intent.getStringExtra("uuid");

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (btManager == null) {
                    out.beginObject().name("error").value("Bluetooth not available").endObject();
                    return;
                }
                BluetoothAdapter btAdapter = btManager.getAdapter();
                if (btAdapter == null || !btAdapter.isEnabled()) {
                    out.beginObject().name("error").value("Bluetooth is disabled or not available").endObject();
                    return;
                }

                BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
                if (scanner == null) {
                    out.beginObject().name("error").value("BLE scanner not available").endObject();
                    return;
                }

                final List<ScanResult> results = new ArrayList<>();
                final CountDownLatch latch = new CountDownLatch(1);

                ScanCallback callback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        results.add(result);
                    }
                    @Override
                    public void onScanFailed(int errorCode) {
                        Logger.logError(LOG_TAG, "BLE scan failed: " + errorCode);
                        latch.countDown();
                    }
                };

                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

                List<ScanFilter> filters = new ArrayList<>();
                if (filterUuid != null && !filterUuid.isEmpty()) {
                    try {
                        ParcelUuid uuid = ParcelUuid.fromString(filterUuid);
                        filters.add(new ScanFilter.Builder().setServiceUuid(uuid).build());
                    } catch (IllegalArgumentException e) {
                        Logger.logError(LOG_TAG, "Invalid UUID: " + filterUuid);
                    }
                }

                try {
                    scanner.startScan(filters, settings, callback);
                    latch.await(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (SecurityException e) {
                    out.beginObject().name("error").value("Permission denied: " + e.getMessage()).endObject();
                    return;
                } finally {
                    try {
                        scanner.stopScan(callback);
                    } catch (Exception ignored) {}
                }

                out.beginArray();
                for (ScanResult r : results) {
                    out.beginObject();
                    out.name("address").value(r.getDevice().getAddress());
                    out.name("name").value(r.getDevice().getName());
                    out.name("rssi").value(r.getRssi());
                    out.name("tx_power").value(r.getTxPower());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        out.name("timestamp_nanos").value(r.getTimestampNanos());
                    }
                    // Service UUIDs from scan record
                    if (r.getScanRecord() != null) {
                        List<ParcelUuid> uuids = r.getScanRecord().getServiceUuids();
                        if (uuids != null && !uuids.isEmpty()) {
                            out.name("service_uuids");
                            out.beginArray();
                            for (ParcelUuid u : uuids) {
                                out.value(u.toString());
                            }
                            out.endArray();
                        }
                    }
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    private static void bleInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                out.beginObject();
                if (btManager == null) {
                    out.name("error").value("Bluetooth not available");
                } else {
                    BluetoothAdapter btAdapter = btManager.getAdapter();
                    out.name("bluetooth_enabled").value(btAdapter != null && btAdapter.isEnabled());
                    out.name("ble_supported").value(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                    if (btAdapter != null) {
                        out.name("address").value(btAdapter.getAddress());
                        out.name("name").value(btAdapter.getName());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            out.name("le_max_advertising_data_length")
                                    .value(btAdapter.getLeMaximumAdvertisingDataLength());
                        }
                    }
                }
                out.endObject();
            }
        });
    }
}
