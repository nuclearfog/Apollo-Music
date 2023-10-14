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

package org.nuclearfog.apollo.ui.adapters.viewpager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.fragments.profile.AlbumSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.ArtistAlbumFragment;
import org.nuclearfog.apollo.ui.fragments.profile.ArtistSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.FolderSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.GenreSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.LastAddedFragment;
import org.nuclearfog.apollo.ui.fragments.profile.PlaylistSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.PopularSongFragment;

/**
 * {@link ProfileActivity} viewpager adapter
 *
 * @author nuclearfog
 */
public class ProfileAdapter extends FragmentStatePagerAdapter {

	/**
	 * index of the album song fragment
	 */
	public static final int IDX_ALBUM_SONG = 0;

	/**
	 * index of the artist song fragment
	 */
	public static final int IDX_ARTIST_SONG = 0;

	/**
	 * index of the artist album fragment
	 */
	public static final int IDX_ARTIST_ALBUM = 1;

	private ProfileActivity.Type type;
	private Bundle args;

	/**
	 * Constructor of <code>PagerAdapter<code>
	 */
	public ProfileAdapter(FragmentManager fm, Bundle args, ProfileActivity.Type type) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.type = type;
		this.args = args;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@NonNull
	public Fragment getItem(int position) {
		Fragment fragment;
		switch (type) {
			case ALBUM:
				fragment = new AlbumSongFragment();
				break;

			case GENRE:
				fragment = new GenreSongFragment();
				break;

			case FOLDER:
				fragment = new FolderSongFragment();
				break;

			case FAVORITE:
				fragment = new FavoriteSongFragment();
				break;

			case PLAYLIST:
				fragment = new PlaylistSongFragment();
				break;

			case LAST_ADDED:
				fragment = new LastAddedFragment();
				break;

			case POPULAR:
				fragment = new PopularSongFragment();
				break;

			case ARTIST:
				if (position == IDX_ARTIST_SONG) {
					fragment = new ArtistSongFragment();
					break;
				} else if (position == IDX_ARTIST_ALBUM) {
					fragment = new ArtistAlbumFragment();
					break;
				}

			default:
				fragment = new Fragment();
				break;
		}
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		switch (type) {
			case ALBUM:
			case GENRE:
			case FOLDER:
			case FAVORITE:
			case PLAYLIST:
			case LAST_ADDED:
			case POPULAR:
				return 1;

			case ARTIST:
				return 2;

			default:
				return 0;
		}
	}
}