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
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.store.RecentStore;
import java.util.List;

/**
 * Used to return the last listened to albums.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class RecentLoader extends AsyncExecutor<Void, List<Album>> {

	private RecentStore recentStore;


	public RecentLoader(Context context) {
		super(context);
		recentStore = RecentStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Album> doInBackground(Void v) {
		return recentStore.getRecentAlbums();
	}
}