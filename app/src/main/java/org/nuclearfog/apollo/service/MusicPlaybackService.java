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

package org.nuclearfog.apollo.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Files.FileColumns;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.player.AudioEffects;
import org.nuclearfog.apollo.player.MultiPlayer;
import org.nuclearfog.apollo.player.MultiPlayer.OnPlaybackStatusCallback;
import org.nuclearfog.apollo.receiver.UnmountBroadcastReceiver;
import org.nuclearfog.apollo.receiver.WidgetBroadcastReceiver;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.utils.CursorFactory;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * A background {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 *
 * @author nuclearfog
 */
public class MusicPlaybackService extends Service implements OnAudioFocusChangeListener, OnPlaybackStatusCallback {
	/**
	 *
	 */
	private static final String TAG = "MusicPlaybackService";
	/**
	 *
	 */
	private static final String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
	/**
	 *
	 */
	private static final String MUSIC_PACKAGE_NAME = "com.android.music";
	/**
	 * Called to indicate a general service commmand.
	 */
	public static final String SERVICECMD = APOLLO_PACKAGE_NAME + ".musicservicecommand";
	/**
	 * used to determine if app is in foreground
	 */
	public static final String EXTRA_FOREGROUND = "nowinforeground";
	/**
	 * Indicates that the music has paused or resumed
	 */
	public static final String CHANGED_PLAYSTATE = APOLLO_PACKAGE_NAME + ".playstatechanged";
	/**
	 * Indicates that music playback position within a title was changed
	 */
	private static final String CHANGED_POSITION = APOLLO_PACKAGE_NAME + ".positionchanged";
	/**
	 * Indicates the meta data has changed in some way, like a track change
	 */
	public static final String CHANGED_META = APOLLO_PACKAGE_NAME + ".metachanged";
	/**
	 * Indicates the queue has been updated
	 */
	public static final String CHANGED_QUEUE = APOLLO_PACKAGE_NAME + ".queuechanged";
	/**
	 * Indicates the repeat mode chaned
	 */
	public static final String CHANGED_REPEATMODE = APOLLO_PACKAGE_NAME + ".repeatmodechanged";
	/**
	 * Indicates the shuffle mode chaned
	 */
	public static final String CHANGED_SHUFFLEMODE = APOLLO_PACKAGE_NAME + ".shufflemodechanged";
	/**
	 * Called to go toggle between pausing and playing the music
	 */
	public static final String ACTION_TOGGLEPAUSE = APOLLO_PACKAGE_NAME + ".togglepause";
	/**
	 * Called to go to stop the playback
	 */
	public static final String ACTION_STOP = APOLLO_PACKAGE_NAME + ".stop";
	/**
	 * Called to go to the previous track
	 */
	public static final String ACTION_PREVIOUS = APOLLO_PACKAGE_NAME + ".previous";
	/**
	 * Called to go to the next track
	 */
	public static final String ACTION_NEXT = APOLLO_PACKAGE_NAME + ".next";
	/**
	 * Called to change the repeat mode
	 */
	public static final String ACTION_REPEAT = APOLLO_PACKAGE_NAME + ".repeat";
	/**
	 * Called to change the shuffle mode
	 */
	public static final String ACTION_SHUFFLE = APOLLO_PACKAGE_NAME + ".shuffle";
	/**
	 * Used to easily notify a list that it should refresh. i.e. A playlist changes
	 */
	public static final String ACTION_REFRESH = APOLLO_PACKAGE_NAME + ".refresh";
	/**
	 *
	 */
	public static final String CMDNAME = "command";
	/**
	 * Moves a list to the next position in the queue
	 */
	public static final int MOVE_NEXT = 0xAE960453;
	/**
	 * Moves a list to the last position in the queue
	 */
	public static final int MOVE_LAST = 0xB03ED8F4;
	/**
	 * Shuffles no songs, turns shuffling off
	 */
	public static final int SHUFFLE_NONE = 0xD47F8582;
	/**
	 * Shuffles all songs
	 */
	public static final int SHUFFLE_NORMAL = 0xC5F90214;
	/**
	 * Party shuffle
	 */
	public static final int SHUFFLE_AUTO = 0x45EBC386;
	/**
	 * Turns repeat off
	 */
	public static final int REPEAT_NONE = 0x28AEE9F7;
	/**
	 * Repeats the current track in a list
	 */
	public static final int REPEAT_CURRENT = 0x4478C4B2;
	/**
	 * Repeats all the tracks in a list
	 */
	public static final int REPEAT_ALL = 0xEE3F9E0B;

	/**
	 * Song play time used as threshold for rewinding to the beginning of the
	 * track instead of skipping to the previous track when getting the PREVIOUS
	 * command
	 */
	private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000L;
	/**
	 * The max size allowed for the track history
	 */
	private static final int MAX_HISTORY_SIZE = 100;
	/**
	 * Keeps a mapping of the track history
	 */
	private LinkedList<Integer> mHistory = new LinkedList<>();
	/**
	 * current playlist containing track ID's
	 */
	private LinkedList<Long> mPlayList = new LinkedList<>();
	/**
	 * shuffle list containing track indexes of the current playlist
	 */
	private ArrayList<Integer> mShuffleList = new ArrayList<>();
	/**
	 * random generator used for shuffle
	 */
	private Random mRandom = new Random();
	/**
	 * app wide settings
	 */
	private PreferenceUtils settings;
	/**
	 * audio manager to gain audio focus
	 */
	private AudioManager mAudio;
	/**
	 * Broadcast receiver for widget actions
	 */
	private WidgetBroadcastReceiver mIntentReceiver;
	/**
	 * broadcast listener for unmounting external storage
	 */
	private BroadcastReceiver mUnmountReceiver;
	/**
	 * handler used to shutdown service after idle
	 */
	private ShutdownHandler shutdownHandler;
	/**
	 * The media player
	 */
	private MultiPlayer mPlayer;
	/**
	 * MediaSession to init media button support
	 */
	private MediaSessionCompat mSession;
	/**
	 * Used to build the notification
	 */
	private NotificationHelper mNotificationHelper;
	/**
	 * Recently listened database
	 */
	private RecentStore mRecentsCache;
	/**
	 * most played tracks database
	 */
	private PopularStore mPopularCache;
	/**
	 * Used to know when the service is active
	 */
	private boolean mServiceInUse = false;
	/**
	 * Used to indicate if the queue can be saved
	 */
	private boolean mQueueIsSaveable = true;
	/**
	 * Used to track what type of audio focus loss caused the playback to pause
	 */
	private boolean mPausedByTransientLossOfFocus = false;
	/**
	 * used to check if service is running in the foreground
	 */
	private boolean isForeground = false;
	/**
	 * current song to play
	 */
	@Nullable
	private volatile Song currentSong;
	/**
	 * current album of the song to play
	 */
	@Nullable
	private volatile Album currentAlbum;

	private int mServiceStartId = -1;
	private int mShuffleMode = SHUFFLE_NONE;
	private int mRepeatMode = REPEAT_ALL;
	private int mShufflePos = -1;
	private int mPlayPos = -1;
	private int mNextPlayPos = -1;
	/**
	 * used to distinguish between different cards when saving/restoring playlists
	 */
	private int mCardId = -1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(Intent intent) {
		mServiceInUse = true;
		shutdownHandler.stop();
		return new ServiceStub(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRebind(Intent intent) {
		mServiceInUse = true;
		shutdownHandler.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		mServiceInUse = false;
		return releaseServiceUiAndStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Initialize the favorites and recents databases
		mRecentsCache = RecentStore.getInstance(this);
		mPopularCache = PopularStore.getInstance(this);
		// initialize broadcast receiver
		mIntentReceiver = new WidgetBroadcastReceiver(this);
		mUnmountReceiver = new UnmountBroadcastReceiver(this);
		//
		mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// Initialize the media player
		mPlayer = new MultiPlayer(getMainLooper(), this);
		// init media session
		mSession = new MediaSessionCompat(getApplicationContext(), TAG);
		mSession.setCallback(new MediaButtonCallback(this), null);
		mSession.setActive(true);
		// Initialize the notification helper
		mNotificationHelper = new NotificationHelper(this, mSession);
		// Initialize the preferences
		settings = PreferenceUtils.getInstance(this);
		// init shutdown handler
		shutdownHandler = new ShutdownHandler(this);
		getCardId();

		// init external storage listener
		IntentFilter filterStorage = new IntentFilter();
		filterStorage.addAction(Intent.ACTION_MEDIA_EJECT);
		filterStorage.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filterStorage.addDataScheme("file");
		// init the intent filter and each action
		IntentFilter filterAction = new IntentFilter();
		filterAction.addAction(SERVICECMD);
		filterAction.addAction(ACTION_TOGGLEPAUSE);
		filterAction.addAction(ACTION_STOP);
		filterAction.addAction(ACTION_NEXT);
		filterAction.addAction(ACTION_PREVIOUS);
		filterAction.addAction(ACTION_REPEAT);
		filterAction.addAction(ACTION_SHUFFLE);
		// register all receiver
		ContextCompat.registerReceiver(this, mIntentReceiver, filterAction, ContextCompat.RECEIVER_EXPORTED);
		ContextCompat.registerReceiver(this, mUnmountReceiver, filterStorage, ContextCompat.RECEIVER_EXPORTED);

		// send session ID to external equalizer if set
		if (settings.isExternalAudioFxPrefered() && !settings.isAudioFxEnabled()) {
			Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
			intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mPlayer.getAudioSessionId());
			intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
			sendBroadcast(intent);
		}
		// Bring the queue back
		reloadQueue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		// Remove any sound effects
		Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, APOLLO_PACKAGE_NAME);
		sendBroadcast(audioEffectsIntent);
		AudioEffects.release();
		// Release the player
		mPlayer.release();
		// release player callbacks
		mSession.release();
		// Unregister the mount listener
		unregisterReceiver(mUnmountReceiver);
		unregisterReceiver(mIntentReceiver);
		// remove notification
		mNotificationHelper.dismissNotification();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				if (mPlayer.isPlaying())
					mPausedByTransientLossOfFocus = true;
				// fall through

			case AudioManager.AUDIOFOCUS_LOSS:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				pause(true);
				break;

			case AudioManager.AUDIOFOCUS_GAIN:
				mPausedByTransientLossOfFocus = false;
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mServiceStartId = startId;
		if (intent != null) {
			if (intent.hasExtra(EXTRA_FOREGROUND)) {
				isForeground = intent.getBooleanExtra(EXTRA_FOREGROUND, false);
				// create player control notification if player is not stopped
				if (!ACTION_STOP.equals(intent.getAction())) {
					mNotificationHelper.createNotification();
					if (isForeground && !isPlaying()) {
						shutdownHandler.start();
					}
				}
			}
			handleCommandIntent(intent);
			return START_STICKY;
		}
		// Make sure the service will shut down on its own if it was
		// just started but not bound to and nothing is playing
		shutdownHandler.start();
		return START_NOT_STICKY;
	}


	@Override
	public boolean onPlaybackEnd(boolean gotoNext) {
		if (gotoNext) {
			// repeat current track by seeking to 0
			if (mRepeatMode == REPEAT_CURRENT) {
				seekTo(0);
				return true;
			}
			// no repeat mode set, check if reached end of the queue, then stop playback
			else if (mRepeatMode == REPEAT_NONE && mPlayPos < 0) {
				notifyChange(CHANGED_PLAYSTATE);
			}
			// go to next track if any
			else if (mNextPlayPos >= 0) {
				// go to next track
				mPlayPos = mNextPlayPos;
				setNextTrack(mRepeatMode != REPEAT_NONE);
				updateTrackInformation();
				return true;
			}
		} else {
			pause(true);
		}
		return false;
	}


	@Override
	public void onPlaybackError() {
		if (isPlaying()) {
			gotoNext();
		} else {
			openCurrentAndNext();
		}
	}

	/**
	 * used by widgets or other intents to change playback state
	 */
	public synchronized void handleCommandIntent(Intent intent) {
		String action = intent.getAction();
		// next track
		if (ACTION_NEXT.equals(action)) {
			gotoNext();
		}
		// previous track
		else if (ACTION_PREVIOUS.equals(action)) {
			gotoPrev();
		}
		// pause/play track
		else if (ACTION_TOGGLEPAUSE.equals(action)) {
			if (mPlayer.isPlaying()) {
				pause(false);
				mPausedByTransientLossOfFocus = false;
			} else {
				play();
			}
		}
		// stop track/dismiss notification
		else if (ACTION_STOP.equals(action)) {
			stop();
			mPausedByTransientLossOfFocus = false;
			releaseServiceUiAndStop();
		}
		// repeat set
		else if (ACTION_REPEAT.equals(action)) {
			if (mRepeatMode == REPEAT_NONE) {
				setRepeatMode(REPEAT_ALL);
			} else if (mRepeatMode == REPEAT_ALL) {
				setRepeatMode(REPEAT_CURRENT);
				if (mShuffleMode != SHUFFLE_NONE) {
					setShuffleMode(SHUFFLE_NONE);
				}
			} else {
				setRepeatMode(REPEAT_NONE);
			}
		}
		// shuffle set
		else if (ACTION_SHUFFLE.equals(action)) {
			if (mShuffleMode == SHUFFLE_NONE) {
				setShuffleMode(SHUFFLE_NORMAL);
				if (mRepeatMode == REPEAT_CURRENT) {
					setRepeatMode(REPEAT_ALL);
				}
			} else if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
				setShuffleMode(SHUFFLE_NONE);
			}
		}
		// handle play/pause button
		else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
			KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (isPlaying()) {
					pause(false);
				} else {
					play();
				}
			}
		}
		MediaButtonReceiver.handleIntent(mSession, intent);
	}

	/**
	 * @return True if music is playing, false otherwise
	 */
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	/**
	 * called if multimediacard is rejected by user
	 */
	public synchronized void onEject() {
		saveQueue(true);
		mQueueIsSaveable = false;
		stop();
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * called if multimedia card was mounted
	 */
	public synchronized void onMediaMount() {
		getCardId();
		reloadQueue();
		mQueueIsSaveable = true;
	}

	/**
	 * Returns the shuffle mode
	 *
	 * @return The current shuffle mode ({@link #SHUFFLE_AUTO,#SHUFFLE_NORMAL,#SHUFFLE_NONE}
	 */
	public int getShuffleMode() {
		return mShuffleMode;
	}

	/**
	 * Returns the repeat mode
	 *
	 * @return The current repeat mode {@link #REPEAT_ALL,#REPEAT_CURRENT,#REPEAT_NONE}
	 */
	public int getRepeatMode() {
		return mRepeatMode;
	}

	/**
	 * Returns the album name
	 *
	 * @return The current song album Name
	 */
	@Nullable
	public Album getCurrentAlbum() {
		return currentAlbum;
	}

	/**
	 * Returns the song name
	 *
	 * @return The current song name
	 */
	@Nullable
	public Song getCurrentSong() {
		return currentSong;
	}

	/**
	 * Stops playback.
	 */
	synchronized void stop() {
		if (mPlayer.initialized()) {
			mPlayer.stop();
		}
		clearCurrentTrackInformation();
		notifyChange(CHANGED_PLAYSTATE);
	}

	/**
	 * Resumes or starts playback.
	 */
	synchronized void play() {
		if (mAudio != null) {
			AudioAttributesCompat.Builder attrCompat = new AudioAttributesCompat.Builder();
			attrCompat.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC);
			attrCompat.setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
			AudioFocusRequestCompat.Builder request = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN);
			request.setAudioAttributes(attrCompat.build());
			request.setOnAudioFocusChangeListener(this);
			int returnCode = AudioManagerCompat.requestAudioFocus(mAudio, request.build());
			if (returnCode == AudioManager.AUDIOFOCUS_GAIN) {
				if (mPlayer.initialized()) {
					long duration = mPlayer.getDuration();
					if (mRepeatMode != REPEAT_CURRENT && duration > 2000L && mPlayer.getPosition() >= duration - 2000L) {
						gotoNext();
					}
					if (mPlayer.play()) {
						notifyChange(CHANGED_PLAYSTATE);
					}
				} else if (!mPlayer.busy() && mPlayList.isEmpty()) {
					setShuffleMode(SHUFFLE_AUTO);
				}
			} else {
				Log.v(TAG, "could not gain audio focus!");
			}
		}
	}

	/**
	 * Temporarily pauses playback.
	 */
	synchronized void pause(boolean force) {
		if (mPlayer.pause(force) && force) {
			notifyChange(CHANGED_PLAYSTATE);
		}
	}

	/**
	 * Changes from the current track to the next track
	 */
	synchronized void gotoNext() {
		if (!mPlayList.isEmpty()) {
			if (!mPlayer.isPlaying() || !mPlayer.next()) {
				// reload next tracks if an error occured
				mPlayPos = incrementPosition(mPlayPos, true);
				openCurrentAndNext();
				play();
			}
		} else if (makeShuffleList(true)) {
			mPlayPos = 0;
			openCurrentAndNext();
			play();
		}
	}

	/**
	 * restart current track or go to preview track
	 */
	synchronized void gotoPrev() {
		if (!mPlayer.busy()) {
			if (mPlayer.getPosition() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
				mPlayPos = decrementPosition(mPlayPos);
				stop();
				openCurrentAndNext();
				play();
			} else {
				seekTo(0);
				play();
			}
		}
	}

	/**
	 * Seeks the current track to a specific time
	 *
	 * @param position The time to seek to
	 */
	synchronized void seekTo(long position) {
		if (!mPlayer.busy()) {
			mPlayer.setPosition(position);
			notifyChange(CHANGED_POSITION);
		}
	}

	/**
	 * open file uri
	 *
	 * @param uri URI of the local file
	 */
	synchronized void openFile(@NonNull Uri uri) {
		stop();
		updateTrackInformation(uri);
		Song song = currentSong;
		// check if track is valid
		if (song != null) {
			// add at the beginning of the playlist
			mPlayList.addFirst(song.getId());
			mPlayPos = 0;
			// update metadata
			notifyChange(CHANGED_QUEUE);
			if (mPlayer.setDataSource(getApplicationContext(), uri)) {
				play();
				setNextTrack(false);
			} else {
				stop();
			}
		}
		// restore track information after error
		else {
			updateTrackInformation();
		}
	}

	/**
	 * Notify the change-receivers that something has changed.
	 *
	 * @param what what changed e.g. {@link #CHANGED_PLAYSTATE,#CHANGED_META}
	 */
	synchronized void notifyChange(String what) {
		Song song = currentSong;
		Album album = currentAlbum;
		// send broadcast
		Intent intent = new Intent(what);
		intent.putExtra("playing", isPlaying());
		if (song != null) {
			intent.putExtra("id", song.getId());
			intent.putExtra("artist", song.getArtist());
			intent.putExtra("album", song.getAlbum());
			intent.putExtra("track", song.getName());
			intent.putExtra("isfavorite", MusicUtils.isFavorite(song, this));
		}
		Intent musicIntent = new Intent(intent);
		musicIntent.setAction(what.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
		sendBroadcast(musicIntent);
		sendBroadcast(intent);

		switch (what) {
			case CHANGED_META:
				// Increase the play count for favorite songs.
				if (song != null)
					mPopularCache.addSong(song);
				if (album != null)
					mRecentsCache.addAlbum(album);
				updateMetadata();
				// fall through

			case CHANGED_PLAYSTATE:
				if (isForeground) {
					mNotificationHelper.updateNotification();
					if (mPlayer.isPlaying()) {
						shutdownHandler.stop();
					} else {
						shutdownHandler.start();
					}
				} else {
					mNotificationHelper.dismissNotification();
					shutdownHandler.stop();
				}
				// fall through

			case CHANGED_POSITION:
				updatePlaybackstate();
				break;

			case CHANGED_QUEUE:
				saveQueue(true);
				if (isPlaying()) {
					setNextTrack(false);
				}
				break;

			default:
				saveQueue(false);
				break;
		}
		mIntentReceiver.updateWidgets(this, what);
	}

	/**
	 * Returns the audio session ID
	 *
	 * @return The current media player audio session ID
	 */
	synchronized int getAudioSessionId() {
		return mPlayer.getAudioSessionId();
	}

	/**
	 * Sets the shuffle mode
	 *
	 * @param shufflemode The shuffle mode to use
	 */
	synchronized void setShuffleMode(int shufflemode) {
		if (mShuffleMode != shufflemode || mPlayList.isEmpty()) {
			// setup party shuffle
			if (shufflemode == SHUFFLE_AUTO) {
				if (makeShuffleList(true)) {
					mShuffleMode = SHUFFLE_AUTO;
					mPlayPos = 0;
					mShufflePos = 0;
					openCurrentAndNext();
					play();
				}
			}
			// setup queue shuffle
			else if (shufflemode == SHUFFLE_NORMAL) {
				if (makeShuffleList(false)) {
					mShuffleMode = SHUFFLE_NORMAL;
					mPlayPos = 0;
					mShufflePos = 0;
					openCurrentAndNext();
					play();
				}
			}
			// reset shuffle mode
			else if (shufflemode == SHUFFLE_NONE) {
				clearShuffleList();
				setNextTrack(false);
			}
			saveQueue(false);
			notifyChange(CHANGED_SHUFFLEMODE);
		}
	}

	/**
	 * Sets the position of a track in the queue
	 *
	 * @param index The position to place the track
	 */
	synchronized void setQueuePosition(int index) {
		mPlayPos = index;
		openCurrentAndNext();
		play();
	}

	/**
	 * Returns the position in the queue
	 *
	 * @return the current position in the queue
	 */
	int getQueuePosition() {
		return mPlayPos;
	}

	/**
	 * clear the curren queue and stop playback
	 */
	synchronized void clearQueue() {
		stop();
		mPlayPos = -1;
		mPlayList.clear();
		clearCurrentTrackInformation();
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * remove single track from queue at specific position
	 *
	 * @param pos position of the track in the queue
	 */
	synchronized void removeQueueTrack(int pos) {
		if (pos >= 0 && pos < mPlayList.size()) {
			// remove track at position
			mPlayList.remove(pos);
			// check if current play position is higher than the removed track
			if (mPlayPos > pos) {
				mPlayPos--;
				notifyChange(CHANGED_POSITION);
			}
			// check if current track was removed then stop playback
			else if (mPlayPos == pos) {
				if (mPlayPos > 0)
					mPlayPos = Math.min(mPlayPos, mPlayList.size() - 1);
				stop();
			}
			// stop playback if queue is empty
			if (mPlayList.isEmpty()) {
				mPlayPos = -1;
				stop();
			}
			// notify that queue changed
			else {
				notifyChange(CHANGED_QUEUE);
			}
		}
	}

	/**
	 * Removes all instances of the track with the given ID from the playlist.
	 *
	 * @param ids track IDs to remove
	 * @return how many instances of the track were removed
	 */
	synchronized int removeQueueTracks(long[] ids) {
		int numremoved = 0;
		for (long id : ids) {
			int pos;
			do {
				// get index of the track ID
				pos = mPlayList.indexOf(id);
				if (pos >= 0) {
					mPlayList.remove(pos);
					// check if current play position is higher than the removed track
					if (mPlayPos > pos) {
						mPlayPos--;
					}
					// check if current track was removed then stop playback
					else if (mPlayPos == pos) {
						if (mPlayPos > 0)
							mPlayPos = Math.min(mPlayPos, mPlayList.size() - 1);
						stop();
					}
					numremoved++;
				}
			} while (pos >= 0);
			if (mPlayList.isEmpty()) {
				break;
			}
		}
		// stop playback if queue is empty
		if (mPlayList.isEmpty()) {
			mPlayPos = -1;
			stop();
		}
		// notify if any tracks were removed
		if (numremoved > 0) {
			notifyChange(CHANGED_QUEUE);
			notifyChange(CHANGED_POSITION);
		}
		return numremoved;
	}

	/**
	 * Returns the queue
	 *
	 * @return The queue containing song IDs
	 */
	synchronized long[] getQueue() {
		Long[] ids = mPlayList.toArray(new Long[0]);
		long[] list = new long[ids.length];
		for (int i = 0; i < ids.length; i++)
			list[i] = ids[i];
		return list;
	}

	/**
	 * Returns the current position in time of the currenttrack
	 *
	 * @return The current playback position in miliseconds
	 */
	synchronized long getPosition() {
		if (mPlayer.initialized()) {
			return mPlayer.getPosition();
		}
		return 0;
	}

	/**
	 * Sets the repeat mode
	 *
	 * @param repeatmode The repeat mode to use
	 */
	synchronized void setRepeatMode(int repeatmode) {
		mRepeatMode = repeatmode;
		setNextTrack(false);
		saveQueue(false);
		notifyChange(CHANGED_REPEATMODE);
	}

	/**
	 * Opens a list for playback
	 *
	 * @param list     The list of tracks to open
	 * @param position The position to start playback at
	 */
	synchronized void open(long[] list, int position) {
		stop();
		if (mShuffleMode == SHUFFLE_AUTO) {
			mShuffleMode = SHUFFLE_NORMAL;
		}
		mPlayList.clear();
		for (long trackId : list)
			mPlayList.add(trackId);
		mPlayPos = position >= 0 ? position : mRandom.nextInt(mPlayList.size() - 1);
		notifyChange(CHANGED_QUEUE);
		mHistory.clear();
		openCurrentAndNext();
		play();
	}

	/**
	 * Moves an item in the queue from one position to another
	 *
	 * @param from The position the item is currently at
	 * @param to   The position the item is being moved to
	 */
	synchronized void moveQueueItem(int from, int to) {
		if (from >= mPlayList.size()) {
			from = mPlayList.size() - 1;
		}
		if (to >= mPlayList.size()) {
			to = mPlayList.size() - 1;
		}
		// move track
		long trackId = mPlayList.remove(from);
		mPlayList.add(to, trackId);
		// set current play pos
		if (mPlayPos == from) {
			mPlayPos = to;
		} else if (mPlayPos >= from && mPlayPos <= to) {
			mPlayPos--;
		} else if (mPlayPos <= from && mPlayPos >= to) {
			mPlayPos++;
		}
		mNextPlayPos = incrementPosition(mPlayPos, false);
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 *
	 */
	synchronized void stopForeground() {
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
		mNotificationHelper.dismissNotification();
		shutdownHandler.stop();
		isForeground = false;
	}

	/**
	 * Queues a new list for playback
	 *
	 * @param list   The list to queue
	 * @param action The action to take
	 */
	synchronized void enqueue(long[] list, int action) {
		if (action == MOVE_NEXT) {
			addToPlayList(list, mPlayPos + 1);
			if (mPlayPos < 0) {
				mPlayPos = 0;
				stop();
				openCurrentAndNext();
				play();
			}
		} else if (action == MOVE_LAST) {
			addToPlayList(list, Integer.MAX_VALUE);
		}
	}

	/**
	 * releases playback service and removes notification/playback controls
	 *
	 * @return true if service remains unchanged
	 */
	synchronized boolean releaseServiceUiAndStop() {
		if (!isPlaying() && !mPausedByTransientLossOfFocus) {
			ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
			mNotificationHelper.dismissNotification();
			if (!mServiceInUse) {
				saveQueue(true);
				stopSelf(mServiceStartId);
				return false;
			}
		}
		return true;
	}

	/**
	 * update current track metadata of the media session (used to update player control notification)
	 */
	private void updateMetadata() {
		MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
		Song song = currentSong;
		Album album = currentAlbum;
		if (song != null) {
			builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getName());
			builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist());
			builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum());
			builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());
		}
		if (album != null) {
			builder.putString(MediaMetadataCompat.METADATA_KEY_DATE, album.getRelease());
			builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, album.getTrackCount());
		}
		mSession.setMetadata(builder.build());
	}

	/**
	 * update playback state of the media session (used to update player control notification)
	 */
	private void updatePlaybackstate() {
		PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setState(mPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, getPosition(), 1.0f);
		builder.setActions(PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY |
				PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				PlaybackStateCompat.ACTION_PLAY_FROM_URI | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE | PlaybackStateCompat.ACTION_SET_REPEAT_MODE);
		mSession.setPlaybackState(builder.build());
	}

	/**
	 *
	 */
	private void getCardId() {
		try {
			Cursor mCursor = CursorFactory.makeCardCursor(this);
			if (mCursor != null) {
				if (mCursor.moveToFirst()) {
					mCardId = mCursor.getInt(0);
				}
				mCursor.close();
			}
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds a music ID list to the current playlist
	 *
	 * @param list     The list to add
	 * @param position The position to place the tracks
	 */
	private void addToPlayList(long[] list, int position) {
		if (position < 0) {
			position = 0;
		} else if (position > mPlayList.size()) {
			position = mPlayList.size();
		}
		for (long trackId : list) {
			mPlayList.add(position++, trackId);
		}
		if (mPlayPos == -1) {
			mPlayPos = 0;
			openCurrentAndNext();
		}
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * update current track information
	 */
	private void updateTrackInformation() {
		if (mPlayPos >= 0 && mPlayPos < mPlayList.size()) {
			long trackId = mPlayList.get(mPlayPos);
			clearCurrentTrackInformation();
			Cursor cursor = CursorFactory.makeTrackCursor(this, trackId);
			updateTrackInformation(cursor);
		}
	}

	/**
	 * update track & album cursor uring Uri
	 *
	 * @param uri uri of the audio track
	 */
	private void updateTrackInformation(Uri uri) {
		clearCurrentTrackInformation();
		Cursor cursor = null;
		// get information from MediaStore directly
		if (uri.toString().startsWith(Media.EXTERNAL_CONTENT_URI.toString())) {
			cursor = CursorFactory.makeTrackCursor(this, uri, true);
		}
		// use audio ID to get information
		else if (uri.getLastPathSegment() != null && uri.getLastPathSegment().matches("audio:\\d{1,18}")) {
			long id = Long.parseLong(uri.getLastPathSegment().substring(6));
			cursor = CursorFactory.makeTrackCursor(this, id);
		}
		// use file path to get information
		else if (uri.getScheme() != null && uri.getScheme().startsWith("file")) {
			cursor = CursorFactory.makeTrackCursor(this, uri.getPath());
		}
		// use absolute path
		else if (uri.getPath() != null && uri.getPath().startsWith("/root/")) {
			String path = uri.getPath().substring(5);
			cursor = CursorFactory.makeTrackCursor(this, path);
		}
		// fallback, use file name/relative path to get information
		else {
			// get file information
			Cursor fileCursor = CursorFactory.makeTrackCursor(this, uri, false);
			// search for file in the MediaStore
			if (fileCursor != null) {
				if (fileCursor.moveToFirst()) {
					// find track by file path
					int idxName = fileCursor.getColumnIndex(FileColumns.DOCUMENT_ID);
					// if not found, find track by file name (less precise)
					if (idxName < 0)
						idxName = fileCursor.getColumnIndex(FileColumns.DISPLAY_NAME);
					// if found, get track information
					if (idxName >= 0) {
						String name = fileCursor.getString(idxName);
						if (name != null) {
							int cut = name.indexOf(":");
							if (cut > 0 && cut < name.length() + 1) {
								name = name.substring(cut + 1);
							}
							// set track information
							cursor = CursorFactory.makeTrackCursor(this, name);
						}
					}
					fileCursor.close();
				}
			}
		}
		if (cursor != null) {
			updateTrackInformation(cursor);
		} else {
			clearCurrentTrackInformation();
			Log.e(TAG, "failed to open track!");
		}
	}

	/**
	 * read track information from cursor then close
	 *
	 * @param cursor cursor with track information
	 */
	private void updateTrackInformation(@Nullable Cursor cursor) {
		Song song = null;
		Album album = null;
		try {
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					long songId = cursor.getLong(0);
					String songName = cursor.getString(1);
					String artistName = cursor.getString(2);
					String albumName = cursor.getString(3);
					long length = cursor.getLong(4);
					long artistId = cursor.getLong(5);
					long albumId = cursor.getLong(6);
					String path = cursor.getString(7);
					song = new Song(songId, artistId, albumId, songName, artistName, albumName, length, path);
				}
				cursor.close();
			}
			if (song != null) {
				cursor = CursorFactory.makeAlbumCursor(this, song.getAlbumId());
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						long id = cursor.getLong(cursor.getColumnIndexOrThrow(Media._ID));
						String name = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM));
						String artist = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));
						int count = cursor.getInt(cursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS));
						String year = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.FIRST_YEAR));
						album = new Album(id, name, artist, count, year, true);
					}
					cursor.close();
				}
			}
			currentAlbum = album;
			currentSong = song;
			notifyChange(CHANGED_META);
		} catch (Exception exception) {
			Log.e(TAG, "failed to set track information");
		}
	}


	/**
	 *
	 */
	private void clearCurrentTrackInformation() {
		currentAlbum = null;
		currentSong = null;
	}

	/**
	 * Called to open a new file as the current track and prepare the next for
	 * playback
	 */
	private void openCurrentAndNext() {
		if (openCurrentTrack()) {
			setNextTrack(false);
		}
	}

	/**
	 * prepare current track of the queue for playback and update track information
	 * if an error occurs try the next tracks
	 */
	private boolean openCurrentTrack() {
		if (mPlayList.isEmpty() || mPlayPos < 0) {
			clearCurrentTrackInformation();
			return false;
		}
		if (mPlayer.isPlaying())
			stop();
		updateTrackInformation();
		boolean fileOpened = false;
		Song song = currentSong;
		long id = song != null ? song.getId() : 0;
		if (id != -1L) {
			fileOpened = openTrack(id);
		}
		// check if file was opened successfully
		if (!fileOpened) {
			if (mPlayList.size() > 1) {
				// trying to play one of the next 10 tracks, give up if no success
				for (int i = 0; i < 10 && !fileOpened && mPlayPos >= 0; i++) {
					mPlayPos = incrementPosition(mPlayPos, false);
					// check if the end of the queue is reached
					if (mPlayPos < 0) {
						stop();
					}
					// skip faulty track and try open next track
					else {
						updateTrackInformation();
						song = currentSong;
						id = song != null ? song.getId() : 0;
						if (id != -1L && openTrack(id)) {
							fileOpened = true;
						}
					}
				}
			}
			if (!fileOpened) {
				Log.w(TAG, "Failed to open file for playback");
				// give up and prepare shutdown
				stop();
			}
		}
		return fileOpened;
	}

	/**
	 * Sets the track track to be played
	 */
	private void setNextTrack(boolean force) {
		int nextPos = mPlayPos;
		for (int i = 0; i < 10; i++) {
			nextPos = incrementPosition(nextPos, force);
			if (nextPos >= 0 && nextPos < mPlayList.size()) {
				long id = mPlayList.get(nextPos);
				Uri uri = Uri.parse(Media.EXTERNAL_CONTENT_URI + "/" + id);
				if (mPlayer.setNextDataSource(getApplicationContext(), uri)) {
					mNextPlayPos = nextPos;
					break;
				}
			} else {
				mPlayer.setNextDataSource(getApplicationContext(), null);
				mNextPlayPos = -1;
				break;
			}
		}
	}

	/**
	 * increment current play position of the queue
	 *
	 * @param force True to force the player onto the track next, false otherwise.
	 * @return The next position to play.
	 */
	private int incrementPosition(int pos, boolean force) {
		// return current play position
		if (!force && mRepeatMode == REPEAT_CURRENT) {
			return Math.max(pos, 0);
		}
		switch (mShuffleMode) {
			// shuffle current tracks in the queue
			case SHUFFLE_NORMAL:
				// only add current track to history when moving to another track
				if (force && pos >= 0) {
					mHistory.add(pos);
				}
				// clear old history entries when exceeding maximum capacity
				if (mHistory.size() > MAX_HISTORY_SIZE) {
					mHistory.removeFirst();
				}
				// reset shuffle list after reaching the end or refreshing
				if (mShuffleList.size() != mPlayList.size() || mShufflePos < 0 || mShufflePos >= mShuffleList.size()) {
					// create a new shuffle list. if fail, prevent playing
					mShufflePos = 0;
					if (!makeShuffleList(false)) {
						return -1;
					}
				}
				// get index of the new track
				return mShuffleList.get(mShufflePos++);

			// Party shuffle
			case SHUFFLE_AUTO:
				makeShuffleList(true);
				return pos + 1;

			default:
				if (pos >= mPlayList.size() - 1) {
					if (mRepeatMode == REPEAT_NONE && !force) {
						return -1;
					}
					if (mRepeatMode == REPEAT_ALL || force) {
						return 0;
					}
					return -1;
				} else {
					return pos + 1;
				}
		}
	}

	/**
	 * decrement current play position in the queue
	 *
	 * @param pos position to decrement
	 * @return play position
	 */
	private int decrementPosition(int pos) {
		if (mShuffleMode == SHUFFLE_NORMAL) {
			// Go to previously-played track and remove it from the history
			if (mHistory.isEmpty()) {
				return -1;
			} else {
				return mHistory.removeLast();
			}
		} else {
			if (pos > 0) {
				return pos - 1;
			} else {
				return mPlayList.size() - 1;
			}
		}
	}

	/**
	 * Creates a shuffled playlist used for party mode
	 *
	 * @param partyShuffle true to create a party shuffle list with all available tracks
	 */
	private boolean makeShuffleList(boolean partyShuffle) {
		try {
			if (partyShuffle) {
				Cursor cursor = CursorFactory.makeTrackCursor(this);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						mPlayList.clear();
						do {
							long id = cursor.getLong(0);
							mPlayList.add(id);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			}
			if (!mPlayList.isEmpty()) {
				mShuffleList.clear();
				mShuffleList.ensureCapacity(mPlayList.size());
				for (int index = 0; index < mPlayList.size(); index++) {
					mShuffleList.add(index);
				}
				Collections.shuffle(mShuffleList, mRandom);
				// move played tracks at the end
				if (!mHistory.isEmpty()) {
					for (int i = 0; i < mShuffleList.size(); i++) {
						if (mHistory.contains(mShuffleList.get(i))) {
							int index = mShuffleList.remove(i);
							mShuffleList.add(index);
						}
					}
				}
				return true;
			} else {
				clearShuffleList();
			}
		} catch (RuntimeException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * reset shuffle list
	 */
	private void clearShuffleList() {
		mShuffleList.clear();
		mShufflePos = -1;
		mShuffleMode = SHUFFLE_NONE;
	}

	/**
	 * open media file using its content ID and prepare for playback
	 *
	 * @param id content ID
	 * @return true if playback is initialized successfully
	 */
	private boolean openTrack(long id) {
		Uri uri = Uri.parse(Media.EXTERNAL_CONTENT_URI + "/" + id);
		if (mPlayer.setDataSource(getApplicationContext(), uri)) {
			return true;
		} else {
			stop();
			return false;
		}
	}

	/**
	 * Saves the queue
	 *
	 * @param full True if the queue is full
	 */
	private void saveQueue(boolean full) {
		if (mQueueIsSaveable) {
			if (full) {
				settings.setPlayList(mPlayList, mCardId);
				if (mShuffleMode != SHUFFLE_NONE) {
					settings.setTrackHistory(mHistory);
				}
			}
			settings.setCursorPosition(mPlayPos);
			if (mPlayer.initialized()) {
				settings.setSeekPosition(mPlayer.getPosition());
			}
			settings.setRepeatAndShuffleMode(mRepeatMode, mShuffleMode);
		}
	}

	/**
	 * Reloads the queue as the user left it the last time they stopped using
	 * Apollo
	 */
	private void reloadQueue() {
		int id = settings.getCardId();
		if (id == mCardId) {
			mPlayList.clear();
			mPlayList.addAll(settings.getPlaylist());
		}
		if (!mPlayList.isEmpty()) {
			int pos = settings.getCursorPosition();
			if (pos >= 0 && pos < mPlayList.size()) {
				mPlayPos = pos;
			}
			synchronized (this) {
				clearCurrentTrackInformation();
				openCurrentAndNext();
			}
			if (mPlayer.initialized()) {
				long seekpos = settings.getSeekPosition();
				seekTo(seekpos >= 0 && seekpos < mPlayer.getDuration() ? seekpos : 0);
			}
			//
			mRepeatMode = settings.getRepeatMode();
			mShuffleMode = settings.getShuffleMode();
			if (mShuffleMode != SHUFFLE_NONE) {
				mHistory.clear();
				mHistory.addAll(settings.getTrackHistory());
			}
			if (mShuffleMode == SHUFFLE_AUTO) {
				if (!makeShuffleList(true)) {
					mShuffleMode = SHUFFLE_NONE;
				}
			}
		}
		notifyChange(CHANGED_QUEUE);
	}
}