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
import android.database.Cursor;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.CursorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Used to return the genres on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreLoader extends WrappedAsyncTaskLoader<List<Genre>> {

	/**
	 * regex pattern to split genre group separated by
	 */
	private static final Pattern SEPARATOR = Pattern.compile("\\s*[,;|]\\s*");

	/**
	 * Constructor of <code>GenreLoader</code>
	 *
	 * @param context The {@link Context} to use
	 */
	public GenreLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Genre> loadInBackground() {
		TreeSet<Genre> result = new TreeSet<>();
		// Create the Cursor
		Cursor mCursor = CursorFactory.makeGenreCursor(getContext());
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
					Genre genre = new Genre(entry.getValue(), entry.getKey());
					result.add(genre);
				}
			}
			mCursor.close();
		}
		return new ArrayList<>(result);
	}
}