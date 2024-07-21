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

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.GenreLoader;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.async.worker.ExcludeMusicWorker;
import org.nuclearfog.apollo.model.Genre;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.adapters.listview.GenreAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the genres on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class GenreFragment extends Fragment implements OnItemClickListener, AsyncCallback<List<Genre>>, Observer<String> {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x2D9C34D;

	/**
	 *
	 */
	private static final String TAG = "ArtistFragment";

	/**
	 *
	 */
	public static final String REFRESH = TAG + ".REFRESH";

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;
	private AsyncCallback<Boolean> onGenreHidden = this::onGenreHidden;

	/**
	 * The adapter for the list
	 */
	private GenreAdapter mAdapter;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	/**
	 * app settings
	 */
	private PreferenceUtils preference;

	private GenreLoader genreLoader;
	private GenreSongLoader genreSongLoader;
	private ExcludeMusicWorker excludeMusicWorker;

	/**
	 * context menus selection
	 */
	@Nullable
	private Genre selectedGenre;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public GenreFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Init views
		View mRootView = inflater.inflate(R.layout.list_base, container, false);
		ListView mList = mRootView.findViewById(R.id.list_base);
		TextView emptyHolder = mRootView.findViewById(R.id.list_base_empty_info);
		//
		preference = PreferenceUtils.getInstance(requireContext());
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		mAdapter = new GenreAdapter(requireContext());
		genreLoader = new GenreLoader(requireContext());
		genreSongLoader = new GenreSongLoader(requireContext());
		excludeMusicWorker = new ExcludeMusicWorker(requireContext());
		// Enable the options menu
		setHasOptionsMenu(true);
		mList.setEmptyView(emptyHolder);
		mList.setAdapter(mAdapter);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// start loader
		genreLoader.execute(null, this);
		return mRootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		genreLoader.cancel();
		genreSongLoader.cancel();
		excludeMusicWorker.cancel();
		super.onDestroyView();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the position of the selected item
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			// Create a new genre
			selectedGenre = mAdapter.getItem(info.position);
			if (selectedGenre != null) {
				// Play the genre
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the genre to the queue
				menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// hide genre from list
				if (selectedGenre.isVisible()) {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_GENRE, Menu.NONE, R.string.context_menu_hide_genre);
				} else {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_GENRE, Menu.NONE, R.string.context_menu_unhide_genre);
				}
			}
		} else {
			selectedGenre = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selectedGenre != null) {
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					String genreIds = ApolloUtils.serializeIDs(selectedGenre.getGenreIds());
					genreSongLoader.execute(genreIds, onPlaySongs);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					genreIds = ApolloUtils.serializeIDs(selectedGenre.getGenreIds());
					genreSongLoader.execute(genreIds, onAddToQueue);
					return true;

				case ContextMenuItems.HIDE_GENRE:
					excludeMusicWorker.execute(selectedGenre, onGenreHidden);
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
		Genre mGenre = mAdapter.getItem(position);
		// Create a new bundle to transfer the artist info
		Bundle bundle = new Bundle();
		bundle.putString(Config.IDS, ApolloUtils.serializeIDs(mGenre.getGenreIds()));
		bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Genres.CONTENT_TYPE);
		bundle.putString(Config.NAME, mGenre.getName());
		// Create the intent to launch the profile activity
		Intent intent = new Intent(requireActivity(), ProfileActivity.class);
		intent.putExtras(bundle);
		requireActivity().startActivity(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Genre> genres) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Genre genre : genres) {
				if (preference.getExcludeTracks() || genre.isVisible()) {
					mAdapter.add(genre);
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
				genreLoader.execute(null, this);
				break;
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

	/**
	 * called after a genre was hidden
	 */
	private void onGenreHidden(Boolean hidden) {
		if (getActivity() != null && selectedGenre != null) {
			if (hidden) {
				AppMsg.makeText(requireActivity(), R.string.item_hidden, AppMsg.STYLE_CONFIRM).show();
			}
			MusicUtils.refresh(requireActivity());
		}
	}
}