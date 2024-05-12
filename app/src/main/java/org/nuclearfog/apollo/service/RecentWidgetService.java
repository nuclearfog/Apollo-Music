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

package org.nuclearfog.apollo.service;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageCache;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.ui.widgets.RecentWidgetProvider;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to build the recently listened list for the
 * {@link RecentWidgetProvider}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentWidgetService extends RemoteViewsService {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetRemoteViewsFactory(getApplicationContext());
	}

	/**
	 * This is the factory that will provide data to the collection widget.
	 */
	private static class WidgetRemoteViewsFactory implements RemoteViewsFactory {
		/**
		 * Number of views (ImageView and TextView)
		 */
		private static final int VIEW_TYPE_COUNT = 1;

		/**
		 * max recent item number
		 */
		private static final int RECENT_LIMIT = 20;

		/**
		 * Image cache
		 */
		private ImageFetcher mFetcher;

		private Context mContext;

		private List<Album> albums = new ArrayList<>();

		/**
		 * Constructor of <code>WidgetRemoteViewsFactory</code>
		 *
		 * @param context The {@link Context} to use.
		 */
		public WidgetRemoteViewsFactory(Context context) {
			// Initialize the image cache
			mFetcher = ImageFetcher.getInstance(context);
			mFetcher.setImageCache(ImageCache.getInstance(context));
			mContext = context.getApplicationContext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return Math.min(albums.size(), RECENT_LIMIT);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RemoteViews getViewAt(int position) {
			Album album = albums.get(position);
			// Create the remote views
			RemoteViews mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents_items);
			// Set the album names
			mViews.setTextViewText(R.id.app_widget_recents_line_one, album.getName());
			// Set the artist names
			mViews.setTextViewText(R.id.app_widget_recents_line_two, album.getArtist());
			// Set the album art
			Bitmap bitmap = mFetcher.getCachedArtwork(album.getName(), album.getArtist(), album.getId());
			if (bitmap != null) {
				mViews.setImageViewBitmap(R.id.app_widget_recents_base_image, bitmap);
			} else {
				mViews.setImageViewResource(R.id.app_widget_recents_base_image, R.drawable.default_artwork);
			}
			// Open the profile of the touched album
			Intent profileIntent = new Intent();
			profileIntent.putExtra(Config.ID, album.getId());
			profileIntent.putExtra(Config.NAME, album.getName());
			profileIntent.putExtra(Config.ARTIST_NAME, album.getArtist());
			profileIntent.putExtra(RecentWidgetProvider.SET_ACTION, RecentWidgetProvider.OPEN_PROFILE);
			mViews.setOnClickFillInIntent(R.id.app_widget_recents_items, profileIntent);
			// Play the album when the artwork is touched
			Intent playAlbumIntent = new Intent();
			playAlbumIntent.putExtra(Config.ID, album.getId());
			playAlbumIntent.putExtra(RecentWidgetProvider.SET_ACTION, RecentWidgetProvider.PLAY_ALBUM);
			mViews.setOnClickFillInIntent(R.id.app_widget_recents_base_image, playAlbumIntent);
			return mViews;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getViewTypeCount() {
			return VIEW_TYPE_COUNT;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDataSetChanged() {
			albums.clear();
			Cursor mCursor = CursorFactory.makeRecentCursor(mContext);
			if (mCursor != null) {
				if (mCursor.moveToFirst()) {
					do {
						long id = mCursor.getLong(0);
						String albumName = mCursor.getString(1);
						String artist = mCursor.getString(2);
						int songCount = mCursor.getInt(3);
						String year = mCursor.getString(4);
						albums.add(new Album(id, albumName, artist, songCount, year, true));
					} while (mCursor.moveToNext());
				}
				mCursor.close();
			}
		}


		@Override
		public void onDestroy() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RemoteViews getLoadingView() {
			// Nothing to do
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate() {
			// Nothing to do
		}
	}
}