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

/**
 * A class that represents a playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Playlist extends Music {

	/**
	 * unique ID to define this playlist as favorite list
	 */
	public static final long FAVORITE_ID = 0xF092D5DEB4A19EEL;

	/**
	 * unique ID to define this playlist as "last added" list
	 */
	public static final long LAST_ADDED_ID = 0xF57622096950ABBCL;

	/**
	 * unique ID to define this playlist as "most played" list
	 */
	public static final long POPULAR_ID = 0x502CDB3BD99EE393L;

	/**
	 * Constructor of <code>Genre</code>
	 *
	 * @param playlistId   The Id of the playlist
	 * @param playlistName The playlist name
	 */
	public Playlist(long playlistId, String playlistName) {
		super(playlistId, playlistName, true);
	}

	/**
	 * check if playlist is default playlist
	 *
	 * @return true if playlist is one of the default playlists
	 */
	public boolean isDefault() {
		return getId() == FAVORITE_ID || getId() == LAST_ADDED_ID || getId() == POPULAR_ID;
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
		if (obj instanceof Playlist) {
			Playlist playlist = (Playlist) obj;
			return getId() == playlist.getId() && getName().equals(playlist.getName());
		}
		return false;
	}
}