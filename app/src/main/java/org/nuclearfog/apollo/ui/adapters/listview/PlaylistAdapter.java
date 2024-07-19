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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Playlist;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;

/**
 * This adapter is used to display all of the playlists on a user's
 * device for {@link org.nuclearfog.apollo.ui.fragments.PlaylistFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class PlaylistAdapter extends AlphabeticalAdapter<Playlist> {

	/**
	 * item layout reource
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * fragment layout inflater
	 */
	private LayoutInflater inflater;

	/**
	 * Constructor of <code>PlaylistAdapter</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public PlaylistAdapter(Context context) {
		super(context, LAYOUT);
		inflater = LayoutInflater.from(context);
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
			convertView = inflater.inflate(LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			// Hide the second and third lines of text
			holder.mLineTwo.setVisibility(View.GONE);
			holder.mLineThree.setVisibility(View.GONE);
			// Make line one slightly larger
			holder.mLineOne.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.text_size_large));
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the data holder
		Playlist playlist = getItem(position);
		if (playlist != null) {
			// Set each playlist name (line one)
			holder.mLineOne.setText(playlist.getName());
		}
		return convertView;
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
	public long getItemId(int position) {
		Playlist list = getItem(position);
		if (list != null)
			return list.getId();
		return super.getItemId(position);
	}
}