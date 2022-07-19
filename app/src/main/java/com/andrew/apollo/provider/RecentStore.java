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

package com.andrew.apollo.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

/**
 * The {@link RecentStore} is used to display a a grid or list of
 * recently listened to albums. In order to populate the this grid or list with
 * the correct data, we keep a cache of the album ID, name, and time it was
 * played to be retrieved later.
 * <p>
 * In {@link com.andrew.apollo.ui.activities.ProfileActivity}, when viewing the profile for an artist, the first
 * image the carousel header is the last album the user listened to for that
 * particular artist. That album is retrieved using
 * {@link #getAlbumName(String)}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentStore extends SQLiteOpenHelper {

	/**
	 * column projection of the RECENT table
	 */
	private static final String[] RECENT_PROJECTION = {
			RecentStoreColumns.ID,
			RecentStoreColumns.ALBUMNAME,
			RecentStoreColumns.ARTISTNAME,
			RecentStoreColumns.TIMEPLAYED
	};

	/**
	 * SQL query to create table with recently listened albums
	 */
	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
			+ RecentStoreColumns.ID + " LONG PRIMARY KEY,"
			+ RecentStoreColumns.ALBUMNAME + " TEXT NOT NULL,"
			+ RecentStoreColumns.ARTISTNAME + " TEXT NOT NULL,"
			+ RecentStoreColumns.ALBUMSONGCOUNT + " TEXT NOT NULL,"
			+ RecentStoreColumns.TIMEPLAYED + " LONG NOT NULL,"
			+ RecentStoreColumns.ALBUMYEAR + " TEXT);";

	/**
	 * select recent album by ID
	 */
	private static final String RECENT_SELECT_ID = RecentStoreColumns.ID + "=?";

	/**
	 * select recent album by name
	 */
	private static final String RECENT_SELECT_NAME = RecentStoreColumns.ARTISTNAME + "=?";

	/**
	 * default sort order
	 */
	private static final String RECENT_ORDER = RecentStoreColumns.TIMEPLAYED + " DESC";

	/**
	 * Name of database file
	 */
	public static final String DATABASENAME = "albumhistory.db";

	/**
	 * Version constant to increment when the database should be rebuilt
	 */
	private static final int VERSION = 2;

	/**
	 * singleton instance of this class
	 */
	private static RecentStore sInstance;

	/**
	 * Constructor of <code>RecentStore</code>
	 *
	 * @param context The {@link Context} to use
	 */
	private RecentStore(Context context) {
		super(context, DATABASENAME, null, VERSION);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return A new instance of this class.
	 */
	public static synchronized RecentStore getInstance(Context context) {
		if (sInstance == null) {
			// initialize with application context to avoid memory leak
			sInstance = new RecentStore(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + RecentStoreColumns.NAME);
		onCreate(db);
	}

	/**
	 * Used to store artist IDs in the database.
	 *
	 * @param albumName  The album name.
	 * @param artistName The artist album name.
	 * @param songCount  The number of tracks for the album.
	 * @param albumYear  The year the album was released.
	 */
	public void addAlbumId(long albumId, String albumName, String artistName, String songCount, String albumYear) {
		if (albumId > 0 && albumName != null && artistName != null && songCount != null) {
			SQLiteDatabase database = getWritableDatabase();
			ContentValues values = new ContentValues(6);

			values.put(RecentStoreColumns.ID, albumId);
			values.put(RecentStoreColumns.ALBUMNAME, albumName);
			values.put(RecentStoreColumns.ARTISTNAME, artistName);
			values.put(RecentStoreColumns.ALBUMSONGCOUNT, songCount);
			values.put(RecentStoreColumns.ALBUMYEAR, albumYear);
			values.put(RecentStoreColumns.TIMEPLAYED, System.currentTimeMillis());

			database.beginTransaction();
			database.insertWithOnConflict(RecentStoreColumns.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			database.setTransactionSuccessful();
			database.endTransaction();
		}
	}

	/**
	 * Used to retrieve the most recently listened album for an artist.
	 *
	 * @param artistName The artistName to reference.
	 * @return The most recently listened album for an artist.
	 */
	@Nullable
	public String getAlbumName(String artistName) {
		String result = null;
		if (!TextUtils.isEmpty(artistName)) {
			String[] having = {artistName};
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = database.query(RecentStoreColumns.NAME, RECENT_PROJECTION, RECENT_SELECT_NAME,
					having, null, null, RECENT_ORDER);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(1);
				}
				cursor.close();
			}
		}
		return result;
	}

	/**
	 * remove recent album from history
	 *
	 * @param albumId ID of the album to remove
	 */
	public void removeItem(long albumId) {
		String[] args = {Long.toString(albumId)};
		SQLiteDatabase database = getWritableDatabase();
		database.delete(RecentStoreColumns.NAME, RECENT_SELECT_ID, args);
	}


	public interface RecentStoreColumns {

		/* Table name */
		String NAME = "albumhistory";

		/* Album IDs column */
		String ID = "albumid";

		/* Album name column */
		String ALBUMNAME = "itemname";

		/* Artist name column */
		String ARTISTNAME = "artistname";

		/* Album song count column */
		String ALBUMSONGCOUNT = "albumsongcount";

		/* Album year column. It's okay for this to be null */
		String ALBUMYEAR = "albumyear";

		/* Time played column */
		String TIMEPLAYED = "timeplayed";
	}
}