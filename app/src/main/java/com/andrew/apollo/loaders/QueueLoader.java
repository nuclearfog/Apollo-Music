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

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to return the current playlist or queue.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * Constructor of <code>QueueLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public QueueLoader(Context context) {
        super(context);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makeQueueCursor(Context context) {
        return new NowPlayingCursor(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        NowPlayingCursor mCursor = new NowPlayingCursor(getContext());
        // Gather the data
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
                // Convert the duration into seconds
                int durationInSecs = (int) duration / 1000;
                // Create a new song
                Song song = new Song(id, songName, artist, album, durationInSecs);
                // Add everything up
                mSongList.add(song);
            } while (mCursor.moveToNext());
        }
        mCursor.close();
        return mSongList;
    }
}