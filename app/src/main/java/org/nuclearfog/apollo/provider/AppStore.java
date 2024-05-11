package org.nuclearfog.apollo.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;

public abstract class AppStore {

	/**
	 * path to the database file
	 */
	private File databasePath;

	/**
	 * database
	 */
	private SQLiteDatabase db;

	/**
	 *
	 */
	protected AppStore(Context context, String name) {
		databasePath = context.getDatabasePath(name);
		try {
			db = context.openOrCreateDatabase(databasePath.toString(), Context.MODE_PRIVATE, null);
		} catch (SQLiteException exception) {
			SQLiteDatabase.deleteDatabase(databasePath);
			db = context.openOrCreateDatabase(databasePath.toString(), Context.MODE_PRIVATE, null);
		}
		onCreate(db);
	}


	protected SQLiteDatabase getWritableDatabase() {
		db.beginTransaction();
		return db;
	}


	public SQLiteDatabase getReadableDatabase() {
		return db;
	}


	protected void commit() {
		if (db.inTransaction()) {
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}

	protected abstract void onCreate(SQLiteDatabase db);
}
