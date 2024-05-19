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

package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.ExcludeStore;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Used to return the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class SongLoader extends AsyncExecutor<Void, List<Song>> {

	private static final String TAG = "SongLoader";


	public SongLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			ExcludeStore exclude_db = ExcludeStore.getInstance(context);
			try {
				// init filter list
				Set<Long> excludedIds = exclude_db.getIds(ExcludeStore.Type.SONG);
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeTrackCursor(context);
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
							// set visibility
							boolean visible = !excludedIds.contains(id);
							// Create a new song
							Song song = new Song(id, songName, artist, album, duration, visible);
							// Add everything up
							result.add(song);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs:", exception);
			}
		}
		return result;
	}
}