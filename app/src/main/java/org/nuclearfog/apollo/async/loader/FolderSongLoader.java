package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Return all songs in a folder
 *
 * @author nuclearfog
 */
public class FolderSongLoader extends AsyncExecutor<String, List<Song>> {

	private static final String TAG = " FolderSongLoader";


	public FolderSongLoader(Context context) {
		super(context);
	}


	@Override
	protected List<Song> doInBackground(String param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			try {
				Cursor cursor = CursorFactory.makeFolderSongCursor(context, param);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						int idxName = param.length() + 1;
						do {
							String filename = cursor.getString(7);
							// fetch only music files from the current folder
							// sub folders will be skipped
							if (filename.indexOf('/', idxName) < 0) {
								long id = cursor.getLong(0);
								String songTitle = cursor.getString(1);
								String artistName = cursor.getString(2);
								String albumTitle = cursor.getString(3);
								long duration = cursor.getLong(4);
								Song song = new Song(id, songTitle, artistName, albumTitle, duration);
								result.add(song);
							}
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs from folder:", exception);
			}
		}
		return result;
	}
}