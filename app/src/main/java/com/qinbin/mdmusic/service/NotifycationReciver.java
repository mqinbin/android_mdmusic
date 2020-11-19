package com.qinbin.mdmusic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.qinbin.mdmusic.R;
import com.qinbin.mdmusic.ui.activity.NowPlayingActivity;


/**
 * Created by QinBin on 2015/6/21.
 */
public class NotifycationReciver extends BroadcastReceiver {
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PREV = "PREV";

    public static final int NOTIFICATION_ID = 99;
    public static final int REQ_CODE = 99;

    private PendingIntent prevPI;
    private PendingIntent nextPI;
    private PendingIntent playPI;
    private PendingIntent pausePI;
    private IntentFilter intentFilter;
    private final NotificationManager nm;


    private MusicService musicService;
    private MediaController mediaController;
    private MediaController.TransportControls transportControls;


    public PlaybackState playbackState;
    public MediaMetadata nowPlayingMedadata;

    public NotifycationReciver(MusicService musicService) {
        this.musicService = musicService;

        nm = (NotificationManager) musicService.getSystemService(Context.NOTIFICATION_SERVICE);

        mediaController = new MediaController(musicService, musicService.getToken());
        transportControls = mediaController.getTransportControls();


        mediaController.registerCallback(controllerCallback);


        String packageName = musicService.getPackageName();
        prevPI = PendingIntent.getBroadcast(musicService, REQ_CODE, new Intent(ACTION_PREV).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        nextPI = PendingIntent.getBroadcast(musicService, REQ_CODE, new Intent(ACTION_NEXT).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        playPI = PendingIntent.getBroadcast(musicService, REQ_CODE, new Intent(ACTION_PLAY).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        pausePI = PendingIntent.getBroadcast(musicService, REQ_CODE, new Intent(ACTION_PAUSE).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);

        intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PREV);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);

        musicService.registerReceiver(this, intentFilter);

        playbackState = mediaController.getPlaybackState();
        nowPlayingMedadata = mediaController.getMetadata();
        startOrStopNotification();

    }

    boolean registed = false;

    private void startOrStopNotification() {
        if (playbackState != null && nowPlayingMedadata != null
                && (playbackState.getState() == PlaybackState.STATE_PLAYING || playbackState.getState() == PlaybackState.STATE_PAUSED)
        ) {
            startNotification();
        } else {
            stopNotification();
        }
    }

    private void startNotification() {
        if (!registed) {
            musicService.registerReceiver(this, intentFilter);
            registed = true;
        }
        musicService.startForeground(NOTIFICATION_ID, createNotification());

    }

    private void stopNotification() {
        if (registed) {
            musicService.unregisterReceiver(this);
            registed = false;
        }
        nm.cancel(NOTIFICATION_ID);
        musicService.stopForeground(false);

    }

    private Notification createNotification() {


        String uriStr = nowPlayingMedadata.getString(MediaMetadata.METADATA_KEY_ART_URI);
        Bitmap cover = null;
        if (uriStr.startsWith("android.resource")) {
            cover = BitmapFactory.decodeResource(musicService.getResources(), Integer.parseInt(uriStr.substring(uriStr.lastIndexOf("/") + 1)));
        } else {
            Uri uri = Uri.parse(uriStr);
            cover = BitmapFactory.decodeFile(uri.getPath());
        }
        Notification.Builder notificationBuilder = new Notification.Builder(musicService);
        int playPauseActionIndex = 0;
        long actions = mediaController.getPlaybackState().getActions();
        Log.d("createNotification", Long.toBinaryString(actions));
        if ((actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) > 0) {
            notificationBuilder.addAction(R.drawable.previous_enable, "prev", prevPI);
            playPauseActionIndex = 1;
        }
        if ((actions & PlaybackState.ACTION_PAUSE) > 0) {
            notificationBuilder.addAction(R.drawable.pause_vector, "pause", pausePI);
        } else {
            notificationBuilder.addAction(R.drawable.play_vector, "play", playPI);
        }
        if ((actions & PlaybackState.ACTION_SKIP_TO_NEXT) > 0) {
            notificationBuilder.addAction(R.drawable.next, "next", nextPI);
        }


        MediaSession.Token sessionToken = musicService.getToken();
        notificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(sessionToken)
                        .setShowActionsInCompactView(playPauseActionIndex))
                .setContentInfo(nowPlayingMedadata.getDescription().getTitle())
                .setContentText(nowPlayingMedadata.getDescription().getSubtitle())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(createContentPanding())
                .setLargeIcon(cover);


        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String chanelName = "one";
            String chanelId = "1";
            NotificationChannel channel = new NotificationChannel(chanelId, chanelName, NotificationManager.IMPORTANCE_NONE);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) musicService.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            notificationBuilder.setChannelId(chanelId);
        }

        Notification notification = notificationBuilder.build();

        return notification;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_PLAY.equals(action)) {
            transportControls.play();
        } else if (ACTION_PAUSE.equals(action)) {
            transportControls.pause();
        } else if (ACTION_PREV.equals(action)) {
            transportControls.skipToPrevious();
        } else if (ACTION_NEXT.equals(action)) {
            transportControls.skipToNext();
        }

    }

    MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            playbackState = state;
            startOrStopNotification();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            nowPlayingMedadata = metadata;
            startOrStopNotification();
        }
    };

    private PendingIntent createContentPanding() {
        Intent intent = new Intent(musicService, NowPlayingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra("MediaMetadata", nowPlayingMedadata);
        return PendingIntent.getActivity(musicService, REQ_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}
