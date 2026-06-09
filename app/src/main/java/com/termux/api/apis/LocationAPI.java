package com.termux.api.apis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.JsonWriter;

import androidx.annotation.RequiresPermission;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;

import java.io.IOException;

public class LocationAPI {

    private static final String LOG_TAG = "LocationAPI";

    private static final String REQUEST_LAST_KNOWN = "last";
    private static final String REQUEST_ONCE = "once";
    private static final String REQUEST_UPDATES = "updates";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            @Override
            public void writeJson(final JsonWriter out) throws Exception {
                LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                String provider = intent.getStringExtra("provider");
                if (provider == null)
                    provider = LocationManager.GPS_PROVIDER;
                if (!(provider.equals(LocationManager.GPS_PROVIDER) || provider.equals(LocationManager.NETWORK_PROVIDER) || provider
                        .equals(LocationManager.PASSIVE_PROVIDER))) {
                    out.beginObject()
                            .name("API_ERROR")
                            .value("Unsupported provider '" + provider + "' - only '" + LocationManager.GPS_PROVIDER + "', '"
                                    + LocationManager.NETWORK_PROVIDER + "' and '" + LocationManager.PASSIVE_PROVIDER + "' supported").endObject();
                    return;
                }

                String request = intent.getStringExtra("request");
                if (request == null)
                    request = REQUEST_ONCE;
                switch (request) {
                    case REQUEST_LAST_KNOWN:
                        Location lastKnownLocation = manager.getLastKnownLocation(provider);
                        // Fix for issue #365: if no last known for requested provider, try other providers
                        if (lastKnownLocation == null) {
                            lastKnownLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        if (lastKnownLocation == null) {
                            lastKnownLocation = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        }
                        locationToJson(lastKnownLocation, out);
                        break;
                    case REQUEST_ONCE:
                        // Fix for issue #781: handle SecurityException on Android 12+
                        boolean providerEnabled;
                        try {
                            providerEnabled = manager.isProviderEnabled(provider);
                        } catch (SecurityException e) {
                            out.beginObject().name("API_ERROR").value("Permission denied: " + e.getMessage()).endObject();
                            break;
                        }
                        // Fix for issue #365: if GPS not enabled, try network provider
                        String actualProvider = provider;
                        if (!providerEnabled) {
                            if (provider.equals(LocationManager.GPS_PROVIDER) && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                actualProvider = LocationManager.NETWORK_PROVIDER;
                                Logger.logInfo(LOG_TAG, "GPS provider not enabled, falling back to network");
                            } else {
                                out.beginObject().name("API_ERROR").value("Provider '" + provider + "' is not enabled. Please turn on location services.").endObject();
                                break;
                            }
                        }
                        final long timeoutMs = intent.getLongExtra("timeout", 60000);
                        // Fix for issue #226: use final effectively-final variables for lambda
                        final LocationManager finalManager = manager;
                        final String resolvedProvider = actualProvider;
                        final JsonWriter finalOut = out;
                        final Looper[] looper = new Looper[1];
                        // Fix for issue #427: also register continuous updates as fallback for faster results
                        final LocationListener[] fallbackListener = new LocationListener[1];
                        final boolean[] gotResult = {false};
                        Thread looperThread = new Thread(() -> {
                            Looper.prepare();
                            looper[0] = Looper.myLooper();
                            finalManager.requestSingleUpdate(resolvedProvider, new LocationListener() {
                                @Override
                                public void onStatusChanged(String changedProvider, int status, Bundle extras) {}

                                @Override
                                public void onProviderEnabled(String changedProvider) {}

                                @Override
                                public void onProviderDisabled(String changedProvider) {}

                                @Override
                                public void onLocationChanged(Location location) {
                                    try {
                                        gotResult[0] = true;
                                        locationToJson(location, finalOut);
                                    } catch (IOException e) {
                                        Logger.logStackTraceWithMessage(LOG_TAG, "Writing json", e);
                                    } finally {
                                        // Fix for issue #427: remove fallback listener if it was registered
                                        if (fallbackListener[0] != null) {
                                            finalManager.removeUpdates(fallbackListener[0]);
                                        }
                                        Looper.myLooper().quit();
                                    }
                                }
                            }, null);
                            // Fix for issue #427: register continuous updates as fallback after half timeout
                            long halfTimeout = timeoutMs / 2;
                            if (halfTimeout > 2000) {
                                fallbackListener[0] = new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        if (!gotResult[0]) {
                                            try {
                                                gotResult[0] = true;
                                                locationToJson(location, finalOut);
                                            } catch (IOException e) {
                                                Logger.logStackTraceWithMessage(LOG_TAG, "Writing json", e);
                                            } finally {
                                                finalManager.removeUpdates(this);
                                                if (looper[0] != null) looper[0].quit();
                                            }
                                        }
                                    }
                                    @Override public void onStatusChanged(String p, int s, Bundle e) {}
                                    @Override public void onProviderEnabled(String p) {}
                                    @Override public void onProviderDisabled(String p) {}
                                };
                                new Thread(() -> {
                                    try { Thread.sleep(halfTimeout); } catch (InterruptedException ignored) {}
                                    if (!gotResult[0] && looper[0] != null) {
                                        try {
                                            finalManager.requestLocationUpdates(resolvedProvider, 1000, 0, fallbackListener[0]);
                                        } catch (SecurityException ignored) {}
                                    }
                                }).start();
                            }
                            Looper.loop();
                        });
                        looperThread.start();

                        // Wait for result or timeout
                        new Thread(() -> {
                            try {
                                Thread.sleep(timeoutMs);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            if (looper[0] != null) {
                                looper[0].quit();
                            }
                        }).start();
                        break;
                    case REQUEST_UPDATES:
                        Looper.prepare();
                        manager.requestLocationUpdates(provider, 5000, 50.f, new LocationListener() {

                            @Override
                            public void onStatusChanged(String changedProvider, int status, Bundle extras) {
                                // Do nothing.
                            }

                            @Override
                            public void onProviderEnabled(String changedProvider) {
                                // Do nothing.
                            }

                            @Override
                            public void onProviderDisabled(String changedProvider) {
                                // Do nothing.
                            }

                            @Override
                            public void onLocationChanged(Location location) {
                                try {
                                    locationToJson(location, out);
                                    out.flush();
                                } catch (IOException e) {
                                    Logger.logStackTraceWithMessage(LOG_TAG, "Writing json", e);
                                }
                            }
                        }, null);
                        final Looper updatesLooper = Looper.myLooper();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(30 * 1000);
                                } catch (InterruptedException e) {
                                    Logger.logStackTraceWithMessage(LOG_TAG, "INTER", e);
                                }
                                updatesLooper.quit();
                            }
                        }.start();
                        Looper.loop();
                        break;
                    default:
                        out.beginObject()
                                .name("API_ERROR")
                                .value("Unsupported request '" + request + "' - only '" + REQUEST_LAST_KNOWN + "', '" + REQUEST_ONCE + "' and '" + REQUEST_UPDATES
                                        + "' supported").endObject();
                }
            }
        });
    }

    static void locationToJson(Location lastKnownLocation, JsonWriter out) throws IOException {
        if (lastKnownLocation == null) {
            out.beginObject().name("API_ERROR").value("Failed to get location").endObject();
            return;
        }
        out.beginObject();
        out.name("latitude").value(lastKnownLocation.getLatitude());
        out.name("longitude").value(lastKnownLocation.getLongitude());
        out.name("altitude").value(lastKnownLocation.getAltitude());
        out.name("accuracy").value(lastKnownLocation.getAccuracy());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            out.name("vertical_accuracy").value(lastKnownLocation.getVerticalAccuracyMeters());
        }
        out.name("bearing").value(lastKnownLocation.getBearing());
        out.name("speed").value(lastKnownLocation.getSpeed());
        long elapsedMs = (SystemClock.elapsedRealtimeNanos() - lastKnownLocation.getElapsedRealtimeNanos()) / 1000000;
        out.name("elapsedMs").value(elapsedMs);
        out.name("provider").value(lastKnownLocation.getProvider());
        out.endObject();
    }
}
