package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for binding process to a specific network.
 * Fix for issue #771.
 * Uses ConnectivityManager.setProcessDefaultNetwork (API 23+).
 */
public class NetworkBindAPI {

    private static final String LOG_TAG = "NetworkBindAPI";
    private static Network boundNetwork;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Network binding requires Android 6.0+"));
            return;
        }

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "bind":
                bindToNetwork(apiReceiver, context, intent);
                break;
            case "unbind":
                unbindNetwork(apiReceiver, context, intent);
                break;
            case "status":
                bindStatus(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void bindToNetwork(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String transportType = intent.getStringExtra("transport");
        final String host = intent.getStringExtra("host");

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null) {
                    out.println("ERROR: ConnectivityManager not available");
                    return;
                }

                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                if ("wifi".equals(transportType)) {
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                } else if ("cellular".equals(transportType)) {
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                } else {
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                }

                if (host != null && !host.isEmpty()) {
                    builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }

                cm.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        boundNetwork = network;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cm.bindProcessToNetwork(network);
                        }
                        Logger.logInfo(LOG_TAG, "Bound to network: " + network);
                    }

                    @Override
                    public void onLost(Network network) {
                        if (network.equals(boundNetwork)) {
                            boundNetwork = null;
                            cm.bindProcessToNetwork(null);
                        }
                    }
                });

                out.println("Network binding requested" + (transportType != null ? " (transport=" + transportType + ")" : ""));
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void unbindNetwork(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.bindProcessToNetwork(null);
                    boundNetwork = null;
                    out.println("Network unbound");
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void bindStatus(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                out.beginObject();
                out.name("bound_network").value(boundNetwork != null ? boundNetwork.toString() : "none");
                if (cm != null) {
                    Network active = cm.getActiveNetwork();
                    out.name("active_network").value(active != null ? active.toString() : "none");
                }
                out.endObject();
            }
        });
    }
}
