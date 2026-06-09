package com.termux.api.apis;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.IOException;

/**
 * API that enables playback of standard audio formats such as:
 * mp3, wav, flac, etc... using Android's default MediaPlayer
 */
public class MediaPlayerAPI {

    private static final String LOG_TAG = "MediaPlayerAPI";

    /**
     * Starts our MediaPlayerService
     */
    public static void onReceive(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        // Fix for issue #323: handle stdin playback by piping to temp file first
        if ("playstdin".equals(intent.getStringExtra("action"))) {
            // We need to handle this specially - read stdin in background
            ResultReturner.returnData(context, intent, new ResultReturner.WithInput() {
                @Override
                public void writeResult(java.io.PrintWriter out) {
                    try {
                        // Read all stdin data and write to temp file
                        java.io.File tempFile = java.io.File.createTempFile("termux_mplayer_", ".tmp",
                                context.getCacheDir());
                        tempFile.deleteOnExit();
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();

                        // Now start the service to play the temp file
                        Intent playerService = new Intent(context, MediaPlayerService.class);
                        playerService.setAction("play");
                        playerService.putExtra("file", tempFile.getCanonicalPath());
                        context.startService(playerService);
                        out.println("Playing stdin stream (" + tempFile.length() + " bytes)");
                    } catch (Exception e) {
                        out.println("Error: " + e.getMessage());
                    }
                }
            });
            return;
        }

        // Create intent for starting our player service and make sure
        // we retain all relevant info from this intent
        Intent playerService = new Intent(context, MediaPlayerService.class);
        playerService.setAction(intent.getAction());
        playerService.putExtras(intent.getExtras());

        context.startService(playerService);
    }

    /**
     * Converts time in seconds to a formatted time string: HH:MM:SS
     * Hours will not be included if it is 0
     */
    public static String getTimeString(int totalSeconds) {
        int hours = (totalSeconds / 3600);
        int mins = (totalSeconds % 3600) / 60;
        int secs = (totalSeconds % 60);

        String result = "";

        // only show hours if we have them
        if (hours > 0) {
            result += String.format("%02d:", hours);
        }
        result += String.format("%02d:%02d", mins, secs);
        return result;
    }


    /**
     * All media functionality exists in this background service
     */
    public static class MediaPlayerService extends Service implements MediaPlayer.OnErrorListener,
            MediaPlayer.OnCompletionListener {

        protected static MediaPlayer mediaPlayer;

        // Fix for issue #516: MediaSession for headset controls
        protected static MediaSession mediaSession;

        // do we currently have a track to play?
        protected static boolean hasTrack;

        protected static String trackName;

        private static final String LOG_TAG = "MediaPlayerService";

        /**
         * Returns our MediaPlayer instance and ensures it has all the necessary callbacks
         */
        protected MediaPlayer getMediaPlayer() {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setVolume(1.0f, 1.0f);
            }
            // Fix for issue #516: initialize MediaSession for headset controls
            if (mediaSession == null) {
                initMediaSession();
            }
            return mediaPlayer;
        }

        // Fix for issue #516: initialize MediaSession for headset button support
        protected void initMediaSession() {
            mediaSession = new MediaSession(getApplicationContext(), "TermuxMediaPlayer");
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    if (hasTrack && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        updatePlaybackState(PlaybackState.STATE_PLAYING);
                    }
                }

                @Override
                public void onPause() {
                    if (hasTrack && mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        updatePlaybackState(PlaybackState.STATE_PAUSED);
                    }
                }

                @Override
                public void onStop() {
                    if (hasTrack && mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        hasTrack = false;
                        updatePlaybackState(PlaybackState.STATE_STOPPED);
                    }
                }

                @Override
                public void onSkipToNext() {
                    // Not supported for single-track playback
                }

                @Override
                public void onSkipToPrevious() {
                    // Not supported for single-track playback
                }
            });
            updatePlaybackState(PlaybackState.STATE_NONE);
        }

        // Fix for issue #516: update playback state for MediaSession
        protected void updatePlaybackState(int state) {
            if (mediaSession == null) return;
            long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                    PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP;
            PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, 0, 1.0f);
            mediaSession.setPlaybackState(stateBuilder.build());
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
                mediaSession.setActive(true);
            } else {
                mediaSession.setActive(false);
            }
        }

        /**
         * What we received from TermuxApiReceiver but now within this service
         */
        public int onStartCommand(Intent intent, int flags, int startId) {
            Logger.logDebug(LOG_TAG, "onStartCommand");

            String command = intent.getAction();
            MediaPlayer player = getMediaPlayer();
            Context context = getApplicationContext();

            // get command handler and display result
            MediaCommandHandler handler = getMediaCommandHandler(command);
            MediaCommandResult result = handler.handle(player, context, intent);
            postMediaCommandResult(context, intent, result);

            // Stop service after single-shot commands like "info" (#877)
            if ("info".equals(command)) {
                stopSelf();
            }

            return Service.START_NOT_STICKY;
        }

        public void onDestroy() {
            Logger.logDebug(LOG_TAG, "onDestroy");

            super.onDestroy();
            cleanUpMediaPlayer();
        }

        /**
         * Releases MediaPlayer resources
         */
        protected static void cleanUpMediaPlayer() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            // Fix for issue #516: release MediaSession
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            Logger.logVerbose(LOG_TAG, "onError: what: " + what + ", extra: "  + extra);
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            hasTrack = false;
            mediaPlayer.reset();
            // Fix for issue #516: update MediaSession playback state on completion
            updatePlaybackState(PlaybackState.STATE_STOPPED);
        }

        protected static MediaCommandHandler getMediaCommandHandler(final String command) {
            switch (command == null ? "" : command) {
                case "info":
                    return infoHandler;
                case "play":
                    return playHandler;
                case "playstdin":
                    return playStdinHandler;  // Fix for issue #323
                case "pause":
                    return pauseHandler;
                case "resume":
                    return resumeHandler;
                case "stop":
                    return stopHandler;
                default:
                    return (player, context, intent) -> {
                        MediaCommandResult result = new MediaCommandResult();
                        result.error = "Unknown command: " + command;
                        return result;
                    };
            }
        }

        /**
         * Returns result of executing a media command to termux
         */
        protected static void postMediaCommandResult(final Context context, final Intent intent,
                                                     final MediaCommandResult result) {

            ResultReturner.returnData(context, intent, out -> {
                out.append(result.message).append("\n");
                if (result.error != null) {
                    out.append(result.error).append("\n");
                }
                out.flush();
                out.close();
            });
        }

        /**
         * -----
         * Media Command Handlers
         * -----
         */

        static MediaCommandHandler infoHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();

                if (hasTrack) {
                    String status = player.isPlaying() ? "Playing" : "Paused";
                    int duration = player.getDuration() / 1000;
                    int position = player.getCurrentPosition() / 1000;
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\n  \"status\": \"").append(status).append("\",");
                    sb.append("\n  \"track\": \"").append(trackName.replace("\"", "\\\"")).append("\",");
                    sb.append("\n  \"position\": ").append(position).append(",");
                    sb.append("\n  \"duration\": ").append(duration).append("\n}");
                    result.message = sb.toString();
                } else {
                    result.message = "{\n  \"status\": \"No track\"\n}";
                }
                return result;
            }
        };

        static MediaCommandHandler playHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();

                File mediaFile;
                try {
                    mediaFile = new File(intent.getStringExtra("file"));
                } catch (NullPointerException e) {
                    result.error = "No file was specified";
                    return result;
                }

                if (hasTrack) {
                    player.stop();
                    player.reset();
                    hasTrack = false;
                }

                try {
                    player.setDataSource(mediaFile.getCanonicalPath());
                    player.prepare();
                } catch (IOException e) {
                    result.error = e.getMessage();
                    return result;
                }

                player.start();
                hasTrack = true;
                trackName = mediaFile.getName();
                // Fix for issue #516: update MediaSession playback state
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                result.message = "Now Playing: " + trackName;
                return result;
            }
        };

        static MediaCommandHandler pauseHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();

                if (hasTrack) {
                    if (player.isPlaying()) {
                        player.pause();
                        result.message = "Paused playback";
                    } else {
                        result.message = "Playback already paused";
                    }
                } else {
                    result.message = "No track to pause";
                }
                return result;
            }
        };

        /**
         * Creates string showing current position in active track
         */
        protected static String getPlaybackPositionString(MediaPlayer player) {
            int duration = player.getDuration() / 1000;
            int position = player.getCurrentPosition() / 1000;
            return getTimeString(position) + " / " + getTimeString(duration);
        }

        static MediaCommandHandler resumeHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();
                if (hasTrack) {
                    String positionString = String.format("Track: %s\nCurrent Position: %s", trackName, getPlaybackPositionString(player));

                    if (player.isPlaying()) {
                        result.message = "Already playing track!\n" + positionString;
                    } else {
                        player.start();
                        result.message = "Resumed playback\n" + positionString;
                    }
                } else {
                    result.message = "No previous track to resume!\nPlease supply a new media file";
                }
                return result;
            }
        };

        static MediaCommandHandler stopHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();

                if (hasTrack) {
                    player.stop();
                    player.reset();
                    hasTrack = false;
                    result.message = "Stopped playback\nTrack cleared";
                } else {
                    result.message = "No track to stop";
                }
                return result;
            }
        };

        // Fix for issue #323: play media from stdin by piping to temp file
        static MediaCommandHandler playStdinHandler = new MediaCommandHandler() {
            @Override
            public MediaCommandResult handle(MediaPlayer player, Context context, Intent intent) {
                MediaCommandResult result = new MediaCommandResult();
                try {
                    // Read all data from stdin via the input socket
                    java.io.InputStream in = new java.io.FileInputStream(
                            intent.getStringExtra("stdin_fd"));
                    java.io.File tempFile = java.io.File.createTempFile("termux_mplayer_", ".tmp",
                            context.getCacheDir());
                    tempFile.deleteOnExit();
                    java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.close();
                    in.close();

                    if (hasTrack) {
                        player.stop();
                        player.reset();
                        hasTrack = false;
                    }

                    player.setDataSource(tempFile.getCanonicalPath());
                    player.prepare();
                    player.start();
                    hasTrack = true;
                    trackName = "stdin";
                    result.message = "Now Playing: stdin stream";
                } catch (Exception e) {
                    result.error = "Error playing stdin: " + e.getMessage();
                }
                return result;
            }
        };
    }

    /**
     * Interface for handling media commands
     */
    interface MediaCommandHandler {
        MediaCommandResult handle(MediaPlayer player, final Context context, final Intent intent);
    }

    /**
     * Simple POJO to store the result of executing a media command
     */
    static class MediaCommandResult {
        public String message = "";
        public String error;
    }
}
