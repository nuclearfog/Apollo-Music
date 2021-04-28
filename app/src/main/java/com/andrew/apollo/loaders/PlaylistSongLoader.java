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

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.CursorCreator;

import java.util.LinkedList;
import java.util.List;


/**
 * Used to query {@link MediaStore.Audio.Playlists#EXTERNAL_CONTENT_URI} and
 * return the songs for a particular playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {


    /**
     * The Id of the playlist the songs belong to.
     */
    private Long mPlaylistID;

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public PlaylistSongLoader(Context context, Long playlistId) {
        super(context);
        mPlaylistID = playlistId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = CursorCreator.makePlaylistSongCursor(getContext(), mPlaylistID);
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
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, duration);
                    // Add everything up
                    result.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }
}