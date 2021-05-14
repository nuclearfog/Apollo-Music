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

import com.andrew.apollo.model.Album;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to query {@link RecentStore} and return the last listened to albums.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentLoader extends WrappedAsyncTaskLoader<List<Album>> {

    /**
     * Constructor of <code>RecentLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public RecentLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Album> loadInBackground() {
        List<Album> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = CursorFactory.makeRecentCursor(getContext());
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the album id
                    long id = mCursor.getLong(0);
                    // filter invalid albums
                    if (id < 0)
                        continue;
                    // Copy the album name
                    String albumName = mCursor.getString(1);
                    // Copy the artist name
                    String artist = mCursor.getString(2);
                    // Copy the number of songs
                    int songCount = mCursor.getInt(3);
                    // Copy the release year
                    String year = mCursor.getString(4);
                    // Create a new album
                    Album album = new Album(id, albumName, artist, songCount, year);
                    // Add everything up
                    result.add(album);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }
}