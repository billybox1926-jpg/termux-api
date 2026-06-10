package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.List;

/**
 * API for MPRIS (Media Player Remote Interfacing Specification) bridge.
 * Fix for issue #531.
 * Exposes media session metadata and controls via JSON.
 * Requires Android 5.0+ (API 21) for MediaSessionManager.
 */
public class MprisAPI {

    private static final String LOG_TAG = "MprisAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: MPRIS requires Android 5.0+"));
            return;
        }

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listPlayers(apiReceiver, context, intent);
                break;
            case "status":
                playerStatus(apiReceiver, context, intent);
                break;
            case "control":
                controlPlayer(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    /**
     * List active media sessions (MPRIS-compatible player list).
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void listPlayers(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                MediaSessionManager msm = (MediaSessionManager)
                        context.getSystemService(Context.MEDIA_SESSION_SERVICE);
                if (msm == null) {
                    out.beginObject().name("error").value("MediaSessionManager not available").endObject();
                    return;
                }

                List<MediaController> controllers = msm.getActiveSessions(
                        new android.content.ComponentName(context, NotificationListAPI.NotificationService.class));

                out.beginArray();
                for (MediaController mc : controllers) {
                    out.beginObject();
                    out.name("package").value(mc.getPackageName());
                    out.name("session_token").value(mc.getSessionToken().toString());
                    PlaybackState ps = mc.getPlaybackState();
                    if (ps != null) {
                        out.name("playback_state").value(playbackStateToString(ps.getState()));
                    }
                    MediaMetadata mm = mc.getMetadata();
                    if (mm != null) {
                        out.name("title").value(mm.getString(MediaMetadata.METADATA_KEY_TITLE));
                        out.name("artist").value(mm.getString(MediaMetadata.METADATA_KEY_ARTIST));
                        out.name("album").value(mm.getString(MediaMetadata.METADATA_KEY_ALBUM));
                        out.name("duration").value(mm.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    }
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    /**
     * Get detailed status of the active media player.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void playerStatus(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                MediaController controller = getActiveController(context);
                if (controller == null) {
                    out.beginObject().name("error").value("No active media session").endObject();
                    return;
                }

                out.beginObject();
                out.name("package").value(controller.getPackageName());

                PlaybackState ps = controller.getPlaybackState();
                if (ps != null) {
                    out.name("state").value(playbackStateToString(ps.getState()));
                    out.name("position").value(ps.getPosition());
                    out.name("playback_speed").value(ps.getPlaybackSpeed());
                }

                MediaMetadata mm = controller.getMetadata();
                if (mm != null) {
                    out.name("title").value(mm.getString(MediaMetadata.METADATA_KEY_TITLE));
                    out.name("artist").value(mm.getString(MediaMetadata.METADATA_KEY_ARTIST));
                    out.name("album").value(mm.getString(MediaMetadata.METADATA_KEY_ALBUM));
                    out.name("duration").value(mm.getLong(MediaMetadata.METADATA_KEY_DURATION));
                }

                out.endObject();
            }
        });
    }

    /**
     * Send a control command to the active media player.
     * Required extra: command (play, pause, play-pause, stop, next, previous)
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void controlPlayer(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String command = intent.getStringExtra("command");
        if (command == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'command' extra"));
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, out -> {
            MediaController controller = getActiveController(context);
            if (controller == null) {
                out.println("ERROR: No active media session");
                return;
            }

            MediaController.TransportControls tc = controller.getTransportControls();
            if (tc == null) {
                out.println("ERROR: Transport controls not available");
                return;
            }

            switch (command) {
                case "play":
                    tc.play();
                    break;
                case "pause":
                    tc.pause();
                    break;
                case "play-pause":
                    tc.play();
                    break;
                case "stop":
                    tc.stop();
                    break;
                case "next":
                    tc.skipToNext();
                    break;
                case "previous":
                    tc.skipToPrevious();
                    break;
                default:
                    out.println("ERROR: Unknown command: " + command);
                    return;
            }
            out.println("Sent: " + command);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static MediaController getActiveController(Context context) {
        MediaSessionManager msm = (MediaSessionManager)
                context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (msm == null) return null;

        List<MediaController> controllers = msm.getActiveSessions(
                new android.content.ComponentName(context, NotificationListAPI.NotificationService.class));

        if (controllers.isEmpty()) return null;
        return controllers.get(0);
    }

    private static String playbackStateToString(int state) {
        switch (state) {
            case PlaybackState.STATE_PLAYING: return "playing";
            case PlaybackState.STATE_PAUSED: return "paused";
            case PlaybackState.STATE_STOPPED: return "stopped";
            case PlaybackState.STATE_BUFFERING: return "buffering";
            default: return "unknown";
        }
    }
}
