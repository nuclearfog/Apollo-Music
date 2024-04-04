package org.nuclearfog.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.model.Folder;
import org.nuclearfog.apollo.provider.ExcludeStore;
import org.nuclearfog.apollo.provider.ExcludeStore.Type;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * return all music folders from storage
 */
public class FolderLoader extends WrappedAsyncTaskLoader<List<Folder>> {

	private static final String TAG = "FolderLoader";

	private ExcludeStore exclude_db;

	/**
	 * @param context Activity context
	 */
	public FolderLoader(Context context) {
		super(context);
		exclude_db = ExcludeStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Folder> loadInBackground() {
		// init tree set to sort folder by name
		Map<String, Folder> folderMap = new TreeMap<>();
		try {
			Set<Long> excludedIds = exclude_db.getIds(Type.SONG);
			Cursor cursor = CursorFactory.makeFolderCursor(getContext());
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						String path = cursor.getString(0);
						long songId = cursor.getLong(1);
						boolean visible = !excludedIds.contains(songId);
						Folder folder = new Folder(path, visible);

						Folder entry = folderMap.get(folder.getName());
						if (entry != null && entry.isVisible() && !folder.isVisible()) {
							folderMap.remove(folder.getName());
						}
						folderMap.put(folder.getName(), folder);
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		} catch (Exception exception) {
			Log.e(TAG, "error loading music folder", exception);
		}
		return new ArrayList<>(folderMap.values());
	}
}