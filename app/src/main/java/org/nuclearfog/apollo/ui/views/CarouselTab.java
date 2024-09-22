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

package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class CarouselTab extends FrameLayoutWithOverlay {

	private ImageFetcher mFetcher;
	private ImageView mPhoto;
	private ImageView mAlbumArt;
	private TextView mLabelView;
	private View mColorstrip, mAlphaLayer;

	/**
	 *
	 */
	public CarouselTab(Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public CarouselTab(Context context, AttributeSet attrs) {
		super(context, attrs);
		View view = LayoutInflater.from(context).inflate(R.layout.profile_tab, this, false);
		mAlphaLayer = view.findViewById(R.id.profile_tab_alpha_overlay);
		mColorstrip = view.findViewById(R.id.profile_tab_colorstrip);
		mPhoto = view.findViewById(R.id.profile_tab_photo);
		mAlbumArt = view.findViewById(R.id.profile_tab_album_art);
		mLabelView = view.findViewById(R.id.profile_tab_label);
		mFetcher = ApolloUtils.getImageFetcher(context);
		// add child views
		addView(view);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// Set the alpha layer
		setAlphaLayer(mAlphaLayer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if (selected) {
			mColorstrip.setVisibility(View.VISIBLE);
		} else {
			mColorstrip.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Used to set the artist image in the artist profile.
	 *
	 * @param context The {@link Context} to use.
	 * @param artist  The name of the artist in the profile the user is viewing.
	 */
	public void setArtistPhoto(Context context, String artist) {
		if (!TextUtils.isEmpty(artist)) {
			mFetcher.loadArtistImage(artist, mPhoto);
		} else {
			setDefault(context);
		}
	}

	/**
	 * Used to set the album art in the album profile.
	 *
	 * @param context The {@link Context} to use.
	 * @param album   The name of the album in the profile the user is viewing.
	 */
	public void setAlbumPhoto(Context context, String album, String artist) {
		if (!TextUtils.isEmpty(album)) {
			mFetcher.loadAlbumImage(artist, album, MusicUtils.getIdForAlbum(context, album, artist), mAlbumArt, mPhoto);
			mAlbumArt.setVisibility(View.VISIBLE);
		} else {
			setDefault(context);
		}
	}

	/**
	 * Used to fetch for the album art via Last.fm.
	 *
	 * @param context The {@link Context} to use.
	 * @param album   The name of the album in the profile the user is viewing.
	 * @param artist  The name of the album artist in the profile the user is viewing
	 */
	public void fetchAlbumPhoto(Context context, String album, String artist) {
		if (!TextUtils.isEmpty(album)) {
			mFetcher.removeFromCache(ImageFetcher.generateAlbumCacheKey(album, artist));
			mFetcher.loadAlbumImage(artist, album, -1L, mAlbumArt);
		} else {
			setDefault(context);
		}
	}

	/**
	 * Used to set the album art in the artist profile.
	 *
	 * @param context The {@link Context} to use.
	 * @param artist  The name of the artist in the profile the user is viewing.
	 */
	public void setArtistAlbumPhoto(final Context context, final String artist) {
		final String lastAlbum = RecentStore.getInstance(context).getAlbumName(artist);
		if (!TextUtils.isEmpty(lastAlbum)) {
			// Set the last album the artist played
			mFetcher.loadAlbumImage(artist, lastAlbum, MusicUtils.getIdForAlbum(context, lastAlbum, artist), mPhoto);
		} else {
			setDefault(context);
		}
	}

	/**
	 * Used to set the header image for playlists and genres.
	 *
	 * @param context     The {@link Context} to use.
	 * @param profileName The key used to fetch the image.
	 */
	public void setPlaylistOrGenrePhoto(Context context, String profileName) {
		if (!TextUtils.isEmpty(profileName)) {
			Bitmap image = mFetcher.getCachedBitmap(profileName);
			if (image != null) {
				mPhoto.setImageBitmap(image);
			} else {
				setDefault(context);
			}
		} else {
			setDefault(context);
		}
	}

	/**
	 * @param context The {@link Context} to use.
	 */
	public void setDefault(Context context) {
		mPhoto.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.header_temp));
	}

	/**
	 * @param label The string to set as the labe.
	 */
	public void setLabel(String label) {
		mLabelView.setText(label);
	}

	/**
	 * Selects the label view.
	 */
	public void showSelectedState() {
		mLabelView.setSelected(true);
	}

	/**
	 * Deselects the label view.
	 */
	public void showDeselectedState() {
		mLabelView.setSelected(false);
	}

	/**
	 * @return The {@link ImageView} used to set the header photo.
	 */
	public ImageView getPhoto() {
		return mPhoto;
	}

	/**
	 * @return The {@link ImageView} used to set the album art .
	 */
	public ImageView getAlbumArt() {
		return mAlbumArt;
	}
}