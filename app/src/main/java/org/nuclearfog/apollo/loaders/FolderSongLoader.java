package org.nuclearfog.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to get all songs of a music folder
 * decompiled from Apollo.APK version 1.6
 */
public class FolderSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

	/**
	 * folder to search tracks
	 */
	private String folderName;

	/**
	 * @param paramContext Application context
	 * @param folderName   name of the music folder
	 */
	public FolderSongLoader(Context paramContext, String folderName) {
		super(paramContext);
		this.folderName = folderName;
	}


	@Override
	public List<Song> loadInBackground() {
		List<Song> result = new LinkedList<>();
		Cursor cursor = CursorFactory.makeFolderSongCursor(getContext(), folderName);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				int idxName = folderName.length() + 1;
				do {
					String filename = cursor.getString(5);
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
		return result;
	}
}