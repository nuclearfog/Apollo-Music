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
import android.provider.MediaStore;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Genres.Members#EXTERNAL_CONTENT_URI}
 * and return the songs for a particular genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The Id of the genre the songs belong to.
     */
    private final Long mGenreID;

    /**
     * Constructor of <code>GenreSongHandler</code>
     *
     * @param context The {@link Context} to use.
     */
    public GenreSongLoader(Context context, Long genreId) {
        super(context);
        mGenreID = genreId;
    }

    /**
     * @param context The {@link Context} to use.
     * @param genreId The Id of the genre the songs belong to.
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeGenreSongCursor(Context context, Long genreId) {
        // Match the songs up with the genre
        String selection = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1" +
                " AND " + MediaStore.Audio.Genres.Members.TITLE + "!=''";//$NON-NLS-2$
        return context.getContentResolver().query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId), new String[]{
                        /* 0 */
                        MediaStore.Audio.Genres.Members._ID,
                        /* 1 */
                        MediaStore.Audio.Genres.Members.TITLE,
                        /* 2 */
                        "album",
                        /* 3 */
                        "artist",
                        /* 4 */
                        "duration"
                        //MediaStore.Audio.Genres.Members.DURATION
                }, selection, null, MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        Cursor mCursor = makeGenreSongCursor(getContext(), mGenreID);
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = mCursor.getLong(0);
                    // Copy the song name
                    String songName = mCursor.getString(1);
                    // Copy the album name
                    String album = mCursor.getString(2);
                    // Copy the artist name
                    String artist = mCursor.getString(3);
                    // Copy the duration
                    long duration = mCursor.getLong(4);
                    // Convert the duration into seconds
                    int durationInSecs = (int) duration / 1000;
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, durationInSecs);
                    // Add everything up
                    mSongList.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return mSongList;
    }
}