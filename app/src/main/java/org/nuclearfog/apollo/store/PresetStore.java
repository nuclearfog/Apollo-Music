package org.nuclearfog.apollo.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.nuclearfog.apollo.model.AudioPreset;

import java.util.LinkedList;
import java.util.List;

/**
 * Database used to store audio effect presets
 *
 * @author nuclearfog
 */
public class PresetStore extends AppStore {

	/**
	 * sqlite database create query
	 */
	private static final String TABLE_PRESET = "CREATE TABLE IF NOT EXISTS " + PresetColumns.TABLE + "("
			+ PresetColumns.NAME + " TEXT PRIMARY KEY,"
			+ PresetColumns.EQUALIZER + " TEXT NOT NULL,"
			+ PresetColumns.TIME + " INTEGER,"
			+ PresetColumns.BASS + " INTEGER,"
			+ PresetColumns.REVERB + " INTEGER)";

	/**
	 * database columns
	 */
	private static final String[] COLUMNS = {
			PresetColumns.NAME,
			PresetColumns.BASS,
			PresetColumns.REVERB,
			PresetColumns.EQUALIZER
	};

	/**
	 * preset selection query
	 */
	private static final String PRESET_SELECT = PresetColumns.NAME + "=?";

	/**
	 * default sort order
	 */
	private static final String SORT = PresetColumns.TIME + " DESC";

	/**
	 * database name
	 */
	private static final String DB_NAME = "fx_presets";

	private static PresetStore instance;


	private PresetStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * get singleton instance
	 */
	public static PresetStore getInstance(Context context) {
		if (instance == null) {
			instance = new PresetStore(context);
		}
		return instance;
	}


	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_PRESET);
	}

	/**
	 * called to save custom audio preset
	 *
	 * @param preset preset to save
	 */
	public synchronized void savePreset(AudioPreset preset) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues column = new ContentValues();
		column.put(PresetColumns.NAME, preset.getName());
		column.put(PresetColumns.BASS, preset.getBassLevel());
		column.put(PresetColumns.REVERB, preset.getReverbLevel());
		column.put(PresetColumns.TIME, System.currentTimeMillis());
		StringBuilder buf = new StringBuilder();
		for (int band : preset.getBands()) {
			buf.append(band).append(";");
		}
		if (buf.length() > 0) {
			buf.deleteCharAt(buf.length() - 1);
		}
		column.put(PresetColumns.EQUALIZER, buf.toString());
		db.insertWithOnConflict(PresetColumns.TABLE, "", column, SQLiteDatabase.CONFLICT_REPLACE);
		commit();
	}

	/**
	 * delete existing preset
	 *
	 * @param preset preset to remove
	 */
	public synchronized void deletePreset(AudioPreset preset) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(PresetColumns.TABLE, PRESET_SELECT, new String[]{preset.getName()});
		commit();
	}

	/**
	 * loads all audio presets stored by the user
	 */
	public synchronized List<AudioPreset> loadPresets() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(PresetColumns.TABLE, COLUMNS, null, null, null, null, SORT);
		List<AudioPreset> result = new LinkedList<>();
		if (cursor.moveToFirst()) {
			do {
				String name = cursor.getString(0);
				int bass = cursor.getInt(1);
				int reverb = cursor.getInt(2);
				String[] bands = cursor.getString(3).split(";");
				int[] level = new int[bands.length];
				for (int i = 0; i < bands.length; i++) {
					level[i] = Integer.parseInt(bands[i]);
				}
				AudioPreset preset = new AudioPreset(name, level, bass, reverb);
				result.add(preset);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return result;
	}

	/**
	 * database table
	 */
	interface PresetColumns {
		/**
		 * table name
		 */
		String TABLE = "audio_presets";
		/**
		 * preset name (primary key)
		 * value type is String
		 */
		String NAME = "preset_name";
		/**
		 * a string of numbers of the equalizer levels
		 */
		String EQUALIZER = "eq_bands";
		/**
		 * bass level (integer)
		 */
		String BASS = "bass_boost";
		/**
		 * reverb level (integer)
		 */
		String REVERB = "reverb";
		/**
		 * time when the preset was created/updated
		 */
		String TIME = "timestamp";
	}
}