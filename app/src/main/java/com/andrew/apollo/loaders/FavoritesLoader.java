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
import android.database.sqlite.SQLiteDatabase;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.FavoritesStore.FavoriteColumns;

import java.util.LinkedList;
import java.util.List;

import static com.andrew.apollo.provider.FavoritesStore.FavoriteColumns.ALBUMNAME;
import static com.andrew.apollo.provider.FavoritesStore.FavoriteColumns.ARTISTNAME;
import static com.andrew.apollo.provider.FavoritesStore.FavoriteColumns.ID;
import static com.andrew.apollo.provider.FavoritesStore.FavoriteColumns.NAME;
import static com.andrew.apollo.provider.FavoritesStore.FavoriteColumns.SONGNAME;

/**
 * Used to query the {@link FavoritesStore} for the tracks marked as favorites.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoritesLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * Definition of the Columns to get from database
     */
    public static final String[] COLUMNS = {
            FavoriteColumns.ID + " as _id", FavoriteColumns.ID,
            FavoriteColumns.SONGNAME, FavoriteColumns.ALBUMNAME,
            FavoriteColumns.ARTISTNAME, FavoriteColumns.PLAYCOUNT
    };

    /**
     * SQLite sport order
     */
    public static final String ORDER = FavoriteColumns.PLAYCOUNT + " DESC";

    /**
     * Constructor of <code>FavoritesHandler</code>
     *
     * @param context The {@link Context} to use.
     */
    public FavoritesLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = makeFavoritesCursor();
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(ID));
                    // Copy the song name
                    String songName = mCursor.getString(mCursor.getColumnIndexOrThrow(SONGNAME));
                    // Copy the artist name
                    String artist = mCursor.getString(mCursor.getColumnIndexOrThrow(ARTISTNAME));
                    // Copy the album name
                    String album = mCursor.getString(mCursor.getColumnIndexOrThrow(ALBUMNAME));
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
     * @return The {@link Cursor} used to run the favorites query.
     */
    private Cursor makeFavoritesCursor() {
        SQLiteDatabase data = FavoritesStore.getInstance(getContext()).getReadableDatabase();
        return data.query(NAME, COLUMNS, null, null, null, null, ORDER);
    }
}