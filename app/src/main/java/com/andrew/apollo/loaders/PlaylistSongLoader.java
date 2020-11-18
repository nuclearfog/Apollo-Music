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
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Playlists#EXTERNAL_CONTENT_URI} and
 * return the songs for a particular playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private ArrayList<Song> mSongList = Lists.newArrayList();

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
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context    The {@link Context} to use.
     * @param playlistID The playlist the songs belong to.
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makePlaylistSongCursor(Context context, Long playlistID) {
        String mSelection = AudioColumns.IS_MUSIC + "=1" +
                " AND " + AudioColumns.TITLE + " != ''";//$NON-NLS-2$
        return context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID),
                new String[]{
                        /* 0 */
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        "artist",
                        /* 3 */
                        "album",
                        /* 4 */
                        "duration"
                        //AudioColumns.DURATION
                }, mSelection, null,
                MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        Cursor mCursor = makePlaylistSongCursor(getContext(), mPlaylistID);
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
                    mSongList.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return mSongList;
    }
}