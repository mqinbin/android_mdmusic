package com.qinbin.mdmusic.ui.activity;

import android.animation.Animator;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.qinbin.mdmusic.R;
import com.qinbin.mdmusic.engine.Lrc;
import com.qinbin.mdmusic.ui.MediaControllerConsumer;
import com.qinbin.mdmusic.ui.adapter.PlayingAdapter;
import com.qinbin.mdmusic.ui.anim.DepthPageTransformer;
import com.qinbin.mdmusic.utils.MediaBeanHelper;

import java.util.List;


/**
 * Created by Teacher on 2016/6/25.
 */
public class NowPlayingActivity extends BaseActivity implements MediaControllerConsumer, View.OnClickListener {

    private ViewPager vp;
    private PlayingAdapter adapter;

    ViewPager.OnPageChangeListener opcl;
    private CheckBox playPauseCB;
    private MediaController.TransportControls transportControls;
    private ImageView nextIv;
    private ImageView prevIv;
    private SeekBar seekBar;
    private TextView totalTv;
    private TextView pastTv;
    private TextView lrcTv;
    private ViewGroup bottomControls;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        vp = (ViewPager) findViewById(R.id.playing_vp);
        playPauseCB = (CheckBox) findViewById(R.id.playPauseCb);
        nextIv = (ImageView) findViewById(R.id.nextIv);
        prevIv = (ImageView) findViewById(R.id.prevIv);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        totalTv = (TextView) findViewById(R.id.totalTv);
        pastTv = (TextView) findViewById(R.id.pastTv);
        lrcTv = (TextView) findViewById(R.id.lrcTv);
        bottomControls = (ViewGroup) findViewById(R.id.bottomControls);

        seekBar.setOnSeekBarChangeListener(seekBarListener);

        playPauseCB.setOnClickListener(playPauseOcl);
        nextIv.setOnClickListener(this);
        prevIv.setOnClickListener(this);

        adapter = new PlayingAdapter();
        opcl = adapter;
        vp.setAdapter(adapter);
        vp.addOnPageChangeListener(opcl);

        vp.setPageTransformer(true, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {

            }
        });
        vp.setPageTransformer(true, new DepthPageTransformer());
        // 注意顺序
        addMediaControllerConsumer(adapter);
        addMediaControllerConsumer(this);

        vp.setTransitionName("SHARE");
        Transition transition = new ChangeBounds();
        transition.setDuration(2000);
        getWindow().setSharedElementEnterTransition(transition);
        getWindow().setSharedElementExitTransition(transition);


        getWindow().getSharedElementEnterTransition().addListener(transitionListener);
    }

    private Transition.TransitionListener transitionListener = new Transition.TransitionListener() {
        @Override
        public void onTransitionStart(Transition transition) {
            bottomControls.setVisibility(View.GONE);
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            bottomControls.setVisibility(View.VISIBLE);

            Animator animator = ViewAnimationUtils.createCircularReveal(
                    bottomControls,
                    bottomControls.getWidth() / 2, 0,
                    0, (float) Math.hypot(bottomControls.getWidth() / 2, bottomControls.getHeight())
            );
            animator.setDuration(3000);
            animator.start();
        }

        @Override
        public void onTransitionCancel(Transition transition) {
        }

        @Override
        public void onTransitionPause(Transition transition) {
        }

        @Override
        public void onTransitionResume(Transition transition) {
        }
    };

    @Override
    public void onObtainMediaController(MediaController mediaController) {
        transportControls = mediaController.getTransportControls();
        mediaController.registerCallback(callback);

        callback.onPlaybackStateChanged(mediaController.getPlaybackState());
        // 注意顺序
        callback.onQueueChanged(mediaController.getQueue());
        callback.onMetadataChanged(mediaController.getMetadata());
    }

    @Override
    public void onReleaseMediaController(MediaController mediaController) {
        mediaController.unregisterCallback(callback);
    }

    static Handler handler = new Handler();
    PlaybackState state;
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            // 防止任务重复执行
            handler.removeCallbacks(this);
            // 判断状态 和是否在触摸SeekBar
            if (state != null && !isTouchingSeekBar) {
//              当时的进度  state.getPosition();
//              播放速度  state.getPlaybackSpeed();
//              设置了进度的时间  state.getLastPositionUpdateTime();
//                          System.currentTimeMillis();    时钟的时间，可以被用户修改的，不应该用这个
//              当前时间    SystemClock.elapsedRealtime(); 开机时间
//              （当前时间 - 设置了进度的时间） * 播放速度 + 当时的进度
                int progress = (int) ((SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime()) * state.getPlaybackSpeed() + state.getPosition());
                seekBar.setProgress(progress);
            }
            handler.postDelayed(this, 100);
        }
    };

    List<MediaSession.QueueItem> queue;
    MediaMetadata metadata;
    private MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            NowPlayingActivity.this.queue = queue;
//            if(metadata!= null && queue!= null ) {
//                int pageIndex = MediaBeanHelper.findIndex(queue, metadata.getDescription().getMediaId());
//                vp.setCurrentItem(pageIndex);
//            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            NowPlayingActivity.this.metadata = metadata;
            if (metadata != null && queue != null) {
                int pageIndex = MediaBeanHelper.findIndex(queue, metadata.getDescription().getMediaId());
                vp.removeOnPageChangeListener(opcl);
                vp.setCurrentItem(pageIndex);
                vp.addOnPageChangeListener(opcl);
            }

            if (metadata == null) {
                seekBar.setMax(0);
                totalTv.setVisibility(View.GONE);
                pastTv.setVisibility(View.GONE);


            } else {
                int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                seekBar.setMax(duration);
                totalTv.setVisibility(View.VISIBLE);
                pastTv.setVisibility(View.VISIBLE);

                totalTv.setText(String.format("%02d:%02d", duration / 1000 / 60, duration / 1000 % 60));
            }

            if (metadata == null) {
                lrc = null;
            } else {
                lrc = Lrc.Factory.create(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION));
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            NowPlayingActivity.this.state = state;
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                playPauseCB.setChecked(true);
                handler.post(updateSeekBarRunnable);
            } else {
                playPauseCB.setChecked(false);
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            nextIv.setEnabled(hasActions(state.getActions(), PlaybackState.ACTION_SKIP_TO_NEXT));
            prevIv.setEnabled(hasActions(state.getActions(), PlaybackState.ACTION_SKIP_TO_PREVIOUS));
        }
    };

    private boolean hasActions(long actions, long action) {
//        return  (actions & action ) == action;
        return (actions & action) > 0;
    }

    boolean isTouchingSeekBar;
    private View.OnClickListener playPauseOcl = new View.OnClickListener() {
        @Override
        public void onClick(View v2) {
            // 假定 playPauseCB能够如实的反应音乐是否在播放，

            if (playPauseCB.isChecked()) {// 点击之前是没有勾选的
                //播放
                transportControls.play();
            } else {
                // 暂停
                transportControls.pause();
            }
        }
    };

    Lrc lastLrc;
    Lrc lrc;
    Lrc.LrcLine lastLrcLine;
    Lrc.LrcLine lrcLine;


    private void updateLrcTv(int progress) {

        if (lrc == null) {
            if (lastLrc != lrc) {
                lrcTv.setText(null);
            } else {
                return;
            }

        }
        lastLrc = lrc;

        if (lrc != null) {
            lrcLine = lrc.getLrcLine(progress);
            if (lastLrcLine == lrcLine) {
                return;
            } else {
                if (lrcLine == null) {
                    lrcTv.setText(null);
                } else {
                    lrcTv.setText(lrcLine.getContent());
                }
            }
            lastLrcLine = lrcLine;
        }
    }

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            pastTv.setText(String.format("%02d:%02d", progress / 1000 / 60, progress / 1000 % 60));
            if (fromUser) {
                transportControls.seekTo(progress);
            }

            updateLrcTv(progress);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isTouchingSeekBar = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isTouchingSeekBar = false;
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nextIv:
                transportControls.skipToNext();
                break;
            case R.id.prevIv:
                transportControls.skipToPrevious();
                break;
        }
    }
}
