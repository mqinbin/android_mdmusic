package com.qinbin.mdmusic.engine;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.qinbin.mdmusic.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Created by QinBin on 2015/6/17.
 */
public class MusicProvider {
    static Pattern commonAudio = Pattern.compile(".*\\.((aif)|(aiff)|(au)|(mp1)|(mp2)|(mp3)|(m4a)|(asx)|(m3u)|(pls)|(mlv)|(mpe)|(mpeg)|(mpg)|(mpv)|(mpa)|(ra/)|(rm)|(ram)|(snd)|(wav)|(voc)|(ins)|(cda))", Pattern.CASE_INSENSITIVE);

    public static interface Callback {
        void onMusicReady(boolean success);
    }

    private Context context;
    private List<MediaMetadata> mediaMetadatas;
    private File coverDir;
    private MediaMetadataRetriever mmr;
    private ContentResolver resolver;

    private enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    State current = State.NON_INITIALIZED;

    public MusicProvider(Context context) {
        this.context = context;
        mediaMetadatas = new ArrayList<MediaMetadata>();
        coverDir = context.getCacheDir();

    }


    public List<MediaMetadata> getMediaMetadatas() {
        if (current != State.INITIALIZED) {
            throw new IllegalStateException("have not ready");
        }
        return mediaMetadatas;
    }

    public boolean retrieveMediaAsync(Callback callback) {
        if (current == State.INITIALIZING) {
            return false;
        }

        new DbQueryAsyncTask(callback).execute();
        return true;
    }

    int unFinishedRefreshFileCount;

    public boolean refreshMediaAsync(final Callback callback) {
        if (current == State.INITIALIZING) {
            return false;
        }
        current = State.INITIALIZING;
        Log.w("refreshMusics", "3");

        new Thread() {
            private MediaScannerConnection.OnScanCompletedListener scanCompletedListener = new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    unFinishedRefreshFileCount--;
                    Log.w("refreshMusics", "ooxx");
                    if (unFinishedRefreshFileCount == 0) {
                        Log.w("refreshMusics", "5");

                        new DbQueryAsyncTask(callback).execute();
                    }
                }
            };

            @Override
            public void run() {
                String[] old = new String[mediaMetadatas.size()];
                for (int i = 0; i < old.length; i++) {
                    old[i] = mediaMetadatas.get(i).getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION);
                }
                Log.w("old", "" + old[0]);
                Log.w("refreshMusics", "4.1");
                List<String> sdmusic = new ArrayList<>();
                screenOutMusic(Environment.getExternalStorageDirectory().getAbsoluteFile(), sdmusic);
                Log.w("refreshMusics", "4.2");
                mediaMetadatas.clear();
                Log.e("refreshMediaAsnc", " mediaMetadatas.size()" + mediaMetadatas.size());

                HashSet<String> diff = getDiff(Arrays.asList(old), sdmusic);
                unFinishedRefreshFileCount = diff.size();
                Log.w("refreshMusics", "4.3");
                String[] mimeType = new String[diff.size()];
                for (int i = 0; i < unFinishedRefreshFileCount; i++) {
                    mimeType[i] = "audio/*";
                }
                Log.w("refreshMusics", "4.4");
                MediaScannerConnection.scanFile(context, diff.toArray(new String[diff.size()]), mimeType, scanCompletedListener);
                Log.w("refreshMusics", "4.5");

            }
        }.start();
        return true;
    }

    public static <E> HashSet<E> getDiff(Collection<E> list1, Collection<E> list2) {
        HashSet result = new HashSet();
        result.addAll(list1);
        result.addAll(list2);
        HashSet same = new HashSet();
        same.addAll(list1);
        same.retainAll(list2);

        result.removeAll(same);

        return result;
    }


    private static void screenOutMusic(File path, List<String> list) {
        if (path.isDirectory()) {
            File[] paths = path.listFiles();
            for (File file : paths) {
                screenOutMusic(file, list);
            }
        } else {
            if (commonAudio.matcher(path.getAbsolutePath()).matches()) {
                list.add(path.getAbsolutePath());
            }
        }
    }


    private class DbQueryAsyncTask extends AsyncTask<Void, Void, Void> {
        private Callback callback;

        public DbQueryAsyncTask(Callback callback) {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            current = State.INITIALIZING;
            mmr = new MediaMetadataRetriever();
            resolver = context.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            MediaMetadata.Builder builder = new MediaMetadata.Builder();
            Cursor cursor = resolver.query(uri, null, " " + MediaStore.Audio.Media.DURATION + " > ? ", new String[]{"5000"}, null);
            int i = 0;
            while (cursor.moveToNext()) {
                i++;
                mediaMetadatas.add(getMediaMetadataFromCursor(cursor, builder));
            }
            Log.d("doInBackground", "cursor_count" + i);
            cursor.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            resolver = null;
            mmr.release();
            current = State.INITIALIZED;
            callback.onMusicReady(true);

        }
    }

    ;

    private MediaMetadata getMediaMetadataFromCursor(Cursor cursor, MediaMetadata.Builder builder) {
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        builder.putString(MediaMetadata.METADATA_KEY_ALBUM, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
        builder.putString(MediaMetadata.METADATA_KEY_ARTIST, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
        builder.putString(MediaMetadata.METADATA_KEY_DATE, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)));
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

        File target = new File(coverDir, id);

        builder.putLong(MediaMetadata.METADATA_KEY_DURATION, cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
        builder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id);
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, path);


        String coverUri = null;
        if (!target.exists()) {//|| target.lastModified() < System.currentTimeMillis() - 1000 * 60 * 60 *24 ){
            if (getMusicCover(mmr, path, target)) {
                coverUri = Uri.fromFile(target).toString();
                builder.putString(MediaMetadata.METADATA_KEY_ART_URI, Uri.fromFile(target).toString());
            } else {
                try {
                    target.createNewFile();
                    coverUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.default_corver).toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (target.length() > 0) {
                coverUri = Uri.fromFile(target).toString();
            } else {
                coverUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.default_corver).toString();
            }

        }
        builder.putString(MediaMetadata.METADATA_KEY_ART_URI, coverUri);

        return builder.build();
    }


    public MediaMetadata queryMediaMetadata(String mediaId) {
        if (mediaMetadatas == null || mediaMetadatas.size() == 0) {
            return null;
        } else {
            for (MediaMetadata mediaMetadata : mediaMetadatas) {
                if (mediaMetadata.getDescription().getMediaId().equals(mediaId)) {
                    return mediaMetadata;
                }
            }
        }
        return null;
    }

    ;

    public static boolean getMusicCover(MediaMetadataRetriever mmr, String musicPath, File target) {
        try {
            mmr.setDataSource(musicPath);
            byte[] data = mmr.getEmbeddedPicture();
            if (data == null) {
                return false;
            } else {
                FileOutputStream fos = new FileOutputStream(target);
                fos.write(data);
                fos.close();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }


}
