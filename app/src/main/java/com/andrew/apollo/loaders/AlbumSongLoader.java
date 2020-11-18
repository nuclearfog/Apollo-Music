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

package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Media#EXTERNAL_CONTENT_URI} and return
 * the Song for a particular album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The Id of the album the songs belong to.
     */
    private Long mAlbumID;

    /**
     * Constructor of <code>AlbumSongHandler</code>
     *
     * @param context The {@link Context} to use.
     * @param albumId The Id of the album the songs belong to.
     */
    public AlbumSongLoader(Context context, Long albumId) {
        super(context);
        mAlbumID = albumId;
    }

    /**
     * @param context The {@link Context} to use.
     * @param albumId The Id of the album the songs belong to.
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeAlbumSongCursor(Context context, Long albumId) {
        // Match the songs up with the artist
        String selection = AudioColumns.IS_MUSIC + "=1" +
                " AND " + AudioColumns.TITLE + " != ''" +
                " AND " + AudioColumns.ALBUM_ID + "=" + albumId;
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        "artist",
                        /* 3 */
                        "album",
                        /* 4 */
                        //AudioColumns.DURATION
                        "duration"
                }, selection, null, PreferenceUtils.getInstance(context).getAlbumSongSortOrder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        Cursor mCursor = makeAlbumSongCursor(getContext(), mAlbumID);
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = mCursor.getLong(0);
                    // Copy the song name
                    String songName = mCursor.getString(1);
                    // Copy the artist name
                    String artist = mCursor.getString(2);
                    // Copy the album name
                    String album = mCursor.getString(3);
                    // Copy the duration
                    long duration = mCursor.getLong(4);
                    // Make the duration label
                    int seconds = (int) (duration / 1000);
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, seconds);
                    // Add everything up
                    mSongList.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return mSongList;
    }
}