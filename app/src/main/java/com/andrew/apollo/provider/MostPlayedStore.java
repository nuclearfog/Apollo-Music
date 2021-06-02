package com.andrew.apollo.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

/**
 *
 */
public class MostPlayedStore extends SQLiteOpenHelper {

    /**
     * column projection of track table
     */
    private static final String[] MOSTPLAYED_COLUMNS = {
            MostPlayedColumns.ID,
            MostPlayedColumns.SONGNAME,
            MostPlayedColumns.ALBUMNAME,
            MostPlayedColumns.ARTISTNAME,
            MostPlayedColumns.PLAYCOUNT
    };

    /**
     * query to create track table
     */
    private static final String MOSTPLAYED_TABLE = "CREATE TABLE IF NOT EXISTS " + MostPlayedColumns.NAME + " (" +
            MostPlayedColumns.ID + " LONG PRIMARY KEY," + MostPlayedColumns.SONGNAME + " TEXT NOT NULL," +
            MostPlayedColumns.ALBUMNAME + " TEXT NOT NULL," + MostPlayedColumns.ARTISTNAME + " TEXT NOT NULL," +
            MostPlayedColumns.PLAYCOUNT + " LONG NOT NULL," + MostPlayedColumns.DURATION + " LONG);";

    /**
     * condition to find track in most played table
     */
    private static final String TRACK_SELECT = MostPlayedColumns.ID + "=?";

    /**
     * database filename
     */
    public static final String DB_NAME = "mostplayed.db";

    /**
     * database version
     */
    private static final int VERSION = 1;

    private static MostPlayedStore singleton;


    private MostPlayedStore(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    /**
     * create singleton instance
     *
     * @return singleton instance of this class
     */
    public static MostPlayedStore getInstance(Context context) {
        if (singleton == null) {
            singleton = new MostPlayedStore(context.getApplicationContext());
        }
        return singleton;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MOSTPLAYED_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MostPlayedColumns.NAME);
        onCreate(db);
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
        // increment by 1
        long playCount = getPlayCount(songId) + 1;
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues(6);
        database.beginTransaction();

        values.put(MostPlayedColumns.ID, songId);
        values.put(MostPlayedColumns.SONGNAME, songName);
        values.put(MostPlayedColumns.ALBUMNAME, albumName);
        values.put(MostPlayedColumns.ARTISTNAME, artistName);
        values.put(MostPlayedColumns.PLAYCOUNT, playCount);
        values.put(MostPlayedColumns.DURATION, duration);

        database.insertWithOnConflict(MostPlayedColumns.NAME, null, values, CONFLICT_REPLACE);
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    /**
     * remove track from most played databse
     *
     * @param trackId ID of the track to remove
     */
    public void removeItem(long trackId) {
        String[] args = {Long.toString(trackId)};
        SQLiteDatabase database = getWritableDatabase();
        database.delete(MostPlayedColumns.NAME, TRACK_SELECT, args);
        database.close();
    }

    /**
     * Used to retrieve how often a track was played
     *
     * @param songId The song Id to reference
     * @return The play count for a song
     */
    private long getPlayCount(long songId) {
        long result = 0;
        if (songId >= 0) {
            String[] having = {Long.toString(songId)};
            SQLiteDatabase database = getReadableDatabase();
            Cursor cursor = database.query(MostPlayedColumns.NAME, MOSTPLAYED_COLUMNS, TRACK_SELECT, having, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = cursor.getLong(4);
                }
                cursor.close();
            }
            database.close();
        }
        return result;
    }

    /**
     * columns of the most played tracks table
     */
    public interface MostPlayedColumns {

        /* Table name */
        String NAME = "mostplayed";

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
