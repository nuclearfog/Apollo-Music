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

package org.nuclearfog.apollo.store;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.model.Song;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class FavoritesStore extends AppStore {

	/**
	 * query to create favorite table
	 */
	private static final String FAVORITE_TABLE = "CREATE TABLE IF NOT EXISTS " + FavoriteColumns.NAME + " ("
			+ FavoriteColumns.ID + " LONG PRIMARY KEY,"
			+ FavoriteColumns.SONGNAME + " TEXT NOT NULL,"
			+ FavoriteColumns.ALBUMNAME + " TEXT NOT NULL,"
			+ FavoriteColumns.ARTISTNAME + " TEXT NOT NULL,"
			+ FavoriteColumns.PLAYCOUNT + " LONG NOT NULL,"
			+ FavoriteColumns.DURATION + " LONG);";

	/**
	 * Definition of the Columns to get from database
	 */
	private static final String[] COLUMNS = {
			FavoriteColumns.ID,
			FavoriteColumns.SONGNAME,
			FavoriteColumns.ALBUMNAME,
			FavoriteColumns.ARTISTNAME,
			FavoriteColumns.DURATION,
			FavoriteColumns.PLAYCOUNT
	};

	/**
	 * condition to find track in favorite table
	 */
	private static final String FAVORITE_SELECT = FavoriteColumns.ID + "=?";

	/**
	 * SQLite sport order
	 */
	private static final String FAV_ORDER = FavoriteColumns.PLAYCOUNT + " DESC";

	/**
	 * database filename
	 */
	private static final String DB_NAME = "favorites.db";

	/**
	 * singleton instance
	 */
	private static FavoritesStore sInstance;


	private FavoritesStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return A singleton instance of this class
	 */
	public static FavoritesStore getInstance(Context context) {
		if (sInstance == null) {
			// use application context to avoid memory leak
			sInstance = new FavoritesStore(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(FAVORITE_TABLE);
	}

	/**
	 * add song to the favorite database
	 *
	 * @param mSong song instance
	 */
	public synchronized void addFavorite(@NonNull Song mSong) {
		long playCount = getPlayCount(mSong.getId()) + 1; // increment by 1
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues(6);
		values.put(FavoriteColumns.ID, mSong.getId());
		values.put(FavoriteColumns.SONGNAME, mSong.getName());
		values.put(FavoriteColumns.ALBUMNAME, mSong.getAlbum());
		values.put(FavoriteColumns.ARTISTNAME, mSong.getArtist());
		values.put(FavoriteColumns.PLAYCOUNT, playCount);
		values.put(FavoriteColumns.DURATION, mSong.getDuration());
		database.insertWithOnConflict(FavoriteColumns.NAME, null, values, CONFLICT_REPLACE);
		commit();
	}

	/**
	 * remove song from favorits
	 *
	 * @param songId track ID
	 */
	public synchronized void removeFavorite(long songId) {
		String[] args = {Long.toString(songId)};
		SQLiteDatabase database = getWritableDatabase();
		database.delete(FavoriteColumns.NAME, FAVORITE_SELECT, args);
		commit();
	}

	/**
	 * check if track exists in favorite table
	 *
	 * @param trackId ID of the track
	 * @return true if track is favorite
	 */
	public synchronized boolean exists(long trackId) {
		String[] args = {String.valueOf(trackId)};
		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = database.query(FavoriteColumns.NAME, null, FAVORITE_SELECT, args, null, null, null, "1");
		boolean result = false;
		if (cursor != null) {
			result = cursor.moveToFirst();
			cursor.close();
		}
		return result;
	}

	/**
	 * get all favorite songs
	 *
	 * @return list of favorite songs
	 */
	public synchronized List<Song> getFavorites() {
		List<Song> result = new LinkedList<>();
		SQLiteDatabase data = getReadableDatabase();
		Cursor cursor = data.query(FavoriteColumns.NAME, COLUMNS, null, null, null, null, FAV_ORDER);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(0);
					String name = cursor.getString(1);
					String album = cursor.getString(2);
					String artist = cursor.getString(3);
					long duration = cursor.getLong(4);
					Song song = new Song(id, name, artist, album, duration);
					result.add(song);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * Used to retrieve how often a favorited track was played
	 *
	 * @param songId The song Id to reference
	 * @return The play count for a song
	 */
	private long getPlayCount(long songId) {
		long result = 0;
		if (songId >= 0) {
			String[] args = {Long.toString(songId)};
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = database.query(FavoriteColumns.NAME, COLUMNS, FAVORITE_SELECT, args, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					result = cursor.getLong(5);
				}
				cursor.close();
			}
		}
		return result;
	}

	/**
	 * columns of the favorite table
	 */
	private interface FavoriteColumns {

		/**
		 * Table name
		 */
		String NAME = "favorites";

		/**
		 * Song IDs column
		 */
		String ID = "songid";

		/**
		 * Song name column
		 */
		String SONGNAME = "songname";

		/**
		 * Album name column
		 */
		String ALBUMNAME = "albumname";

		/**
		 * Artist name column
		 */
		String ARTISTNAME = "artistname";

		/**
		 * Play count column
		 */
		String PLAYCOUNT = "playcount";

		/**
		 * Duraion of the track
		 */
		String DURATION = "duration";
	}
}