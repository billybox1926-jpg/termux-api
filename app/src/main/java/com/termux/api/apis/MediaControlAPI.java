package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API to dispatch Android media control key events such as play, pause, play‑pause, next,
 * previous and stop. It uses the system {@link AudioManager} to send a {@link Intent#ACTION_MEDIA_BUTTON}
 * broadcast with the appropriate {@link KeyEvent}. The result (success or error) is returned to
 * Termux via {@link ResultReturner}.
 */
public class MediaControlAPI {
    private static final String LOG_TAG = "MediaControlAPI";

    /** Expected extra key for the desired action. */
    public static final String EXTRA_ACTION = "action";

    /** Supported actions. */
    private static final String ACTION_PLAY = "play";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_PLAY_PAUSE = "play-pause";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_REWIND = "rewind";
    private static final String ACTION_FAST_FORWARD = "fast-forward";

    public static void onReceive(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (action == null) {
            ResultReturner.returnData(context, intent, out -> out.append("Missing 'action' extra\n"));
            return;
        }
        int keyCode;
        switch (action) {
            case ACTION_PLAY:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
                break;
            case ACTION_PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
                break;
            case ACTION_PLAY_PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case ACTION_NEXT:
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case ACTION_PREVIOUS:
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            case ACTION_STOP:
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
                break;
            case ACTION_REWIND:
                keyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
                break;
            case ACTION_FAST_FORWARD:
                keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                break;
            default:
                ResultReturner.returnData(context, intent, out -> out.append("Unknown action: " + action + "\n"));
                return;
        }
        dispatchKeyEvent(context, keyCode);
        ResultReturner.returnData(context, intent, out -> out.append("Dispatched action: " + action + "\n"));
    }

    private static void dispatchKeyEvent(Context context, int keyCode) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            Logger.logError(LOG_TAG, "AudioManager not available");
            return;
        }
        // Send down event
        KeyEvent down = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, down);
        context.sendOrderedBroadcast(downIntent, null);
        // Send up event
        KeyEvent up = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, up);
        context.sendOrderedBroadcast(upIntent, null);
    }
}
