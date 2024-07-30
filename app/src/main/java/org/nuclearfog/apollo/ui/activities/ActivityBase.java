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

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.SongLoader;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.receiver.PlaybackStatusReceiver;
import org.nuclearfog.apollo.receiver.PlaybackStatusReceiver.PlayStatusListener;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.views.PlayPauseButton;
import org.nuclearfog.apollo.ui.views.RepeatButton;
import org.nuclearfog.apollo.ui.views.ShuffleButton;
import org.nuclearfog.apollo.ui.views.theme.HoloSelector;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;

import java.util.List;

/**
 * A base {@link AppCompatActivity} used to update the bottom bar and
 * bind to Apollo's service.
 * <p>
 * {@link HomeActivity} extends from this skeleton.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public abstract class ActivityBase extends AppCompatActivity implements ServiceBinderCallback, OnClickListener, OnQueryTextListener, PlayStatusListener {

	/**
	 * request code for permission result
	 */
	private static final int REQ_CHECK_PERM = 0x1139398F;

	private AsyncCallback<List<Song>> onSongsShuffle = this::onSongsShuffle;

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

	private View controls;
	/**
	 * Broadcast receiver
	 */
	private PlaybackStatusReceiver mPlaybackStatus;

	private SongLoader songLoader;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentView());
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
		// media controls
		controls = findViewById(R.id.action_controls);
		// next track button
		View previousButton = findViewById(R.id.action_button_previous);
		// previous track button
		View nextButton = findViewById(R.id.action_button_next);
		// background of bottom action bar
		View bottomActionBar = findViewById(R.id.bottom_action_bar_background);
		// Control the media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Initialize the broadcast receiver
		mPlaybackStatus = new PlaybackStatusReceiver(this);
		songLoader = new SongLoader(this);
		// set bottom action bar color
		bottomActionBar.setBackground(new HoloSelector(this));
		// hide player controls
		controls.setVisibility(View.INVISIBLE);

		previousButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		bottomActionBar.setOnClickListener(this);
		mPlayPauseButton.setOnClickListener(this);
		mShuffleButton.setOnClickListener(this);
		mRepeatButton.setOnClickListener(this);
		mAlbumArt.setOnClickListener(this);

		// check permissions before initialization
		for (String permission : Constants.PERMISSIONS) {
			if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQ_CHECK_PERM);
				return;
			}
		}
		// initialize sub-class
		init(savedInstanceState);
		// Bind Apollo's service
		MusicUtils.bindToService(this, this);
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
		filter.addAction(MusicPlaybackService.CHANGED_QUEUE);
		// Track changes
		filter.addAction(MusicPlaybackService.CHANGED_META);
		// Update a list, probably the playlist fragment's
		filter.addAction(MusicPlaybackService.ACTION_REFRESH);
		// register playstate callback
		ContextCompat.registerReceiver(this, mPlaybackStatus, filter, ContextCompat.RECEIVER_EXPORTED);
		MusicUtils.notifyForegroundStateChanged(this, true);
		if (MusicUtils.isConnected(this)) {
			// update playback control after resuming
			updatePlaybackControls();
			updateBottomActionBarInfo();
		}
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
		MusicUtils.unbindFromService(this);
		songLoader.cancel();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		if (controls.getVisibility() != View.VISIBLE) {
			AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
			anim.setDuration(250);
			controls.startAnimation(anim);
			controls.setVisibility(View.VISIBLE);
		}
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
	public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		// check if permissions are granted
		if (requestCode == REQ_CHECK_PERM && grantResults.length > 0) {
			for (int grantResult : grantResults) {
				if (grantResult == PERMISSION_DENIED) {
					Toast.makeText(getApplicationContext(), R.string.error_permission_denied, Toast.LENGTH_LONG).show();
					finish();
					return;
				}
			}
			// show battery optimization dialog
			ApolloUtils.openBatteryOptimizationDialog(this);
			// initialize subclass
			init(getIntent().getExtras());
			// Bind Apollo's service
			MusicUtils.bindToService(this, this);
		}
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
		return true;
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
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		// album art clicked
		if (v.getId() == R.id.bottom_action_bar_album_art) {
			Album album = MusicUtils.getCurrentAlbum(this);
			if (album != null) {
				NavUtils.openAlbumProfile(this, album);
			} else {
				songLoader.execute(null, onSongsShuffle);
			}
		}
		// background clicked
		else if (v.getId() == R.id.bottom_action_bar_background) {
			// open audio player activity
			if (MusicUtils.getQueue(this).length > 0) {
				Intent intent = new Intent(this, AudioPlayerActivity.class);
				startActivity(intent);
			}
			// shuffle all track if queue is empty
			else {
				songLoader.execute(null, onSongsShuffle);
			}
		}
		// repeat button clicked
		else if (v.getId() == R.id.action_button_repeat) {
			int mode = MusicUtils.cycleRepeat(this);
			mRepeatButton.updateRepeatState(mode);
		}
		// shuffle button clicked
		else if (v.getId() == R.id.action_button_shuffle) {
			MusicUtils.cycleRepeat(this);
			mShuffleButton.updateShuffleState(MusicUtils.getRepeatMode(this));
		}
		// play button clicked
		else if (v.getId() == R.id.action_button_play) {
			boolean succed = MusicUtils.togglePlayPause(this);
			if (!succed) {
				songLoader.execute(null, onSongsShuffle);
			}
		}
		// go to previous track
		else if (v.getId() == R.id.action_button_previous) {
			MusicUtils.previous(this);
		}
		// go to next track
		else if (v.getId() == R.id.action_button_next) {
			MusicUtils.next(this);
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
		mPlayPauseButton.updateState(MusicUtils.isPlaying(this));
	}


	@Override
	public final void onModeChange() {
		// Set the repeat image
		mRepeatButton.updateRepeatState(MusicUtils.getRepeatMode(this));
		// Set the shuffle image
		mShuffleButton.updateShuffleState(MusicUtils.getShuffleMode(this));
	}


	@Override
	public final void refresh() {
		onRefresh();
	}

	/**
	 * Sets the track name, album name, and album art.
	 */
	private void updateBottomActionBarInfo() {
		Song song = MusicUtils.getCurrentTrack(this);
		Album album = MusicUtils.getCurrentAlbum(this);
		// set current track information
		if (song != null) {
			mTrackName.setText(song.getName());
			mArtistName.setText(song.getArtist());
		} else {
			mTrackName.setText("");
			mArtistName.setText("");
		}
		// Set the album art
		if (album != null) {
			ApolloUtils.getImageFetcher(this).loadAlbumImage(album, mAlbumArt);
		} else {
			mAlbumArt.setImageResource(0);
		}
	}

	/**
	 * Sets the correct drawable states for the playback controls.
	 */
	private void updatePlaybackControls() {
		// Set the play and pause image
		mPlayPauseButton.updateState(MusicUtils.isPlaying(this));
		// Set the shuffle image
		mShuffleButton.updateShuffleState(MusicUtils.getShuffleMode(this));
		// Set the repeat image
		mRepeatButton.updateRepeatState(MusicUtils.getRepeatMode(this));
	}

	/**
	 * called after songs loaded asynchronously to shuffle all tracks
	 */
	private void onSongsShuffle(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(this, ids, -1, true);
		updatePlaybackControls();
	}

	/**
	 * get content view to use
	 *
	 * @return layout resource ID
	 */
	protected abstract int getContentView();

	/**
	 * initialize acitivity
	 */
	protected abstract void init(Bundle savedInstanceState);

	/**
	 * notify sub classes to reload information
	 */
	protected abstract void onRefresh();

	/**
	 * notify sub classes that meta information changed
	 */
	protected abstract void onMetaChanged();
}