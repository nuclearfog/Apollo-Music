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

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoritesStore extends SQLiteOpenHelper {

    /* Name of database file */
    public static final String DATABASENAME = "favorites.db";
    /* Version constant to increment when the database should be rebuilt */
    private static final int VERSION = 1;
    private static FavoritesStore sInstance = null;

    /**
     * Constructor of <code>FavoritesStore</code>
     *
     * @param context The {@link Context} to use
     */
    public FavoritesStore(Context context) {
        super(context, DATABASENAME, null, VERSION);
    }

    /**
     * @param context The {@link Context} to use
     * @return A new instance of this class
     */
    public static synchronized FavoritesStore getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FavoritesStore(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FavoriteColumns.NAME + " (" + FavoriteColumns.ID
                + " LONG NOT NULL," + FavoriteColumns.SONGNAME + " TEXT NOT NULL,"
                + FavoriteColumns.ALBUMNAME + " TEXT NOT NULL," + FavoriteColumns.ARTISTNAME
                + " TEXT NOT NULL," + FavoriteColumns.PLAYCOUNT + " LONG NOT NULL);");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FavoriteColumns.NAME);
        onCreate(db);
    }

    /**
     * Used to store song Ids in our database
     *
     * @param songId     The album's ID
     * @param songName   The song name
     * @param albumName  The album name
     * @param artistName The artist name
     */
    public void addSongId(Long songId, String songName, String albumName, String artistName) {
        if (songId == null || songName == null || albumName == null || artistName == null) {
            return;
        }
        Long playCount = getPlayCount(songId);
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues(5);
        database.beginTransaction();

        values.put(FavoriteColumns.ID, songId);
        values.put(FavoriteColumns.SONGNAME, songName);
        values.put(FavoriteColumns.ALBUMNAME, albumName);
        values.put(FavoriteColumns.ARTISTNAME, artistName);
        values.put(FavoriteColumns.PLAYCOUNT, playCount != 0 ? playCount + 1 : 1);

        database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{String.valueOf(songId)});
        database.insert(FavoriteColumns.NAME, null, values);
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * Used to retrieve a single song Id from our database
     *
     * @param songId The song Id to reference
     * @return The song Id
     */
    public Long getSongId(Long songId) {
        if (songId <= -1) {
            return null;
        }
        SQLiteDatabase database = getReadableDatabase();
        String[] projection = new String[]{
                FavoriteColumns.ID, FavoriteColumns.SONGNAME, FavoriteColumns.ALBUMNAME,
                FavoriteColumns.ARTISTNAME, FavoriteColumns.PLAYCOUNT
        };
        String selection = FavoriteColumns.ID + "=?";
        String[] having = new String[]{String.valueOf(songId)};
        Cursor cursor = database.query(FavoriteColumns.NAME, projection, selection, having, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Long id = cursor.getLong(cursor.getColumnIndexOrThrow(FavoriteColumns.ID));
                cursor.close();
                return id;
            }
            cursor.close();
        }
        return null;
    }

    /**
     * Used to retrieve the play count
     *
     * @param songId The song Id to reference
     * @return The play count for a song
     */
    public Long getPlayCount(Long songId) {
        if (songId <= -1) {
            return null;
        }
        SQLiteDatabase database = getReadableDatabase();
        String[] projection = new String[]{
                FavoriteColumns.ID, FavoriteColumns.SONGNAME, FavoriteColumns.ALBUMNAME,
                FavoriteColumns.ARTISTNAME, FavoriteColumns.PLAYCOUNT
        };
        String selection = FavoriteColumns.ID + "=?";
        String[] having = new String[]{String.valueOf(songId)};
        Cursor cursor = database.query(FavoriteColumns.NAME, projection, selection, having, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Long playCount = cursor.getLong(cursor.getColumnIndexOrThrow(FavoriteColumns.PLAYCOUNT));
                cursor.close();
                return playCount;
            }
            cursor.close();
        }
        return (long) 0;
    }

    /**
     * Toggle the current song as favorite
     */
    public void toggleSong(Long songId, String songName, String albumName, String artistName) {
        if (getSongId(songId) == null) {
            addSongId(songId, songName, albumName, artistName);
        } else {
            removeItem(songId);
        }
    }

    /**
     *
     */
    public void removeItem(Long songId) {
        SQLiteDatabase database = getReadableDatabase();
        database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{String.valueOf(songId)});

    }


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
    }
}