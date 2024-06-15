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
 * database for popular tracks with the information how often a track was played
 *
 * @author nuclearfog
 */
public class PopularStore extends AppStore {

	/**
	 * column projection of track table
	 */
	private static final String[] MOSTPLAYED_COLUMNS = {
			PopularColumns.ID,
			PopularColumns.SONGNAME,
			PopularColumns.ALBUMNAME,
			PopularColumns.ARTISTNAME,
			PopularColumns.DURATION
	};

	/**
	 * query to create track table
	 */
	private static final String MOSTPLAYED_TABLE = "CREATE TABLE IF NOT EXISTS " + PopularColumns.NAME + " ("
			+ PopularColumns.ID + " LONG PRIMARY KEY,"
			+ PopularColumns.SONGNAME + " TEXT NOT NULL,"
			+ PopularColumns.ALBUMNAME + " TEXT NOT NULL,"
			+ PopularColumns.ARTISTNAME + " TEXT NOT NULL,"
			+ PopularColumns.PLAYCOUNT + " LONG NOT NULL,"
			+ PopularColumns.DURATION + " LONG);";

	/**
	 * condition to find track in most played table
	 */
	private static final String TRACK_SELECT = PopularColumns.ID + "=?";

	/**
	 * database filename
	 */
	private static final String DB_NAME = "popular.db";

	/**
	 * SQLite sport order
	 */
	private static final String MP_ORDER = PopularColumns.PLAYCOUNT + " DESC";

	/**
	 * singleton instance
	 */
	private static PopularStore singleton;

	/**
	 *
	 */
	private PopularStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * create singleton instance
	 *
	 * @return singleton instance of this class
	 */
	public static PopularStore getInstance(Context context) {
		if (singleton == null) {
			singleton = new PopularStore(context.getApplicationContext());
		}
		return singleton;
	}


	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(MOSTPLAYED_TABLE);
	}

	/**
	 * Used to store song IDs in our database
	 *
	 * @param song song to add
	 */
	public synchronized void addSong(@NonNull Song song) {
		long playCount = getPlayCount(song.getId()) + 1; // increment by 1
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues(6);
		values.put(PopularColumns.ID, song.getId());
		values.put(PopularColumns.SONGNAME, song.getName());
		values.put(PopularColumns.ALBUMNAME, song.getAlbum());
		values.put(PopularColumns.ARTISTNAME, song.getArtist());
		values.put(PopularColumns.PLAYCOUNT, playCount);
		values.put(PopularColumns.DURATION, song.getDuration());
		database.insertWithOnConflict(PopularColumns.NAME, null, values, CONFLICT_REPLACE);
		commit();
	}


	public synchronized List<Song> getSongs() {
		SQLiteDatabase data = getReadableDatabase();
		Cursor cursor = data.query(PopularColumns.NAME, MOSTPLAYED_COLUMNS, null, null, null, null, MP_ORDER);
		List<Song> result = new LinkedList<>();
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
	 * remove track from most played databse
	 *
	 * @param trackId ID of the track to remove
	 */
	public synchronized void removeItem(long trackId) {
		String[] args = {Long.toString(trackId)};
		SQLiteDatabase database = getWritableDatabase();
		database.delete(PopularColumns.NAME, TRACK_SELECT, args);
		commit();
	}

	/**
	 * remove all popular tracks from playlist
	 */
	public synchronized void removeAll() {
		SQLiteDatabase database = getWritableDatabase();
		database.delete(PopularColumns.NAME, null, null);
		commit();
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
			Cursor cursor = database.query(PopularColumns.NAME, MOSTPLAYED_COLUMNS, TRACK_SELECT, having, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					result = cursor.getLong(4);
				}
				cursor.close();
			}
		}
		return result;

	}

	/**
	 * columns of the most played tracks table
	 */
	private interface PopularColumns {

		/**
		 * Table name
		 */
		String NAME = "mostplayed";

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