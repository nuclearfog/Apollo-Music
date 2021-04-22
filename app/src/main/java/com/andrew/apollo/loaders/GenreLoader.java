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

import com.andrew.apollo.model.Genre;

import java.util.LinkedList;
import java.util.List;

import static android.provider.MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;

/**
 * Used to query {@link MediaStore.Audio.Genres#EXTERNAL_CONTENT_URI} and return
 * the genres on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreLoader extends WrappedAsyncTaskLoader<List<Genre>> {

    /**
     * COLUMN projection
     */
    private static final String[] GENRE_COLUMNS = {
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    /**
     * condition to filter empty names
     */
    private static final String SELECTION = "name!=''";

    /**
     * sort genres by name
     */
    private static final String SORT = "name";

    /**
     * Constructor of <code>GenreLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public GenreLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Genre> loadInBackground() {
        List<Genre> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = makeGenreCursor();
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the genre id
                    long id = mCursor.getLong(0);
                    // Copy the genre name
                    String name = mCursor.getString(1);
                    // Create a new genre
                    // Genres separated by a semicolon will be separated into single genres
                    int separator = name.indexOf(";");
                    while (separator > 0) {
                        String subGenre = name.substring(0, separator);
                        name = name.substring(separator + 1);
                        Genre genre = new Genre(id, subGenre);
                        result.add(genre);
                        separator = name.indexOf(";");
                    }
                    Genre genre = new Genre(id, name);
                    // Add everything up
                    result.add(genre);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the genre query.
     */
    private Cursor makeGenreCursor() {
        return getContext().getContentResolver().query(EXTERNAL_CONTENT_URI, GENRE_COLUMNS, SELECTION, null, SORT);
    }
}