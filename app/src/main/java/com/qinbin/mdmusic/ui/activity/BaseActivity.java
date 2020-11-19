package com.qinbin.mdmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import com.qinbin.mdmusic.service.MusicService;
import com.qinbin.mdmusic.service.TokenBinder;
import com.qinbin.mdmusic.ui.MediaControllerConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Teacher on 2016/6/24.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保证服务unbind了，还在运行
        startService(new Intent(this, MusicService.class));

    }

    @Override
    protected void onStart() {
        super.onStart();
        // 需要在unbindService 对应的生命周期方法中绑定服务
        bindService(new Intent(this, MusicService.class), conn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 保证在点击了home键也能解绑服务
        unbindService(conn);
        for (MediaControllerConsumer controllerConsumer : controllerConsumers)
            controllerConsumer.onReleaseMediaController(mediaController);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 避免内存泄漏
        controllerConsumers.clear();
    }

    // 让子类把媒体控制器消费者加进来，
    public final void addMediaControllerConsumer(MediaControllerConsumer mediaControllerConsumer) {
        controllerConsumers.add(mediaControllerConsumer);
    }

    private MediaController mediaController;
    private List<MediaControllerConsumer> controllerConsumers = new ArrayList<>();

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaSession.Token token = ((TokenBinder) service).getToken();
            mediaController = new MediaController(BaseActivity.this, token);
            for (MediaControllerConsumer controllerConsumer : controllerConsumers) {
                controllerConsumer.onObtainMediaController(mediaController);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public void exit() {
        finish();
        stopService(new Intent(this, MusicService.class));
    }

}
