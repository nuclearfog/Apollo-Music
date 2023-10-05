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
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.receiver.PlaybackStatus;
import org.nuclearfog.apollo.receiver.PlaybackStatus.PlayStatusListener;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.views.PlayPauseButton;
import org.nuclearfog.apollo.ui.views.RepeatButton;
import org.nuclearfog.apollo.ui.views.ShuffleButton;
import org.nuclearfog.apollo.ui.views.theme.HoloSelector;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.MusicUtils.ServiceToken;
import org.nuclearfog.apollo.utils.NavUtils;

/**
 * A base {@link AppCompatActivity} used to update the bottom bar and
 * bind to Apollo's service.
 * <p>
 * {@link HomeActivity} extends from this skeleton.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class ActivityBase extends AppCompatActivity implements ServiceConnection, OnClickListener, OnQueryTextListener, PlayStatusListener {

	/**
	 * The service token
	 */
	private ServiceToken mToken;
	/**
	 * Play and pause button (BAB)
	 */
	private PlayPauseButton mPlayPauseButton;
	/**
	 * Repeat button (BAB)
	 */
	private RepeatButton mRepeatButton;
	/**
	 * Shuffle button (BAB)
	 */
	private ShuffleButton mShuffleButton;
	/**
	 * Track name (BAB)
	 */
	private TextView mTrackName;
	/**
	 * Artist name (BAB)
	 */
	private TextView mArtistName;
	/**
	 * Album art (BAB)
	 */
	private ImageView mAlbumArt;
	/**
	 * Broadcast receiver
	 */
	private PlaybackStatus mPlaybackStatus;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Control the media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Bind Apollo's service
		mToken = MusicUtils.bindToService(this, this);
		// Initialize the broadcast receiver
		mPlaybackStatus = new PlaybackStatus(this);
		// Theme the action bar
		// Initialize the bottom action bar
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		initBottomActionBar();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// Set the playback drawables
		updatePlaybackControls();
		// Current info
		updateBottomActionBarInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceDisconnected(ComponentName name) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Search view
		getMenuInflater().inflate(R.menu.search, menu);
		// Settings
		getMenuInflater().inflate(R.menu.activity_base, menu);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		// Add voice search
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		searchView.setSearchableInfo(searchableInfo);
		// Perform the search
		searchView.setOnQueryTextListener(this);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {// Settings
			NavUtils.openSettings(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Set the playback drawables
		updatePlaybackControls();
		// Current info
		updateBottomActionBarInfo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
		// Play and pause changes
		filter.addAction(MusicPlaybackService.CHANGED_PLAYSTATE);
		// Shuffle and repeat changes
		filter.addAction(MusicPlaybackService.CHANGED_SHUFFLEMODE);
		filter.addAction(MusicPlaybackService.CHANGED_REPEATMODE);
		// Track changes
		filter.addAction(MusicPlaybackService.CHANGED_META);
		// Update a list, probably the playlist fragment's
		filter.addAction(MusicPlaybackService.ACTION_REFRESH);
		registerReceiver(mPlaybackStatus, filter);
		MusicUtils.notifyForegroundStateChanged(this, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		// Unregister the receiver
		unregisterReceiver(mPlaybackStatus);
		MusicUtils.notifyForegroundStateChanged(this, false);
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		// Unbind from the service
		if (mToken != null) {
			MusicUtils.unbindFromService(mToken);
		}
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bottom_action_bar_album_art) {
			if (MusicUtils.getCurrentAudioId() != -1L) {
				NavUtils.openAlbumProfile(this, MusicUtils.getAlbumName(), MusicUtils.getArtistName(), MusicUtils.getCurrentAlbumId());
			} else {
				MusicUtils.shuffleAll(this);
			}
		} else if (v.getId() == R.id.bottom_action_bar_background) {
			if (MusicUtils.getCurrentAudioId() != -1L) {
				Intent intent = new Intent(this, AudioPlayerActivity.class);
				startActivity(intent);
			} else {
				MusicUtils.shuffleAll(this);
			}
		}
	}


	@Override
	public boolean onQueryTextSubmit(String query) {
		// Open the search activity
		NavUtils.openSearch(this, query);
		return true;
	}


	@Override
	public boolean onQueryTextChange(String newText) {
		// Nothing to do
		return false;
	}


	@Override
	public final void onMetaChange() {
		// Current info
		updateBottomActionBarInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
		onMetaChanged();
	}


	@Override
	public final void onStateChange() {
		// Set the play and pause image
		mPlayPauseButton.updateState();
	}


	@Override
	public final void onModeChange() {
		// Set the repeat image
		mRepeatButton.updateRepeatState();
		// Set the shuffle image
		mShuffleButton.updateShuffleState();
	}


	@Override
	public final void refresh() {
		onRefresh();
	}

	/**
	 * notify sub classes to reload information
	 */
	protected abstract void onRefresh();

	/**
	 * notify sub classes that meta information changed
	 */
	protected abstract void onMetaChanged();

	/**
	 * Initializes the items in the bottom action bar.
	 */
	private void initBottomActionBar() {
		// Play and pause button
		mPlayPauseButton = findViewById(R.id.action_button_play);
		// Shuffle button
		mShuffleButton = findViewById(R.id.action_button_shuffle);
		// Repeat button
		mRepeatButton = findViewById(R.id.action_button_repeat);
		// Track name
		mTrackName = findViewById(R.id.bottom_action_bar_line_one);
		// Artist name
		mArtistName = findViewById(R.id.bottom_action_bar_line_two);
		// Album art
		mAlbumArt = findViewById(R.id.bottom_action_bar_album_art);
		// background of bottom action bar
		View bottomActionBar = findViewById(R.id.bottom_action_bar_background);
		// set bottom action bar color
		bottomActionBar.setBackground(new HoloSelector(this));
		// Display the now playing screen or shuffle if this isn't anything playing
		bottomActionBar.setOnClickListener(this);
		// Open to the currently playing album profile
		mAlbumArt.setOnClickListener(this);
	}

	/**
	 * Sets the track name, album name, and album art.
	 */
	private void updateBottomActionBarInfo() {
		// Set the track name
		mTrackName.setText(MusicUtils.getTrackName());
		// Set the artist name
		mArtistName.setText(MusicUtils.getArtistName());
		// Set the album art
		ApolloUtils.getImageFetcher(this).loadCurrentArtwork(mAlbumArt);
	}

	/**
	 * Sets the correct drawable states for the playback controls.
	 */
	private void updatePlaybackControls() {
		// Set the play and pause image
		mPlayPauseButton.updateState();
		// Set the shuffle image
		mShuffleButton.updateShuffleState();
		// Set the repeat image
		mRepeatButton.updateRepeatState();
	}
}