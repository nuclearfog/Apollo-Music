/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.loaders.FavoritesLoader;
import com.andrew.apollo.loaders.LastAddedLoader;
import com.andrew.apollo.loaders.PlaylistLoader;
import com.andrew.apollo.loaders.SongLoader;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.FavoritesStore.FavoriteColumns;
import com.andrew.apollo.provider.RecentStore;
import com.devspark.appmsg.AppMsg;

import java.io.File;
import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap;
    private static final long[] sEmptyList;
    public static IApolloService mService = null;
    private static int sForegroundActivities = 0;
    private static ContentValues[] mContentValuesCache = null;

    static {
        mConnectionMap = new WeakHashMap<>();
        sEmptyList = new long[0];
    }

    /* This class is never initiated */
    public MusicUtils() {
    }

    /**
     * @param context  The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static ServiceToken bindToService(Context context, ServiceConnection callback) {
        Context realActivity = ((Activity) context).getParent();
        if (realActivity == null) {
            realActivity = context;
        }
        ContextWrapper contextWrapper = new ContextWrapper(realActivity);
        contextWrapper.startService(new Intent(contextWrapper, MusicPlaybackService.class));
        ServiceBinder binder = new ServiceBinder(callback);
        if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicPlaybackService.class), binder, 0)) {
            mConnectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            return;
        }
        ContextWrapper mContextWrapper = token.mWrappedContext;
        ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
        if (mBinder == null) {
            return;
        }
        mContextWrapper.unbindService(mBinder);
        if (mConnectionMap.isEmpty()) {
            mService = null;
        }
    }

    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context   The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number    The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     * albums, songs, genres, and playlists.
     */
    public static String makeLabel(Context context, int pluralInt, int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs    The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    public static String makeTimeString(Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs -= hours * 3600;
        mins = secs / 60;
        secs -= mins * 60;

        String durationFormat = context.getResources().getString(hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        try {
            if (mService != null) {
                mService.next();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Changes to the previous track.
     * <p>
     * NOTE The AIDL isn't used here in order to properly use the previous
     * action. When the user is shuffling, because {@link
     * MusicPlaybackService #openCurrentAndNext()} is used, the user won't
     * be able to travel to the previously skipped track. To remedy this,
     * {@link MusicPlaybackService #openCurrent()} is called in {@link
     * MusicPlaybackService#prev()}. {@code #startService(Intent intent)}
     * is called here to specifically invoke the onStartCommand used by
     * {@link MusicPlaybackService}, which states if the current position
     * less than 2000 ms, start the track over, otherwise move to the
     * previously listened track.
     */
    public static void previous(Context context) {
        Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    /**
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            if (mService != null) {
                switch (mService.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;

                    case MusicPlaybackService.REPEAT_ALL:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        if (mService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                            mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        }
                        break;

                    default:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        try {
            if (mService != null) {
                switch (mService.getShuffleMode()) {
                    case MusicPlaybackService.SHUFFLE_NONE:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
                        if (mService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                            mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        }
                        break;
                    case MusicPlaybackService.SHUFFLE_NORMAL:
                    case MusicPlaybackService.SHUFFLE_AUTO:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    default:
                        break;
                }
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (mService != null) {
            try {
                return mService.isPlaying();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return false;
    }

    /**
     * @return The current shuffle mode.
     */
    public static int getShuffleMode() {
        if (mService != null) {
            try {
                return mService.getShuffleMode();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * @return The current repeat mode.
     */
    public static int getRepeatMode() {
        if (mService != null) {
            try {
                return mService.getRepeatMode();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * @return The current track name.
     */
    public static String getTrackName() {
        if (mService != null) {
            try {
                return mService.getTrackName();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @return The current artist name.
     */
    public static String getArtistName() {
        if (mService != null) {
            try {
                return mService.getArtistName();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @return The current album name.
     */
    public static String getAlbumName() {
        if (mService != null) {
            try {
                return mService.getAlbumName();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @return The current album Id.
     */
    public static long getCurrentAlbumId() {
        if (mService != null) {
            try {
                return mService.getAlbumId();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * @return The current song Id.
     */
    public static long getCurrentAudioId() {
        if (mService != null) {
            try {
                return mService.getAudioId();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * @return The current artist Id.
     */
    public static long getCurrentArtistId() {
        if (mService != null) {
            try {
                return mService.getArtistId();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * @return The audio session Id.
     */
    public static int getAudioSessionId() {
        if (mService != null) {
            try {
                return mService.getAudioSessionId();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * @return The queue.
     */
    public static long[] getQueue() {
        try {
            if (mService != null) {
                return mService.getQueue();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
        return sEmptyList;
    }

    /**
     * @param id The ID of the track to remove.
     * @return removes track from a playlist or the queue.
     */
    public static int removeTrack(long id) {
        try {
            if (mService != null) {
                return mService.removeTrack(id);
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
        return 0;
    }

    /**
     * @return The position of the current track in the queue.
     */
    public static int getQueuePosition() {
        try {
            if (mService != null) {
                return mService.getQueuePosition();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
        return 0;
    }

    /**
     * @param position The position to move the queue to
     */
    public static void setQueuePosition(int position) {
        if (mService != null) {
            try {
                mService.setQueuePosition(position);
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * @param cursor The {@link Cursor} used to perform our query.
     * @return The song list for a MIME type.
     */
    public static long[] getSongListForCursor(Cursor cursor) {
        if (cursor != null) {
            int len = cursor.getCount();
            long[] list = new long[len];
            cursor.moveToFirst();
            int columnIndex;
            try {
                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            } catch (IllegalArgumentException notaplaylist) {
                columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            }
            for (int i = 0; i < len; i++) {
                list[i] = cursor.getLong(columnIndex);
                cursor.moveToNext();
            }
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the artist.
     * @return The song list for an artist.
     */
    public static long[] getSongListForArtist(Context context, long id) {
        String[] projection = new String[]{BaseColumns._ID
        };
        String selection = AudioColumns.ARTIST_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK);
        if (cursor != null) {
            long[] mList = getSongListForCursor(cursor);
            cursor.close();
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the album.
     * @return The song list for an album.
     */
    public static long[] getSongListForAlbum(Context context, long id) {
        String[] projection = new String[]{BaseColumns._ID};
        String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            long[] mList = getSongListForCursor(cursor);
            cursor.close();
            return mList;
        }
        return sEmptyList;
    }

    /**
     * Plays songs by an artist.
     *
     * @param context  The {@link Context} to use.
     * @param artistId The artist Id.
     * @param position Specify where to start.
     */
    public static void playArtist(Context context, long artistId, int position) {
        long[] artistList = getSongListForArtist(context, artistId);
        if (artistList != null) {
            playAll(artistList, position, false);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the genre.
     * @return The song list for an genre.
     */
    public static long[] getSongListForGenre(Context context, long id) {
        String[] projection = new String[]{BaseColumns._ID};
        Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        String selection = AudioColumns.IS_MUSIC + "=1" + " AND " + MediaColumns.TITLE + "!=''";
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, null);
        if (cursor != null) {
            long[] mList = getSongListForCursor(cursor);
            cursor.close();
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param uri The source of the file
     */
    public static void playFile(Uri uri) {
        if (uri == null || mService == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-filedescriptor codepath.
        String filename;
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        try {
            mService.stop();
            mService.openFile(filename);
            mService.play();
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @param list         The list of songs to play.
     * @param position     Specify where to start.
     * @param forceShuffle True to force a shuffle, false otherwise.
     */
    public static void playAll(long[] list, int position, boolean forceShuffle) {
        if (list.length == 0 || mService == null) {
            return;
        }
        try {
            if (forceShuffle) {
                mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            } else {
                mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
            }
            long currentId = mService.getAudioId();
            int currentQueuePosition = getQueuePosition();
            if (position != -1 && currentQueuePosition == position && currentId == list[position]) {
                long[] playlist = getQueue();
                if (Arrays.equals(list, playlist)) {
                    mService.play();
                    return;
                }
            }
            if (position < 0) {
                position = 0;
            }
            mService.open(list, forceShuffle ? 0 : position);
            mService.play();
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @param list The list to enqueue.
     */
    public static void playNext(long[] list) {
        if (mService == null) {
            return;
        }
        try {
            mService.enqueue(list, MusicPlaybackService.NEXT);
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @param context The {@link Context} to use.
     */
    public static void shuffleAll(Context context) {
        Cursor cursor = SongLoader.makeSongCursor(context);
        long[] mTrackList = getSongListForCursor(cursor);
        int position = 0;
        if (mTrackList.length == 0 || mService == null) {
            return;
        }
        try {
            mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            long mCurrentId = mService.getAudioId();
            int mCurrentQueuePosition = getQueuePosition();
            if (mCurrentQueuePosition == position && mCurrentId == mTrackList[position]) {
                long[] mPlaylist = getQueue();
                if (Arrays.equals(mTrackList, mPlaylist)) {
                    mService.play();
                    return;
                }
            }
            mService.open(mTrackList, 0);
            mService.play();
            cursor.close();
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the playlist.
     * @return The ID for a playlist.
     */
    public static long getIdForPlaylist(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{BaseColumns._ID},
                PlaylistsColumns.NAME + "=?", new String[]{name}, PlaylistsColumns.NAME);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    /**
     * Returns the Id for an artist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the artist.
     * @return The ID for an artist.
     */
    public static long getIdForArtist(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{
                        BaseColumns._ID
                }, ArtistColumns.ARTIST + "=?", new String[]{
                        name
                }, ArtistColumns.ARTIST);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    public static long[] getSongListForFolder(Context paramContext, File paramFile) {
        ContentResolver contentResolver = paramContext.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String str = paramFile.toString() + '%';
        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, "is_music=1 AND title!='' AND _data LIKE ?", new String[]{str}, null);
        if (cursor != null) {
            long[] arrayOfLong = getSongListForCursor(cursor);
            cursor.close();
            return arrayOfLong;
        }
        return sEmptyList;
    }

    /**
     * Returns the ID for an album.
     *
     * @param context    The {@link Context} to use.
     * @param albumName  The name of the album.
     * @param artistName The name of the artist
     * @return The ID for an album.
     */
    public static long getIdForAlbum(Context context, String albumName, String artistName) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{BaseColumns._ID},
                AlbumColumns.ALBUM + "=? AND " + AlbumColumns.ARTIST + "=?", new String[]{
                        albumName, artistName}, AlbumColumns.ALBUM);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    /**
     * Plays songs from an album.
     *
     * @param context  The {@link Context} to use.
     * @param albumId  The album Id.
     * @param position Specify where to start.
     */
    public static void playAlbum(Context context, long albumId, int position) {
        long[] albumList = getSongListForAlbum(context, albumId);
        if (albumList != null) {
            playAll(albumList, position, false);
        }
    }

    /*  */
    public static void makeInsertItems(long[] ids, int offset, int len, int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }
        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param name    The name of the new playlist.
     * @return A new playlist ID.
     */
    public static long createPlaylist(Context context, String name) {
        if (name != null && name.length() > 0) {
            ContentResolver resolver = context.getContentResolver();
            String[] projection = new String[]{PlaylistsColumns.NAME};
            String selection = PlaylistsColumns.NAME + " = '" + name + "'";
            Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, projection, selection, null, null);
            if (cursor != null && cursor.getCount() <= 0) {
                ContentValues values = new ContentValues(1);
                values.put(PlaylistsColumns.NAME, name);
                Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                cursor.close();
                if (uri != null && uri.getLastPathSegment() != null) {
                    return Long.parseLong(uri.getLastPathSegment());
                }
            }
            return -1;
        }
        return -1;
    }

    /**
     * @param context    The {@link Context} to use.
     * @param playlistId The playlist ID.
     */
    public static void clearPlaylist(Context context, int playlistId) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        context.getContentResolver().delete(uri, null, null);
    }

    /**
     * @param activity   The {@link Context} to use.
     * @param ids        The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(Activity activity, long[] ids, long playlistid) {
        int size = ids.length;
        ContentResolver resolver = activity.getContentResolver();
        String[] projection = new String[]{"count(*)"};
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int base = cursor.getInt(0);
                int numinserted = 0;
                for (int offSet = 0; offSet < size; offSet += 1000) {
                    makeInsertItems(ids, offSet, 1000, base);
                    numinserted += resolver.bulkInsert(uri, mContentValuesCache);
                }
                String message = activity.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
                AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
            }
            cursor.close();
        }
    }

    /**
     * Removes a single track from a given playlist
     *
     * @param context    The {@link Context} to use.
     * @param id         The id of the song to remove.
     * @param playlistId The id of the playlist being removed from.
     */
    public static void removeFromPlaylist(Context context, long id, long playlistId) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, Playlists.Members.AUDIO_ID + " = ? ", new String[]{
                Long.toString(id)
        });
        String message = context.getResources().getQuantityString(
                R.plurals.NNNtracksfromplaylist, 1, 1);
        AppMsg.makeText((AppCompatActivity) context, message, AppMsg.STYLE_CONFIRM).show();
    }

    /**
     * @param context The {@link Context} to use.
     * @param list    The list to enqueue.
     */
    public static void addToQueue(Context context, long[] list) {
        if (mService == null) {
            return;
        }
        try {
            mService.enqueue(list, MusicPlaybackService.LAST);
            String message = makeLabel(context, R.plurals.NNNtrackstoqueue, list.length);
            AppMsg.makeText((AppCompatActivity) context, message, AppMsg.STYLE_CONFIRM).show();
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @param context The {@link Context} to use
     * @param id      The song ID.
     */
    public static void setRingtone(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (UnsupportedOperationException err) {
            err.printStackTrace();
            return;
        }
        String[] projection = new String[]{BaseColumns._ID, MediaColumns.DATA, MediaColumns.TITLE};
        String selection = BaseColumns._ID + "=" + id;
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                String message = context.getString(R.string.set_as_ringtone,
                        cursor.getString(2));
                AppMsg.makeText((AppCompatActivity) context, message, AppMsg.STYLE_CONFIRM).show();
            }
            cursor.close();
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The id of the album.
     * @return The song count for an album.
     */
    public static String getSongCountForAlbum(Context context, long id) {
        if (id == -1) {
            return null;
        }
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, new String[]{
                AlbumColumns.NUMBER_OF_SONGS
        }, null, null, null);
        String songCount = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                songCount = cursor.getString(0);
            }
            cursor.close();
        }
        return songCount;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The id of the album.
     * @return The release date for an album.
     */
    public static String getReleaseDateForAlbum(Context context, long id) {
        if (id == -1) {
            return null;
        }
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, new String[]{AlbumColumns.FIRST_YEAR},
                null, null, null);
        String releaseDate = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                releaseDate = cursor.getString(0);
            }
            cursor.close();
        }
        return releaseDate;
    }

    /**
     * @param from The index the item is currently at.
     * @param to   The index the item is moving to.
     */
    public static void moveQueueItem(int from, int to) {
        try {
            if (mService != null) {
                mService.moveQueueItem(from, to);
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Toggles the current song as a favorite.
     */
    public static void toggleFavorite() {
        try {
            if (mService != null) {
                mService.toggleFavorite();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * @return True if the current song is a favorite, false otherwise.
     */
    public static boolean isFavorite() {
        try {
            if (mService != null) {
                return mService.isFavorite();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
        return false;
    }

    /**
     * @param context    The {@link Context} to sue
     * @param playlistId The playlist Id
     * @return The track list for a playlist
     */
    public static long[] getSongListForPlaylist(Context context, long playlistId) {
        String[] projection = new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                projection, null, null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * Plays a user created playlist.
     *
     * @param context    The {@link Context} to use.
     * @param playlistId The playlist Id.
     */
    public static void playPlaylist(Context context, long playlistId) {
        long[] playlistList = getSongListForPlaylist(context, playlistId);
        if (playlistList != null) {
            playAll(playlistList, -1, false);
        }
    }

    /**
     * @param cursor The {@link Cursor} used to gather the list in our favorites
     *               database
     * @return The song list for the favorite playlist
     */
    public static long[] getSongListForFavoritesCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        int len = cursor.getCount();
        long[] list = new long[len];
        cursor.moveToFirst();
        int colidx = -1;
        try {
            colidx = cursor.getColumnIndexOrThrow(FavoriteColumns.ID);
        } catch (Exception err) {
            err.printStackTrace();
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(colidx);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list from our favorites database
     */
    public static long[] getSongListForFavorites(Context context) {
        Cursor cursor = FavoritesLoader.makeFavoritesCursor(context);
        if (cursor != null) {
            long[] list = getSongListForFavoritesCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * Play the songs that have been marked as favorites.
     *
     * @param context The {@link Context} to use
     */
    public static void playFavorites(Context context) {
        playAll(getSongListForFavorites(context), 0, false);
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list for the last added playlist
     */
    public static long[] getSongListForLastAdded(Context context) {
        Cursor cursor = LastAddedLoader.makeLastAddedCursor(context);
        if (cursor != null) {
            int count = cursor.getCount();
            long[] list = new long[count];
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            return list;
        }
        return sEmptyList;
    }

    /**
     * Plays the last added songs from the past two weeks.
     *
     * @param context The {@link Context} to use
     */
    public static void playLastAdded(Context context) {
        playAll(getSongListForLastAdded(context), 0, false);
    }

    /**
     * Creates a sub menu used to add items to a new playlist or an existsing
     * one.
     *
     * @param context       The {@link Context} to use.
     * @param groupId       The group Id of the menu.
     * @param subMenu       The {@link SubMenu} to add to.
     * @param showFavorites True if we should show the option to add to the
     *                      Favorites cache.
     */
    public static void makePlaylistMenu(Context context, int groupId, SubMenu subMenu, boolean showFavorites) {
        subMenu.clear();
        if (showFavorites) {
            subMenu.add(groupId, FragmentMenuItems.ADD_TO_FAVORITES, Menu.NONE, R.string.add_to_favorites);
        }
        subMenu.add(groupId, FragmentMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
        Cursor cursor = PlaylistLoader.makePlaylistCursor(context);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    Intent intent = new Intent();
                    String name = cursor.getString(1);
                    if (name != null) {
                        intent.putExtra("playlist", getIdForPlaylist(context, name));
                        subMenu.add(groupId, FragmentMenuItems.PLAYLIST_SELECTED, Menu.NONE, name).setIntent(intent);
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public static void refresh() {
        try {
            if (mService != null) {
                mService.refresh();
            }
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Queries {@link RecentStore} for the last album played by an artist
     *
     * @param context    The {@link Context} to use
     * @param artistName The artist name
     * @return The last album name played by an artist
     */
    public static String getLastAlbumForArtist(Context context, String artistName) {
        return RecentStore.getInstance(context).getAlbumName(artistName);
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    public static void seek(long position) {
        if (mService != null) {
            try {
                mService.seek(position);
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * @return The current position time of the track
     */
    public static long position() {
        if (mService != null) {
            try {
                return mService.position();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static long duration() {
        if (mService != null) {
            try {
                return mService.duration();
            } catch (RemoteException err) {
                err.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Clears the qeueue
     */
    public static void clearQueue() {
        try {
            mService.removeTracks(0, Integer.MAX_VALUE);
        } catch (RemoteException err) {
            err.printStackTrace();
        }
    }

    /**
     * Used to build and show a notification when Apollo is sent into the
     * background
     *
     * @param context The {@link Context} to use.
     */
    public static void notifyForegroundStateChanged(Context context, boolean inForeground) {
        int old = sForegroundActivities;
        if (inForeground) {
            sForegroundActivities++;
        } else {
            sForegroundActivities--;
        }

        if (old == 0 || sForegroundActivities == 0) {
            Intent intent = new Intent(context, MusicPlaybackService.class);
            intent.setAction(MusicPlaybackService.FOREGROUND_STATE_CHANGED);
            intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, sForegroundActivities != 0);
            context.startService(intent);
        }
    }

    /**
     * get the file path of the audio playback
     *
     * @return path to the music file
     */
    public static String getPlaybackFilePath() {
        try {
            if (mService != null)
                return mService.getPath();
        } catch (RemoteException err) {
            err.printStackTrace();
        }
        return null;
    }

    /**
     * Perminately deletes item(s) from the user's device
     *
     * @param context The {@link Context} to use.
     * @param list    The item(s) to delete.
     */
    public static void deleteTracks(Context context, long[] list) {
        String[] projection = new String[]{BaseColumns._ID, MediaColumns.DATA, AudioColumns.ALBUM_ID};
        StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(), null, null);
        if (c != null) {
            // Step 1: Remove selected tracks from the current playlist, as well
            // as from the album art cache
            c.moveToFirst();
            while (!c.isAfterLast()) {
                // Remove from current playlist
                long id = c.getLong(0);
                removeTrack(id);
                // Remove from the favorites playlist
                FavoritesStore.getInstance(context).removeItem(id);
                // Remove any items in the recents database
                RecentStore.getInstance(context).removeItem(c.getLong(2));
                c.moveToNext();
            }

            // Step 2: Remove selected tracks from the database
            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    selection.toString(), null);

            // Step 3: Remove files from card
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String name = c.getString(1);
                File f = new File(name);
                try { // File.delete can throw a security exception
                    if (!f.delete()) {
                        // I'm not sure if we'd ever get here (deletion would
                        // have to fail, but no exception thrown)
                        Log.e("MusicUtils", "Failed to delete file " + name);
                    }
                    c.moveToNext();
                } catch (SecurityException ex) {
                    c.moveToNext();
                }
            }
            c.close();
        }

        String message = makeLabel(context, R.plurals.NNNtracksdeleted, list.length);

        AppMsg.makeText((AppCompatActivity) context, message, AppMsg.STYLE_CONFIRM).show();
        // We deleted a number of tracks, which could affect any number of
        // things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        // Notify the lists to update
        refresh();
    }

    public static void playAllFromUserItemClick(ArrayAdapter<Song> adapter, int position) {
        if (adapter.getViewTypeCount() > 1 && position == 0) {
            return;
        }
        long[] list = MusicUtils.getSongListForAdapter(adapter);
        int pos = adapter.getViewTypeCount() > 1 ? position - 1 : position;
        if (list.length == 0) {
            pos = 0;
        }
        MusicUtils.playAll(list, pos, false);
    }

    private static long[] getSongListForAdapter(ArrayAdapter<Song> adapter) {
        if (adapter == null) {
            return sEmptyList;
        }
        long[] list;
        int count = adapter.getCount() - (adapter.getViewTypeCount() > 1 ? 1 : 0);
        list = new long[count];
        for (int i = 0; i < count; i++) {
            Song song = adapter.getItem(i);
            if (song != null) {
                list[i] = song.mSongId;
            }
        }
        return list;
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;

        /**
         * Constructor of <code>ServiceBinder</code>
         *
         * @param callback The {@link ServiceConnection} to use
         */
        public ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IApolloService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            mService = null;
        }
    }

    public static final class ServiceToken {
        public ContextWrapper mWrappedContext;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The {@link ContextWrapper} to use
         */
        public ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }
}