package org.nuclearfog.apollo.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Set;
import java.util.TreeSet;

/**
 * database used to save lists of excluded IDs (e.g. Album IDs)
 *
 * @author nuclearfog
 */
public class ExcludeStore extends AppStore {

	/**
	 * sqlite query to create a new table
	 */
	private static final String TABLE_EXCLUDE_TRACKS = "CREATE TABLE IF NOT EXISTS " + ExcludeTable.NAME + "("
			+ ExcludeTable.ID + " INTEGER,"
			+ ExcludeTable.TYPE + " INTEGER);";

	/**
	 * select where condition
	 */
	private static final String EXCLUDE_SELECT = ExcludeTable.ID + "=? AND " + ExcludeTable.TYPE + "=?";
	private static final String EXCLUDE_SELECT_TYPE = ExcludeTable.TYPE + "=?";

	/**
	 * select columns
	 */
	private static final String[] COLUMNS = {
			ExcludeTable.ID
	};

	/**
	 * database filename
	 */
	private static final String DB_NAME = "exclude.db";


	private static ExcludeStore sInstance;

	/**
	 *
	 */
	private ExcludeStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return A singleton instance of this class
	 */
	public static ExcludeStore getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new ExcludeStore(context);
		}
		return sInstance;
	}


	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_EXCLUDE_TRACKS);
	}

	/**
	 * add item ID to exclude lsit
	 *
	 * @param type type of item to exclude
	 * @param ids  IDs to exclude
	 */
	public synchronized void addIds(Type type, long... ids) {
		SQLiteDatabase database = getWritableDatabase();
		for (long id : ids) {
			ContentValues column = new ContentValues(2);
			column.put(ExcludeTable.ID, id);
			column.put(ExcludeTable.TYPE, type.id);
			database.insertWithOnConflict(ExcludeTable.NAME, null, column, SQLiteDatabase.CONFLICT_IGNORE);
		}
		commit();
	}

	/**
	 * remove item IDs from the database
	 *
	 * @param type type of item to exclude
	 * @param ids  IDs to exclude
	 */
	public synchronized void removeIds(Type type, long... ids) {
		SQLiteDatabase database = getWritableDatabase();
		for (long id : ids) {
			String[] args = {Long.toString(id), Integer.toString(type.id)};
			database.delete(ExcludeTable.NAME, EXCLUDE_SELECT, args);
		}
		commit();
	}

	/**
	 * get a set of excluded IDs
	 *
	 * @param type type of items to get the exclude list from
	 * @return a set of IDs
	 */
	public synchronized Set<Long> getIds(Type type) {
		Set<Long> result = new TreeSet<>();
		SQLiteDatabase database = getReadableDatabase();
		String[] args = {Integer.toString(type.id)};
		Cursor cursor = database.query(ExcludeTable.NAME, COLUMNS, EXCLUDE_SELECT_TYPE, args, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				result.add(cursor.getLong(0));
			} while (cursor.moveToNext());
		}
		cursor.close();
		return result;
	}

	/**
	 * excluded tracks table columns
	 */
	private interface ExcludeTable {
		/**
		 * table name
		 */
		String NAME = "excluded_music";
		/**
		 * ID of the excluded item (e.g. album ID)
		 */
		String ID = "id";
		/**
		 * type of the excluded item {@link Type}
		 */
		String TYPE = "type";
	}

	/**
	 * Used to categorize the ID to exclude (e.g. album ID, song ID)
	 */
	public enum Type {
		SONG(0),
		ALBUM(1),
		ARTIST(2),
		GENRE(3);

		private final int id;

		Type(int id) {
			this.id = id;
		}
	}
}