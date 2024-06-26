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
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.store.ExcludeStore;
import org.nuclearfog.apollo.store.ExcludeStore.Type;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Used to return the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ArtistLoader extends AsyncExecutor<Void, List<Artist>> {

	private static final String TAG = "ArtistLoader";


	public ArtistLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Artist> doInBackground(Void v) {
		List<Artist> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			ExcludeStore exclude_db = ExcludeStore.getInstance(context);
			try {
				// init filter list
				Set<Long> excluded_ids = exclude_db.getIds(Type.ARTIST);
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeArtistCursor(context);
				// Gather the data
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							// Copy the artist id
							long id = mCursor.getLong(0);
							// Copy the artist name
							String artistName = mCursor.getString(1);
							// Copy the number of albums
							int albumCount = mCursor.getInt(2);
							// Copy the number of songs
							int songCount = mCursor.getInt(3);
							// visibility of the artist
							boolean visible = !excluded_ids.contains(id);
							// Create a new artist
							Artist artist = new Artist(id, artistName, songCount, albumCount, visible);
							// Add everything up
							result.add(artist);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading artists", exception);
			}
		}
		return result;
	}
}