package org.nuclearfog.apollo.async.worker;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.io.File;

/**
 * Async worker used to delete tracks from storage directly
 *
 * @author nuclearfog
 */
public class TrackDeleteWorker extends AsyncExecutor<Long[], Integer> {

	/**
	 * selection to remove track from database
	 */
	private static final String DATABASE_REMOVE_TRACK = AudioColumns._ID + "=?";


	public TrackDeleteWorker(Context context) {
		super(context);
	}


	@Override
	protected Integer doInBackground(Long[] param) {
		Context context = getContext();
		if (context != null) {
			Cursor cursor = CursorFactory.makeTrackListCursor(context, ApolloUtils.toLongArray(param));
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int count = 0;
					String[] result = new String[cursor.getCount()];
					FavoritesStore favStore = FavoritesStore.getInstance(context);
					RecentStore recents = RecentStore.getInstance(context);
					PopularStore popular = PopularStore.getInstance(context);
					ContentResolver resolver = context.getContentResolver();
					for (int i = 0; i < result.length; i++) {
						// Remove from current playlist
						long trackId = cursor.getLong(0);
						result[i] = cursor.getString(1);
						long albumId = cursor.getLong(2);
						String[] idStr = {Long.toString(trackId)};
						// Remove from the favorites playlist
						favStore.removeFavorite(trackId);
						// Remove any items in the recents database
						recents.removeAlbum(albumId);
						// remove track from most played list
						popular.removeItem(trackId);
						// remove track from database
						resolver.delete(Media.EXTERNAL_CONTENT_URI, DATABASE_REMOVE_TRACK, idStr);
						// move to next track
						cursor.moveToNext();
						// delete file
						try {
							File file = new File(result[i]);
							if (file.delete()) {
								count++;
							}
						} catch (RuntimeException exception) {
							if (BuildConfig.DEBUG) {
								Log.e("MusicUtils", "Failed to delete file " + result[i]);
							}
						}
					}
					return count;
				}
				cursor.close();
			}
		}
		return null;
	}
}