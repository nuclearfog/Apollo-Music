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

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.model.Genre;
import org.nuclearfog.apollo.provider.ExcludeStore;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Used to return the genres on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class GenreLoader extends AsyncExecutor<Void, List<Genre>> {

	private static final String TAG = "GenreLoader";

	/**
	 * regex pattern to split genre group separated by
	 */
	private static final Pattern SEPARATOR = Pattern.compile("\\s*[,;|]\\s*");


	public GenreLoader(@NonNull Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Genre> doInBackground(Void v) {
		Set<Genre> result = new TreeSet<>();
		Context context = getContext();
		if (context != null) {
			ExcludeStore exclude_db = ExcludeStore.getInstance(context);
			try {
				// init filter list
				Set<Long> excluded_ids = exclude_db.getIds(ExcludeStore.Type.GENRE);
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeGenreCursor(context);
				// Gather the data
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						HashMap<String, List<Long>> group = new HashMap<>();
						do {
							// get Column information
							long id = mCursor.getLong(0);
							String name = mCursor.getString(1);

							// Split genre groups into single genre names
							String[] genres = SEPARATOR.split(name);

							// solve conflicts. add multiple genre IDs for the same genre name.
							for (String genre : genres) {
								List<Long> ids = group.get(genre);
								if (ids == null) {
									ids = new LinkedList<>();
									group.put(genre, ids);
								}
								ids.add(id);
							}
						} while (mCursor.moveToNext());
						// add all elements to sorted list
						for (Map.Entry<String, List<Long>> entry : group.entrySet()) {
							boolean visibility = true;
							Long[] ids = entry.getValue().toArray(new Long[0]);
							String name = entry.getKey();
							for (long id : ids) {
								if (excluded_ids.contains(id)) {
									visibility = false;
									break;
								}
							}
							Genre genre = new Genre(ids, name, visibility);
							result.add(genre);
						}
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading genres:", exception);
			}
		}
		return new ArrayList<>(result);
	}
}