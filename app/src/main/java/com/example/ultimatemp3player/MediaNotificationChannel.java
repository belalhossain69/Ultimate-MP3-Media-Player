package com.example.ultimatemp3player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class MediaNotificationChannel {

    // Adding _v1 ensures a fresh start if you had previous channel bugs
    public static final String CHANNEL_ID = "ultimate_mp3_player_channel_v1";
    private static final String CHANNEL_NAME = "Ultimate MP3 Player";
    private static final String CHANNEL_DESC = "Media playback controls";

    public static void create(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW // Keeps it silent and smooth
            );
            channel.setDescription(CHANNEL_DESC);
            channel.setShowBadge(false);

            // This is important for media players to show controls on the lock screen
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}