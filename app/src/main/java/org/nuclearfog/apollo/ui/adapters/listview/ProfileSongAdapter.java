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
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This {@link ArrayAdapter} is used to display the songs for a particular
 * artist, album, playlist, or genre for {@link org.nuclearfog.apollo.ui.fragments.profile.ArtistSongFragment},
 * {@link org.nuclearfog.apollo.ui.fragments.profile.AlbumSongFragment},{@link org.nuclearfog.apollo.ui.fragments.profile.PlaylistSongFragment},
 * {@link org.nuclearfog.apollo.ui.fragments.profile.GenreSongFragment},{@link org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment},
 * {@link org.nuclearfog.apollo.ui.fragments.profile.LastAddedFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileSongAdapter extends ArrayAdapter<Song> {

	/**
	 * Default display setting: title/album
	 */
	public static final int DISPLAY_DEFAULT_SETTING = 0x709121EE;

	/**
	 * Playlist display setting: title/artist-album
	 */
	public static final int DISPLAY_PLAYLIST_SETTING = 0x57909C67;

	/**
	 * Album display setting: title/duration
	 */
	public static final int DISPLAY_ALBUM_SETTING = 0xCCCED4CB;

	/**
	 * The header view
	 */
	private static final int ITEM_VIEW_TYPE_HEADER = 0;

	/**
	 * * The data in the list.
	 */
	private static final int ITEM_VIEW_TYPE_MUSIC = 1;

	/**
	 * Number of views (ImageView, TextView, header)
	 */
	private static final int VIEW_TYPE_COUNT = 2;

	/**
	 * Count of the view header
	 */
	public static final int HEADER_COUNT = 1;

	/**
	 * item layout
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * invisible placeholder view used to determine the space used by the carousel view
	 */
	private View mHeader;

	/**
	 * Display setting for the second line in a song fragment
	 */
	private int mDisplaySetting;

	/**
	 * flag to set drag and drop icon
	 */
	private boolean enableDnD;

	/**
	 * Constructor of <code>ProfileSongAdapter</code>
	 *
	 * @param context The {@link Context} to use
	 * @param setting defines the content of the second line
	 */
	public ProfileSongAdapter(Context context, int setting, boolean enableDrag) {
		super(context, LAYOUT);
		// create placeholder view
		mHeader = View.inflate(context, R.layout.profile_tab_carousel, null);
		mHeader.setVisibility(View.INVISIBLE);
		// Know what to put in line two
		this.mDisplaySetting = setting;
		this.enableDnD = enableDrag;
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Return a faux header at position 0
		if (position == 0) {
			return mHeader;
		}
		// Recycle MusicHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			// Hide the third line of text
			holder.mLineThree.setVisibility(View.GONE);
			convertView.setTag(holder);
			if (enableDnD) {
				View dragDropView = convertView.findViewById(R.id.edit_track_list_item_handle);
				dragDropView.setVisibility(View.VISIBLE);
			}
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the album
		Song song = getItem(position);
		if (song != null) {
			// Set each track name (line one)
			holder.mLineOne.setText(song.getName());
			// Set the line two
			if (mDisplaySetting == DISPLAY_ALBUM_SETTING) {
				holder.mLineOneRight.setVisibility(View.GONE);
				holder.mLineTwo.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
			} else if (mDisplaySetting == DISPLAY_PLAYLIST_SETTING) {
				if (song.getDuration() < 0L) {
					holder.mLineOneRight.setVisibility(View.GONE);
				} else {
					holder.mLineOneRight.setVisibility(View.VISIBLE);
					holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
				}
				String sb = song.getArtist() + " - " + song.getAlbum();
				holder.mLineTwo.setText(sb);
			} else {
				holder.mLineOneRight.setVisibility(View.VISIBLE);
				holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
				holder.mLineTwo.setText(song.getAlbum());
			}
		}
		return convertView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		return super.getCount() + HEADER_COUNT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return super.getCount() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getItemViewType(int position) {
		if (position == 0)
			return ITEM_VIEW_TYPE_HEADER;
		return ITEM_VIEW_TYPE_MUSIC;
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
	public void insert(@Nullable Song song, int index) {
		super.insert(song, index - HEADER_COUNT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Nullable
	@Override
	public Song getItem(int position) {
		if (position >= HEADER_COUNT)
			return super.getItem(position - HEADER_COUNT);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		if (position >= HEADER_COUNT)
			return super.getItem(position - HEADER_COUNT).getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}
}