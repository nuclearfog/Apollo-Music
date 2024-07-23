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

package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This adapter is used to display all of the albums on a user's
 * device for {@link org.nuclearfog.apollo.ui.fragments.RecentFragment}
 * and {@link org.nuclearfog.apollo.ui.fragments.AlbumFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumAdapter extends AlphabeticalAdapter<Album> {

	/**
	 * Image cache and image fetcher
	 */
	private ImageFetcher mImageFetcher;

	/**
	 * The resource Id of the layout to inflate
	 */
	private int mLayoutId;

	/**
	 * Determines if the grid or list should be the default style
	 */
	private boolean mLoadExtraData = false;

	/**
	 * Constructor of <code>AlbumAdapter</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public AlbumAdapter(Context context, int columns, @LayoutRes int mLayoutId) {
		super(context, columns, mLayoutId);
		this.mLayoutId = mLayoutId;
		// Initialize the cache & image fetcher
		mImageFetcher = ApolloUtils.getImageFetcher(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@NonNull
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(mLayoutId, parent, false);
			holder = new MusicHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the data holder
		Album album = getItem(position);
		if (album != null) {
			// Set each album name (line one)
			holder.mLineOne.setText(album.getName());
			// Set the artist name (line two)
			holder.mLineTwo.setText(album.getArtist());
			// Asynchronously load the album images into the adapter
			mImageFetcher.loadAlbumImage(album, holder.mImage);
			// List view only items
			if (mLoadExtraData) {
				// Set the number of songs (line three)
				if (holder.mLineThree != null) {
					String count = StringUtils.makeLabel(getContext(), R.plurals.Nsongs, album.getTrackCount());
					holder.mLineThree.setText(count);
				}
				// register album art click listener
				ApolloUtils.registerItemViewListener(holder.mImage, parent, position, album.getId());
			}
			if (album.isVisible()) {
				convertView.setAlpha(1.0f);
			} else {
				convertView.setAlpha(Constants.OPACITY_HIDDEN);
			}
		}
		return convertView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		Music item = getItem(position);
		if (item != null)
			return item.getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * @param pause True to temporarily pause the disk cache, false otherwise.
	 */
	public void setPauseDiskCache(boolean pause) {
		if (mImageFetcher != null) {
			mImageFetcher.setPauseDiskCache(pause);
		}
	}

	/**
	 * Flushes the disk cache.
	 */
	public void flush() {
		mImageFetcher.flush();
	}

	/**
	 * enable extra information
	 */
	public void setLoadExtraData() {
		mLoadExtraData = true;
		notifyDataSetChanged();
	}
}