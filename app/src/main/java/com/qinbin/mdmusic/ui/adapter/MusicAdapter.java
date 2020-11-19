package com.qinbin.mdmusic.ui.adapter;

import android.graphics.drawable.AnimationDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.qinbin.mdmusic.R;
import com.qinbin.mdmusic.ui.MediaControllerConsumer;
import com.qinbin.mdmusic.utils.MediaBeanHelper;
import com.squareup.picasso.Picasso;

import java.util.List;


/**
 * Created by Teacher on 2016/6/24.
 */
public class MusicAdapter extends RecyclerView.Adapter implements MediaControllerConsumer {


    private MediaController.TransportControls transportControls;

    @Override
    public int getItemCount() {
        return queue == null ? 0 : queue.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = View.inflate(parent.getContext(), R.layout.item_music, null);
        itemView.setOnClickListener(playOcl);

        MusicViewHolder musicViewHolder = new MusicViewHolder(itemView);
        itemView.setTag(musicViewHolder);
        return musicViewHolder;
    }

    MediaMetadata currentPlay = null;
    PlaybackState currentState = null;


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MediaSession.QueueItem queueItem = queue.get(position);
        MusicViewHolder vh = (MusicViewHolder) holder;
        vh.artistTv.setText(queueItem.getDescription().getSubtitle());

        vh.titleTv.setText(queueItem.getDescription().getTitle());

        Picasso.with(vh.artistTv.getContext()).load(queueItem.getDescription().getIconUri()).into(vh.coverIv);

        if (currentPlay == null ||  // 当前播放为null
                !queueItem.getDescription().getMediaId().equals(currentPlay.getDescription().getMediaId())
            // 当前播放的和此条目不一致

        ) {
            vh.playStateIv.setVisibility(View.GONE);
        } else {
            vh.playStateIv.setVisibility(View.VISIBLE);
            vh.playStateIv.setImageResource(R.drawable.play_anim_list);
            if (currentState != null && currentState.getState() == PlaybackState.STATE_PLAYING) {
                ((AnimationDrawable) vh.playStateIv.getDrawable()).start();
            } else {
                ((AnimationDrawable) vh.playStateIv.getDrawable()).stop();
            }

        }
    }


    @Override
    public void onObtainMediaController(MediaController mediaController) {
        mediaController.registerCallback(callback);
        transportControls = mediaController.getTransportControls();

        callback.onQueueChanged(mediaController.getQueue());
        callback.onMetadataChanged(mediaController.getMetadata());
        callback.onPlaybackStateChanged(mediaController.getPlaybackState());
    }

    @Override
    public void onReleaseMediaController(MediaController mediaController) {
        mediaController.unregisterCallback(callback);
    }

    List<MediaSession.QueueItem> queue;
    private MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            MusicAdapter.this.queue = queue;
            notifyDataSetChanged();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateUi(metadata, currentState);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updateUi(currentPlay, state);
        }
    };

    private void updateUi(MediaMetadata metadata, PlaybackState newState) {
        currentState = newState;
        if (currentPlay != null) {
            int position = MediaBeanHelper.findIndex(queue, currentPlay.getDescription().getMediaId());
            notifyItemChanged(position);
        }


        this.currentPlay = metadata;
        if (currentPlay != null) {
            int position = MediaBeanHelper.findIndex(queue, currentPlay.getDescription().getMediaId());
            notifyItemChanged(position);
        }
    }

    private class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView coverIv;
        TextView titleTv;
        TextView artistTv;
        ImageView playStateIv;
        MediaSession.QueueItem queueItem;
        String mediaId;

        public MusicViewHolder(View itemView) {
            super(itemView);
            coverIv = (ImageView) itemView.findViewById(R.id.cover);
            titleTv = (TextView) itemView.findViewById(R.id.title);
            artistTv = (TextView) itemView.findViewById(R.id.artist);
            playStateIv = (ImageView) itemView.findViewById(R.id.playStateIv);
            itemView.setTag(this);
        }
    }

    private View.OnClickListener playOcl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MusicViewHolder vh = (MusicViewHolder) v.getTag();
            String id = queue.get(vh.getAdapterPosition()).getDescription().getMediaId();
            transportControls.playFromMediaId(id, null);
        }
    };
}
