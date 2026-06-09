package com.termux.api.apis;

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.PrintWriter;

public class ClipboardAPI {

    private static final String LOG_TAG = "ClipboardAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clipData = clipboard.getPrimaryClip();

        // Support copying image files to clipboard (#705)
        String imagePath = intent.getStringExtra("image-path");
        if (imagePath != null) {
            ResultReturner.returnData(apiReceiver, intent, out -> {
                try {
                    File imageFile = new File(imagePath);
                    if (!imageFile.exists()) {
                        out.println("Error: Image file does not exist: " + imagePath);
                        return;
                    }
                    Uri imageUri = Uri.fromFile(imageFile);
                    ClipData imageClip = ClipData.newUri(context.getContentResolver(), "image", imageUri);
                    clipboard.setPrimaryClip(imageClip);
                } catch (Exception e) {
                    out.println("Error copying image to clipboard: " + e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Error copying image to clipboard", e);
                }
            });
            return;
        }

        boolean version2 = "2".equals(intent.getStringExtra("api_version"));
        if (version2) {
            boolean set = intent.getBooleanExtra("set", false);
            if (set) {
                ResultReturner.returnData(apiReceiver, intent, new ResultReturner.WithStringInput() {
                    @Override
                    protected boolean trimInput() {
                        return false;
                    }

                    @Override
                    public void writeResult(PrintWriter out) {
                        // Fix for issue #728: Clipboard set is already on a background thread via WithStringInput;
                        // no additional threading needed. goAsync() is handled by ResultReturner.
                        clipboard.setPrimaryClip(ClipData.newPlainText("", inputString));
                        // Fix for issue #332: auto-clear sensitive clipboard after timeout
                        boolean sensitive = intent.getBooleanExtra("sensitive", false);
                        if (sensitive) {
                            int timeout = intent.getIntExtra("sensitive_timeout", 30);
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    if (clipboard.getPrimaryClip() != null) {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
                                        Logger.logDebug(LOG_TAG, "Sensitive clipboard auto-cleared after " + timeout + "s");
                                    }
                                } catch (Exception e) {
                                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to auto-clear clipboard", e);
                                }
                            }, timeout * 1000L);
                        }
                    }
                });
            } else {
                ResultReturner.returnData(apiReceiver, intent, out -> {
                    if (clipData == null) {
                        out.print("");
                    } else {
                        int itemCount = clipData.getItemCount();
                        for (int i = 0; i < itemCount; i++) {
                            Item item = clipData.getItemAt(i);
                            CharSequence text = item.coerceToText(context);
                            if (!TextUtils.isEmpty(text)) {
                                out.print(text);
                            }
                        }
                        // Fix for issue #720: Add trailing newline after clipboard text so piping works correctly
                        out.println();
                    }
                });
            }
        } else {
            // Fix for issue #767: preserve empty text by using empty string fallback instead of null
            final String newClipText = intent.getStringExtra("text");
            if (newClipText != null) {
                // Set clip.
                clipboard.setPrimaryClip(ClipData.newPlainText("", newClipText));
                // Fix for issue #332: auto-clear sensitive clipboard after timeout
                boolean sensitive = intent.getBooleanExtra("sensitive", false);
                if (sensitive) {
                    int timeout = intent.getIntExtra("sensitive_timeout", 30);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            if (clipboard.getPrimaryClip() != null) {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
                                Logger.logDebug(LOG_TAG, "Sensitive clipboard auto-cleared after " + timeout + "s");
                            }
                        } catch (Exception e) {
                            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to auto-clear clipboard", e);
                        }
                    }, timeout * 1000L);
                }
            }

            ResultReturner.returnData(apiReceiver, intent, out -> {
                if (newClipText == null) {
                    // Get clip.
                    if (clipData == null) {
                        out.print("");
                    } else {
                        int itemCount = clipData.getItemCount();
                        for (int i = 0; i < itemCount; i++) {
                            Item item = clipData.getItemAt(i);
                            try {
                                // Fix for issue #748: coerceToText can return null on some devices
                                CharSequence text = item.coerceToText(context);
                                if (text != null && text.length() > 0) {
                                    out.print(text);
                                }
                            } catch (Exception e) {
                                Logger.logError(LOG_TAG, "Failed to coerce clipboard item to text: " + e.getMessage());
                            }
                        }
                        // Fix for issue #720: Add trailing newline after clipboard text so piping works correctly
                        out.println();
                    }
                }
            });
        }
    }
}

