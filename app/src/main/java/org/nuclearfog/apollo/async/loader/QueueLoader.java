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

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the current playlist or queue.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class QueueLoader extends AsyncExecutor<List<Long>, List<Song>> {


	public QueueLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(List<Long> param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			// Create the Cursor
			Cursor cursor = CursorFactory.makeNowPlayingCursor(context, param);
			// Gather the data
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						// Copy the song Id
						long id = cursor.getLong(0);
						// Copy the song name
						String songName = cursor.getString(1);
						// Copy the artist name
						String artist = cursor.getString(2);
						// Copy the album name
						String album = cursor.getString(3);
						// Copy the duration
						long duration = cursor.getLong(4);
						// Create a new song
						Song song = new Song(id, songName, artist, album, duration);
						// Add everything up
						result.add(song);
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		}
		return result;
	}
}