package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.file.TermuxFileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * API for recording video with camera2 + MediaRecorder.
 * Actions: "start" and "quit" (stop + save).
 * Extras: "file" (output path), "camera" (0=back, 1=front),
 *         "duration" (max seconds, default 60), "size" (e.g. "1280x720").
 */
public class CameraVideoAPI {

    private static final String LOG_TAG = "CameraVideoAPI";

    // Shared state for the current recording session
    private static volatile MediaRecorder sMediaRecorder;
    private static volatile CameraDevice sCameraDevice;
    private static volatile CameraCaptureSession sCaptureSession;
    private static volatile Looper sLooper;
    private static volatile File sOutputFile;
    private static volatile boolean sRecording = false;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        String action = intent.getStringExtra("action");
        if (action == null) action = "start";
        final String finalAction = action;

        switch (finalAction) {
            case "start":
                startRecording(apiReceiver, context, intent);
                break;
            case "quit":
            case "stop":
                stopRecording(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("Error: Unknown action '" + finalAction + "'. Use 'start' or 'quit'."));
        }
    }

    // ── start ──────────────────────────────────────────────

    private static void startRecording(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            if (sRecording) {
                out.println("Error: Already recording. Use 'quit' first.");
                return;
            }

            String filePath = intent.getStringExtra("file");
            if (filePath == null || filePath.isEmpty()) {
                out.println("Error: Missing 'file' extra");
                return;
            }

            String videoFilePath = TermuxFileUtils.getCanonicalPath(filePath, null, true);
            String videoDirPath = FileUtils.getFileDirname(videoFilePath);
            Error error = TermuxFileUtils.validateDirectoryFileExistenceAndPermissions(
                    "video directory", videoDirPath, true, true, true, false, true);
            if (error != null) {
                out.println("ERROR: " + error.getErrorLogString());
                return;
            }

            String cameraId = intent.getStringExtra("camera");
            if (cameraId == null || cameraId.isEmpty()) cameraId = "0";
            final String finalCameraId = cameraId;

            int maxDuration = intent.getIntExtra("duration", 60);
            if (maxDuration <= 0) maxDuration = 60;
            final int finalMaxDuration = maxDuration;

            sOutputFile = new File(videoFilePath);

            // Prepare MediaRecorder on a background thread with its own Looper
            new Thread(() -> {
                try {
                    Looper.prepare();
                    sLooper = Looper.myLooper();

                    prepareMediaRecorder(context, sOutputFile, finalCameraId, intent);
                    openCameraAndStartRecording(context, finalCameraId, out, finalMaxDuration);

                    Looper.loop();
                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "startRecording error", e);
                    out.println("Error: " + e.getMessage());
                    cleanup();
                }
            }).start();
        });
    }

    private static void prepareMediaRecorder(Context context, File outputFile, String cameraId, Intent intent) {
        sMediaRecorder = new MediaRecorder();

        // Audio + video sources
        sMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        sMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        // Output format + encoder
        sMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        sMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        sMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        // Resolution
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
            Size videoSize = chooseVideoSize(chars);
            sMediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        } catch (CameraAccessException e) {
            // Fallback to 720p
            sMediaRecorder.setVideoSize(1280, 720);
        }

        sMediaRecorder.setVideoFrameRate(30);
        sMediaRecorder.setVideoEncodingBitRate(6_000_000); // 6 Mbps
        sMediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            sMediaRecorder.prepare();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "MediaRecorder prepare failed", e);
            cleanup();
            throw new RuntimeException("MediaRecorder prepare failed: " + e.getMessage(), e);
        }
    }

    private static Size chooseVideoSize(CameraCharacteristics chars) {
        try {
            android.hardware.camera2.params.StreamConfigurationMap map =
                    chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                Size[] videoSizes = map.getOutputSizes(MediaRecorder.class);
                if (videoSizes != null && videoSizes.length > 0) {
                    // Pick 1280x720 if available, else largest <= 1920x1080
                    List<Size> sizes = Arrays.asList(videoSizes);
                    // Prefer 1280x720
                    for (Size s : sizes) {
                        if (s.getWidth() == 1280 && s.getHeight() == 720) return s;
                    }
                    // Else largest <= 1080p
                    Comparator<Size> byArea = (a, b) ->
                            Long.signum((long) a.getWidth() * a.getHeight() - (long) b.getWidth() * b.getHeight());
                    List<Size> filtered = new ArrayList<>();
                    for (Size s : sizes) {
                        if (s.getWidth() <= 1920 && s.getHeight() <= 1080) filtered.add(s);
                    }
                    if (!filtered.isEmpty()) return Collections.max(filtered, byArea);
                    // Fallback to smallest
                    return Collections.min(sizes, byArea);
                }
            }
        } catch (Exception ignored) {}
        return new Size(1280, 720);
    }

    private static void openCameraAndStartRecording(final Context context, String cameraId,
                                                     final PrintWriter out, int maxDuration) throws Exception {
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        //noinspection MissingPermission
        manager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                sCameraDevice = camera;
                try {
                    startPreviewAndRecording(camera, out, maxDuration);
                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Recording start failed", e);
                    out.println("Error: " + e.getMessage());
                    cleanup();
                }
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                out.println("Error: Camera disconnected");
                cleanup();
            }

            @Override
            public void onError(CameraDevice camera, int errorCode) {
                out.println("Error: Camera error " + errorCode);
                cleanup();
            }
        }, null);
    }

    private static void startPreviewAndRecording(CameraDevice camera, PrintWriter out, int maxDuration) throws CameraAccessException {
        List<Surface> surfaces = new ArrayList<>();

        // MediaRecorder surface
        Surface recorderSurface = sMediaRecorder.getSurface();
        surfaces.add(recorderSurface);

        // Dummy preview surface
        android.graphics.SurfaceTexture previewTexture = new android.graphics.SurfaceTexture(1);
        Surface dummySurface = new Surface(previewTexture);
        surfaces.add(dummySurface);

        camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                sCaptureSession = session;
                try {
                    CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    builder.addTarget(recorderSurface);
                    builder.addTarget(dummySurface);
                    session.setRepeatingRequest(builder.build(), null, null);

                    sMediaRecorder.start();
                    sRecording = true;
                    out.println("Recording started: " + sOutputFile.getAbsolutePath());
                    out.println("Max duration: " + maxDuration + "s");

                    // Auto-stop after maxDuration
                    new Thread(() -> {
                        try {
                            Thread.sleep(maxDuration * 1000L);
                            if (sRecording) {
                                Logger.logInfo(LOG_TAG, "Max duration reached, stopping");
                                stopRecording(null, null, null);
                            }
                        } catch (InterruptedException ignored) {}
                    }).start();

                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Recording start failed", e);
                    out.println("Error: " + e.getMessage());
                    cleanup();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                out.println("Error: Camera configure failed");
                cleanup();
            }
        }, null);
    }

    // ── stop ───────────────────────────────────────────────

    private static void stopRecording(TermuxApiReceiver apiReceiver, Context context, Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, out -> {
            if (!sRecording) {
                out.println("Error: No recording in progress");
                return;
            }

            try {
                sMediaRecorder.stop();
                sRecording = false;
                out.println("Recording saved: " + sOutputFile.getAbsolutePath());
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Stop recording error", e);
                out.println("Error stopping recording: " + e.getMessage());
            } finally {
                cleanup();
                if (sLooper != null) sLooper.quit();
            }
        });
    }

    // ── cleanup ────────────────────────────────────────────

    private static synchronized void cleanup() {
        sRecording = false;
        if (sMediaRecorder != null) {
            try { sMediaRecorder.reset(); } catch (Exception ignored) {}
            try { sMediaRecorder.release(); } catch (Exception ignored) {}
            sMediaRecorder = null;
        }
        if (sCaptureSession != null) {
            try { sCaptureSession.stopRepeating(); } catch (Exception ignored) {}
            try { sCaptureSession.abortCaptures(); } catch (Exception ignored) {}
            sCaptureSession.close();
            sCaptureSession = null;
        }
        if (sCameraDevice != null) {
            sCameraDevice.close();
            sCameraDevice = null;
        }
        Logger.logDebug(LOG_TAG, "CameraVideo cleanup done");
    }
}
