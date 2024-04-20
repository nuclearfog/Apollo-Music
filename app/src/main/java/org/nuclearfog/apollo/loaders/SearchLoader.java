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
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class SearchLoader extends AsyncExecutor<String, List<Song>> {

	private static final String TAG = "SearchLoader";


	public SearchLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(String param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			try {
				// Gather the data
				Cursor mCursor = CursorFactory.makeSearchCursor(context, param);
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							// Copy the song Id
							long id = mCursor.getLong(0);
							// Copy the artist name
							String artist = mCursor.getString(1);
							// Copy the album name
							String album = mCursor.getString(2);
							// Copy the song name
							String songName = mCursor.getString(3);
							// Copy duration
							long duration = mCursor.getLong(4);
							// Create a new song
							Song song = new Song(id, songName, artist, album, duration);
							// Add everything up
							result.add(song);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading search results:", exception);
			}
		}
		return result;
	}
}