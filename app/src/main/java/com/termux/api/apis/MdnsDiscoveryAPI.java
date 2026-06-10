package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.nsd.NsdManager;
import android.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * API for mDNS/DNS-SD service discovery.
 * Fix for issue #688.
 * Uses NsdManager for local network service discovery.
 */
public class MdnsDiscoveryAPI {

    private static final String LOG_TAG = "MdnsDiscoveryAPI";
    private static final long DEFAULT_TIMEOUT_MS = 10000;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "discover":
                discoverServices(apiReceiver, context, intent);
                break;
            case "info":
                discoveryInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void discoverServices(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String serviceType = intent.getStringExtra("service_type");
        final long timeoutMs = intent.getLongExtra("timeout_ms", DEFAULT_TIMEOUT_MS);

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                NsdManager nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
                if (nsdManager == null) {
                    out.beginObject().name("error").value("NsdManager not available").endObject();
                    return;
                }

                String type = (serviceType != null && !serviceType.isEmpty()) ?
                        serviceType : "_services._dns-sd._udp";

                List<NsdServiceInfo> discovered = new CopyOnWriteArrayList<>();
                CountDownLatch latch = new CountDownLatch(1);

                NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
                    @Override
                    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                        Logger.logError(LOG_TAG, "Discovery failed: " + errorCode);
                        latch.countDown();
                    }
                    @Override
                    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                        Logger.logError(LOG_TAG, "Stop discovery failed: " + errorCode);
                    }
                    @Override
                    public void onDiscoveryStarted(String serviceType) {
                        Logger.logDebug(LOG_TAG, "Discovery started: " + serviceType);
                    }
                    @Override
                    public void onDiscoveryStopped(String serviceType) {
                        Logger.logDebug(LOG_TAG, "Discovery stopped: " + serviceType);
                    }
                    @Override
                    public void onServiceFound(NsdServiceInfo serviceInfo) {
                        discovered.add(serviceInfo);
                    }
                    @Override
                    public void onServiceLost(NsdServiceInfo serviceInfo) {
                        discovered.remove(serviceInfo);
                    }
                };

                try {
                    nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener);
                    latch.await(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    out.beginObject().name("error").value(e.getMessage()).endObject();
                    return;
                } finally {
                    try {
                        nsdManager.stopServiceDiscovery(listener);
                    } catch (Exception ignored) {}
                }

                out.beginArray();
                for (NsdServiceInfo info : discovered) {
                    out.beginObject();
                    out.name("service_name").value(info.getServiceName());
                    out.name("service_type").value(info.getServiceType());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        out.name("port").value(info.getPort());
                    }
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    private static void discoveryInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("nsd_available").value(true);
                out.name("note").value("Use 'discover' action with optional 'service_type' extra. " +
                        "Default discovers all services.");
                out.endObject();
            }
        });
    }
}
