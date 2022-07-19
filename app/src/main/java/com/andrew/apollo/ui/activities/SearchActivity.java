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

package com.andrew.apollo.ui.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.SearchAdapter;
import com.andrew.apollo.adapters.recycler.RecycleHolder;
import com.andrew.apollo.loaders.MusicSearchLoader;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Music;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeUtils;

import java.util.List;

/**
 * Provides the search interface for Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchActivity extends ActivityBase implements LoaderCallbacks<List<Music>>,
		OnScrollListener, OnQueryTextListener, OnItemClickListener {

	/**
	 * ID of the loader
	 */
	private static final int LOADER_ID = 0xF97E2FD6;

	/**
	 * Grid view column count. ONE - list, TWO - normal grid
	 */
	private static final int ONE = 1, TWO = 2;

	/**
	 * The service token
	 */
	private ServiceToken mToken;

	/**
	 * The query
	 */
	private String mFilterString;

	/**
	 * List view adapter
	 */
	private SearchAdapter mAdapter;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_search);
		Toolbar toolbar = findViewById(R.id.grid_search_toolbar);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		// Set the overflow style
		mResources.setOverflowStyle(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.app_name);
		}
		// init view
		GridView mGridView = findViewById(R.id.grid_search);
		TextView emptyText = findViewById(R.id.grid_search_empty_info);
		View background = findViewById(R.id.grid_search_container);
		// Fade it in
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		// Control the media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Bind Apollo's service
		mToken = MusicUtils.bindToService(this, this);
		background.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.pager_background, null));
		// Get the query
		String query = getIntent().getStringExtra(SearchManager.QUERY);
		mFilterString = !TextUtils.isEmpty(query) ? query : null;
		// Action bar subtitle
		mResources.setSubtitle("\"" + mFilterString + "\"");
		// Initialize the adapter
		mAdapter = new SearchAdapter(this);
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
		if (ApolloUtils.isLandscape(this)) {
			mGridView.setNumColumns(TWO);
		} else {
			mGridView.setNumColumns(ONE);
		}
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String query = intent.getStringExtra(SearchManager.QUERY);
		mFilterString = !TextUtils.isEmpty(query) ? query : null;
		// Set the prefix
		mAdapter.setPrefix(mFilterString);
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		MusicUtils.notifyForegroundStateChanged(this, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		super.onStop();
		MusicUtils.notifyForegroundStateChanged(this, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
		if (MusicUtils.isConnected()) {
			MusicUtils.unbindFromService(mToken);
			mToken = null;
		}
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

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Music>> onCreateLoader(int id, Bundle args) {
		return new MusicSearchLoader(this, mFilterString);
	}


	@Override
	public void onLoadFinished(@NonNull Loader<List<Music>> loader, List<Music> data) {
		// disable loader until user interaction
		LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
		// set data
		mAdapter.clear();
		for (Music music : data) {
			mAdapter.add(music);
		}
	}


	@Override
	public void onLoaderReset(@NonNull androidx.loader.content.Loader<List<Music>> loader) {
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
		mFilterString = !TextUtils.isEmpty(newText) ? newText : null;
		// Set the prefix
		mAdapter.setPrefix(mFilterString);
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
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
			Album album = (Album) music;
			NavUtils.openAlbumProfile(this, album.getName(), album.getArtist(), album.getId());
		}
		// If it's a song, play it and leave
		else if (music instanceof Song) {
			Song song = (Song) music;
			long[] list = new long[]{song.getId()};
			MusicUtils.playAll(list, 0, false);
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
}