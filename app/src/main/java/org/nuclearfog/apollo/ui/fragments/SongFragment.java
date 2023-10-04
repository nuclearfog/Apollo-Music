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

import android.content.Context;
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
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.loaders.SongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.provider.FavoritesStore;
import org.nuclearfog.apollo.ui.activities.ActivityBase;
import org.nuclearfog.apollo.ui.activities.ActivityBase.MusicStateListener;
import org.nuclearfog.apollo.ui.adapters.listview.SongAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * This class is used to display all of the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>, OnItemClickListener, MusicStateListener, Observer<String> {

	/**
	 *
	 */
	public static final String SCROLL_TOP = "SongFragment.scroll_top";

	/**
	 *
	 */
	public static final String REFRESH = "SongFragment.refresh";

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x26153793;

	/**
	 * LoaderCallbacks identifier
	 */
	private static final int LOADER_ID = 0x70B1F21F;

	/**
	 * The adapter for the list
	 */
	private SongAdapter mAdapter;

	/**
	 * The list view
	 */
	private ListView mList;

	private FragmentViewModel viewModel;

	/**
	 * current track
	 */
	@Nullable
	private Song mSong;

	/**
	 * True if the list should execute {@code #restartLoader()}.
	 */
	private boolean mShouldRefresh = false;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public SongFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		// Register the music status listener
		if (context instanceof ActivityBase) {
			((ActivityBase) context).setMusicStateListenerListener(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		// Create the adapter
		mAdapter = new SongAdapter(requireContext(), false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View mRootView = inflater.inflate(R.layout.list_base, container, false);
		TextView emptyText = mRootView.findViewById(R.id.list_base_empty_info);
		// setup the list view
		mList = mRootView.findViewById(R.id.list_base);
		mList.setAdapter(mAdapter);
		mList.setEmptyView(emptyText);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		return mRootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// Enable the options menu
		setHasOptionsMenu(true);
		// Start the loader
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		viewModel.getSelectedItem().removeObserver(this);
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
			long[] trackIds = {mSong.getId()};

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
					FavoritesStore.getInstance(requireContext()).addSongId(mSong);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(trackIds).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					long mPlaylistId = item.getIntent().getLongExtra("playlist", 0L);
					MusicUtils.addToPlaylist(requireActivity(), trackIds, mPlaylistId);
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackIds);
					mShouldRefresh = true;
					return true;
			}
		}
		return super.onContextItemSelected(item);
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
	@NonNull
	@Override
	public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
		return new SongLoader(requireContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadFinished(@NonNull Loader<List<Song>> loader, @NonNull List<Song> data) {
		if (!isRemoving() && !isDetached()) {
			// disable loader
			LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Song song : data) {
				mAdapter.add(song);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
		if (mAdapter != null) {
			// Clear the data in the adapter
			mAdapter.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		switch (action) {
			case REFRESH:
				LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
				break;

			case SCROLL_TOP:
				// current unique track ID
				long trackId = MusicUtils.getCurrentAudioId();
				for (int pos = 0; pos < mAdapter.getCount(); pos++) {
					if (mAdapter.getItemId(pos) == trackId) {
						mList.setSelection(pos);
						break;
					}
				}
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void restartLoader() {
		if (!isRemoving() && !isDetached()) {
			// Update the list when the user deletes any items
			if (mShouldRefresh) {
				LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
				mShouldRefresh = false;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMetaChanged() {
		// Nothing to do
	}
}