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

package org.nuclearfog.apollo.loaders;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Playlist;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class PlaylistLoader extends AsyncExecutor<Void, List<Playlist>> {

	private static final String TAG = "PlaylistLoader";

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
	protected List<Playlist> doInBackground(Void v) {
		List<Playlist> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			try {
				// Add the default playlists to the adapter
				Resources resources = context.getResources();
				/* Favorites list */
				Playlist favorites = new Playlist(Playlist.FAVORITE_ID, resources.getString(R.string.playlist_favorites));
				result.add(favorites);
				/* Last added list */
				Playlist mostPlayed = new Playlist(Playlist.POPULAR_ID, resources.getString(R.string.playlist_most_played));
				result.add(mostPlayed);
				/* Last added list */
				Playlist lastAdded = new Playlist(Playlist.LAST_ADDED_ID, resources.getString(R.string.playlist_last_added));
				result.add(lastAdded);
				// Create the Cursor
				Cursor mCursor = CursorFactory.makePlaylistCursor(context);
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
			} catch (Exception exception) {
				Log.e(TAG, "error loading playlist:", exception);
			}
		}
		return result;
	}
}