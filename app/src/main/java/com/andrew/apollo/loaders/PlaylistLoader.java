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
import android.content.res.Resources;
import android.database.Cursor;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistLoader extends WrappedAsyncTaskLoader<List<Playlist>> {

	/**
	 * Constructor of <code>PlaylistLoader</code>
	 *
	 * @param context The {@link Context} to use
	 */
	public PlaylistLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Playlist> loadInBackground() {
		List<Playlist> result = new LinkedList<>();
		// Add the default playlists to the adapter
		makeDefaultPlaylists(result);
		// Create the Cursor
		Cursor mCursor = CursorFactory.makePlaylistCursor(getContext());
		// Gather the data
		if (mCursor != null) {
			if (mCursor.moveToFirst()) {
				do {
					// Copy the playlist id
					long id = mCursor.getLong(0);
					// Copy the playlist name
					String name = mCursor.getString(1);
					// Create a new playlist
					Playlist playlist = new Playlist(id, name);
					// Add everything up
					result.add(playlist);
				} while (mCursor.moveToNext());
			}
			mCursor.close();
		}
		return result;
	}

	/**
	 * Adds the favorites and last added playlists
	 *
	 * @param mPlaylistList list with playlists
	 */
	private void makeDefaultPlaylists(List<Playlist> mPlaylistList) {
		Resources resources = getContext().getResources();
		/* Favorites list */
		Playlist favorites = new Playlist(Playlist.FAVORITE_ID, resources.getString(R.string.playlist_favorites));
		mPlaylistList.add(favorites);
		/* Last added list */
		Playlist mostPlayed = new Playlist(Playlist.POPULAR_ID, resources.getString(R.string.playlist_most_played));
		mPlaylistList.add(mostPlayed);
		/* Last added list */
		Playlist lastAdded = new Playlist(Playlist.LAST_ADDED_ID, resources.getString(R.string.playlist_last_added));
		mPlaylistList.add(lastAdded);

	}
}