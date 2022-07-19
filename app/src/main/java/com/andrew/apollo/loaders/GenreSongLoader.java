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

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.CursorFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the songs for a particular genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

	/**
	 * Genre IDs to get songs from
	 */
	private long[] mGenreID;

	/**
	 * Constructor of <code>GenreSongHandler</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public GenreSongLoader(Context context, long[] genreId) {
		super(context);
		mGenreID = genreId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Song> loadInBackground() {
		List<Song> result = new LinkedList<>();
		for (long genreId : mGenreID) {
			// Create the Cursor
			if (genreId == 0)
				continue;
			Cursor mCursor = CursorFactory.makeGenreSongCursor(getContext(), genreId);
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
						// Create a new song
						Song song = new Song(id, songName, artist, album, duration);
						// Add everything up
						result.add(song);
					} while (mCursor.moveToNext());
				}
				mCursor.close();
			}
		}
		// sort tracks by song name
		Collections.sort(result, new Comparator<Song>() {
			@Override
			public int compare(Song track1, Song track2) {
				return track1.getName().compareTo(track2.getName());
			}
		});
		return result;
	}
}