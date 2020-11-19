package com.qinbin.mdmusic.engine;

import android.media.MediaMetadata;
import android.media.MediaPlayer;

import java.io.IOException;


public class LocalPlayer {
    public static class MediaPlayerListenerAdapter implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    }


    MediaPlayer mediaPlayer = new MediaPlayer();

    public long play(MediaMetadata mediaMetadata) throws  IOException{
        if(mediaPlayer==null){
            mediaPlayer = new MediaPlayer();
        }
        if(mediaMetadata ==null){
            mediaPlayer.release();
            return -1;
        }
        mediaPlayer.reset();
        mediaPlayer.setDataSource(mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION));
        mediaPlayer.prepare();
        mediaPlayer.start();
        return 0;
    }

    public long pause() {
        mediaPlayer.pause();

        return mediaPlayer.getCurrentPosition();
    }


    public long continuePlay() {

        mediaPlayer.start();
        return mediaPlayer.getCurrentPosition();
    }

    public void stop(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void seekTo(long position){
        mediaPlayer.seekTo((int) position);
    }

    public boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }
    public long getPosition(){
        return mediaPlayer.getCurrentPosition();
    }

    public void setMediaPlayerListenerAdapter(MediaPlayerListenerAdapter listener) {
        mediaPlayer.setOnCompletionListener(listener);
        mediaPlayer.setOnErrorListener(listener);
    }
}
