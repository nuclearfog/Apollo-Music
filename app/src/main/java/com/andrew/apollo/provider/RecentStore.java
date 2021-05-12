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

import com.andrew.apollo.ui.activities.ProfileActivity;

/**
 * The {@link RecentStore} is used to display a a grid or list of
 * recently listened to albums. In order to populate the this grid or list with
 * the correct data, we keep a cache of the album ID, name, and time it was
 * played to be retrieved later.
 * <p>
 * In {@link ProfileActivity}, when viewing the profile for an artist, the first
 * image the carousel header is the last album the user listened to for that
 * particular artist. That album is retrieved using
 * {@link #getAlbumName(String)}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentStore extends SQLiteOpenHelper {

    private static final String[] RECENT_PROJECTION = {
            RecentStoreColumns.ID,
            RecentStoreColumns.ALBUMNAME,
            RecentStoreColumns.ARTISTNAME,
            RecentStoreColumns.TIMEPLAYED
    };

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
            + RecentStoreColumns.ID + " LONG NOT NULL," + RecentStoreColumns.ALBUMNAME
            + " TEXT NOT NULL," + RecentStoreColumns.ARTISTNAME + " TEXT NOT NULL,"
            + RecentStoreColumns.ALBUMSONGCOUNT + " TEXT NOT NULL,"
            + RecentStoreColumns.ALBUMYEAR + " TEXT," + RecentStoreColumns.TIMEPLAYED
            + " LONG NOT NULL);";

    private static final String RECENT_SELECT_ID = RecentStoreColumns.ID + "=?";

    private static final String RECENT_SELECT_NAME = RecentStoreColumns.ARTISTNAME + "=?";

    private static final String RECENT_ORDER = RecentStoreColumns.TIMEPLAYED + " DESC";


    /* Name of database file */
    public static final String DATABASENAME = "albumhistory.db";
    /* Version constant to increment when the database should be rebuilt */
    private static final int VERSION = 1;
    private static RecentStore sInstance = null;

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
    public void addAlbumId(Long albumId, String albumName, String artistName, String songCount, String albumYear) {
        if (albumId != null && albumName != null && artistName != null && songCount != null) {
            SQLiteDatabase database = getWritableDatabase();
            ContentValues values = new ContentValues(6);
            String[] args = {String.valueOf(albumId)};

            values.put(RecentStoreColumns.ID, albumId);
            values.put(RecentStoreColumns.ALBUMNAME, albumName);
            values.put(RecentStoreColumns.ARTISTNAME, artistName);
            values.put(RecentStoreColumns.ALBUMSONGCOUNT, songCount);
            values.put(RecentStoreColumns.ALBUMYEAR, albumYear);
            values.put(RecentStoreColumns.TIMEPLAYED, System.currentTimeMillis());

            database.beginTransaction();
            database.delete(RecentStoreColumns.NAME, RECENT_SELECT_ID, args);
            database.insert(RecentStoreColumns.NAME, null, values);
            database.setTransactionSuccessful();
            database.endTransaction();
        }
    }

    /**
     * Used to retrieve the most recently listened album for an artist.
     *
     * @param key The key to reference.
     * @return The most recently listened album for an artist.
     */
    public String getAlbumName(String key) {
        if (!TextUtils.isEmpty(key)) {
            SQLiteDatabase database = getReadableDatabase();
            String[] having = {key};
            Cursor cursor = database.query(RecentStoreColumns.NAME, RECENT_PROJECTION, RECENT_SELECT_NAME,
                    having, null, null, RECENT_ORDER);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(RecentStoreColumns.ALBUMNAME));
                    cursor.close();
                    return album;
                }
                cursor.close();
            }
        }
        return null;
    }

    /**
     * remove recent album from history
     *
     * @param albumId ID of the album to remove
     */
    public void removeItem(long albumId) {
        String[] args = {Long.toString(albumId)};
        SQLiteDatabase database = getReadableDatabase();
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