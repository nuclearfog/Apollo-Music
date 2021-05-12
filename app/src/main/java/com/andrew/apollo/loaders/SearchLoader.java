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
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Media;
import android.text.TextUtils;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.CursorCreator;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private String query;

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context The {@link Context} to use
     * @param query   The search query
     */
    public SearchLoader(Context context, String query) {
        super(context);
        // Create the Cursor
        this.query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        // Gather the data
        Cursor mCursor = CursorCreator.makeSearchCursor(getContext(), query);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = -1;
                    // Copy the song name
                    String songName = mCursor.getString(mCursor.getColumnIndexOrThrow(Media.TITLE));
                    // Check for a song Id
                    if (!TextUtils.isEmpty(songName)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(Media._ID));
                    }
                    // Copy the album name
                    String album = mCursor.getString(mCursor.getColumnIndexOrThrow(Albums.ALBUM));
                    // Check for a album Id
                    if (id < 0 && !TextUtils.isEmpty(album)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(Albums._ID));
                    }
                    // Copy the artist name
                    String artist = mCursor.getString(mCursor.getColumnIndexOrThrow(Artists.ARTIST));
                    // check for a artist Id
                    if (id < 0 && !TextUtils.isEmpty(artist)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(Artists._ID));
                    }
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, -1);
                    // Add everything up
                    result.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }
}