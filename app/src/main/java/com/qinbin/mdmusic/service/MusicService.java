package com.qinbin.mdmusic.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.qinbin.mdmusic.engine.LocalPlayer;
import com.qinbin.mdmusic.engine.MusicProvider;
import com.qinbin.mdmusic.engine.PlayMode;
import com.qinbin.mdmusic.utils.MediaBeanHelper;

import java.io.IOException;
import java.util.List;


/**
 * 服务没有调用stop 或 unbind ，直接杀死之后会可能重启
 */
public class MusicService extends Service implements IToken {
    private MediaSession mediaSession;
    private MusicProvider musicProvider;
    private LocalPlayer localPlayer;
    private NotifycationReciver notifycationReciver;


    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSession(this, "MainActivity");
        mediaSession.setCallback(sessionCallback);



        musicProvider = new MusicProvider(this);
        musicProvider.retrieveMediaAsync(providerCallback);

        localPlayer = new LocalPlayer();
        localPlayer.setMediaPlayerListenerAdapter(playerListener);
//
//        Notification.Builder builder = new Notification.Builder(this);
//        builder.setSmallIcon(R.mipmap.ic_launcher);
//        builder.setContentText("我在前台等你");
//        builder.setContentTitle("杀不死");
//
//        // 可以将程序提高为前台进程，但需要显示一个通知栏
//        // 如果第一个参数是0的话，没有通知栏，但服务会被杀死
//        startForeground(1, builder.build());


        Bundle bundle = new Bundle();
        bundle.putSerializable("PlayMode", playMode);
        mediaSession.setExtras(bundle);

        notifycationReciver = new NotifycationReciver(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notifycationReciver);
        localPlayer.stop();
        //TODO
    }

    List<MediaSession.QueueItem> queueItems;
    private List<MediaMetadata> mediaMetadatas;
    private MusicProvider.Callback providerCallback = new MusicProvider.Callback() {

        @Override
        public void onMusicReady(boolean success) {
            if (success) {
                mediaMetadatas = musicProvider.getMediaMetadatas();
                Log.e("onMusicReady", "" + mediaMetadatas.size());
                queueItems = MediaBeanHelper.convertMMToQI(musicProvider.getMediaMetadatas());
                mediaSession.setQueue(queueItems);
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return new TokenBinder(this);
    }

    MediaMetadata mediaMetadata;
    private MediaSession.Callback sessionCallback;

    {
        sessionCallback = new MediaSession.Callback() {
//        @Override
//        public void onPlay() {
//            Log.d("sessionCallback", "onPlay");
//            //Toast.makeText(MainActivity.this,"onPlay",Toast.LENGTH_SHORT).show();
//            super.onPlay();
//            //
//
//            PlaybackState.Builder builder = new PlaybackState.Builder();
//            builder.setState(PlaybackState.STATE_PLAYING, 0, 1);
//            mediaSession.setPlaybackState(builder.build());
//        }


            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                try {
                    mediaMetadata = MediaBeanHelper.findMediaMetadata(mediaMetadatas, mediaId);
                    currentPosition = MediaBeanHelper.findIndex(queueItems, mediaId);

                    long position = localPlayer.play(mediaMetadata);

                    mediaSession.setMetadata(mediaMetadata);

                    mediaSession.setPlaybackState(generatePlayBackState(position, PlaybackState.STATE_PLAYING));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onPlay() {
                long position = localPlayer.continuePlay();

                mediaSession.setPlaybackState(generatePlayBackState(position, PlaybackState.STATE_PLAYING));
            }

            @Override
            public void onPause() {
                long position = localPlayer.pause();

                mediaSession.setPlaybackState(generatePlayBackState(position, PlaybackState.STATE_PAUSED));
            }

            @Override
            public void onSkipToNext() {
                int nextPosition = currentPosition + 1;
                MediaMetadata mediaMetadata = mediaMetadatas.get(nextPosition);
                onPlayFromMediaId(mediaMetadata.getDescription().getMediaId(), null);
            }

            @Override
            public void onSkipToPrevious() {
                int nextPosition = currentPosition - 1;
                MediaMetadata mediaMetadata = mediaMetadatas.get(nextPosition);
                onPlayFromMediaId(mediaMetadata.getDescription().getMediaId(), null);
            }

            @Override
            public void onSeekTo(long pos) {
                localPlayer.seekTo(pos);
                if (localPlayer.isPlaying()) {
                    mediaSession.setPlaybackState(generatePlayBackState(pos, PlaybackState.STATE_PLAYING));
                } else {
                    mediaSession.setPlaybackState(generatePlayBackState(pos, PlaybackState.STATE_PAUSED));
                }
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                playMode = (PlayMode) extras.getSerializable("PlayMode");
                mediaSession.setExtras(extras);
            }
        };
    }

    int currentPosition = -1;
    long actions = 0b11111111111111;

    @SuppressLint("WrongConstant")
    private PlaybackState generatePlayBackState(long playPosition, int state) {
        PlaybackState.Builder builder = new PlaybackState.Builder();
        builder.setState(state, playPosition, 1);
        if (currentPosition == 0) {
            actions = actions & ~PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        } else {
            actions = actions | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (currentPosition == queueItems.size() - 1) {
            actions = actions & ~PlaybackState.ACTION_SKIP_TO_NEXT;
        } else {
            actions = actions | PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        if(localPlayer.isPlaying()){
            actions = actions & ~PlaybackState.ACTION_PLAY;
            actions = actions | PlaybackState.ACTION_PAUSE;
        }else{
            actions = actions & ~PlaybackState.ACTION_PAUSE;
            actions = actions | PlaybackState.ACTION_PLAY;
        }

        builder.setActions(actions);
        return builder.build();
    }

    PlayMode playMode = PlayMode.Random;
    private LocalPlayer.MediaPlayerListenerAdapter playerListener = new LocalPlayer.MediaPlayerListenerAdapter() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mediaSession.setPlaybackState(generatePlayBackState(-1, PlaybackState.STATE_STOPPED));

            MediaMetadata next = playMode.getNext(mediaMetadatas, mediaMetadata);
            sessionCallback.onPlayFromMediaId(next.getDescription().getMediaId(), null);

        }
    };


    @Override
    public MediaSession.Token getToken() {
        return mediaSession.getSessionToken();
    }
}
