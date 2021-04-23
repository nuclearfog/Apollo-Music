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

import android.text.TextUtils;

/**
 * A class that represents an album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Album extends Music {

    /**
     * The album artist
     */
    private String mArtistName = "";

    /**
     * The number of songs in the album
     */
    private int mSongNumber;

    /**
     * The year the album was released
     */
    private String mYear = "";

    /**
     * Constructor of <code>Album</code>
     *
     * @param albumId    The Id of the album
     * @param albumName  The name of the album
     * @param artistName The album artist
     * @param songNumber The number of songs in the album
     * @param albumYear  The year the album was released
     */
    public Album(long albumId, String albumName, String artistName, int songNumber, String albumYear) {
        super(albumId, albumName);
        if (albumYear != null)
            mYear = albumYear;
        if (artistName != null)
            mArtistName = artistName;
        mSongNumber = songNumber;
    }


    public String getArtist() {
        return mArtistName;
    }


    public int getTrackCount() {
        return mSongNumber;
    }


    public String getRelease() {
        return mYear;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) id;
        result = prime * result + name.hashCode();
        result = prime * result + mArtistName.hashCode();
        result = prime * result + mSongNumber;
        result = prime * result + mYear.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Album other = (Album) obj;
        if (id != other.id) {
            return false;
        }
        if (!TextUtils.equals(name, other.name)) {
            return false;
        }
        if (!TextUtils.equals(mArtistName, other.mArtistName)) {
            return false;
        }
        if (mSongNumber != other.mSongNumber) {
            return false;
        }
        return TextUtils.equals(mYear, other.mYear);
    }
}