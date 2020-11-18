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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;

import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Artists.Albums} and return the albums
 * for a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAlbumLoader extends WrappedAsyncTaskLoader<List<Album>> {

    /**
     * The result
     */
    private ArrayList<Album> mAlbumsList = Lists.newArrayList();

    /**
     * The Id of the artist the albums belong to.
     */
    private Long mArtistID;

    /**
     * Constructor of <code>ArtistAlbumHandler</code>
     *
     * @param context  The {@link Context} to use.
     * @param artistId The Id of the artist the albums belong to.
     */
    public ArtistAlbumLoader(Context context, Long artistId) {
        super(context);
        mArtistID = artistId;
    }

    /**
     * @param context  The {@link Context} to use.
     * @param artistId The Id of the artist the albums belong to.
     */
    public static Cursor makeArtistAlbumCursor(Context context, Long artistId) {
        return context.getContentResolver().query(
                MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), new String[]{
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AlbumColumns.ALBUM,
                        /* 2 */
                        AlbumColumns.ARTIST,
                        /* 3 */
                        AlbumColumns.NUMBER_OF_SONGS,
                        /* 4 */
                        AlbumColumns.FIRST_YEAR
                }, null, null, PreferenceUtils.getInstance(context).getArtistAlbumSortOrder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Album> loadInBackground() {
        // Create the Cursor
        Cursor mCursor = makeArtistAlbumCursor(getContext(), mArtistID);
        // Gather the dataS
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the album id
                    long id = mCursor.getLong(0);
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
                    mAlbumsList.add(album);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return mAlbumsList;
    }
}