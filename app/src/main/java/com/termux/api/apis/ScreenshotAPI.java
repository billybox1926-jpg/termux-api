package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * API for taking screenshots via MediaProjection.
 * Fix for issue #456.
 * Captures the screen and saves as PNG.
 */
public class ScreenshotAPI {

    private static final String LOG_TAG = "ScreenshotAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Screenshot requires Android 5.0+"));
            return;
        }

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "capture":
                captureScreenshot(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void captureScreenshot(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String outputPath = intent.getStringExtra("output");

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                MediaProjection projection = MediaProjectionAPI.getActiveProjection();
                if (projection == null) {
                    out.println("ERROR: No active MediaProjection. Use 'MediaProjection request' first.");
                    return;
                }

                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                int density = metrics.densityDpi;

                ImageReader reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
                VirtualDisplay display = projection.createVirtualDisplay(
                        "TermuxScreenshot", width, height, density,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        reader.getSurface(), null, null);

                // Wait a frame
                Thread.sleep(500);

                Image image = reader.acquireLatestImage();
                if (image == null) {
                    display.release();
                    out.println("ERROR: Failed to capture image");
                    return;
                }

                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                image.close();
                display.release();

                // Crop to actual screen size
                Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                bitmap.recycle();

                String path = (outputPath != null && !outputPath.isEmpty()) ?
                        outputPath :
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                + "/termux_screenshot_" + System.currentTimeMillis() + ".png";

                File file = new File(path);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                cropped.recycle();

                out.println("Screenshot saved: " + file.getAbsolutePath());
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }
}
