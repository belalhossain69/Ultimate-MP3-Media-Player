package com.example.ultimatemp3player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.RenderMode;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnPlayPause, btnNext, btnPrev, btnBack5, btnForward10;
    private SeekBar seekBar;
    private TextView tvSongName;
    private ImageView imgArtwork, imgBackground;
    private VideoView videoIcon;
    private LinearLayout songContainer;
    private LottieAnimationView lottieVisualizer;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isAnimating = false;
    private AnimatorSet songAnimator;
    private int currentSongIndex = 0;
    private BroadcastReceiver musicReceiver;

    private final int[] songs = { R.raw.song_1, R.raw.song_2, R.raw.song_3, R.raw.song_4, R.raw.song_5 };
    private final String[] songNames = { "First Track", "Second Track", "Third Track", "Fourth Track", "Fifth Track" };
    private final int[] artworks = { R.drawable.artwork_1, R.drawable.artwork_2, R.drawable.artwork_3, R.drawable.artwork_4, R.drawable.artwork_5 };
    private final int[] bgImages = { R.drawable.bg_song_1, R.drawable.bg_song_2, R.drawable.bg_song_3, R.drawable.bg_song_4, R.drawable.bg_song_5 };
    private final int[] songAnimations = { R.raw.anim_1, R.raw.anim_2, R.raw.anim_3, R.raw.anim_4, R.raw.anim_5 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        setContentView(R.layout.activity_main);

        initViews();
        lottieVisualizer.setRenderMode(RenderMode.HARDWARE);
        loadSong(currentSongIndex);
        setupBroadcastReceiver();
        setupButtonListeners();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);
        tvSongName = findViewById(R.id.tvSongName);
        imgArtwork = findViewById(R.id.imgArtwork);
        imgBackground = findViewById(R.id.imgBackground);
        videoIcon = findViewById(R.id.videoIcon);
        songContainer = findViewById(R.id.songContainer);
        lottieVisualizer = findViewById(R.id.lottieVisualizer);
        btnBack5 = findViewById(R.id.btnBack5);
        btnForward10 = findViewById(R.id.btnForward10);
        videoIcon.setZOrderOnTop(true);
        videoIcon.getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    private void loadSong(int index) {
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, songs[index]);
        if (mediaPlayer == null) return;

        isAnimating = false;
        if (songAnimator != null) { songAnimator.cancel(); songAnimator = null; }

        tvSongName.setText(songNames[index]);
        imgArtwork.setImageResource(artworks[index]);
        seekBar.setMax(mediaPlayer.getDuration());


        new Handler().postDelayed(() -> {
            imgBackground.setImageResource(bgImages[index]);
            playSongAnimation(index);
            animateSongContainer();
            if (isPlaying) lottieVisualizer.playAnimation();
        }, 150);

        mediaPlayer.start();
        isPlaying = true;
        isAnimating = true;
        lottieVisualizer.setProgress(0f);
        lottieVisualizer.playAnimation();
        updateSeekBar();
        updateMusicService(true);
        mediaPlayer.setOnCompletionListener(mp -> btnNext.performClick());
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (isPlaying) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.pause);
            isPlaying = false;
            isAnimating = false;
            if (songAnimator != null) songAnimator.pause();
            videoIcon.pause();
            lottieVisualizer.pauseAnimation();
        } else {
            mediaPlayer.start();
            btnPlayPause.setImageResource(R.drawable.play_button);
            isPlaying = true;
            isAnimating = true;
            if (songAnimator != null) songAnimator.resume();
            else animateSongContainer();
            if (!videoIcon.isPlaying()) videoIcon.start();
            lottieVisualizer.playAnimation();
            updateSeekBar();
        }
        updateMusicService(isPlaying);
    }

    private void updateMusicService(boolean playingStatus) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("ACTION_UPDATE_DATA");
        intent.putExtra("TRACK_NAME", songNames[currentSongIndex]);
        intent.putExtra("ARTIST_NAME", "AliSoomroMusic");
        intent.putExtra("IMAGE_RES", bgImages[currentSongIndex]);
        intent.putExtra("IS_PLAYING", playingStatus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void setupButtonListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> switchSong(1));
        btnPrev.setOnClickListener(v -> switchSong(-1));
        btnBack5.setOnClickListener(v -> seekBy(-5000));
        btnForward10.setOnClickListener(v -> seekBy(10000));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void switchSong(int delta) {
        isAnimating = false;
        if (songAnimator != null) { songAnimator.cancel(); songAnimator = null; }
        if (mediaPlayer != null) mediaPlayer.stop();
        currentSongIndex = (currentSongIndex + delta + songs.length) % songs.length;
        loadSong(currentSongIndex);
    }

    private void seekBy(int ms) {
        if (mediaPlayer == null) return;
        int newPos = Math.max(0, Math.min(mediaPlayer.getCurrentPosition() + ms, mediaPlayer.getDuration()));
        mediaPlayer.seekTo(newPos);
        seekBar.setProgress(newPos);
    }

    private void playSongAnimation(int index) {
        if (videoIcon == null) return;
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + songAnimations[index]);
        videoIcon.setVideoURI(uri);
        videoIcon.setOnPreparedListener(mp -> { mp.setLooping(true); videoIcon.start(); });
    }

    private void animateSongContainer() {
        if (songAnimator != null) { songAnimator.cancel(); songAnimator = null; }
        songContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                songContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startMarqueeAnimation(getResources().getDisplayMetrics().widthPixels, songContainer.getWidth());
            }
        });
    }

    private void startMarqueeAnimation(float screenWidth, float containerWidth) {
        if (!isPlaying || !isAnimating) return;
        float rightX = screenWidth / 2f + containerWidth / 2f;
        float leftX = -(screenWidth / 2f + containerWidth / 2f);
        songContainer.setTranslationX(rightX);
        ObjectAnimator toCenter = ObjectAnimator.ofFloat(songContainer, "translationX", rightX, 0f).setDuration(2000);
        ObjectAnimator stayCenter = ObjectAnimator.ofFloat(songContainer, "translationX", 0f, 0f).setDuration(1500);
        ObjectAnimator toLeft = ObjectAnimator.ofFloat(songContainer, "translationX", 0f, leftX).setDuration(3500);
        songAnimator = new AnimatorSet();
        songAnimator.playSequentially(toCenter, stayCenter, toLeft);
        songAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (isPlaying && isAnimating) startMarqueeAnimation(screenWidth, containerWidth);
            }
        });
        songAnimator.start();
    }

    private void updateSeekBar() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (mediaPlayer != null && isPlaying) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        }, 0);
    }

    private void setupBroadcastReceiver() {
        musicReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case "ACTION_NEXT_TRACK": btnNext.performClick(); break;
                    case "ACTION_PREV_TRACK": btnPrev.performClick(); break;
                    case "ACTION_TOGGLE_PLAY": togglePlayPause(); break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_NEXT_TRACK");
        filter.addAction("ACTION_PREV_TRACK");
        filter.addAction("ACTION_TOGGLE_PLAY");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(musicReceiver, filter, Context.RECEIVER_EXPORTED);
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicReceiver != null) unregisterReceiver(musicReceiver);
        if (songAnimator != null) songAnimator.cancel();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        handler.removeCallbacksAndMessages(null);
    }
}