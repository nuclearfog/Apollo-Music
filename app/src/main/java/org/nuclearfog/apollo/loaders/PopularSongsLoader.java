package org.nuclearfog.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Loader to get the most played tracks, sorted by popularity
 *
 * @author nuclearfog
 */
public class PopularSongsLoader extends AsyncExecutor<Void, List<Song>> {

	private static final String TAG = "PopularSongsLoader";


	public PopularSongsLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			try {
				// Create the Cursor
				Cursor mCursor = CursorFactory.makePopularCursor(context);
				// Gather the data
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							// Copy the song Id
							long id = mCursor.getLong(0);
							// Copy the song name
							String songName = mCursor.getString(1);
							// Copy the artist name
							String artist = mCursor.getString(3);
							// Copy the album name
							String album = mCursor.getString(2);
							// Copy the duration value in milliseconds
							long duration = mCursor.getLong(5);
							// Create a new song
							Song song = new Song(id, songName, artist, album, duration);
							// Add everything up
							result.add(song);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs", exception);
			}
		}
		return result;
	}
}