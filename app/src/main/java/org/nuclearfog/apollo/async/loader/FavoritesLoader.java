/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;

import java.util.List;

/**
 * Used to query the {@link FavoritesStore} for the tracks marked as favorites.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class FavoritesLoader extends AsyncExecutor<Void, List<Song>> {

	private FavoritesStore favoriteStore;


	public FavoritesLoader(Context context) {
		super(context);
		favoriteStore = FavoritesStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		return favoriteStore.getFavorites();
	}
}