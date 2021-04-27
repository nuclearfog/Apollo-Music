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
import android.provider.MediaStore.Audio.Artists;

import com.andrew.apollo.model.Artist;
import com.andrew.apollo.utils.CursorCreator;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to query {@link Artists#EXTERNAL_CONTENT_URI} and
 * return the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {

    /**
     * Constructor of <code>ArtistLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public ArtistLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Artist> loadInBackground() {
        List<Artist> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = CursorCreator.makeArtistCursor(getContext());
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the artist id
                    long id = mCursor.getLong(0);
                    // Copy the artist name
                    String artistName = mCursor.getString(1);
                    // Copy the number of albums
                    int albumCount = mCursor.getInt(2);
                    // Copy the number of songs
                    int songCount = mCursor.getInt(3);
                    // Create a new artist
                    Artist artist = new Artist(id, artistName, songCount, albumCount);
                    // Add everything up
                    result.add(artist);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }
}