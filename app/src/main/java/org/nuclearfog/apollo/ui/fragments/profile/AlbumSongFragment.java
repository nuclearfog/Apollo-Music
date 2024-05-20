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


import static org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter.DISPLAY_ALBUM_SETTING;

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
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.util.List;

/**
 * This class is used to display all of the songs from a particular album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumSongFragment extends ProfileFragment implements AsyncCallback<List<Song>> {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x169012DB;

	/**
	 * The adapter for the list
	 */
	private ProfileSongAdapter mAdapter;

	private AlbumSongLoader mLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Song mSong;

	/**
	 * Album ID of the songs
	 */
	private long albumId;


	@Override
	protected void init(Bundle args) {
		// init adapter
		mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_ALBUM_SETTING, false);
		// init loader
		mLoader = new AlbumSongLoader(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		setAdapter(mAdapter);
		// Start the loader
		if (args != null) {
			albumId = args.getLong(Config.ID);
			mLoader.execute(albumId, this);
		}
	}


	@Override
	public void onDestroy() {
		mLoader.cancel();
		super.onDestroy();
	}


	@Override
	protected void onItemClick(View v, int pos, long id) {
		MusicUtils.playAllFromUserItemClick(requireContext(), mAdapter, pos);
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
			// Creat a new song
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
			// Make the song a ringtone
			menu.add(GROUP_ID, ContextMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			// Delete the song
			menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
		} else {
			// remove selected track
			mSong = null;
		}
	}


	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && mSong != null) {
			long[] trackId = {mSong.getId()};

			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(requireContext(), trackId, 0, false);
					return true;

				case ContextMenuItems.PLAY_NEXT:
					MusicUtils.playNext(trackId);
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

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackId);
					return true;
			}
		}
		return false;
	}


	@Override
	public void onResult(@NonNull List<Song> data) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adpater
			for (Song song : data) {
				mAdapter.add(song);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void moveToCurrent() {
		Song song = MusicUtils.getCurrentTrack();
		if (song != null) {
			for (int pos = 0; pos < mAdapter.getCount(); pos++) {
				if (mAdapter.getItemId(pos) == song.getId()) {
					scrollTo(pos);
					break;
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refresh() {
		mLoader.execute(albumId, this);
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
}