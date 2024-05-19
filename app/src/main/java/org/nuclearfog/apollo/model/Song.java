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

package org.nuclearfog.apollo.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * A class that represents a song.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Song extends Music implements Parcelable, Comparable<Song> {


	public static final Creator<? extends Song> CREATOR = new Creator<Song>() {

		@Override
		public Song createFromParcel(Parcel source) {
			long id = source.readLong();
			String name = source.readString();
			String artist = source.readString();
			String album = source.readString();
			long duration = source.readLong();
			boolean visible = source.readInt() == 1;
			return new Song(id, name, artist, album, duration, visible);
		}


		@Override
		public Song[] newArray(int size) {
			return new Song[size];
		}
	};
	/**
	 * The song artist
	 */
	private String artist_name = "";

	/**
	 * The song album
	 */
	private String album_name = "";

	/**
	 * The song duration in milliseconds
	 */
	private long duration = -1;

	/**
	 * playlist position of the track
	 */
	private int playlist_index = -1;

	/**
	 * ID of the song's artist
	 */
	private long artist_id = -1;

	/**
	 * ID of the song's album
	 */
	private long album_id = -1;

	/**
	 * path to the song file
	 */
	private String path = "";

	/**
	 * @param artist_id Id of the song artist
	 */
	public Song(long song_id, long artist_id, long album_id, String song_name, String artist_name, String album_name, long length, String path) {
		this(song_id, song_name, artist_name, album_name, length);
		this.artist_id = artist_id;
		this.album_id = album_id;
		this.path = path;
	}

	/**
	 * @param playlist_index playlist position of the track
	 */
	public Song(long songId, String song_name, String artist_name, String album_name, long length, int playlist_index) {
		this(songId, song_name, artist_name, album_name, length);
		this.playlist_index = playlist_index;
	}

	/**
	 *
	 */
	public Song(long song_id, String song_name, String artist_name, String album_name, long length) {
		this(song_id, song_name, artist_name, album_name, length, true);
	}

	/**
	 * Constructor of <code>Song</code>
	 *
	 * @param song_id     The Id of the song
	 * @param song_name   The song_name of the song
	 * @param artist_name The song artist
	 * @param album_name  The song album
	 * @param length      The duration of a song in milliseconds
	 * @param visibility  Visibility of the track
	 */
	public Song(long song_id, String song_name, String artist_name, String album_name, long length, boolean visibility) {
		super(song_id, song_name, visibility);
		if (artist_name != null) {
			this.artist_name = artist_name;
		}
		if (album_name != null) {
			this.album_name = album_name;
		}
		if (length > 0) {
			duration = length;
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public int compareTo(Song song) {
		if (playlist_index >= 0 && song.getPlaylistIndex() >= 0)
			return Integer.compare(getPlaylistIndex(), song.getPlaylistIndex());
		return song.getName().compareToIgnoreCase(getName());
	}

	/**
	 * get artist of this song
	 *
	 * @return artist name
	 */
	public String getArtist() {
		return artist_name;
	}

	/**
	 * album name of the track
	 *
	 * @return album name
	 */
	public String getAlbum() {
		return album_name;
	}

	/**
	 * track duration in milliseconds
	 *
	 * @return duration in milliseconds
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * get song artist ID
	 *
	 * @return artist ID or '0' if not set
	 */
	public long getArtistId() {
		return artist_id;
	}

	/**
	 * get album ID
	 *
	 * @return album ID or '0' if not set
	 */
	public long getAlbumId() {
		return album_id;
	}

	/**
	 * get local song path
	 *
	 * @return path string or empty if not set
	 */
	public String getPath() {
		return path;
	}

	/**
	 * get the playlist index of the song
	 *
	 * @return playlist index or '-1' if not set
	 */
	public int getPlaylistIndex() {
		return playlist_index;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + getAlbum().hashCode();
		result = prime * result + getArtist().hashCode();
		result = prime * result + Long.hashCode(getDuration());
		result = prime * result + Long.hashCode(getId());
		result = prime * result + getPlaylistIndex();
		result = prime * result + getName().hashCode();
		result = prime * result + Long.hashCode(getArtistId());
		result = prime * result + Long.hashCode(getAlbumId());
		result = prime * result + getPath().hashCode();
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
			return getId() == other.getId() && album_name.equals(other.album_name) && artist_name.equals(other.artist_name) &&
					getName().equals(other.getName()) && duration == other.duration && other.playlist_index == playlist_index;
		}
		return false;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeLong(getId());
		dest.writeString(getName());
		dest.writeString(getArtist());
		dest.writeString(getAlbum());
		dest.writeLong(getDuration());
		dest.writeInt(isVisible() ? 1 : 0);
	}
}