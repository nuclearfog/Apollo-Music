package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Backend loader to search for music information
 *
 * @author nuclearfog
 */
public class MusicSearchLoader extends AsyncExecutor<String, List<Music>> {

	private static final String TAG = "MusicSearchLoader";


	public MusicSearchLoader(Context context) {
		super(context);
	}


	@Override
	protected List<Music> doInBackground(String param) {
		List<Music> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			try {
				// search for artists
				Cursor cursor = CursorFactory.makeArtistSearchCursor(context, param);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							long id = cursor.getLong(0);
							String artistName = cursor.getString(1);
							int albumCount = cursor.getInt(2);
							int songCount = cursor.getInt(3);
							Artist artist = new Artist(id, artistName, songCount, albumCount, true);
							result.add(artist);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				// search for albums
				cursor = CursorFactory.makeAlbumSearchCursor(context, param);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							long id = cursor.getLong(0);
							String albumName = cursor.getString(1);
							String artist = cursor.getString(2);
							int songCount = cursor.getInt(3);
							String year = cursor.getString(4);
							Album album = new Album(id, albumName, artist, songCount, year, true);
							result.add(album);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				// Search for tracks
				cursor = CursorFactory.makeTrackSearchCursor(context, param);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							String mime = cursor.getString(6);
							if (mime.startsWith("audio/") || mime.equals("application/ogg")
									|| mime.equals("application/x-ogg")) {
								long id = cursor.getLong(0);
								String songName = cursor.getString(1);
								String artist = cursor.getString(2);
								String album = cursor.getString(3);
								long duration = cursor.getLong(4);
								Song song = new Song(id, songName, artist, album, duration);
								result.add(song);
							}
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading search results:", exception);
			}
		}
		return result;
	}
}