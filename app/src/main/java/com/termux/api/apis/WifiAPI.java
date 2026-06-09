package com.termux.api.apis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.List;

public class WifiAPI {

    private static final String LOG_TAG = "WifiAPI";

    public static void onReceiveWifiConnectionInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveWifiConnectionInfo");

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @SuppressLint("HardwareIds")
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                out.beginObject();
                if (info == null) {
                    out.name("API_ERROR").value("No current connection");
                } else {
                    // Fix for issue #304: check if location is enabled to warn about stale data
                    String bssid = info.getBSSID();
                    String ssid = info.getSSID().replaceAll("\"", "");
                    // Fix for issue #304: on Android 8.1+ with location disabled, BSSID/SSID are hidden
                    if (bssid == null || bssid.equals("02:00:00:00:00:00") || ssid.equals("<unknown ssid>")) {
                        out.name("_warning").value("Location may be enabled but WiFi location data is hidden. Ensure location is enabled in settings.");
                    }
                    out.name("bssid").value(bssid);
                    out.name("frequency_mhz").value(info.getFrequency());
                    //noinspection deprecation - formatIpAddress is deprecated, but we only have a ipv4 address here:
                    out.name("ip").value(Formatter.formatIpAddress(info.getIpAddress()));
                    out.name("link_speed_mbps").value(info.getLinkSpeed());
                    out.name("mac_address").value(info.getMacAddress());
                    out.name("network_id").value(info.getNetworkId());
                    out.name("rssi").value(info.getRssi());
                    out.name("ssid").value(ssid);
                    out.name("ssid_hidden").value(info.getHiddenSSID());
                    out.name("supplicant_state").value(info.getSupplicantState().toString());
                }
                out.endObject();
            }
        });
    }

    static boolean isLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Fix for issue #268: check location permission before scan
    static boolean hasLocationPermission(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void onReceiveWifiScanInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveWifiScanInfo");

        // Fix for issue #268: check for location permission
        if (!hasLocationPermission(context)) {
            ResultReturner.returnData(apiReceiver, intent, out -> {
                out.println("Error: ACCESS_FINE_LOCATION permission not granted. Termux:API needs location permission to scan WiFi networks. Grant it in Android app settings.");
            });
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> scans = manager.getScanResults();
                if (scans == null) {
                    out.beginObject().name("API_ERROR").value("Failed getting scan results").endObject();
                } else if (scans.isEmpty() && !isLocationEnabled(context)) {
                    // https://issuetracker.google.com/issues/37060483:
                    // "WifiManager#getScanResults() returns an empty array list if GPS is turned off"
                    String errorMessage = "Location needs to be enabled on the device";
                    out.beginObject().name("API_ERROR").value(errorMessage).endObject();
                } else {
                    out.beginArray();
                    for (ScanResult scan : scans) {
                        out.beginObject();
                        out.name("bssid").value(scan.BSSID);
                        out.name("frequency_mhz").value(scan.frequency);
                        out.name("rssi").value(scan.level);
                        out.name("ssid").value(scan.SSID);
                        out.name("timestamp").value(scan.timestamp);

                        int channelWidth = scan.channelWidth;
                        String channelWidthMhz = "???";
                        switch (channelWidth) {
                            case ScanResult.CHANNEL_WIDTH_20MHZ:
                                channelWidthMhz = "20";
                                break;
                            case ScanResult.CHANNEL_WIDTH_40MHZ:
                                channelWidthMhz = "40";
                                break;
                            case ScanResult.CHANNEL_WIDTH_80MHZ:
                                channelWidthMhz = "80";
                                break;
                            case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
                                channelWidthMhz = "80+80";
                                break;
                            case ScanResult.CHANNEL_WIDTH_160MHZ:
                                channelWidthMhz = "160";
                                break;
                        }
                        out.name("channel_bandwidth_mhz").value(channelWidthMhz);
                        if (channelWidth != ScanResult.CHANNEL_WIDTH_20MHZ) {
                            // centerFreq0 says "Not used if the AP bandwidth is 20 MHz".
                            out.name("center_frequency_mhz").value(scan.centerFreq0);
                        }
                        if (!TextUtils.isEmpty(scan.capabilities)) {
                            out.name("capabilities").value(scan.capabilities);
                        }
                        if (!TextUtils.isEmpty(scan.operatorFriendlyName)) {
                            out.name("operator_name").value(scan.operatorFriendlyName.toString());
                        }
                        if (!TextUtils.isEmpty(scan.venueName)) {
                            out.name("venue_name").value(scan.venueName.toString());
                        }
                        out.endObject();
                    }
                    out.endArray();
                }
            }
        });
    }

    // Fix for issue #330: use Settings Panel on Android 10+
    public static void onReceiveWifiEnable(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveWifiEnable");

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) {
                WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                boolean state = intent.getBooleanExtra("enabled", false);
                // Fix for issue #330: setWifiEnabled deprecated on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use Settings Panel on Android 10+
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(panelIntent);
                } else {
                    try {
                        manager.setWifiEnabled(state);
                    } catch (SecurityException e) {
                        Logger.logStackTraceWithMessage(LOG_TAG, "Failed to toggle WiFi", e);
                        try {
                            out.beginObject().name("API_ERROR").value("Failed to toggle WiFi: " + e.getMessage()).endObject();
                        } catch (Exception jsonException) {
                            throw new RuntimeException(jsonException);
                        }
                    }
                }
            }
        });
    }

    // Fix for issue #334/#678: actively trigger WiFi rescan
    public static void onReceiveWifiRescan(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveWifiRescan");

        // Check location permission for scan (#268)
        if (!hasLocationPermission(context)) {
            ResultReturner.returnData(apiReceiver, intent, out -> {
                out.println("Error: ACCESS_FINE_LOCATION permission not granted.");
            });
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                boolean scanStarted = manager.startScan();
                out.beginObject();
                out.name("scan_initiated").value(scanStarted);
                out.endObject();
            }
        });
    }

}
