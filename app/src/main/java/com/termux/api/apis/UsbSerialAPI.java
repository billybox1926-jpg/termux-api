package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.HashMap;

/**
 * API for USB-serial bridge to file.
 * Fix for issue #395.
 * Lists USB-serial devices and provides info for bridging.
 */
public class UsbSerialAPI {

    private static final String LOG_TAG = "UsbSerialAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listSerialDevices(apiReceiver, context, intent);
                break;
            case "info":
                serialInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void listSerialDevices(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                if (usbManager == null) {
                    out.beginObject().name("error").value("UsbManager not available").endObject();
                    return;
                }

                HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
                out.beginArray();
                for (UsbDevice device : devices.values()) {
                    // Look for CDC-ACM or serial-type interfaces
                    boolean isSerial = false;
                    for (int i = 0; i < device.getInterfaceCount(); i++) {
                        int cls = device.getInterface(i).getInterfaceClass();
                        if (cls == 2 || cls == 10) { // CDC-ACM or CDC-DATA
                            isSerial = true;
                            break;
                        }
                    }
                    if (isSerial) {
                        out.beginObject();
                        out.name("device_name").value(device.getDeviceName());
                        out.name("vendor_id").value(String.format("0x%04x", device.getVendorId()));
                        out.name("product_id").value(String.format("0x%04x", device.getProductId()));
                        out.name("manufacturer").value(device.getManufacturerName());
                        out.name("product").value(device.getProductName());
                        out.name("serial").value(device.getSerialNumber());
                        out.name("interface_count").value(device.getInterfaceCount());
                        out.endObject();
                    }
                }
                out.endArray();
            }
        });
    }

    private static void serialInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> devices = usbManager != null ? usbManager.getDeviceList() : null;
                int serialCount = 0;
                if (devices != null) {
                    for (UsbDevice device : devices.values()) {
                        for (int i = 0; i < device.getInterfaceCount(); i++) {
                            int cls = device.getInterface(i).getInterfaceClass();
                            if (cls == 2 || cls == 10) {
                                serialCount++;
                                break;
                            }
                        }
                    }
                }
                out.beginObject();
                out.name("usb_devices_total").value(devices != null ? devices.size() : 0);
                out.name("serial_devices").value(serialCount);
                out.name("note").value("USB-serial bridge requires a compatible serial driver. " +
                        "Use 'list' to find CDC-ACM devices.");
                out.endObject();
            }
        });
    }
}
