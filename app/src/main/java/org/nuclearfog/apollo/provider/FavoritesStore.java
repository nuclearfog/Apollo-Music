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

package org.nuclearfog.apollo.provider;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.model.Song;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoritesStore extends AppStore {

	/**
	 * column projection of favorite table
	 */
	private static final String[] FAV_COLUMNS = {
			FavoriteColumns.ID,
			FavoriteColumns.SONGNAME,
			FavoriteColumns.ALBUMNAME,
			FavoriteColumns.ARTISTNAME,
			FavoriteColumns.PLAYCOUNT
	};

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
	 * condition to find track in favorite table
	 */
	private static final String FAVORITE_SELECT = FavoriteColumns.ID + "=?";

	/**
	 * database filename
	 */
	private static final String DB_NAME = "favorites.db";

	/**
	 * singleton instance
	 */
	private static FavoritesStore sInstance;

	/**
	 *
	 */
	private final Object LOCK = new Object();


	/**
	 * Constructor of <code>FavoritesStore</code>
	 *
	 * @param context The {@link Context} to use
	 */
	private FavoritesStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return A new instance of this class
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
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FAVORITE_TABLE);
	}

	/**
	 * add song to the favorite database
	 *
	 * @param mSong song instance
	 */
	public void addSongId(@NonNull Song mSong) {
		synchronized (LOCK) {
			addSongId(mSong.getId(), mSong.getName(), mSong.getAlbum(), mSong.getArtist(), mSong.durationMillis());
		}
	}

	/**
	 * Used to store song IDs in our database
	 *
	 * @param songId     The album's ID
	 * @param songName   The song name
	 * @param albumName  The album name
	 * @param artistName The artist name
	 * @param duration   Track duration in milliseconds
	 */
	public void addSongId(long songId, String songName, String albumName, String artistName, long duration) {
		synchronized (LOCK) {
			if (songId > 0 && songName != null && albumName != null && artistName != null) {
				// increment by 1
				long playCount = getPlayCount(songId) + 1;
				SQLiteDatabase database = getWritableDatabase();
				ContentValues values = new ContentValues(6);
				database.beginTransaction();

				values.put(FavoriteColumns.ID, songId);
				values.put(FavoriteColumns.SONGNAME, songName);
				values.put(FavoriteColumns.ALBUMNAME, albumName);
				values.put(FavoriteColumns.ARTISTNAME, artistName);
				values.put(FavoriteColumns.PLAYCOUNT, playCount);
				values.put(FavoriteColumns.DURATION, duration);

				database.insertWithOnConflict(FavoriteColumns.NAME, null, values, CONFLICT_REPLACE);
				commit();
			}
		}
	}

	/**
	 * check if track exists in favorite table
	 *
	 * @param trackId ID of the track
	 * @return true if track is favorite
	 */
	public boolean exists(long trackId) {
		synchronized (LOCK) {
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
	}

	/**
	 * remove song from favorits
	 *
	 * @param songId track ID
	 */
	public void removeItem(long songId) {
		synchronized (LOCK) {
			String[] args = {Long.toString(songId)};
			SQLiteDatabase database = getWritableDatabase();
			database.delete(FavoriteColumns.NAME, FAVORITE_SELECT, args);
		}
	}

	/**
	 * Used to retrieve how often a favorited track was played
	 *
	 * @param songId The song Id to reference
	 * @return The play count for a song
	 */
	private long getPlayCount(long songId) {
		synchronized (LOCK) {
			long result = 0;
			if (songId >= 0) {
				String[] having = {Long.toString(songId)};
				SQLiteDatabase database = getReadableDatabase();
				Cursor cursor = database.query(FavoriteColumns.NAME, FAV_COLUMNS, FAVORITE_SELECT, having, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						result = cursor.getLong(4);
					}
					cursor.close();
				}
			}
			return result;
		}
	}

	/**
	 * columns of the favorite table
	 */
	public interface FavoriteColumns {

		/* Table name */
		String NAME = "favorites";

		/* Song IDs column */
		String ID = "songid";

		/* Song name column */
		String SONGNAME = "songname";

		/* Album name column */
		String ALBUMNAME = "albumname";

		/* Artist name column */
		String ARTISTNAME = "artistname";

		/* Play count column */
		String PLAYCOUNT = "playcount";

		/* Duraion of the track */
		String DURATION = "duration";
	}
}