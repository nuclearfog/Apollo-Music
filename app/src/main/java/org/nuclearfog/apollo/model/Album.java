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
 * A class that represents an album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Album extends Music implements Parcelable {

	private static final long serialVersionUID = 4612921914269834447L;

	public static final Creator<? extends Album> CREATOR = new Creator<Album>() {

		@Override
		public Album createFromParcel(Parcel source) {
			long id = source.readLong();
			String name = source.readString();
			boolean visible = source.readInt() == 1;
			String artist = source.readString();
			int count = source.readInt();
			String release = source.readString();
			return new Album(id, name, artist, count, release, visible);
		}


		@Override
		public Album[] newArray(int size) {
			return new Album[size];
		}
	};

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
	 * @param visible    Visibility of this album
	 */
	public Album(long albumId, String albumName, String artistName, int songNumber, String albumYear, boolean visible) {
		super(albumId, albumName, visible);
		if (albumYear != null)
			mYear = albumYear;
		if (artistName != null)
			mArtistName = artistName;
		mSongNumber = songNumber;
	}

	/**
	 * get artist name of the album
	 *
	 * @return name of the artist
	 */
	public String getArtist() {
		return mArtistName;
	}

	/**
	 * get number of tracks in this album
	 *
	 * @return number of tracks
	 */
	public int getTrackCount() {
		return mSongNumber;
	}

	/**
	 * get release date
	 *
	 * @return release date string or empty string if not defined
	 */
	public String getRelease() {
		return mYear;
	}


	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeLong(getId());
		dest.writeString(getName());
		dest.writeInt(isVisible() ? 1 : 0);
		dest.writeString(getArtist());
		dest.writeInt(getTrackCount());
		dest.writeString(getRelease());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (int) getId();
		result = prime * result + getName().hashCode();
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
		if (obj instanceof Album) {
			Album album = (Album) obj;
			return getId() == album.getId() && mSongNumber == album.mSongNumber &&
					getName().equals(album.getName()) && mArtistName.equals(album.mArtistName) && mYear.equals(album.mYear);
		}
		return false;
	}
}