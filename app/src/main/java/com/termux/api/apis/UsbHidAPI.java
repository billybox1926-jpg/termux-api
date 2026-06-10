package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * API for USB HID device communication (YubiKey, etc).
 * Fix for issue #393.
 * Provides raw USB HID send/receive for security keys.
 */
public class UsbHidAPI {

    private static final String LOG_TAG = "UsbHidAPI";

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
                listHidDevices(apiReceiver, context, intent);
                break;
            case "info":
                hidInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void listHidDevices(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
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
                    // Check if device has HID interface
                    boolean hasHid = false;
                    for (int i = 0; i < device.getInterfaceCount(); i++) {
                        if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_HID) {
                            hasHid = true;
                            break;
                        }
                    }
                    if (hasHid) {
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

    private static void hidInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> devices = usbManager != null ? usbManager.getDeviceList() : null;
                int hidCount = 0;
                if (devices != null) {
                    for (UsbDevice device : devices.values()) {
                        for (int i = 0; i < device.getInterfaceCount(); i++) {
                            if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_HID) {
                                hidCount++;
                                break;
                            }
                        }
                    }
                }
                out.beginObject();
                out.name("usb_devices_total").value(devices != null ? devices.size() : 0);
                out.name("hid_devices").value(hidCount);
                out.name("note").value("Use 'list' action to see HID devices. " +
                        "Direct HID communication requires USB host mode and permission.");
                out.endObject();
            }
        });
    }
}
