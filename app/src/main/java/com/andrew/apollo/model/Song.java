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

package com.andrew.apollo.model;

import androidx.annotation.NonNull;

/**
 * A class that represents a song.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Song {

    /**
     * The unique Id of the song
     */
    public long mSongId;

    /**
     * The song name
     */
    public String mSongName = "";

    /**
     * The song artist
     */
    public String mArtistName = "";

    /**
     * The song album
     */
    public String mAlbumName = "";

    /**
     * The song duration in seconds
     */
    public int mDuration;

    /**
     * Constructor of <code>Song</code>
     *
     * @param songId     The Id of the song
     * @param songName   The name of the song
     * @param artistName The song artist
     * @param albumName  The song album
     * @param duration   The duration of a song in seconds
     */
    public Song(long songId, String songName, String artistName, String albumName, int duration) {
        if (songName != null)
            mSongName = songName;
        if (artistName != null)
            mArtistName = artistName;
        if (albumName != null)
            mAlbumName = albumName;
        mSongId = songId;
        mDuration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (mAlbumName == null ? 0 : mAlbumName.hashCode());
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + mDuration;
        result = prime * result + (int) mSongId;
        result = prime * result + (mSongName == null ? 0 : mSongName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof Song) {
            Song other = (Song) obj;
            return mAlbumName.equals(other.mAlbumName) && mArtistName.equals(other.mArtistName) &&
                    mSongName.equals(other.mSongName) && mDuration == other.mDuration && mSongId == other.mSongId;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String toString() {
        return mSongName;
    }
}