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


/**
 * A class that represents a song.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Song extends Music {

    /**
     * The song artist
     */
    private String mArtistName = "";

    /**
     * The song album
     */
    private String mAlbumName = "";

    /**
     * The song duration in seconds
     */
    private int mDuration = -1;

    /**
     * playlist position of the track
     */
    private int playlistPos = -1;

    /**
     * Constructor of <code>Song</code>
     *
     * @param songId     The Id of the song
     * @param songName   The name of the song
     * @param artistName The song artist
     * @param albumName  The song album
     * @param length     The duration of a song in milliseconds
     */
    public Song(long songId, String songName, String artistName, String albumName, long length) {
        super(songId, songName);
        if (artistName != null)
            mArtistName = artistName;
        if (albumName != null)
            mAlbumName = albumName;
        if (length > 0)
            mDuration = (int) length / 1000;
    }

    /**
     * @param playlistPos playlist position of the track
     */
    public Song(long songId, String songName, String artistName, String albumName, long length, int playlistPos) {
        this(songId, songName, artistName, albumName, length);
        this.playlistPos = playlistPos;
    }

    /**
     * get artist of this song
     *
     * @return artist name
     */
    public String getArtist() {
        return mArtistName;
    }

    /**
     * album name of the track
     *
     * @return album name
     */
    public String getAlbum() {
        return mAlbumName;
    }

    /**
     * track duration in seconds
     *
     * @return duration in seconds
     */
    public int duration() {
        return mDuration;
    }

    /**
     * track duration in milliseconds
     *
     * @return duration in milliseconds
     */
    public long durationMillis() {
        if (mDuration > 0)
            return (long) mDuration * 1000;
        return -1;
    }

    /**
     * track position in a playlist
     *
     * @return playlist position
     */
    public int getPlaylistPos() {
        return playlistPos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + mAlbumName.hashCode();
        result = prime * result + mArtistName.hashCode();
        result = prime * result + mDuration;
        result = prime * result + (int) id;
        result = prime * result + playlistPos;
        result = prime * result + name.hashCode();
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
                    name.equals(other.name) && mDuration == other.mDuration && id == other.id && other.playlistPos == playlistPos;
        }
        return false;
    }
}