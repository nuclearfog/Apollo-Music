package org.nuclearfog.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.Nullable;

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
public class MusicSearchLoader extends WrappedAsyncTaskLoader<List<Music>> {


	/**
	 * search string as argument
	 */
	private String search;


	public MusicSearchLoader(Context context, String search) {
		super(context);
		this.search = search;
	}


	@Nullable
	@Override
	public List<Music> loadInBackground() {
		List<Music> result = new LinkedList<>();
		// search for artists
		Cursor cursor = CursorFactory.makeArtistSearchCursor(getContext(), search);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(0);
					String artistName = cursor.getString(1);
					int albumCount = cursor.getInt(2);
					int songCount = cursor.getInt(3);
					Artist artist = new Artist(id, artistName, songCount, albumCount);
					result.add(artist);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		// search for albums
		cursor = CursorFactory.makeAlbumSearchCursor(getContext(), search);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(0);
					String albumName = cursor.getString(1);
					String artist = cursor.getString(2);
					int songCount = cursor.getInt(3);
					String year = cursor.getString(4);
					Album album = new Album(id, albumName, artist, songCount, year);
					result.add(album);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		// Search for tracks
		cursor = CursorFactory.makeTrackSearchCursor(getContext(), search);
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
		return result;
	}
}