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

import com.andrew.apollo.model.Song;

import java.util.LinkedList;
import java.util.List;

import static com.andrew.apollo.loaders.SongLoader.TRACK_COLUMNS;

/**
 * Used to query {@link MediaStore.Audio.Genres.Members#EXTERNAL_CONTENT_URI}
 * and return the songs for a particular genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * selection condition
     */
    private static final String SELECTION = "is_music=1 AND title!=''";

    /**
     * order by
     */
    private static final String ORDER = "title_key";

    /**
     * The Id of the genre the songs belong to.
     */
    private Long mGenreID;

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
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = makeGenreSongCursor();
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
                    // Convert the duration into seconds
                    int durationInSecs = (int) duration / 1000;
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, durationInSecs);
                    // Add everything up
                    result.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }

    /**
     * @return The {@link Cursor} used to run the query.
     */
    private Cursor makeGenreSongCursor() {
        // Match the songs up with the genre
        Uri media = MediaStore.Audio.Genres.Members.getContentUri("external", mGenreID);
        return getContext().getContentResolver().query(media, TRACK_COLUMNS, SELECTION, null, ORDER);
    }
}