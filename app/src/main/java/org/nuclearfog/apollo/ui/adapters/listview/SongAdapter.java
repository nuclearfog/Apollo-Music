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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This {@link ArrayAdapter} is used to display all of the songs on a user's
 * device for {@link org.nuclearfog.apollo.ui.fragments.SongFragment}. It is also used to show the queue in
 * {@link org.nuclearfog.apollo.ui.fragments.QueueFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongAdapter extends ArrayAdapter<Song> {

	/**
	 * item layout
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * transparency mask for a RGB color
	 */
	private static final int TRANSPARENCY_MASK = 0x40FFFFFF;

	/**
	 * fragment layout inflater
	 */
	private LayoutInflater inflater;

	/**
	 * current item position of the current track
	 */
	private int nowplayingPos = -1;

	/**
	 * background color of the selected track
	 */
	private int selectedColor;

	/**
	 * flag to enable drag and drop icon
	 */
	private boolean enableDnD;

	/**
	 * Constructor of <code>SongAdapter</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public SongAdapter(Context context, boolean enableDrag) {
		super(context, LAYOUT);
		PreferenceUtils prefs = PreferenceUtils.getInstance(context);
		selectedColor = prefs.getDefaultThemeColor() & TRANSPARENCY_MASK;
		inflater = LayoutInflater.from(context);
		enableDnD = enableDrag;
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(LAYOUT, parent, false);
			if (enableDnD)
				convertView.findViewById(R.id.edit_track_list_item_handle).setVisibility(View.VISIBLE);
			holder = new MusicHolder(convertView);
			// Hide the third line of text
			holder.mLineThree.setVisibility(View.GONE);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// set background color
		if (position == nowplayingPos) {
			convertView.setBackgroundColor(selectedColor);
		} else {
			convertView.setBackgroundColor(0);
		}
		// Retrieve the data holder
		Song song = getItem(position);
		if (song != null) {
			// Set each song name (line one)
			holder.mLineOne.setText(song.getName());
			// Set the song duration (line one, right)
			holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), song.duration()));
			// Set the album name (line two)
			holder.mLineTwo.setText(song.getArtist());
			if (song.isVisible()) {
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
		Song song = getItem(position);
		if (song != null)
			return song.getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(@Nullable Song song) {
		int pos = getPosition(song);
		if (pos < nowplayingPos)
			nowplayingPos--;
		super.remove(song);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insert(Song song, int to) {
		if (to <= nowplayingPos)
			nowplayingPos++;
		super.insert(song, to);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * moves the track item to another position
	 *
	 * @param from index where the track is located
	 * @param to   index where the track should be moved
	 */
	@MainThread
	public void moveTrack(int from, int to) {
		if (from != to) {
			if (from != nowplayingPos) {
				// move tracks around selected track
				Song mSong = getItem(from);
				remove(mSong);
				insert(mSong, to);
			} else {
				// move selected track to new position
				Song mSong = getItem(from);
				remove(mSong);
				insert(mSong, to);
				nowplayingPos = to;
			}
		} else {
			// nothing changed, revert layout changes
			notifyDataSetChanged();
		}
	}

	/**
	 * set current track ID
	 *
	 * @param pos position of the current track
	 */
	public void setCurrentTrackPos(int pos) {
		nowplayingPos = pos;
		notifyDataSetChanged();
	}
}