package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for microphone audio streaming to file.
 * Fix for issue #360.
 * Records audio from the microphone and writes to a file.
 */
public class MicStreamAPI {

    private static final String LOG_TAG = "MicStreamAPI";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "record":
                recordAudio(apiReceiver, context, intent);
                break;
            case "info":
                micInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void recordAudio(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String outputFile = intent.getStringExtra("output");
        final int durationMs = intent.getIntExtra("duration_ms", 5000);

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                if (bufferSize <= 0) {
                    out.println("ERROR: AudioRecord not supported");
                    return;
                }

                AudioRecord recorder = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    out.println("ERROR: AudioRecord initialization failed");
                    return;
                }

                String path = (outputFile != null && !outputFile.isEmpty()) ?
                        outputFile : context.getCacheDir() + "/termux_recording_" + System.currentTimeMillis() + ".pcm";

                java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
                byte[] buffer = new byte[bufferSize];
                recorder.startRecording();

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < durationMs) {
                    int read = recorder.read(buffer, 0, bufferSize);
                    if (read > 0) {
                        fos.write(buffer, 0, read);
                    }
                }

                recorder.stop();
                recorder.release();
                fos.close();

                out.println("Recording saved: " + path);
                out.println("Duration: " + durationMs + "ms, Format: PCM 16-bit mono " + SAMPLE_RATE + "Hz");
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    private static void micInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                out.beginObject();
                out.name("supported").value(bufferSize > 0);
                out.name("sample_rate").value(SAMPLE_RATE);
                out.name("channels").value(1);
                out.name("format").value("PCM_16BIT");
                out.name("buffer_size").value(bufferSize);
                out.endObject();
            }
        });
    }
}
