package com.qinbin.mdmusic.ui.adapter;

import android.graphics.Color;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.qinbin.mdmusic.ui.MediaControllerConsumer;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Teacher on 2016/6/25.
 */
public class PlayingAdapter extends PagerAdapter implements MediaControllerConsumer, ViewPager.OnPageChangeListener {


    private MediaController.TransportControls transportControls;

    @Override
    public int getCount() {
        return queue == null ? 0 : queue.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (cache.isEmpty()) {
            ImageView imageview = new ImageView(container.getContext());
            imageview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageview.setBackgroundColor(Color.WHITE);
            cache.add(imageview);
        }
        ImageView imageview = cache.remove(0);
        MediaSession.QueueItem queueItem = queue.get(position);

        Picasso.with(container.getContext()).load(queueItem.getDescription().getIconUri()).into(imageview);
        container.addView(imageview);
        return imageview;

    }

    List<ImageView> cache = new ArrayList<>();

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ImageView imageView = (ImageView) object;
        cache.add(imageView);
        container.removeView(imageView);
    }

    @Override
    public void onObtainMediaController(MediaController mediaController) {
        mediaController.registerCallback(callback);
        transportControls = mediaController.getTransportControls();
        callback.onQueueChanged(mediaController.getQueue());
    }

    @Override
    public void onReleaseMediaController(MediaController mediaController) {
        mediaController.unregisterCallback(callback);
    }

    List<MediaSession.QueueItem> queue;
    private MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            PlayingAdapter.this.queue = queue;
            notifyDataSetChanged();
        }
    };

    @Override
    public void onPageSelected(int position) {
        String id = queue.get(position).getDescription().getMediaId();
        transportControls.playFromMediaId(id, null);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }


    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
