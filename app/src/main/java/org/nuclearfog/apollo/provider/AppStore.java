package org.nuclearfog.apollo.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;

/**
 * Provides methods to access app databases
 *
 * @author nuclearfog
 */
public abstract class AppStore {

	/**
	 * database
	 */
	private SQLiteDatabase db;

	/**
	 *
	 */
	protected AppStore(Context context, String name) {
		File databasePath = context.getDatabasePath(name);
		try {
			db = context.openOrCreateDatabase(databasePath.toString(), Context.MODE_PRIVATE, null);
		} catch (SQLiteException exception) {
			// delete old database and create new database
			SQLiteDatabase.deleteDatabase(databasePath);
			db = context.openOrCreateDatabase(databasePath.toString(), Context.MODE_PRIVATE, null);
		}
		onCreate(db);
	}

	/**
	 * get database instance for write action
	 * call #commit() to confirm
	 *
	 * @return database instance
	 */
	protected SQLiteDatabase getWritableDatabase() {
		db.beginTransaction();
		return db;
	}

	/**
	 * get database for read operation
	 *
	 * @return database instance
	 */
	public SQLiteDatabase getReadableDatabase() {
		return db;
	}

	/**
	 * commit changes to database after write action
	 */
	protected void commit() {
		if (db.inTransaction()) {
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}

	/**
	 * called after database initialization
	 *
	 * @param db database instance
	 */
	protected abstract void onCreate(SQLiteDatabase db);
}
