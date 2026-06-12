package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * API for VPN management via VpnService.
 * Fix for issue #545.
 * Provides basic VPN tunnel setup using Android's VpnService.
 */
public class VpnAPI {

    private static final String LOG_TAG = "VpnAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "prepare":
                prepareVpn(apiReceiver, context, intent);
                break;
            case "status":
                vpnStatus(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    /**
     * Prepare the VPN (shows system consent dialog).
     * Returns the prepare Intent result.
     */
    private static void prepareVpn(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                Intent prepareIntent = VpnService.prepare(context);
                if (prepareIntent != null) {
                    prepareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(prepareIntent);
                    out.println("VPN preparation dialog shown. User consent required.");
                } else {
                    out.println("VPN already prepared (previously granted).");
                }
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Check VPN status and return info.
     */
    private static void vpnStatus(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("vpn_service_available").value(true);
                out.name("note").value("Use 'prepare' action to request VPN consent. " +
                        "Full VPN tunnel requires a dedicated service implementation.");
                out.endObject();
            }
        });
    }

    /**
     * VpnService implementation for basic tunnel.
     * This is a minimal implementation -- full VPN requires native code
     * or integration with a VPN protocol library.
     */
    public static class TermuxVpnService extends VpnService {

        private static final String LOG_TAG = "TermuxVpnService";
        private ParcelFileDescriptor vpnInterface;

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent == null) return START_NOT_STICKY;

            String action = intent.getStringExtra("action");
            if ("connect".equals(action)) {
                String address = intent.getStringExtra("address");
                String route = intent.getStringExtra("route");
                String dns = intent.getStringExtra("dns");
                connectVpn(address, route, dns);
            } else if ("disconnect".equals(action)) {
                disconnectVpn();
            }
            return START_NOT_STICKY;
        }

        private void connectVpn(String address, String route, String dns) {
            try {
                Builder builder = new Builder();
                builder.setSession("TermuxVPN");
                builder.setMtu(1500);

                if (address != null) builder.addAddress(address, 32);
                if (route != null) builder.addRoute(route, 0);
                if (dns != null) builder.addDnsServer(dns);

                builder.addDisallowedApplication("com.termux.api");

                vpnInterface = builder.establish();
                if (vpnInterface != null) {
                    Logger.logInfo(LOG_TAG, "VPN interface established: " + vpnInterface.getFd());
                }
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "VPN connect failed", e);
            }
        }

        private void disconnectVpn() {
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "VPN disconnect error", e);
                }
                vpnInterface = null;
            }
            stopSelf();
        }

        @Override
        public void onDestroy() {
            disconnectVpn();
            super.onDestroy();
        }
    }
}
