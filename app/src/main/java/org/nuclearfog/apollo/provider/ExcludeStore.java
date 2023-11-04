package org.nuclearfog.apollo.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author nuclearfog
 */
public class ExcludeStore extends SQLiteOpenHelper {

	/**
	 * database filename
	 */
	private static final String DB_NAME = "exclude.db";

	/**
	 * database version
	 */
	private static final int VERSION = 1;

	/**
	 *
	 */
	private static final String TABLE_EXCLUDE_TRACKS = "CREATE TABLE IF NOT EXISTS " + ExcludeTable.NAME + "("
			+ ExcludeTable.ID + " INTEGER,"
			+ ExcludeTable.TYPE + " INTEGER);";

	private static final String EXCLUDE_SELECT = ExcludeTable.ID + "=? AND " + ExcludeTable.TYPE + "=?";

	private static final String EXCLUDE_SELECT_TYPE = ExcludeTable.TYPE + "=?";

	private static final Object LOCK = new Object();

	private static ExcludeStore sInstance;

	/**
	 *
	 */
	private ExcludeStore(Context context) {
		super(context, DB_NAME, null, VERSION);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return A new instance of this class
	 */
	public static ExcludeStore getInstance(Context context) {
		if (sInstance == null) {
			// use application context to avoid memory leak
			sInstance = new ExcludeStore(context.getApplicationContext());
		}
		return sInstance;
	}



	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_EXCLUDE_TRACKS);
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	/**
	 * add song IDs to exclude lsit
	 */
	public void addIds(Type type, long... ids) {
		synchronized (LOCK) {
			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();
			for (long id: ids) {
				ContentValues column = new ContentValues(2);
				column.put(ExcludeTable.ID, id);
				column.put(ExcludeTable.TYPE, type.id);
				database.insertWithOnConflict(ExcludeTable.NAME, null, column, SQLiteDatabase.CONFLICT_IGNORE);
			}
			database.setTransactionSuccessful();
			database.endTransaction();
		}
	}

	/**
	 * remove song IDs from exclode list
	 */
	public void removeIds(Type type, long... ids) {
		synchronized (LOCK) {
			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();
			for (long id: ids) {
				database.delete(ExcludeTable.NAME, EXCLUDE_SELECT, new String[]{Long.toString(id), Integer.toString(type.id)});
			}
			database.setTransactionSuccessful();
			database.endTransaction();
		}
	}

	/**
	 * get excluded song IDs
	 */
	public Set<Long> getIds(Type type) {
		Set<Long> result = new TreeSet<>();
		synchronized (LOCK) {
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = database.query(ExcludeTable.NAME, new String[]{ExcludeTable.ID}, EXCLUDE_SELECT_TYPE, new String[]{Integer.toString(type.id)}, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					result.add(cursor.getLong(0));
				} while(cursor.moveToNext());
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * excluded tracks table columns
	 */
	public interface ExcludeTable {

		String NAME = "excluded_music";

		String ID = "id";

		String TYPE = "type";
	}

	/**
	 *
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