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

package org.nuclearfog.apollo.ui.fragments;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.SongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.listview.SongAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class SongFragment extends Fragment implements AsyncCallback<List<Song>>, OnItemClickListener, Observer<String> {

	/**
	 *
	 */
	private static final String TAG = "SongFragment";

	/**
	 *
	 */
	public static final String SCROLL_TOP = TAG + ".SCROLL_TOP";

	/**
	 *
	 */
	public static final String REFRESH = TAG + ".REFRESH";

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x26153793;

	/**
	 * The adapter for the list
	 */
	private SongAdapter mAdapter;

	/**
	 * The list view
	 */
	private ListView mList;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	/**
	 * app settings
	 */
	private PreferenceUtils preference;

	private SongLoader mLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Song selectedSong = null;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public SongFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View mRootView = inflater.inflate(R.layout.list_base, container, false);
		TextView emptyText = mRootView.findViewById(R.id.list_base_empty_info);
		mList = mRootView.findViewById(R.id.list_base);
		//
		preference = PreferenceUtils.getInstance(requireContext());
		mAdapter = new SongAdapter(requireContext(), false);
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		mLoader = new SongLoader(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		// setup the list view
		mList.setAdapter(mAdapter);
		mList.setEmptyView(emptyText);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		// start loader
		mLoader.execute(null, this);
		return mRootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		mLoader.cancel();
		super.onDestroyView();
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
			selectedSong = mAdapter.getItem(info.position);
			if (selectedSong != null) {
				// Play the song
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Play next
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
				// hide genre from list
				if (selectedSong.isVisible()) {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_SONG, Menu.NONE, R.string.context_menu_hide_track);
				} else {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_SONG, Menu.NONE, R.string.context_menu_unhide_track);
				}
				// Delete the song
				menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			}
		} else {
			// remove selection if an error occurs
			selectedSong = null;
		}
	}


	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selectedSong != null) {
			long[] trackIds = {selectedSong.getId()};

			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(requireContext(), trackIds, 0, false);
					return true;

				case ContextMenuItems.PLAY_NEXT:
					MusicUtils.playNext(trackIds);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), trackIds);
					return true;

				case ContextMenuItems.ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireContext()).addItem(selectedSong);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(trackIds).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					long mPlaylistId = item.getIntent().getLongExtra("playlist", -1L);
					if (mPlaylistId != -1L) {
						MusicUtils.addToPlaylist(requireActivity(), trackIds, mPlaylistId);
					}
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), selectedSong.getArtist());
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), selectedSong.getId());
					return true;

				case ContextMenuItems.HIDE_SONG:
					MusicUtils.excludeSong(requireContext(), selectedSong);
					MusicUtils.refresh();
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), selectedSong.getName(), trackIds);
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MusicUtils.playAllFromUserItemClick(requireContext(), mAdapter, position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Song> songs) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Song song : songs) {
				if (preference.getExcludeTracks() || song.isVisible()) {
					mAdapter.add(song);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		switch (action) {
			case REFRESH:
			case MusicBrowserPhoneFragment.REFRESH:
				mLoader.execute(null, this);
				break;

			case MusicBrowserPhoneFragment.META_CHANGED:
				// current unique track ID
				Song song = MusicUtils.getCurrentTrack();
				if (song != null) {
					for (int pos = 0; pos < mAdapter.getCount(); pos++) {
						if (mAdapter.getItemId(pos) == song.getId()) {
							mList.setSelection(pos);
							break;
						}
					}
				}
				break;

			case SCROLL_TOP:
				if (mList.getCount() > 0)
					mList.smoothScrollToPosition(0);
				break;
		}
	}
}