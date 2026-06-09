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

    public static void onReceive(final TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = getAction(intent);
        switch (action) {
            case "start":
                startRecording(apiReceiver, context, intent);
                break;
            case "quit":
            case "stop":
                stopRecording(apiReceiver, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                    @Override
                    public void writeResult(final PrintWriter out) {
                        out.println("Error: Unknown action '" + action + "'. Use 'start' or 'quit'.");
                    }
                });
        }
    }

    private static String getAction(final Intent intent) {
        final String action = intent.getStringExtra("action");
        return (action == null || action.isEmpty()) ? "start" : action;
    }

    // ── start ──────────────────────────────────────────────

    private static void startRecording(final TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
            @Override
            public void writeResult(final PrintWriter out) {
                if (sRecording) {
                    out.println("Error: Already recording. Use 'quit' first.");
                    return;
                }

                final String filePath = intent.getStringExtra("file");
                if (filePath == null || filePath.isEmpty()) {
                    out.println("Error: Missing 'file' extra");
                    return;
                }

                final String videoFilePath = TermuxFileUtils.getCanonicalPath(filePath, null, true);
                final String videoDirPath = FileUtils.getFileDirname(videoFilePath);
                final Error error = TermuxFileUtils.validateDirectoryFileExistenceAndPermissions(
                        "video directory", videoDirPath, true, true, true, false, true);
                if (error != null) {
                    out.println("ERROR: " + error.getErrorLogString());
                    return;
                }

                final String requestedCameraId = intent.getStringExtra("camera");
                final String cameraId = (requestedCameraId == null || requestedCameraId.isEmpty()) ? "0" : requestedCameraId;

                final int requestedMaxDuration = intent.getIntExtra("duration", 60);
                final int maxDuration = requestedMaxDuration <= 0 ? 60 : requestedMaxDuration;

                final File outputFile = new File(videoFilePath);
                sOutputFile = outputFile;

                // Prepare MediaRecorder on a background thread with its own Looper.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Looper.prepare();
                            sLooper = Looper.myLooper();

                            prepareMediaRecorder(context, outputFile, cameraId);
                            openCameraAndStartRecording(context, cameraId, out, maxDuration);

                            Looper.loop();
                        } catch (Exception e) {
                            Logger.logStackTraceWithMessage(LOG_TAG, "startRecording error", e);
                            out.println("Error: " + e.getMessage());
                            cleanup();
                            quitLooper();
                        }
                    }
                }).start();
            }
        });
    }

    private static void prepareMediaRecorder(final Context context, final File outputFile, final String cameraId) {
        sMediaRecorder = new MediaRecorder();

        // Audio + video sources
        sMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        sMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        // Output format + encoder
        sMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        sMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        sMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        // Resolution
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            final CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
            final Size videoSize = chooseVideoSize(chars);
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

    private static Size chooseVideoSize(final CameraCharacteristics chars) {
        try {
            final android.hardware.camera2.params.StreamConfigurationMap map =
                    chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                final Size[] videoSizes = map.getOutputSizes(MediaRecorder.class);
                if (videoSizes != null && videoSizes.length > 0) {
                    // Pick 1280x720 if available, else largest <= 1920x1080.
                    final List<Size> sizes = Arrays.asList(videoSizes);
                    for (final Size size : sizes) {
                        if (size.getWidth() == 1280 && size.getHeight() == 720) return size;
                    }

                    final Comparator<Size> byArea = new Comparator<Size>() {
                        @Override
                        public int compare(final Size a, final Size b) {
                            return Long.signum((long) a.getWidth() * a.getHeight() -
                                    (long) b.getWidth() * b.getHeight());
                        }
                    };

                    final List<Size> filtered = new ArrayList<>();
                    for (final Size size : sizes) {
                        if (size.getWidth() <= 1920 && size.getHeight() <= 1080) filtered.add(size);
                    }
                    if (!filtered.isEmpty()) return Collections.max(filtered, byArea);
                    return Collections.min(sizes, byArea);
                }
            }
        } catch (Exception ignored) {}
        return new Size(1280, 720);
    }

    private static void openCameraAndStartRecording(final Context context, final String cameraId,
                                                     final PrintWriter out, final int maxDuration) throws Exception {
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        //noinspection MissingPermission
        manager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(final CameraDevice camera) {
                sCameraDevice = camera;
                try {
                    startPreviewAndRecording(camera, out, maxDuration);
                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Recording start failed", e);
                    out.println("Error: " + e.getMessage());
                    cleanup();
                    quitLooper();
                }
            }

            @Override
            public void onDisconnected(final CameraDevice camera) {
                out.println("Error: Camera disconnected");
                cleanup();
                quitLooper();
            }

            @Override
            public void onError(final CameraDevice camera, final int errorCode) {
                out.println("Error: Camera error " + errorCode);
                cleanup();
                quitLooper();
            }
        }, null);
    }

    private static void startPreviewAndRecording(final CameraDevice camera, final PrintWriter out,
                                                  final int maxDuration) throws CameraAccessException {
        final List<Surface> surfaces = new ArrayList<>();

        // MediaRecorder surface
        final Surface recorderSurface = sMediaRecorder.getSurface();
        surfaces.add(recorderSurface);

        // Dummy preview surface
        final android.graphics.SurfaceTexture previewTexture = new android.graphics.SurfaceTexture(1);
        final Surface dummySurface = new Surface(previewTexture);
        surfaces.add(dummySurface);

        camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(final CameraCaptureSession session) {
                sCaptureSession = session;
                try {
                    final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    builder.addTarget(recorderSurface);
                    builder.addTarget(dummySurface);
                    session.setRepeatingRequest(builder.build(), null, null);

                    sMediaRecorder.start();
                    sRecording = true;
                    out.println("Recording started: " + sOutputFile.getAbsolutePath());
                    out.println("Max duration: " + maxDuration + "s");

                    // Auto-stop after maxDuration.
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(maxDuration * 1000L);
                                if (sRecording) {
                                    Logger.logInfo(LOG_TAG, "Max duration reached, stopping");
                                    stopRecordingInternal(null);
                                }
                            } catch (InterruptedException ignored) {}
                        }
                    }).start();

                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Recording start failed", e);
                    out.println("Error: " + e.getMessage());
                    cleanup();
                    quitLooper();
                }
            }

            @Override
            public void onConfigureFailed(final CameraCaptureSession session) {
                out.println("Error: Camera configure failed");
                cleanup();
                quitLooper();
            }
        }, null);
    }

    // ── stop ───────────────────────────────────────────────

    private static void stopRecording(final TermuxApiReceiver apiReceiver, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
            @Override
            public void writeResult(final PrintWriter out) {
                stopRecordingInternal(out);
            }
        });
    }

    private static synchronized void stopRecordingInternal(final PrintWriter out) {
        if (!sRecording) {
            if (out != null) out.println("Error: No recording in progress");
            return;
        }

        try {
            sMediaRecorder.stop();
            sRecording = false;
            if (out != null) out.println("Recording saved: " + sOutputFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Stop recording error", e);
            if (out != null) out.println("Error stopping recording: " + e.getMessage());
        } finally {
            cleanup();
            quitLooper();
        }
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

    private static void quitLooper() {
        if (sLooper != null) {
            sLooper.quit();
            sLooper = null;
        }
    }
}
