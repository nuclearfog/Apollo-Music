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
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Playlists;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.receiver.PlaybackStatus;
import org.nuclearfog.apollo.receiver.PlaybackStatus.PlayStatusListener;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.viewpager.QueueAdapter;
import org.nuclearfog.apollo.ui.fragments.QueueFragment;
import org.nuclearfog.apollo.ui.views.PlayPauseButton;
import org.nuclearfog.apollo.ui.views.RepeatButton;
import org.nuclearfog.apollo.ui.views.RepeatingImageButton;
import org.nuclearfog.apollo.ui.views.RepeatingImageButton.RepeatListener;
import org.nuclearfog.apollo.ui.views.ShuffleButton;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;
import org.nuclearfog.apollo.utils.StringUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Apollo's "now playing" interface.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AudioPlayerActivity extends AppCompatActivity implements ServiceBinderCallback, OnSeekBarChangeListener,
		OnQueryTextListener, OnClickListener, RepeatListener, PlayStatusListener {

	/**
	 * MIME type for sharing songs
	 */
	private static final String MIME_AUDIO = "audio/*";
	/**
	 * Play and pause button
	 */
	private PlayPauseButton mPlayPauseButton;
	/**
	 * Repeat button
	 */
	private RepeatButton mRepeatButton;
	/**
	 * Shuffle button
	 */
	private ShuffleButton mShuffleButton;
	/**
	 * Track name
	 */
	private TextView mTrackName;
	/**
	 * Artist name
	 */
	private TextView mArtistName;
	/**
	 * Album art
	 */
	private ImageView mAlbumArt;

	/**
	 * album art borders
	 */
	private View controls, albumArtBorder1, albumArtBorder2;
	/**
	 * Tiny artwork
	 */
	private ImageView mAlbumArtSmall;
	/**
	 * Current time
	 */
	private TextView mCurrentTime;
	/**
	 * Total time
	 */
	private TextView mTotalTime;
	/**
	 * Queue switch
	 */
	private ImageView mQueueSwitch;
	/**
	 * Progess
	 */
	private SeekBar mProgress;
	/**
	 * Broadcast receiver
	 */
	private PlaybackStatus mPlaybackStatus;
	/**
	 * thread pool used to run a task to periodically update seekbar and time
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
	/**
	 * ViewPager
	 */
	private ViewPager mViewPager;
	/**
	 * Image cache
	 */
	private ImageFetcher mImageFetcher;
	/**
	 * Theme resources
	 */
	private ThemeUtils mResources;

	private PreferenceUtils mPrefs;

	private FragmentViewModel viewModel;

	private boolean intentHandled = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set the layout
		setContentView(R.layout.activity_player_base);
		mPlayPauseButton = findViewById(R.id.action_button_play);
		mShuffleButton = findViewById(R.id.action_button_shuffle);
		mRepeatButton = findViewById(R.id.action_button_repeat);
		RepeatingImageButton mPreviousButton = findViewById(R.id.action_button_previous);
		RepeatingImageButton mNextButton = findViewById(R.id.action_button_next);
		controls = findViewById(R.id.audio_player_controls);
		mTrackName = findViewById(R.id.audio_player_track_name);
		mArtistName = findViewById(R.id.audio_player_artist_name);
		mAlbumArt = findViewById(R.id.audio_player_album_art);
		albumArtBorder1 = findViewById(R.id.audio_player_album_border);
		albumArtBorder2 = findViewById(R.id.audio_player_album_border_bottom);
		mAlbumArtSmall = findViewById(R.id.audio_player_switch_album_art);
		mCurrentTime = findViewById(R.id.audio_player_current_time);
		mTotalTime = findViewById(R.id.audio_player_total_time);
		mQueueSwitch = findViewById(R.id.audio_player_switch_queue);
		mQueueSwitch.setImageResource(R.drawable.btn_switch_queue);
		mProgress = findViewById(R.id.player_progress);

		// set toolbar
		Toolbar toolbar = findViewById(R.id.player_toolbar);
		if (toolbar != null) // toolbar only available in portrait mode
			setSupportActionBar(toolbar);
		// Initialze the theme resources
		mResources = new ThemeUtils(this);
		// app preferences
		mPrefs = PreferenceUtils.getInstance(this);
		// Set the overflow style
		mResources.setOverflowStyle(this);
		// Control the media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Initialize the image fetcher/cache
		mImageFetcher = ApolloUtils.getImageFetcher(this);
		// Initialize the broadcast receiver
		mPlaybackStatus = new PlaybackStatus(this);
		// Theme the action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			mResources.themeActionBar(actionBar, R.string.app_name);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		if (savedInstanceState != null) {
			intentHandled = true;
		}
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		// View pager
		mViewPager = findViewById(R.id.audio_player_pager);
		// Offscreen pager loading limit
		mViewPager.setOffscreenPageLimit(1);
		// Initialize the pager adapter and attach
		QueueAdapter mPagerAdapter = new QueueAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);
		// set colors
		int themeColor = mPrefs.getDefaultThemeColor();
		mShuffleButton.setColor(themeColor);
		mRepeatButton.setColor(themeColor);
		mProgress.getProgressDrawable().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		mProgress.getThumb().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		controls.setVisibility(View.INVISIBLE);
		// Bind Apollo's service
		MusicUtils.bindToService(this, this);

		mPreviousButton.setRepeatListener(this);
		mNextButton.setRepeatListener(this);
		mPreviousButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mShuffleButton.setOnClickListener(this);
		mRepeatButton.setOnClickListener(this);
		mPlayPauseButton.setOnClickListener(this);
		mProgress.setOnSeekBarChangeListener(this);
		mTrackName.setOnClickListener(this);
		mArtistName.setOnClickListener(this);
		mQueueSwitch.setOnClickListener(this);
		mAlbumArtSmall.setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		startPlayback(intent);
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
		//
		ContextCompat.registerReceiver(this, mPlaybackStatus, filter, ContextCompat.RECEIVER_EXPORTED);
		// bind activity to service
		MusicUtils.notifyForegroundStateChanged(this, true);
		// update playback control after resume
		if (MusicUtils.isConnected(this)) {
			updatePlaybackControls();
			updateNowPlayingInfo();
			updateTime();
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
		mImageFetcher.flush();
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onBackPressed() {
		NavUtils.goHome(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		threadPool.shutdown();
		MusicUtils.unbindFromService(this);
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Search view
		getMenuInflater().inflate(R.menu.search, menu);
		// Theme the search icon
		MenuItem searchAction = menu.findItem(R.id.menu_search);
		SearchView searchView = (SearchView) searchAction.getActionView();
		// Add voice search
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		searchView.setSearchableInfo(searchableInfo);
		// Favorite action
		getMenuInflater().inflate(R.menu.favorite, menu);
		// Shuffle all
		getMenuInflater().inflate(R.menu.shuffle, menu);
		// Share, ringtone, and equalizer
		getMenuInflater().inflate(R.menu.audio_player, menu);
		// Settings
		getMenuInflater().inflate(R.menu.activity_base, menu);
		// Perform the search
		searchView.setOnQueryTextListener(this);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
		MenuItem favorite = menu.findItem(R.id.menu_favorite);
		// Add fav icon
		Song song = MusicUtils.getCurrentTrack(this);
		if (song != null) {
			boolean exits = FavoritesStore.getInstance(this).exists(song.getId());
			mResources.setFavoriteIcon(favorite, exits);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int vId = item.getItemId();
		if (vId == android.R.id.home) {
			// Go back to the home activity
			NavUtils.goHome(this);
		} else if (vId == R.id.menu_shuffle) {
			// Shuffle all the songs
			MusicUtils.shuffleAll(this);
			// Refresh the queue
			refreshQueue();
		} else if (vId == R.id.menu_favorite) {
			// Toggle the current track as a favorite and update the menu item
			Song song = MusicUtils.getCurrentTrack(this);
			if (song != null) {
				FavoritesStore favoriteStore = FavoritesStore.getInstance(this);
				if (favoriteStore.exists(song.getId()))
					favoriteStore.removeFavorite(song.getId());
				else
					favoriteStore.addFavorite(song);
				invalidateOptionsMenu();
			}
		} else if (vId == R.id.menu_audio_player_ringtone) {
			// Set the current track as a ringtone
			Song song = MusicUtils.getCurrentTrack(this);
			if (song != null) {
				MusicUtils.setRingtone(this, song.getId());
			}
		} else if (vId == R.id.menu_audio_player_share) {
			// Share the current meta data
			shareCurrentTrack();
		} else if (vId == R.id.menu_audio_player_equalizer) {
			if (mPrefs.isExternalAudioFxPrefered() && ApolloUtils.isEqualizerInstalled(this)) {
				// Sound effects
				NavUtils.openEffectsPanel(this);
			} else {
				Intent intent = new Intent(this, AudioFxActivity.class);
				intent.putExtra(AudioFxActivity.KEY_SESSION_ID, MusicUtils.getAudioSessionId(this));
				startActivity(intent);
			}
		} else if (vId == R.id.menu_settings) {
			// Settings
			NavUtils.openSettings(this);
		} else if (vId == R.id.menu_audio_player_delete) {
			// Delete current song
			Song currentSong = MusicUtils.getCurrentTrack(this);
			if (currentSong != null) {
				long[] ids = {currentSong.getId()};
				MusicUtils.openDeleteDialog(this, currentSong.getName(), ids);
			}
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		// Check whether we were asked to start any playback
		startPlayback(getIntent());
		// Set the playback drawables
		updatePlaybackControls();
		// Current info
		updateNowPlayingInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
		// refresh queue after connected
		refreshQueue();
		// update time and seekbar
		updateTime();
		// init time/seekbar updater
		threadPool.scheduleAtFixedRate(new TimeHandler(this), TimeHandler.CYCLE_MS, TimeHandler.CYCLE_MS, TimeUnit.MILLISECONDS);
		// enable enimation
		AnimatorUtils.pulse(mCurrentTime, !MusicUtils.isPlaying(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
		long mPosOverride = MusicUtils.getDurationMillis(this) * progress / 1000L;
		mCurrentTime.setText(StringUtils.makeTimeString(this, mPosOverride));
		if (fromuser) {
			MusicUtils.seek(this, mPosOverride);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStartTrackingTouch(SeekBar bar) {
		// set blinking time indicator permanently visible
		mCurrentTime.setVisibility(View.VISIBLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MusicUtils.REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.onPostDelete(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
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
	public void onClick(@NonNull View v) {
		if (v.getId() == R.id.audio_player_artist_name || v.getId() == R.id.audio_player_track_name) {
			Album album = MusicUtils.getCurrentAlbum(this);
			if (album != null) {
				NavUtils.openAlbumProfile(this, album);
			}
		}
		// Show the queue, hide the artwork
		else if (v.getId() == R.id.audio_player_switch_queue) {
			mViewPager.setVisibility(View.VISIBLE);
			mAlbumArtSmall.setVisibility(View.VISIBLE);
			// hide this button
			mQueueSwitch.setVisibility(View.INVISIBLE);
			// Fade out the artwork
			if (albumArtBorder1 != null)
				AnimatorUtils.fade(albumArtBorder1, false);
			if (albumArtBorder2 != null)
				AnimatorUtils.fade(albumArtBorder2, false);
			AnimatorUtils.fade(mAlbumArt, false);
			// Scroll to the current track
			setQueueTrack();
		}
		// Show the artwork, hide the queue
		else if (v.getId() == R.id.audio_player_switch_album_art) {
			mViewPager.setVisibility(View.INVISIBLE);
			mQueueSwitch.setVisibility(View.VISIBLE);
			// hide this button
			mAlbumArtSmall.setVisibility(View.INVISIBLE);
			// Fade in the album art
			if (albumArtBorder1 != null)
				AnimatorUtils.fade(albumArtBorder1, true);
			if (albumArtBorder2 != null)
				AnimatorUtils.fade(albumArtBorder2, true);
			AnimatorUtils.fade(mAlbumArt, true);
		}
		// repeat button clicked
		else if (v.getId() == R.id.action_button_repeat) {
			int mode = MusicUtils.cycleRepeat(this);
			mRepeatButton.updateRepeatState(mode);
		}
		// shuffle button clicked
		else if (v.getId() == R.id.action_button_shuffle) {
			int mode = MusicUtils.cycleShuffle(this);
			mShuffleButton.updateShuffleState(mode);
		}
		// play/pause button clicked
		else if (v.getId() == R.id.action_button_play) {
			MusicUtils.togglePlayPause(this);
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
	public void onRepeat(@NonNull View v, int repcnt) {
		if (v.getId() == R.id.action_button_previous) {
			if (repcnt > 0)
				scan(false);
		} else if (v.getId() == R.id.action_button_next) {
			if (repcnt > 0)
				scan(true);
		}
	}


	@Override
	public void onMetaChange() {
		// Current info
		updateNowPlayingInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
		// jumpt to current track
		setQueueTrack();
	}


	@Override
	public void onStateChange() {
		// Set the play and pause image
		boolean playing = MusicUtils.isPlaying(this);
		mPlayPauseButton.updateState(playing);
		AnimatorUtils.pulse(mCurrentTime, !playing);
	}


	@Override
	public void onModeChange() {
		// Set the repeat image
		mRepeatButton.updateRepeatState(MusicUtils.getRepeatMode(this));
		// Set the shuffle image
		mShuffleButton.updateShuffleState(MusicUtils.getShuffleMode(this));
	}


	@Override
	public void refresh() {
	}

	/**
	 * Sets the track name, album name, and album art.
	 */
	private void updateNowPlayingInfo() {
		Song song = MusicUtils.getCurrentTrack(this);
		Album album = MusicUtils.getCurrentAlbum(this);
		if (song != null && album != null) {
			// Set the track name
			mTrackName.setText(song.getName());
			// Set the artist name
			mArtistName.setText(song.getArtist());
			// Set the total time
			mTotalTime.setText(StringUtils.makeTimeString(this, MusicUtils.getDurationMillis(this)));
			// Set the album art
			mImageFetcher.loadAlbumImage(album, mAlbumArt);
			// Set the small artwork
			mImageFetcher.loadAlbumImage(album, mAlbumArtSmall);
		}
	}

	/**
	 *
	 */
	private long parseIdFromIntent(@NonNull Intent intent, String longKey, String stringKey) {
		long id = intent.getLongExtra(longKey, -1L);
		if (id == -1L) {
			String idString = intent.getStringExtra(stringKey);
			if (idString != null) {
				try {
					id = Long.parseLong(idString);
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		return id;
	}

	/**
	 * Checks whether the passed intent contains a playback request,
	 * and starts playback if that's the case
	 */
	private void startPlayback(@Nullable Intent intent) {
		if (intent != null && !intentHandled) {
			intentHandled = true;
			Uri uri = intent.getData();
			String mimeType = intent.getType();
			// open file
			if (uri != null && !uri.toString().trim().isEmpty()) {
				MusicUtils.playFile(this, uri);
				refreshQueue();
			}
			// open playlist
			else if (Playlists.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(intent, "playlistId", "playlist");
				if (id != -1L) {
					MusicUtils.playPlaylist(this, id);
					refreshQueue();
				}
			}
			// open album
			else if (Albums.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(intent, "albumId", "album");
				if (id != -1L) {
					int position = intent.getIntExtra("position", 0);
					MusicUtils.playAlbum(this, id, position);
					refreshQueue();
				}
			}
			// open artist
			else if (Artists.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(intent, "artistId", "artist");
				if (id != -1L) {
					int position = intent.getIntExtra("position", 0);
					MusicUtils.playArtist(this, id, position);
					refreshQueue();
				}
			}
		}
	}

	/**
	 * Sets the correct drawable states for the playback controls.
	 */
	private void updatePlaybackControls() {
		// fade in player control after initialization
		if (controls.getVisibility() != View.VISIBLE && controls.getAnimation() == null) {
			AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
			anim.setDuration(500);
			controls.setVisibility(View.VISIBLE);
			controls.startAnimation(anim);
		}
		// Set the play and pause image
		mPlayPauseButton.updateState(MusicUtils.isPlaying(this));
		// Set the shuffle image
		mShuffleButton.updateShuffleState(MusicUtils.getShuffleMode(this));
		// Set the repeat image
		mRepeatButton.updateRepeatState(MusicUtils.getRepeatMode(this));
	}

	/**
	 * Used to scan backwards in time through the curren track
	 *
	 * @param forward true to scan forward
	 */
	private void scan(boolean forward) {
		long duration = MusicUtils.getDurationMillis(this);
		long position = MusicUtils.getPositionMillis(this);
		if (forward) {
			position = Math.min(duration, position + duration / 32L);
		} else {
			position = Math.max(0, position - duration / 32L);
		}
		MusicUtils.seek(this, position);
		int progress = (int) (position * 1000L / duration);
		mProgress.setProgress(progress);
	}

	/**
	 * Used to shared what the user is currently listening to
	 */
	private void shareCurrentTrack() {
		String path = MusicUtils.getPlaybackFilePath(this);
		if (path != null && !path.isEmpty()) {
			try {
				File file = new File(path);
				Uri fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				shareIntent.setDataAndType(fileUri, MIME_AUDIO);
				shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
					shareIntent.setClipData(ClipData.newRawUri("", fileUri));
				startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
			} catch (Exception err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * reload queue tracks
	 */
	private void refreshQueue() {
		viewModel.notify(QueueFragment.REFRESH);
	}

	/**
	 * set current track in the queue
	 */
	private void setQueueTrack() {
		viewModel.notify(QueueFragment.META_CHANGED);
	}

	/**
	 * update current track progress
	 */
	private void updateTime() {
		long pos = MusicUtils.getPositionMillis(this);
		if (pos >= 0 && pos <= MusicUtils.getDurationMillis(this)) {
			int progress = (int) (1000 * pos / MusicUtils.getDurationMillis(this));
			mProgress.setProgress(progress);
			if (MusicUtils.isPlaying(this)) {
				mCurrentTime.setVisibility(View.VISIBLE);
			}
		} else {
			mCurrentTime.setText("--:--");
			mProgress.setProgress(0);
		}
	}

	/**
	 * Used to update the current time string
	 */
	private static final class TimeHandler implements Runnable {

		public static final int CYCLE_MS = 500;

		private WeakReference<AudioPlayerActivity> mAudioPlayer;


		public TimeHandler(AudioPlayerActivity player) {
			super();
			mAudioPlayer = new WeakReference<>(player);
		}


		@Override
		public void run() {
			AudioPlayerActivity activity = mAudioPlayer.get();
			if (activity != null) {
				try {
					activity.updateTime();
				} catch (Exception exception) {
					// ignore
				}
			}
		}
	}
}