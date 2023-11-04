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
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;


/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAdapter extends ArrayAdapter<Artist> {

	/**
	 * The resource Id of the layout to inflate
	 */
	private int mLayoutId;

	/**
	 * Image cache and image fetcher
	 */
	private ImageFetcher mImageFetcher;

	/**
	 * Loads line three and the background image if the user decides to.
	 */
	private boolean mLoadExtraData = false;

	/**
	 * Constructor of <code>ArtistAdapter</code>
	 *
	 * @param context  The {@link Context} to use.
	 * @param layoutId The resource Id of the view to inflate.
	 */
	public ArtistAdapter(Context context, int layoutId) {
		super(context, 0);
		// Get the layout Id
		mLayoutId = layoutId;
		// Initialize the cache & image fetcher
		mImageFetcher = ApolloUtils.getImageFetcher(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(mLayoutId, parent, false);
			holder = new MusicHolder(convertView);
			if (holder.mLineThree != null && !mLoadExtraData) {
				holder.mLineThree.setVisibility(View.GONE);
			}
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		Artist artist = getItem(position);
		if (artist != null) {
			// Number of albums (line two)
			String numAlbums = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, artist.getAlbumCount());
			// Set each artist name (line one)
			holder.mLineOne.setText(artist.getName());
			// Set the number of albums (line two)
			holder.mLineTwo.setText(numAlbums);
			// Asynchronously load the artist image into the adapter
			mImageFetcher.loadArtistImage(artist.getName(), holder.mImage);
			if (mLoadExtraData) {
				// Number of songs (line three)
				String numTracks = MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, artist.getTrackCount());
				// Set the number of songs (line three)
				if (holder.mLineThree != null)
					holder.mLineThree.setText(numTracks);
				// register artist art click listener
				ApolloUtils.registerItemViewListener(holder.mImage, parent, position, artist.getId());
			}
			if (artist.isVisible()) {
				convertView.setAlpha(1.0f);
			} else {
				convertView.setAlpha(Config.OPACITY_HIDDEN);
			}
		}
		return convertView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		if (getItem(position) != null)
			return getItem(position).getId();
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
	}
}
