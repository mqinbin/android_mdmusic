package com.qinbin.mdmusic.ui;

import android.media.session.MediaController;

/**
 * Created by Teacher on 2016/6/24.
 */
public interface MediaControllerConsumer {
    void onObtainMediaController(MediaController mediaController);
    void onReleaseMediaController(MediaController mediaController);
}
