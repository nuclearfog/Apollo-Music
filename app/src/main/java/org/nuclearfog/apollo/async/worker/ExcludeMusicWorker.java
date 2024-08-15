package org.nuclearfog.apollo.async.worker;

import android.content.Context;
import android.database.Cursor;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Folder;
import org.nuclearfog.apollo.model.Genre;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.ExcludeStore;
import org.nuclearfog.apollo.store.ExcludeStore.Type;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Worker class used to hide music items asynchronously
 *
 * @author nuclearfog
 */
public class ExcludeMusicWorker extends AsyncExecutor<Music, Boolean> {

	private ExcludeStore exclude;


	public ExcludeMusicWorker(Context context) {
		super(context);
		exclude = ExcludeStore.getInstance(context);
	}


	@Override
	protected Boolean doInBackground(Music param) {
		Context context = getContext();
		if (context != null) {
			ExcludeStore.Type type = null;
			long[] ids = {param.getId()};
			if (param instanceof Folder) {
				type = Type.SONG;
				// get all songs of a folder
				String path = ((Folder) param).getPath();
				Cursor cursor = CursorFactory.makeFolderSongCursor(context, path);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						List<Long> idList = new LinkedList<>();
						int idxName = path.length() + 1;
						do {
							String filename = cursor.getString(7);
							if (filename.indexOf('/', idxName) < 0) {
								idList.add(cursor.getLong(0));
							}
						} while (cursor.moveToNext());
						ids = ApolloUtils.toLongArray(idList);
					}
					cursor.close();
				}
			} else if (param instanceof Album) {
				type = Type.ALBUM;
			} else if (param instanceof Artist) {
				type = Type.ARTIST;
			} else if (param instanceof Genre) {
				type = Type.GENRE;
				ids = ((Genre) param).getGenreIds();
			} else if (param instanceof Song) {
				type = Type.SONG;
			}
			if (type != null && ids.length > 0) {
				if (param.isVisible()) {
					exclude.addIds(type, ids);
					return true;
				} else {
					exclude.removeIds(type, ids);
					return false;
				}
			}
		}
		return null;
	}
}