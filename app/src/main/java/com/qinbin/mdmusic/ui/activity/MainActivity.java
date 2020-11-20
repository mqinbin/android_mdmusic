package com.qinbin.mdmusic.ui.activity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.qinbin.mdmusic.R;
import com.qinbin.mdmusic.engine.PlayMode;
import com.qinbin.mdmusic.ui.MediaControllerConsumer;
import com.qinbin.mdmusic.ui.adapter.MusicAdapter;
import com.squareup.picasso.Picasso;


public class MainActivity extends BaseActivity implements MediaControllerConsumer, View.OnClickListener {

    // CheckBox 如果设置了点击事件，那么在onClick中，获得勾选状态的时候已经变了
    private CheckBox cb;
    private MediaController.TransportControls transportControls;
    private TextView titleTv;
    private TextView artistTv;
    private ImageView coverIv;
    private CheckBox playPauseCB;
    private NavigationView nv;
    ActionBarDrawerToggle toggle;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        RecyclerView rv = (RecyclerView) findViewById(R.id.mainRv);
        nv = (NavigationView) findViewById(R.id.mainNv);
        titleTv = (TextView) findViewById(R.id.mainTitleTv);
        artistTv = (TextView) findViewById(R.id.mainArtistTv);
        coverIv = (ImageView) findViewById(R.id.mainLittleCover);
        drawer = (DrawerLayout) findViewById(R.id.main_drawer);

        playPauseCB = (CheckBox) findViewById(R.id.mainPlayPauseCb);
        playPauseCB.setOnClickListener(playPauseOcl);

        rv.setLayoutManager(new GridLayoutManager(this, 2));
        MusicAdapter adapter = new MusicAdapter();
        rv.setAdapter(adapter);

        addMediaControllerConsumer(this);
        addMediaControllerConsumer(adapter);


        nv.setNavigationItemSelectedListener(nvListener);


        toggle = new ActionBarDrawerToggle(this, drawer, 0, 0);
        drawer.addDrawerListener(toggle);
        coverIv.setOnClickListener(this);


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        toggle.onOptionsItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
        // 让toggle显示出来
        getActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        drawer.removeDrawerListener(toggle);
    }

    @Override
    public void onObtainMediaController(MediaController mediaController) {
        transportControls = mediaController.getTransportControls();
        mediaController.registerCallback(callback);

        callback.onMetadataChanged(mediaController.getMetadata());
        callback.onPlaybackStateChanged(mediaController.getPlaybackState());
        callback.onExtrasChanged(mediaController.getExtras());
    }

    @Override
    public void onReleaseMediaController(MediaController mediaController) {
        mediaController.unregisterCallback(callback);
    }

    MediaMetadata metadata;
    private MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            MainActivity.this.metadata = metadata;
            if (metadata == null) {
                titleTv.setText(null);
                artistTv.setText(null);
                coverIv.setImageBitmap(null);
            } else {
                titleTv.setText(metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                artistTv.setText(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                Picasso.with(MainActivity.this).load(metadata.getDescription().getIconUri()).into(coverIv);

            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                playPauseCB.setChecked(true);
            } else {
                playPauseCB.setChecked(false);
            }
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            if (extras != null) {
                PlayMode mode = (PlayMode) extras.getSerializable("PlayMode");
                if (mode != null) {
                    int itemId = 0;
                    if (mode == PlayMode.Order) {
                        itemId = R.id.pm_repeat;
                    } else if (mode == PlayMode.Repeat) {
                        itemId = R.id.pm_repeat_one;
                    } else if (mode == PlayMode.Random) {
                        itemId = R.id.pm_shuffle;
                    }
                    nv.setCheckedItem(itemId);
                }

            }
        }
    };

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

    @Override
    public void onClick(View v) {
        if (metadata != null) {

            Transition transition = new ChangeBounds();
            transition.setDuration(2000);
            getWindow().setSharedElementEnterTransition(transition);
            getWindow().setSharedElementExitTransition(transition);

            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(this, coverIv, "SHARE");
            startActivity(new Intent(this, NowPlayingActivity.class), activityOptions.toBundle());
        }

    }

    private NavigationView.OnNavigationItemSelectedListener nvListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.quit) {
                exit();
            } else {
                PlayMode mode = null;
                Bundle bundle = new Bundle();

                switch (item.getItemId()) {
                    case R.id.pm_repeat:
                        bundle.putSerializable("PlayMode", PlayMode.Order);
                        break;
                    case R.id.pm_repeat_one:
                        bundle.putSerializable("PlayMode", PlayMode.Repeat);
                        break;
                    case R.id.pm_shuffle:
                        bundle.putSerializable("PlayMode", PlayMode.Random);
                        break;
                }
                transportControls.sendCustomAction("PlayMode", bundle);

            }
//           item.setChecked(true);
            return true;
        }
    };
}
