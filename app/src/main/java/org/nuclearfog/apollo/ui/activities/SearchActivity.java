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

package org.nuclearfog.apollo.ui.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.MusicSearchLoader;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.SearchAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.List;

/**
 * Provides the search interface for Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchActivity extends ActivityBase implements AsyncCallback<List<Music>>, OnScrollListener, OnQueryTextListener, OnItemClickListener {

	/**
	 * Grid view column count. ONE - list, TWO - normal grid
	 */
	private static final int ONE = 1, TWO = 2;

	/**
	 * context menu group ID
	 */
	private static final int GROUP_ID = 0xC1A35EE4;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;
	private AsyncCallback<List<Song>> onAddToNewPlaylist = this::onAddToNewPlaylist;
	private AsyncCallback<List<Song>> onAddToExistingPlaylist = this::onAddToExistingPlaylist;
	private AsyncCallback<List<Song>> onSongsDelete = this::onSongsDelete;

	/**
	 * The query
	 */
	private String mFilterString;

	/**
	 * List view adapter
	 */
	private SearchAdapter mAdapter;

	private MusicSearchLoader mLoader;
	private AlbumSongLoader albumSongLoader;
	private ArtistSongLoader artistSongLoader;

	@Nullable
	private Music selection;
	private long selectedPlaylistId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getContentView() {
		return R.layout.grid_search;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void init(Bundle savedInstanceState) {
		// init view
		GridView mGridView = findViewById(R.id.grid_search);
		TextView emptyText = findViewById(R.id.grid_search_empty_info);
		Toolbar toolbar = findViewById(R.id.grid_search_toolbar);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		// Set the overflow style
		mResources.setOverflowStyle(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.app_name);
		}
		// Control the media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Get the query
		String query = getIntent().getStringExtra(SearchManager.QUERY);
		mFilterString = !TextUtils.isEmpty(query) ? query : "";
		// Action bar subtitle
		mResources.setSubtitle("\"" + mFilterString + "\"");
		// Initialize the adapter
		mAdapter = new SearchAdapter(this);
		albumSongLoader = new AlbumSongLoader(this);
		artistSongLoader = new ArtistSongLoader(this);
		// Set the prefix
		mAdapter.setPrefix(mFilterString);
		// Bind the data
		mGridView.setAdapter(mAdapter);
		// Recycle the data
		mGridView.setRecyclerListener(new RecycleHolder());
		// set emty message
		mGridView.setEmptyView(emptyText);
		// Speed up scrolling
		mGridView.setOnScrollListener(this);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		if (ApolloUtils.isLandscape(this)) {
			mGridView.setNumColumns(TWO);
		} else {
			mGridView.setNumColumns(ONE);
		}
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		mLoader = new MusicSearchLoader(this);
		mLoader.execute(mFilterString, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		albumSongLoader.cancel();
		artistSongLoader.cancel();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String query = intent.getStringExtra(SearchManager.QUERY);
		mFilterString = !TextUtils.isEmpty(query) ? query : "";
		// Set the prefix
		mAdapter.setPrefix(mFilterString);
		mLoader.execute(mFilterString, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			selection = mAdapter.getItem(info.position);
			if (selection instanceof Album) {
				menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
			} else if (selection instanceof Song) {
				menu.add(GROUP_ID, ContextMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
				menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
				menu.add(GROUP_ID, ContextMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			}
			menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
			menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
			menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
			MusicUtils.makePlaylistMenu(getApplicationContext(), GROUP_ID, subMenu, true);
		} else {
			selection = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selection != null) {
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					if (selection instanceof Album)
						albumSongLoader.execute(selection.getId(), onPlaySongs);
					else if (selection instanceof Artist)
						artistSongLoader.execute(selection.getId(), onPlaySongs);
					else if (selection instanceof Song)
						MusicUtils.playAll(this, new long[]{selection.getId()}, 0, false);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					if (selection instanceof Album)
						albumSongLoader.execute(selection.getId(), onAddToQueue);
					else if (selection instanceof Artist)
						artistSongLoader.execute(selection.getId(), onAddToQueue);
					else if (selection instanceof Song)
						MusicUtils.addToQueue(this, new long[]{selection.getId()});
					return true;

				case ContextMenuItems.DELETE:
					String artist = selection.getName();
					if (selection instanceof Album)
						albumSongLoader.execute(selection.getId(), onSongsDelete);
					else if (selection instanceof Artist)
						artistSongLoader.execute(selection.getId(), onSongsDelete);
					else if (selection instanceof Song)
						MusicUtils.openDeleteDialog(this, artist, new long[]{selection.getId()});
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					if (selection instanceof Album)
						NavUtils.openArtistProfile(this, ((Album) selection).getArtist());
					else if (selection instanceof Artist)
						NavUtils.openArtistProfile(this, selection.getName());
					else if (selection instanceof Song)
						NavUtils.openArtistProfile(this, ((Song) selection).getArtist());
					return true;

				case ContextMenuItems.PLAY_NEXT:
					if (selection instanceof Song)
						MusicUtils.playNext(this, new long[]{selection.getId()});
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					if (selection instanceof Album)
						albumSongLoader.execute(selection.getId(), onAddToNewPlaylist);
					else if (selection instanceof Artist)
						artistSongLoader.execute(selection.getId(), onAddToNewPlaylist);
					else if (selection instanceof Song)
						PlaylistDialog.show(getSupportFragmentManager(), PlaylistDialog.CREATE, 0, new long[]{selection.getId()}, "");
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					selectedPlaylistId = item.getIntent().getLongExtra("playlist", -1L);
					if (selectedPlaylistId != -1L) {
						if (selection instanceof Album)
							albumSongLoader.execute(selection.getId(), onAddToExistingPlaylist);
						else if (selection instanceof Artist)
							artistSongLoader.execute(selection.getId(), onAddToExistingPlaylist);
						else if (selection instanceof Song)
							MusicUtils.addToPlaylist(this, new long[]{selection.getId()}, selectedPlaylistId);
					}
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					if (selection instanceof Song)
						MusicUtils.setRingtone(this, selection.getId());
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onResult(@NonNull List<Music> result) {
		// set data
		mAdapter.clear();
		for (Music music : result) {
			mAdapter.add(music);
		}
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
	public boolean onQueryTextChange(String newText) {
		if (TextUtils.isEmpty(newText)) {
			return false;
		}
		// Called when the action bar search text has changed. Update
		// the search filter, and restart the loader to do a new query
		// with this filter.
		mFilterString = newText;
		// Set the prefix
		mAdapter.setPrefix(mFilterString);
		mLoader.execute(mFilterString, this);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Music music = mAdapter.getItem(position);
		// If it's an artist, open the artist profile
		if (music instanceof Artist) {
			Artist artist = (Artist) music;
			NavUtils.openArtistProfile(this, artist.getName());
		}
		// If it's an album, open the album profile
		else if (music instanceof Album) {
			NavUtils.openAlbumProfile(this, (Album) music);
		}
		// If it's a song, play it and leave
		else if (music instanceof Song) {
			Song song = (Song) music;
			long[] list = new long[]{song.getId()};
			MusicUtils.playAll(this, list, 0, false);
		}
		// All done
		finish();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		// not used
	}

	/**
	 * play loaded songs
	 */
	private void onPlaySongs(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(this, ids, 0, false);
	}

	/**
	 * add loaded songs to queue
	 */
	private void onAddToQueue(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.addToQueue(this, ids);
	}

	/**
	 * create a new playlist with the loaded songs
	 */
	private void onAddToNewPlaylist(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		PlaylistDialog.show(getSupportFragmentManager(), PlaylistDialog.CREATE, 0, ids, null);
	}

	/**
	 * save the loaded songs into an existing playlist
	 */
	private void onAddToExistingPlaylist(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.addToPlaylist(this, ids, selectedPlaylistId);
	}

	/**
	 * delete the loaded songs
	 */
	private void onSongsDelete(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		String name = selection != null ? selection.getName() : "";
		MusicUtils.openDeleteDialog(this, name, ids);
	}
}