package com.qinbin.mdmusic.engine;

import android.media.MediaMetadata;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 * Created by Teacher on 2016/6/25.
 */
public enum PlayMode implements Serializable{
    Order {
        @Override
        protected int getNext(int total, int currentIndex) {
            return currentIndex + 1 == total ?0 : currentIndex + 1;
        }
    },
    Random {
        java.util.Random random = new Random();
        @Override
        protected int getNext(int total, int currentIndex) {
            return random.nextInt(total);
        }
    },
    Repeat {
        @Override
        protected int getNext(int total, int currentIndex) {
            return currentIndex;
        }
    };

    public MediaMetadata getNext(List<MediaMetadata> mediaMetadataList, MediaMetadata current) {
        if (current == null) {
            return mediaMetadataList.get(0);
        } else {
            int currentIndex = mediaMetadataList.indexOf(current);
            return mediaMetadataList.get(getNext(mediaMetadataList.size(),currentIndex));
        }

    }

    protected   abstract int getNext(int total,int currentIndex);
}
