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

import static org.nuclearfog.apollo.ui.activities.ProfileActivity.PAGE_FAVORIT;
import static org.nuclearfog.apollo.ui.activities.ProfileActivity.PAGE_LAST_ADDED;
import static org.nuclearfog.apollo.ui.activities.ProfileActivity.PAGE_MOST_PLAYED;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.FavoritesLoader;
import org.nuclearfog.apollo.async.loader.LastAddedLoader;
import org.nuclearfog.apollo.async.loader.PlaylistLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.async.loader.PopularSongsLoader;
import org.nuclearfog.apollo.model.Playlist;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.adapters.listview.PlaylistAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.dialogs.DeletePlaylistDialog;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.util.List;

/**
 * This class is used to display all of the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class PlaylistFragment extends Fragment implements AsyncCallback<List<Playlist>>, OnItemClickListener, Observer<String> {

	private static final String TAG = "PlaylistFragment";

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x727BFA75;

	/**
	 * The adapter for the list
	 */
	private PlaylistAdapter mAdapter;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	private PlaylistLoader playlistLoader;
	private PlaylistSongLoader playlistSongLoader;

	/**
	 * context menu selection
	 */
	private Playlist selectedPlaylist;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public PlaylistFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View rootView = inflater.inflate(R.layout.list_base, container, false);
		TextView emptyInfo = rootView.findViewById(R.id.list_base_empty_info);
		ListView mList = rootView.findViewById(R.id.list_base);
		//
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		mAdapter = new PlaylistAdapter(requireContext());
		playlistLoader = new PlaylistLoader(requireContext());
		playlistSongLoader = new PlaylistSongLoader(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		// setup list view
		mList.setAdapter(mAdapter);
		mList.setEmptyView(emptyInfo);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// Start the loader
		playlistLoader.execute(null, this);
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		playlistLoader.cancel();
		playlistSongLoader.cancel();
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
			// Create a new playlist
			selectedPlaylist = mAdapter.getItem(info.position);
			// Delete and rename (user made playlists)
			if (selectedPlaylist != null) {
				// Play the playlist
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the playlist to the queue
				menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				if (!selectedPlaylist.isDefault()) {
					// add options to edit playlist
					menu.add(GROUP_ID, ContextMenuItems.RENAME_PLAYLIST, Menu.NONE, R.string.context_menu_rename_playlist);
					menu.add(GROUP_ID, ContextMenuItems.COPY_PLAYLIST, Menu.NONE, R.string.context_menu_copy_playlist);
					menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
				}
			}
		} else {
			// remove old selection
			selectedPlaylist = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selectedPlaylist != null) {
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					// play favorite playlist
					if (selectedPlaylist.getId() == Playlist.FAVORITE_ID) {
						FavoritesLoader loader = new FavoritesLoader(requireContext());
						loader.execute(null, onPlaySongs);
					}
					// play last added playlist
					else if (selectedPlaylist.getId() == Playlist.LAST_ADDED_ID) {
						LastAddedLoader loader = new LastAddedLoader(requireContext());
						loader.execute(null, onPlaySongs);
					}
					// play popular playlist
					else if (selectedPlaylist.getId() == Playlist.POPULAR_ID) {
						PopularSongsLoader loader = new PopularSongsLoader(requireContext());
						loader.execute(null, onPlaySongs);
					}
					// play custom playlist
					else {
						playlistSongLoader.execute(selectedPlaylist.getId(), onPlaySongs);
					}
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					// add favorite playlist
					if (selectedPlaylist.getId() == Playlist.FAVORITE_ID) {
						FavoritesLoader loader = new FavoritesLoader(requireContext());
						loader.execute(null, onAddToQueue);
					}
					// add last added playlist
					else if (selectedPlaylist.getId() == Playlist.LAST_ADDED_ID) {
						LastAddedLoader loader = new LastAddedLoader(requireContext());
						loader.execute(null, onAddToQueue);
					}
					// add popular playlist
					else if (selectedPlaylist.getId() == Playlist.POPULAR_ID) {
						PopularSongsLoader loader = new PopularSongsLoader(requireContext());
						loader.execute(null, onAddToQueue);
					}
					// add custom playlist to queue
					else {
						playlistSongLoader.execute(selectedPlaylist.getId(), onAddToQueue);
					}
					return true;

				case ContextMenuItems.RENAME_PLAYLIST:
					PlaylistDialog.show(getParentFragmentManager(), PlaylistDialog.MOVE, selectedPlaylist.getId(), null, selectedPlaylist.getName());
					return true;

				case ContextMenuItems.COPY_PLAYLIST:
					PlaylistDialog.show(getParentFragmentManager(), PlaylistDialog.COPY, selectedPlaylist.getId(), null, selectedPlaylist.getName());
					break;

				case ContextMenuItems.DELETE:
					DeletePlaylistDialog.newInstance(selectedPlaylist).show(getParentFragmentManager(), TAG);
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
		Playlist selected = mAdapter.getItem(position);
		if (selected != null) {
			Bundle bundle = new Bundle();
			// Favorites list
			if (selected.getId() == Playlist.FAVORITE_ID) {
				bundle.putString(Config.NAME, getString(R.string.playlist_favorites));
				bundle.putString(Config.MIME_TYPE, PAGE_FAVORIT);
			}
			// Last added
			else if (selected.getId() == Playlist.LAST_ADDED_ID) {
				bundle.putString(Config.NAME, getString(R.string.playlist_last_added));
				bundle.putString(Config.MIME_TYPE, PAGE_LAST_ADDED);
			}
			// most played track
			else if (selected.getId() == Playlist.POPULAR_ID) {
				bundle.putString(Config.NAME, getString(R.string.playlist_most_played));
				bundle.putString(Config.MIME_TYPE, PAGE_MOST_PLAYED);
			}
			// User created playlist
			else {
				bundle.putString(Config.MIME_TYPE, Playlists.CONTENT_TYPE);
				bundle.putString(Config.NAME, selected.getName());
				bundle.putLong(Config.ID, selected.getId());
			}
			// Create the intent to launch the profile activity
			Intent intent = new Intent(requireActivity(), ProfileActivity.class);
			intent.putExtras(bundle);
			requireActivity().startActivity(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Playlist> playlists) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Playlist playlist : playlists) {
				mAdapter.add(playlist);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		if (action.equals(MusicBrowserPhoneFragment.REFRESH)) {
			// Refresh the list when a playlist is deleted or renamed
			playlistLoader.execute(null, this);
		}
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
}