package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.PopularStore;

import java.util.List;

/**
 * Loader to get the most played tracks, sorted by popularity
 *
 * @author nuclearfog
 */
public class PopularSongsLoader extends AsyncExecutor<Void, List<Song>> {

	private PopularStore popularStore;

	public PopularSongsLoader(Context context) {
		super(context);
		popularStore = PopularStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		return popularStore.getSongs();
	}
}