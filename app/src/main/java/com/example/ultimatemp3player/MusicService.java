package com.example.ultimatemp3player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MusicService extends Service {

    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;
    private boolean isPlaying = false;
    private String currentTitle = "Loading...";
    private String currentArtist = "AliSoomroMusic";
    private int currentImageRes = R.drawable.bg_song_1;
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        MediaNotificationChannel.create(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UltimateMP3Player::MusicWakeLock");
        wakeLock.acquire();

        mediaSession = new MediaSessionCompat(this, "MusicSession");
        mediaSession.setActive(true);
        updatePlaybackState();
        updateNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_UPDATE_DATA":
                    currentTitle = intent.getStringExtra("TRACK_NAME");
                    currentArtist = intent.getStringExtra("ARTIST_NAME");
                    currentImageRes = intent.getIntExtra("IMAGE_RES", R.drawable.bg_song_1);
                    isPlaying = intent.getBooleanExtra("IS_PLAYING", false);
                    updatePlaybackState();
                    updateNotification();
                    break;
                case "ACTION_PLAY_PAUSE": sendBroadcast(new Intent("ACTION_TOGGLE_PLAY")); break;
                case "ACTION_NEXT": sendBroadcast(new Intent("ACTION_NEXT_TRACK")); break;
                case "ACTION_PREVIOUS": sendBroadcast(new Intent("ACTION_PREV_TRACK")); break;
                case "ACTION_STOP": stopForeground(true); stopSelf(); break;
            }
        }
        return START_STICKY;
    }

    private void updatePlaybackState() {
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, 0, isPlaying ? 1f : 0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build());
    }

    private void updateNotification() {
        // Run heavy bitmap decoding on background thread to keep UI smooth
        new Thread(() -> {
            Bitmap albumArt;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Decodes at half size for speed
                albumArt = BitmapFactory.decodeResource(getResources(), currentImageRes, options);
            } catch (Exception e) {
                albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.bg_song_1);
            }

            final Bitmap finalBitmap = albumArt;
            new Handler(Looper.getMainLooper()).post(() -> buildAndSendNotification(finalBitmap));
        }).start();
    }

    private void buildAndSendNotification(Bitmap albumArt) {
        if (mediaSession == null) return;

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt).build();
        mediaSession.setMetadata(metadata);

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
        int playIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        Notification notification = new NotificationCompat.Builder(this, MediaNotificationChannel.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_music)
                .setLargeIcon(albumArt)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setSilent(true)
                .setStyle(new MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "Prev", getActionIntent("ACTION_PREVIOUS"))
                .addAction(playIcon, isPlaying ? "Pause" : "Play", getActionIntent("ACTION_PLAY_PAUSE"))
                .addAction(android.R.drawable.ic_media_next, "Next", getActionIntent("ACTION_NEXT"))
                .addAction(R.drawable.ic_close, "Stop", getActionIntent("ACTION_STOP")).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
