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
 *
 * @author nuclearfog
 */
public class FolderLoader extends AsyncExecutor<Void, List<Folder>> {

	private static final String TAG = "FolderLoader";


	public FolderLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Folder> doInBackground(Void v) {
		// init tree set to sort folder by name
		Map<String, Folder> folderMap = new TreeMap<>();
		Context context = getContext();
		if (context != null) {
			ExcludeStore exclude_db = ExcludeStore.getInstance(context);
			try {
				Set<Long> excludedIds = exclude_db.getIds(Type.SONG);
				Cursor cursor = CursorFactory.makeFolderCursor(context);
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
		}
		return new ArrayList<>(folderMap.values());
	}
}