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
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumLoader;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.AlbumAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the albums on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumFragment extends Fragment implements OnScrollListener, OnItemClickListener, AsyncCallback<List<Album>>, Observer<String> {

	/**
	 *
	 */
	private static final String TAG = "AlbumFragment";

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
	private static final int GROUP_ID = 0x515A2A6B;

	/**
	 * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
	 */
	private static final int ONE = 1, TWO = 2, FOUR = 4;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;
	private AsyncCallback<List<Song>> onAddToNewPlaylist = this::onAddToNewPlaylist;
	private AsyncCallback<List<Song>> onAddToExistingPlaylist = this::onAddToExistingPlaylist;
	private AsyncCallback<List<Song>> onSongsDelete = this::onSongsDelete;

	/**
	 * app settings
	 */
	private PreferenceUtils preference;

	/**
	 * The adapter for the grid
	 */
	private AlbumAdapter mAdapter;

	/**
	 * list
	 */
	private GridView mList;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	/**
	 * context menu selection
	 */
	@Nullable
	private Album selectedAlbum = null;
	private long selectedPlaylistId = -1;

	private AlbumLoader albumLoader;
	private AlbumSongLoader albumSongLoader;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// initialize views
		View mRootView = inflater.inflate(R.layout.grid_base, container, false);
		TextView emptyInfo = mRootView.findViewById(R.id.grid_base_empty_info);
		mList = mRootView.findViewById(R.id.grid_base);
		//
		preference = PreferenceUtils.getInstance(requireContext());
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		albumLoader = new AlbumLoader(requireContext());
		albumSongLoader = new AlbumSongLoader(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		// init list
		initList();
		mList.setEmptyView(emptyInfo);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		mList.setOnScrollListener(this);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// Start the loader
		albumLoader.execute(null, this);
		return mRootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		super.onPause();
		mAdapter.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		albumLoader.cancel();
		albumSongLoader.cancel();
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
			// Create a new album
			selectedAlbum = mAdapter.getItem(info.position);
			if (selectedAlbum != null) {
				// Play the album
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the album to the queue
				menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// Add the album to a playlist
				SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
				MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
				// View more content by the album artist
				menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
				// hide album from list
				if (selectedAlbum.isVisible()) {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_ALBUM, Menu.NONE, R.string.context_menu_hide_album);
				} else {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_ALBUM, Menu.NONE, R.string.context_menu_unhide_album);
				}
				// Remove the album from the list
				menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			}
		} else {
			// remove selection
			selectedAlbum = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		// Avoid leaking context menu selections
		if (item.getGroupId() == GROUP_ID && selectedAlbum != null) {
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					albumSongLoader.execute(selectedAlbum.getId(), onPlaySongs);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					albumSongLoader.execute(selectedAlbum.getId(), onAddToQueue);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					albumSongLoader.execute(selectedAlbum.getId(), onAddToNewPlaylist);
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), selectedAlbum.getArtist());
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					selectedPlaylistId = item.getIntent().getLongExtra("playlist", -1L);
					albumSongLoader.execute(selectedAlbum.getId(), onAddToExistingPlaylist);
					return true;

				case ContextMenuItems.DELETE:
					albumSongLoader.execute(selectedAlbum.getId(), onSongsDelete);
					return true;

				case ContextMenuItems.HIDE_ALBUM:
					MusicUtils.excludeAlbum(requireContext(), selectedAlbum);
					MusicUtils.refresh(requireActivity());
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mAdapter.setPauseDiskCache(true);
		} else {
			mAdapter.setPauseDiskCache(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (view.getId() == R.id.image) {
			albumSongLoader.execute(id, onPlaySongs);
		} else {
			Album selectedAlbum = mAdapter.getItem(position);
			if (selectedAlbum != null) {
				NavUtils.openAlbumProfile(requireActivity(), selectedAlbum);
			}
		}
	}


	@Override
	public void onResult(@NonNull List<Album> albums) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Album album : albums) {
				if (preference.getExcludeTracks() || album.isVisible()) {
					mAdapter.add(album);
				}
			}
		}
	}


	@Override
	public void onChanged(String action) {
		switch (action) {
			case REFRESH:
				// re init list
				initList();

			case MusicBrowserPhoneFragment.REFRESH:
				albumLoader.execute(null, this);
				break;

			case MusicBrowserPhoneFragment.META_CHANGED:
				Album current = MusicUtils.getCurrentAlbum(requireActivity());
				if (current != null) {
					for (int i = 0; i < mAdapter.getCount(); i++) {
						Album item = mAdapter.getItem(i);
						if (item != null && item.getId() == current.getId()) {
							mList.setSelection(i);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Nothing to do
	}

	/**
	 * initialize adapter & list
	 */
	private void initList() {
		switch (preference.getAlbumLayout()) {
			case PreferenceUtils.LAYOUT_SIMPLE:
				mAdapter = new AlbumAdapter(requireActivity(), 1, R.layout.list_item_normal);
				mList.setNumColumns(ONE);
				break;

			case PreferenceUtils.LAYOUT_DETAILED:
				if (ApolloUtils.isLandscape(requireContext())) {
					mAdapter = new AlbumAdapter(requireActivity(), 2, R.layout.list_item_detailed);
					mList.setNumColumns(TWO);
				} else {
					mAdapter = new AlbumAdapter(requireActivity(), 1, R.layout.list_item_detailed);
					mList.setNumColumns(ONE);
				}
				mAdapter.setLoadExtraData();
				break;

			default:
				if (ApolloUtils.isLandscape(requireContext())) {
					mAdapter = new AlbumAdapter(requireActivity(), 4, R.layout.grid_item_normal);
					mList.setNumColumns(FOUR);
				} else {
					mAdapter = new AlbumAdapter(requireActivity(), 2, R.layout.grid_item_normal);
					mList.setNumColumns(TWO);
				}
				break;
		}
		// set adapter and empty view for the list
		mList.setAdapter(mAdapter);
	}

	/**
	 * play loaded songs
	 */
	private void onPlaySongs(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(requireActivity(), ids, 0, false);
	}

	/**
	 * add loaded songs to queue
	 */
	private void onAddToQueue(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.addToQueue(requireActivity(), ids);
	}

	/**
	 * create a new playlist with the loaded songs
	 */
	private void onAddToNewPlaylist(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		PlaylistDialog.show(getParentFragmentManager(), PlaylistDialog.CREATE, 0, ids, null);
	}

	/**
	 * save the loaded songs into an existing playlist
	 */
	private void onAddToExistingPlaylist(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.addToPlaylist(requireActivity(), ids, selectedPlaylistId);
	}

	/**
	 * delete the loaded songs
	 */
	private void onSongsDelete(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		String name = selectedAlbum != null ? selectedAlbum.getName() : "";
		MusicUtils.openDeleteDialog(requireActivity(), name, ids);
	}
}