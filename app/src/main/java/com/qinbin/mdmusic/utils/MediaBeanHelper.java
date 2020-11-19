package com.qinbin.mdmusic.utils;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Teacher on 2015/6/28.
 */
public class MediaBeanHelper {

    /**
     * 完成类之间的转化
     */
    public static List<MediaSession.QueueItem> convertMMToQI(List<MediaMetadata> mediaMetadatas){
        List<MediaSession.QueueItem> result = new ArrayList<>();
        Log.d("mediaMetadatas","" +mediaMetadatas.size());
        for (MediaMetadata mediaMetadata : mediaMetadatas) {

            MediaDescription description = mediaMetadata.getDescription();
            Log.d("MediaDescription","" +description);
            Log.d("convertMMToQI" ,"id:" + description.getMediaId());
            long id = Long.parseLong(description.getMediaId());
            MediaSession.QueueItem queueItem = new MediaSession.QueueItem(description,id);
            result.add(queueItem);
        }
        return result;
    }

    /**
     * 找列表中符合id条件的MediaMetadata
     */
    public static MediaMetadata findMediaMetadata(List<MediaMetadata> mediaMetadatas , String id){
        for (MediaMetadata mediaMetadata : mediaMetadatas) {
            if(mediaMetadata.getDescription().getMediaId().equals(id)){
                return mediaMetadata;
            }
        }
        return null;
    }

    public static int findIndex(List<MediaSession.QueueItem> queueItems, String id){
        for (int i = 0; i < queueItems.size(); i++) {
            if(queueItems.get(i).getDescription().getMediaId().equals(id)){
                return i;
            }
        }
        return -1;
    }
}
