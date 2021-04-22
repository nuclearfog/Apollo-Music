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
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.andrew.apollo.model.Song;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private static final String[] PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            "data1", "data2"
    };

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
        Cursor mCursor = makeSearchCursor(query);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = -1;
                    // Copy the song name
                    String songName = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    // Check for a song Id
                    if (!TextUtils.isEmpty(songName)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    }
                    // Copy the album name
                    String album = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
                    // Check for a album Id
                    if (id < 0 && !TextUtils.isEmpty(album)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
                    }
                    // Copy the artist name
                    String artist = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                    // check for a artist Id
                    if (id < 0 && !TextUtils.isEmpty(artist)) {
                        id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
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

    /**
     * * @param context The {@link Context} to use.
     *
     * @param query The user's query.
     * @return The {@link Cursor} used to perform the search.
     */
    private Cursor makeSearchCursor(String query) {
        Uri media = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(query));
        return getContext().getContentResolver().query(media, PROJECTION, null, null, null);
    }
}