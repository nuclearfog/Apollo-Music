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
import org.nuclearfog.apollo.async.loader.ArtistLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.worker.ExcludeMusicWorker;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.ArtistAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ArtistFragment extends Fragment implements AsyncCallback<List<Artist>>, OnScrollListener, OnItemClickListener, Observer<String> {

	/**
	 *
	 */
	private static final String TAG = "ArtistFragment";

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
	private static final int GROUP_ID = 0x793F54E4;

	/**
	 * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
	 */
	private static final int ONE = 1, TWO = 2, FOUR = 4;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;
	private AsyncCallback<List<Song>> onAddToNewPlaylist = this::onAddToNewPlaylist;
	private AsyncCallback<List<Song>> onAddToExistingPlaylist = this::onAddToExistingPlaylist;
	private AsyncCallback<List<Song>> onSongsDelete = this::onSongsDelete;
	private AsyncCallback<Boolean> onArtistHide = this::onArtistHiden;

	/**
	 * The adapter for the grid
	 */
	private ArtistAdapter mAdapter;

	/**
	 * The grid view
	 */
	private GridView mList;

	/**
	 * app preferences
	 */
	private PreferenceUtils preference;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	private ArtistLoader artistLoader;
	private ArtistSongLoader artistSongLoader;
	private ExcludeMusicWorker excludeMusicWorker;

	/**
	 * Represents an artist
	 */
	@Nullable
	private Artist selectedArtist = null;
	private long selectedPlaylistId = -1;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public ArtistFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mRootView = inflater.inflate(R.layout.grid_base, container, false);
		TextView emptyHolder = mRootView.getRootView().findViewById(R.id.grid_base_empty_info);
		mList = mRootView.findViewById(R.id.grid_base);
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		preference = PreferenceUtils.getInstance(requireContext());
		artistLoader = new ArtistLoader(requireContext());
		artistSongLoader = new ArtistSongLoader(requireContext());
		excludeMusicWorker = new ExcludeMusicWorker(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		// init list
		initList();
		// setup list view
		mList.setAdapter(mAdapter);
		mList.setEmptyView(emptyHolder);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		mList.setOnScrollListener(this);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// Start the loader
		artistLoader.execute(null, this);
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
		artistLoader.cancel();
		artistSongLoader.cancel();
		excludeMusicWorker.cancel();
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
			// Create a new model
			selectedArtist = mAdapter.getItem(info.position);
			if (selectedArtist != null) {
				// Play the artist
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the artist to the queue
				menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// Add the artist to a playlist
				SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
				MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
				// hide artist from list
				if (selectedArtist.isVisible()) {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_ARTIST, Menu.NONE, R.string.context_menu_hide_artist);
				} else {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_ARTIST, Menu.NONE, R.string.context_menu_unhide_artist);
				}
				// Delete the artist
				menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			}
		} else {
			// remove selection
			selectedArtist = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selectedArtist != null) {
			// Create a list of the artist's songs
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					artistSongLoader.execute(selectedArtist.getId(), onPlaySongs);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					artistSongLoader.execute(selectedArtist.getId(), onAddToQueue);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					artistSongLoader.execute(selectedArtist.getId(), onAddToNewPlaylist);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					selectedPlaylistId = item.getIntent().getLongExtra(Constants.PLAYLIST_ID, -1L);
					if (selectedPlaylistId != -1)
						artistSongLoader.execute(selectedArtist.getId(), onAddToExistingPlaylist);
					return true;

				case ContextMenuItems.HIDE_ARTIST:
					excludeMusicWorker.execute(selectedArtist, onArtistHide);
					return true;

				case ContextMenuItems.DELETE:
					artistSongLoader.execute(selectedArtist.getId(), onSongsDelete);
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
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
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
	public void onItemClick(AdapterView<?> parent, @NonNull View view, int position, long id) {
		if (view.getId() == R.id.image) {
			artistSongLoader.execute(id, onPlaySongs);
		} else {
			Artist selectedArtist = mAdapter.getItem(position);
			if (selectedArtist != null) {
				NavUtils.openArtistProfile(requireActivity(), selectedArtist.getName());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Artist> artists) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Artist artist : artists) {
				if (preference.getExcludeTracks() || artist.isVisible()) {
					mAdapter.add(artist);
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
				initList();

			case MusicBrowserPhoneFragment.REFRESH:
				artistLoader.execute(null, this);
				break;

			case MusicBrowserPhoneFragment.META_CHANGED:
				Song song = MusicUtils.getCurrentTrack(requireActivity());
				if (song != null) {
					for (int i = 0; i < mAdapter.getCount(); i++) {
						Artist artist = mAdapter.getItem(i);
						if (artist != null && artist.getId() == song.getArtistId()) {
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
		switch (preference.getArtistLayout()) {
			case PreferenceUtils.LAYOUT_SIMPLE:
				mAdapter = new ArtistAdapter(requireActivity(), 1, R.layout.list_item_normal);
				mList.setNumColumns(ONE);
				break;

			case PreferenceUtils.LAYOUT_DETAILED:
				if (ApolloUtils.isLandscape(requireContext())) {
					mAdapter = new ArtistAdapter(requireActivity(), 2, R.layout.list_item_detailed);
					mList.setNumColumns(TWO);
				} else {
					mAdapter = new ArtistAdapter(requireActivity(), 1, R.layout.list_item_detailed);
					mList.setNumColumns(ONE);
				}
				mAdapter.setLoadExtraData();
				break;

			default:
				if (ApolloUtils.isLandscape(requireContext())) {
					mAdapter = new ArtistAdapter(requireActivity(), 4, R.layout.grid_item_normal);
					mList.setNumColumns(FOUR);
				} else {
					mAdapter = new ArtistAdapter(requireActivity(), 2, R.layout.grid_item_normal);
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
		String name = selectedArtist != null ? selectedArtist.getName() : "";
		MusicUtils.openDeleteDialog(requireActivity(), name, ids);
	}

	/**
	 * called after an entry was hidden
	 */
	private void onArtistHiden(Boolean hidden) {
		if (getActivity() != null && selectedArtist != null) {
			if (hidden) {
				AppMsg.makeText(requireActivity(), R.string.item_hidden, AppMsg.STYLE_CONFIRM).show();
			}
			MusicUtils.refresh(requireActivity());
		}
	}
}