package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for Bluetooth headset microphone control.
 * Fix for issue #681.
 * Manages audio routing to/from Bluetooth headset.
 */
public class BluetoothAudioAPI {

    private static final String LOG_TAG = "BluetoothAudioAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "info":
                audioInfo(apiReceiver, context, intent);
                break;
            case "start":
                startBtRecording(apiReceiver, context, intent);
                break;
            case "stop":
                stopBtRecording(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void audioInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                out.beginObject();
                if (am != null) {
                    out.name("bluetooth_sco_on").value(am.isBluetoothScoOn());
                    out.name("speakerphone_on").value(am.isSpeakerphoneOn());
                    out.name("microphone_mute").value(am.isMicrophoneMute());
                    out.name("mode").value(modeToString(am.getMode()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        out.name("communication_device").value(
                                am.getCommunicationDevice() != null ?
                                        am.getCommunicationDevice().toString() : "none");
                    }
                }
                out.endObject();
            }
        });
    }

    private static void startBtRecording(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am == null) {
                    out.println("ERROR: AudioManager not available");
                    return;
                }
                am.setBluetoothScoOn(true);
                am.startBluetoothSco();
                out.println("Bluetooth SCO started for recording");
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    private static void stopBtRecording(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am == null) {
                    out.println("ERROR: AudioManager not available");
                    return;
                }
                am.setBluetoothScoOn(false);
                am.stopBluetoothSco();
                out.println("Bluetooth SCO stopped");
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    private static String modeToString(int mode) {
        switch (mode) {
            case AudioManager.MODE_NORMAL: return "normal";
            case AudioManager.MODE_RINGTONE: return "ringtone";
            case AudioManager.MODE_IN_CALL: return "in_call";
            case AudioManager.MODE_IN_COMMUNICATION: return "in_communication";
            default: return "unknown(" + mode + ")";
        }
    }
}
