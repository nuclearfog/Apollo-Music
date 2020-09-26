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
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.FavoritesStore.FavoriteColumns;
import com.andrew.apollo.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query the {@link FavoritesStore} for the tracks marked as favorites.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoritesLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * Constructor of <code>FavoritesHandler</code>
     *
     * @param context The {@link Context} to use.
     */
    public FavoritesLoader(Context context) {
        super(context);
    }

    /**
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the favorites query.
     */
    public static Cursor makeFavoritesCursor(Context context) {
        return FavoritesStore
                .getInstance(context)
                .getReadableDatabase()
                .query(FavoriteColumns.NAME,
                        new String[]{
                                FavoriteColumns.ID + " as _id", FavoriteColumns.ID,
                                FavoriteColumns.SONGNAME, FavoriteColumns.ALBUMNAME,
                                FavoriteColumns.ARTISTNAME, FavoriteColumns.PLAYCOUNT
                        }, null, null, null, null, FavoriteColumns.PLAYCOUNT + " DESC");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        Cursor mCursor = makeFavoritesCursor(getContext());
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(FavoriteColumns.ID));
                    // Copy the song name
                    String songName = mCursor.getString(mCursor.getColumnIndexOrThrow(FavoriteColumns.SONGNAME));
                    // Copy the artist name
                    String artist = mCursor.getString(mCursor.getColumnIndexOrThrow(FavoriteColumns.ARTISTNAME));
                    // Copy the album name
                    String album = mCursor.getString(mCursor.getColumnIndexOrThrow(FavoriteColumns.ALBUMNAME));
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, -1);
                    // Add everything up
                    mSongList.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return mSongList;
    }
}