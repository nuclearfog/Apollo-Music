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

package org.nuclearfog.apollo.ui.fragments.profile;

import static org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter.DISPLAY_DEFAULT_SETTING;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * This class is used to display all of the songs from a particular genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class GenreSongFragment extends ProfileFragment implements AsyncCallback<List<Song>> {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x55B60E85;

	/**
	 * The adapter for the list
	 */
	private ProfileSongAdapter mAdapter;

	private GenreSongLoader mLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Song mSong;
	private String genreIds = "";


	@Override
	protected void init(Bundle param) {
		// init loader
		mLoader = new GenreSongLoader(requireContext());
		// init adapter
		mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_DEFAULT_SETTING, false);
		// set adapter
		setAdapter(mAdapter);
		// Enable the options menu
		setHasOptionsMenu(true);
		// Start the loader
		if (param != null) {
			genreIds = param.getString(Config.IDS, "");
			mLoader.execute(genreIds, this);
		}
	}


	@Override
	public void onDestroy() {
		mLoader.cancel();
		super.onDestroy();
	}


	@Override
	protected void onItemClick(View view, int position, long id) {
		MusicUtils.playAllFromUserItemClick(requireActivity(), mAdapter, position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterContextMenuInfo) {
			// Get the position of the selected item
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			// Create a new song
			mSong = mAdapter.getItem(info.position);
			// Play the song
			menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
			// Play the song
			menu.add(GROUP_ID, ContextMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
			// Add the song to the queue
			menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
			// Add the song to a playlist
			SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
			MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, true);
			// View more content by the song artist
			menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
			// Make the song a ringtone
			menu.add(GROUP_ID, ContextMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			// Delete the song
			menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
		} else {
			// remove selection if an error occurs
			mSong = null;
		}
	}


	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && mSong != null) {
			long[] trackId = {mSong.getId()};

			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(requireActivity(), trackId, 0, false);
					return true;

				case ContextMenuItems.PLAY_NEXT:
					MusicUtils.playNext(requireActivity(), trackId);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), trackId);
					return true;

				case ContextMenuItems.ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireContext()).addFavorite(mSong);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(trackId).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					long mPlaylistId = item.getIntent().getLongExtra("playlist", -1L);
					if (mPlaylistId != -1L) {
						MusicUtils.addToPlaylist(requireActivity(), trackId, mPlaylistId);
					}
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackId);
					refresh();
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Song> songs) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adpater
			for (Song song : songs) {
				mAdapter.add(song);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drop(int from, int to) {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int which) {
		// not used
	}

	/**
	 * Restarts the loader.
	 */
	@Override
	protected void refresh() {
		mAdapter.clear();
		mLoader.execute(genreIds, this);
	}

	/**
	 * Restarts the loader.
	 */
	@Override
	protected void moveToCurrent() {
		Song song = MusicUtils.getCurrentTrack(requireActivity());
		if (song != null) {
			for (int pos = 0; pos < mAdapter.getCount(); pos++) {
				if (mAdapter.getItemId(pos) == song.getId()) {
					scrollTo(pos);
					break;
				}
			}
		}
	}
}