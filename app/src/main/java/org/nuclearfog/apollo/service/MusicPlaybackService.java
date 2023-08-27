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

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.NotificationHelper;
import org.nuclearfog.apollo.cache.ImageCache;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.player.MultiPlayer;
import org.nuclearfog.apollo.player.MusicPlayerHandler;
import org.nuclearfog.apollo.provider.FavoritesStore;
import org.nuclearfog.apollo.provider.PopularStore;
import org.nuclearfog.apollo.provider.RecentStore;
import org.nuclearfog.apollo.receiver.UnmountBroadcastReceiver;
import org.nuclearfog.apollo.receiver.WidgetBroadcastReceiver;
import org.nuclearfog.apollo.utils.CursorFactory;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * A background {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
public class MusicPlaybackService extends Service implements OnAudioFocusChangeListener {
	/**
	 *
	 */
	private static final String TAG = "MusicPlaybackService";
	/**
	 * For backwards compatibility reasons, also provide sticky
	 * broadcasts under the music package
	 */
	public static final String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
	/**
	 *
	 */
	public static final String MUSIC_PACKAGE_NAME = "com.android.music";
	/**
	 *
	 */
	private static final String HANDLER_NAME = "MusicPlayerHandler";
	/**
	 * Notification channel ID
	 */
	public static final String NOTIFICAITON_CHANNEL_ID = APOLLO_PACKAGE_NAME + ".controlpanel";
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
	public static final String CHANGED_POSITION = APOLLO_PACKAGE_NAME + ".positionchanged";
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
	 * Called to update the service about the foreground state of Apollo's activities
	 */
	public static final String CHANGED_FOREGROUND_STATE = APOLLO_PACKAGE_NAME + ".fgstatechanged";
	/**
	 * Called to go toggle between pausing and playing the music
	 */
	public static final String ACTION_TOGGLEPAUSE = APOLLO_PACKAGE_NAME + ".togglepause";
	/**
	 * Called to go to pause the playback
	 */
	public static final String ACTION_PAUSE = APOLLO_PACKAGE_NAME + ".pause";
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
	 * Used by the alarm intent to shutdown the service after being idle
	 */
	private static final String ACTION_SHUTDOWN = APOLLO_PACKAGE_NAME + ".shutdown";
	/**
	 *
	 */
	public static final String CMDNAME = "command";
	/**
	 *
	 */
	public static final String CMDTOGGLEPAUSE = "togglepause";
	/**
	 *
	 */
	public static final String CMDSTOP = "stop";
	/**
	 *
	 */
	public static final String CMDPAUSE = "pause";
	/**
	 *
	 */
	public static final String CMDPLAY = "play";
	/**
	 *
	 */
	public static final String CMDPREVIOUS = "previous";
	/**
	 *
	 */
	public static final String CMDNEXT = "next";
	/**
	 * Moves a list to the front of the queue
	 */
	public static final int MOVE_NOW = 0x34C4DD47;
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
	 * Indicates when the track ends
	 */
	public static final int MESSAGE_TRACK_ENDED = 0xF7E68B1A;
	/**
	 * Indicates that the current track was changed the next track
	 */
	public static final int MESSAGE_TRACK_WENT_TO_NEXT = 0xB4C13964;
	/**
	 * Indicates the player died
	 */
	public static final int MESSAGE_SERVER_DIED = 0xA2F4FFEE;
	/**
	 * Indicates some sort of focus change, maybe a phone call
	 */
	public static final int MESSAGE_FOCUS_CHANGE = 0xDB9F6A3B;
	/**
	 * Indicates to fade the volume down
	 */
	public static final int MESSAGE_FADEDOWN = 0x9745AB2B;
	/**
	 * Indicates to fade the volume back up
	 */
	public static final int MESSAGE_FADEUP = 0x2A72CF59;
	/**
	 * Idle time before stopping the foreground notfication (1 minute)
	 */
	private static final int IDLE_DELAY = 60000;
	/**
	 * Song play time used as threshold for rewinding to the beginning of the
	 * track instead of skipping to the previous track when getting the PREVIOUS
	 * command
	 */
	private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;
	/**
	 * The max size allowed for the track history
	 */
	public static final int MAX_HISTORY_SIZE = 100;
	/**
	 * Keeps a mapping of the track history
	 */
	private LinkedList<Integer> mHistory = new LinkedList<>();
	/**
	 * current playlist containing track ID's
	 */
	private LinkedList<Long> mPlayList = new LinkedList<>();
	/**
	 * the values of this list points on indexes of {@link #mPlayList} which are randomly shuffled.
	 * after finishing this list, the values will be shuffled again.
	 */
	private ArrayList<Integer> mNormalShuffleList = new ArrayList<>();
	/**
	 * current shuffle list contaning track ID's
	 */
	private ArrayList<Long> mAutoShuffleList = new ArrayList<>();

	private LinkedList<Integer> mHistoryOfNumbers = new LinkedList<>();

	private Set<Integer> mPreviousNumbers = new TreeSet<>();
	/**
	 * random generator used for shuffle
	 */
	private Random mRandom = new Random();
	/**
	 * Service stub
	 */
	private IBinder mBinder = new ServiceStub(this);
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
	 * The media player
	 */
	private MultiPlayer mPlayer;
	/**
	 * MediaSession to init media button support
	 */
	private MediaSessionCompat mSession;
	/**
	 *
	 */
	private MusicPlayerHandler mPlayerHandler;
	/**
	 * Image cache
	 */
	private ImageFetcher mImageFetcher;
	/**
	 * Used to build the notification
	 */
	private NotificationHelper mNotificationHelper;
	/**
	 * Recently listened database
	 */
	private RecentStore mRecentsCache;
	/**
	 * Favorites database
	 */
	private FavoritesStore mFavoritesCache;
	/**
	 * most played tracks database
	 */
	private PopularStore mPopularCache;
	/**
	 * Alarm intent for removing the notification when nothing is playing
	 * for some time
	 */
	private AlarmManager mAlarmManager;
	private PendingIntent mShutdownIntent;
	private boolean mShutdownScheduled;
	/**
	 * The cursor used to retrieve info on the current track and run the
	 * necessary queries to play audio files
	 */
	@Nullable
	private Cursor mCursor;
	/**
	 * The cursor used to retrieve info on the album the current track is
	 * part of, if any.
	 */
	@Nullable
	private Cursor mAlbumCursor;
	/**
	 * used to distinguish between different cards when saving/restoring playlists
	 */
	private int mCardId;
	/**
	 * Used to know when the service is active
	 */
	private boolean mServiceInUse = false;
	/**
	 * Used to know if something should be playing or not
	 */
	private boolean mIsSupposedToBePlaying = false;
	/**
	 * Used to indicate if the queue can be saved
	 */
	private boolean mQueueIsSaveable = true;
	/**
	 * Used to track what type of audio focus loss caused the playback to pause
	 */
	private boolean mPausedByTransientLossOfFocus = false;
	/**
	 * used to chekc if application is running in the foreground
	 */
	private boolean isForeground = true;

	private int mServiceStartId = -1;
	private int mShuffleMode = SHUFFLE_NONE;
	private int mRepeatMode = REPEAT_ALL;
	private int mShuffleIndex = -1;
	private int mPlayPos = -1;
	private int mNextPlayPos = -1;
	private int mMediaMountedCount = 0;
	private int mPrevious = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(Intent intent) {
		cancelShutdown();
		mServiceInUse = true;
		return mBinder;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		mServiceInUse = false;
		saveQueue(true);

		if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
			// Something is currently playing, or will be playing once
			// an in-progress action requesting audio focus ends, so don't stop
			// the service now.
			return true;

			// If there is a playlist but playback is paused, then wait a while
			// before stopping the service, so that pause/resume isn't slow.
			// Also delay stopping the service if we're transitioning between
			// tracks.
		} else if (!mPlayList.isEmpty() || mPlayerHandler.hasMessages(MESSAGE_TRACK_ENDED)) {
			scheduleDelayedShutdown();
			return true;
		}
		stopSelf(mServiceStartId);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRebind(Intent intent) {
		cancelShutdown();
		mServiceInUse = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Initialize the favorites and recents databases
		mRecentsCache = RecentStore.getInstance(this);
		mFavoritesCache = FavoritesStore.getInstance(this);
		mPopularCache = PopularStore.getInstance(this);
		// Initialize the notification helper
		mNotificationHelper = new NotificationHelper(this);
		// Initialize the image fetcher
		mImageFetcher = ImageFetcher.getInstance(this);
		// Initialize the image cache
		mImageFetcher.setImageCache(ImageCache.getInstance(this));
		// initialize broadcast receiver
		mIntentReceiver = new WidgetBroadcastReceiver(this);
		mUnmountReceiver = new UnmountBroadcastReceiver(this);

		mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt the UI.
		HandlerThread thread = new HandlerThread(HANDLER_NAME, THREAD_PRIORITY_BACKGROUND);
		thread.start();

		PlaybackStateCompat state = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY
				| PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE
				| PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build();

		// Initialize the handler
		mPlayerHandler = new MusicPlayerHandler(this, thread.getLooper());
		mSession = new MediaSessionCompat(getApplicationContext(), TAG);
		mSession.setCallback(new MediaButtonCallback(this), mPlayerHandler);
		mSession.setPlaybackState(state);
		mSession.setActive(true);

		// Initialize the preferences
		settings = PreferenceUtils.getInstance(this);
		getCardId();

		// register external storage listener
		IntentFilter filterStorage = new IntentFilter();
		filterStorage.addAction(Intent.ACTION_MEDIA_EJECT);
		filterStorage.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filterStorage.addDataScheme("file");
		registerReceiver(mUnmountReceiver, filterStorage);

		// Initialize the intent filter and each action
		IntentFilter filterAction = new IntentFilter();
		filterAction.addAction(SERVICECMD);
		filterAction.addAction(ACTION_TOGGLEPAUSE);
		filterAction.addAction(ACTION_PAUSE);
		filterAction.addAction(ACTION_STOP);
		filterAction.addAction(ACTION_NEXT);
		filterAction.addAction(ACTION_PREVIOUS);
		filterAction.addAction(ACTION_REPEAT);
		filterAction.addAction(ACTION_SHUFFLE);
		registerReceiver(mIntentReceiver, filterAction);

		// Initialize the delayed shutdown intent
		Intent shutdownIntent = new Intent(this, MusicPlaybackService.class);
		shutdownIntent.setAction(ACTION_SHUTDOWN);

		// Initialize the media player
		mPlayer = new MultiPlayer(this);
		mPlayer.setHandler(mPlayerHandler);

		mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Listen for the idle state
		scheduleDelayedShutdown();

		// Bring the queue back
		reloadQueue();
		notifyChange(CHANGED_QUEUE);
		notifyChange(CHANGED_META);
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
		// remove any pending alarms
		mAlarmManager.cancel(mShutdownIntent);
		// Release the player
		mPlayer.release();
		// release player callbacks
		mSession.release();
		// Remove any callbacks from the handler
		mPlayerHandler.removeCallbacksAndMessages(null);
		// Close the cursor
		closeCursor();
		// Unregister the mount listener
		unregisterReceiver(mIntentReceiver);
		if (mUnmountReceiver != null) {
			unregisterReceiver(mUnmountReceiver);
		}
		mNotificationHelper.cancelNotification();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		mPlayerHandler.obtainMessage(MESSAGE_FOCUS_CHANGE, focusChange, 0).sendToTarget();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mServiceStartId = startId;
		if (intent != null) {
			String action = intent.getAction();
			if (intent.hasExtra(EXTRA_FOREGROUND)) {
				isForeground = intent.getBooleanExtra(EXTRA_FOREGROUND, false);
				if (isForeground) {
					stopForeground(true);
					mNotificationHelper.cancelNotification();
				} else if (isPlaying()) {
					mNotificationHelper.createNotification();
				}
			}
			if (ACTION_SHUTDOWN.equals(action)) {
				mShutdownScheduled = false;
				releaseServiceUiAndStop();
				return START_NOT_STICKY;
			}
			handleCommandIntent(intent);
		}
		// Make sure the service will shut down on its own if it was
		// just started but not bound to and nothing is playing
		scheduleDelayedShutdown();
		return START_STICKY;
	}

	/**
	 * used by widgets or other intents to
	 */
	public void handleCommandIntent(Intent intent) {
		String action = intent.getAction();
		String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;
		// next track
		if (CMDNEXT.equals(command) || ACTION_NEXT.equals(action)) {
			gotoNext(true);
		}
		// previous track
		else if (CMDPREVIOUS.equals(command) || ACTION_PREVIOUS.equals(action)) {
			goToPrev();
		}
		// pause/play track
		else if (CMDTOGGLEPAUSE.equals(command) || ACTION_TOGGLEPAUSE.equals(action)) {
			if (isPlaying()) {
				pause();
				mPausedByTransientLossOfFocus = false;
			} else {
				play();
			}
		}
		// pause track
		else if (CMDPAUSE.equals(command) || ACTION_PAUSE.equals(action)) {
			pause();
			mPausedByTransientLossOfFocus = false;
		}
		// play track
		else if (CMDPLAY.equals(command)) {
			play();
		}
		// stop track/dismiss notification
		else if (CMDSTOP.equals(command) || ACTION_STOP.equals(action)) {
			pause();
			mPausedByTransientLossOfFocus = false;
			seek(0);
			releaseServiceUiAndStop();
			mNotificationHelper.cancelNotification();
			if (!isForeground) {
				mNotificationHelper.cancelNotification();
			}
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
	}

	/**
	 * @return The album art for the current album.
	 */
	public Bitmap getAlbumArt() {
		// Return the cached artwork
		return mImageFetcher.getArtwork(getAlbumName(), getAlbumId(), getArtistName());
	}

	/**
	 * @return True if music is playing, false otherwise
	 */
	public boolean isPlaying() {
		return mIsSupposedToBePlaying;
	}

	/**
	 *
	 */
	public void onEject() {
		saveQueue(true);
		mQueueIsSaveable = false;
		stop(true);
		notifyChange(CHANGED_QUEUE);
		notifyChange(CHANGED_META);
	}

	/**
	 * callback used to notify if an external storage was unmounted
	 */
	public void onUnmount() {
		mMediaMountedCount++;
		getCardId();
		reloadQueue();
		mQueueIsSaveable = true;
		notifyChange(CHANGED_QUEUE);
		notifyChange(CHANGED_META);
	}

	/**
	 * Returns the shuffle mode
	 *
	 * @return The current shuffle mode (all, party, none)
	 */
	public int getShuffleMode() {
		return mShuffleMode;
	}

	/**
	 * Returns the repeat mode
	 *
	 * @return The current repeat mode (all, one, none)
	 */
	public int getRepeatMode() {
		return mRepeatMode;
	}

	/**
	 * Returns the album name
	 *
	 * @return The current song album Name
	 */
	public synchronized String getAlbumName() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.ALBUM);
			if (idx >= 0) {
				return mCursor.getString(idx);
			}
		}
		return "";
	}

	/**
	 * Returns the song name
	 *
	 * @return The current song name
	 */
	public synchronized String getTrackName() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.TITLE);
			if (idx >= 0) {
				return mCursor.getString(idx);
			}
		}
		return "";
	}

	/**
	 * Returns the artist name
	 *
	 * @return The current song artist name
	 */
	public synchronized String getArtistName() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.ARTIST);
			if (idx >= 0) {
				return mCursor.getString(idx);
			}
		}
		return "";
	}

	/**
	 * Returns the album ID
	 *
	 * @return The current song album ID
	 */
	public synchronized long getAlbumId() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(Media.ALBUM_ID);
			if (idx >= 0) {
				return mCursor.getLong(idx);
			}
		}
		return -1L;
	}

	/**
	 * Returns the album ID
	 *
	 * @return The current song album ID
	 */
	public synchronized long getTrackId() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(Media._ID);
			if (idx >= 0) {
				return mCursor.getLong(idx);
			}
		}
		return -1L;
	}

	/**
	 * Stops playback.
	 */
	public void stop() {
		stop(true);
	}

	/**
	 * Resumes or starts playback.
	 */
	public void play() {
		if (mAudio != null) {
			int returnCode = mAudio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			if (returnCode == AudioManager.AUDIOFOCUS_GAIN) {
				if (mPlayer.isInitialized()) {
					long duration = mPlayer.duration();
					if (mRepeatMode != REPEAT_CURRENT && duration > 2000 && mPlayer.position() >= duration - 2000) {
						gotoNext(true);
					}
					mPlayer.start();
					mPlayerHandler.removeMessages(MESSAGE_FADEDOWN);
					mPlayerHandler.sendEmptyMessage(MESSAGE_FADEUP);
					if (!mIsSupposedToBePlaying) {
						mIsSupposedToBePlaying = true;
						notifyChange(CHANGED_PLAYSTATE);
					}
					cancelShutdown();
				} else if (mPlayList.isEmpty()) {
					setShuffleMode(SHUFFLE_AUTO);
				}
			}
			if (!isForeground) {
				mNotificationHelper.updateNotification();
			}
		}
	}

	/**
	 * Temporarily pauses playback.
	 */
	public void pause() {
		mPlayerHandler.removeMessages(MESSAGE_FADEUP);
		if (mIsSupposedToBePlaying) {
			mPlayer.pause();
			scheduleDelayedShutdown();
			mIsSupposedToBePlaying = false;
			notifyChange(CHANGED_PLAYSTATE);
		}
		if (!isForeground) {
			mNotificationHelper.updateNotification();
		}
	}

	/**
	 * Changes from the current track to the next track
	 */
	public void gotoNext(boolean force) {
		if (mPlayList.isEmpty()) {
			scheduleDelayedShutdown();
		} else {
			int pos = getNextPosition(force);
			if (pos < 0) {
				scheduleDelayedShutdown();
				if (mIsSupposedToBePlaying) {
					mIsSupposedToBePlaying = false;
					notifyChange(CHANGED_PLAYSTATE);
				}
			} else {
				stop(false);
				mPlayPos = pos;
				openCurrentAndNext();
				play();
				notifyChange(CHANGED_META);
			}
		}
	}

	/**
	 * restart current track or go to preview track
	 */
	public void goToPrev() {
		if (position() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
			prev();
		} else {
			seek(0);
			play();
		}
	}

	/**
	 * Seeks the current track to a specific time
	 *
	 * @param position The time to seek to
	 * @return The time to play the track at
	 */
	public long seek(long position) {
		if (mPlayer.isInitialized()) {
			if (position < 0) {
				position = 0;
			} else if (position > mPlayer.duration()) {
				position = mPlayer.duration();
			}
			mPlayer.seek(position);
			notifyChange(CHANGED_POSITION);
			return position;
		}
		return -1;
	}

	/**
	 * open file uri
	 *
	 * @param uri URI of the local file
	 */
	public void openFile(Uri uri) {
		stop();
		// If this is a file:// URI, just use the path directly instead
		// of going through the open-from-filedescriptor codepath.
		if (uri.getScheme() != null && uri.getScheme().startsWith("file://")) {
			openTrack(uri.getPath());
		} else if (uri.toString().startsWith(Media.EXTERNAL_CONTENT_URI.toString())) {
			openTrack(uri);
		} else {
			openTrack(Uri.decode(uri.toString()));
		}
		play();
	}

	/**
	 * Called to open a new file as the current track and prepare the next for
	 * playback
	 */
	public void openCurrentAndNext() {
		openCurrentTrack();
		setNextTrack();
	}

	/**
	 * set current playback volume
	 */
	public void setVolume(float volume) {
		mPlayer.setVolume(volume);
	}

	/**
	 * notify if track chages
	 */
	public synchronized void onWentToNext() {
		mPlayPos = mNextPlayPos;
		if (mCursor != null) {
			mCursor.close();
		}
		updateCursor(mPlayList.get(mPlayPos));
		notifyChange(CHANGED_META);
		setNextTrack();
		if (!isForeground) {
			mNotificationHelper.updateNotification();
		}
	}

	/**
	 * notify if current track ends
	 */
	public void onTrackEnded() {
		if (mRepeatMode == REPEAT_CURRENT) {
			seek(0);
			play();
		} else {
			gotoNext(false);
		}
	}

	/**
	 * notify on audio focus loss
	 *
	 * @param msg type of focus loss
	 */
	public void onAudioFocusLoss(int msg) {
		if (isPlaying()) {
			mPausedByTransientLossOfFocus = msg == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
			pause();
		}
	}

	/**
	 * notify on audio focus gain
	 *
	 * @return true if player started
	 */
	public boolean onAudioFocusGain() {
		if (!isPlaying() && mPausedByTransientLossOfFocus) {
			mPausedByTransientLossOfFocus = false;
			mPlayer.setVolume(0f);
			play();
			return true;
		}
		return false;
	}

	/**
	 * Notify the change-receivers that something has changed.
	 */
	void notifyChange(String what) {
		if (!what.equals(CHANGED_POSITION)) {
			long audioId = getAudioId();
			long albumId = getAlbumId();
			String albumName = getAlbumName();
			String artistName = getArtistName();
			String trackName = getTrackName();

			Intent intent = new Intent(what);
			intent.putExtra("id", audioId);
			intent.putExtra("artist", artistName);
			intent.putExtra("album", albumName);
			intent.putExtra("track", trackName);
			intent.putExtra("playing", isPlaying());
			intent.putExtra("isfavorite", isFavorite());
			sendBroadcast(intent);

			Intent musicIntent = new Intent(intent);
			musicIntent.setAction(what.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
			sendBroadcast(musicIntent);

			if (what.equals(CHANGED_META)) {
				// Increase the play count for favorite songs.
				if (mFavoritesCache.exists(audioId)) {
					mFavoritesCache.addSongId(audioId, trackName, albumName, artistName, getDurationMillis());
				}
				mPopularCache.addSongId(audioId, trackName, albumName, artistName, getDurationMillis());
				// Add the track to the recently played list.
				String songCount = MusicUtils.getSongCountForAlbum(this, albumId);
				String release = MusicUtils.getReleaseDateForAlbum(this, albumId);
				mRecentsCache.addAlbumId(albumId, albumName, artistName, songCount, release);
			} else if (what.equals(CHANGED_QUEUE)) {
				saveQueue(true);
				if (isPlaying()) {
					setNextTrack();
				}
			} else {
				saveQueue(false);
			}
			mIntentReceiver.updateWidgets(this, what);
		}
	}

	/**
	 * Returns the audio session ID
	 *
	 * @return The current media player audio session ID
	 */
	int getAudioSessionId() {
		return mPlayer.getAudioSessionId();
	}

	/**
	 * Indicates if the media storeage device has been mounted or not
	 *
	 * @return 1 if Intent.ACTION_MEDIA_MOUNTED is called, 0 otherwise
	 */
	int getMediaMountedCount() {
		return mMediaMountedCount;
	}

	/**
	 * Removes the range of tracks specified from the play list. If a file
	 * within the range is the file currently being played, playback will move
	 * to the next file after the range.
	 *
	 * @param first The first file to be removed
	 * @param last  The last file to be removed
	 * @return the number of tracks deleted
	 */
	synchronized int removeTracks(int first, int last) {
		int numremoved = removeTracksInternal(first, last);
		if (numremoved > 0) {
			notifyChange(CHANGED_QUEUE);
		}
		return numremoved;
	}

	/**
	 * Sets the shuffle mode
	 *
	 * @param shufflemode The shuffle mode to use
	 */
	synchronized void setShuffleMode(int shufflemode) {
		if (mShuffleMode != shufflemode || mPlayList.isEmpty()) {
			mShuffleMode = shufflemode;
			// setup party shuffle
			if (mShuffleMode == SHUFFLE_AUTO) {
				if (makeAutoShuffleList()) {
					doAutoShuffleUpdate();
					mPlayPos = 0;
					openCurrentAndNext();
					play();
					notifyChange(CHANGED_META);
				} else {
					mShuffleMode = SHUFFLE_NONE;
				}
			}
			// setup queue shuffle
			else if (mShuffleMode == SHUFFLE_NORMAL) {
				if (makeNormalShuffleList()) {
					mPlayPos = 0;
					openCurrentAndNext();
					play();
					notifyChange(CHANGED_META);
				} else {
					mShuffleMode = SHUFFLE_NONE;
				}
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
		stop(false);
		mPlayPos = index;
		openCurrentAndNext();
		play();
		notifyChange(CHANGED_META);
		if (mShuffleMode == SHUFFLE_AUTO) {
			doAutoShuffleUpdate();
		}
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
	 * Returns the path to current song
	 *
	 * @return The path to the current song
	 */
	synchronized String getPath() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.DATA);
			if (idx >= 0) {
				return mCursor.getString(idx);
			}
		}
		return "";
	}

	/**
	 * Returns the artist ID
	 *
	 * @return The current song artist ID
	 */
	synchronized long getArtistId() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.ARTIST_ID);
			if (idx >= 0) {
				return mCursor.getLong(idx);
			}
		}
		return -1;
	}

	/**
	 * Returns the current audio ID
	 *
	 * @return The current track ID
	 */
	synchronized long getAudioId() {
		if (mPlayPos >= 0 && mPlayer.isInitialized()) {
			return mPlayList.get(mPlayPos);
		}
		return -1L;
	}

	/**
	 * Removes all instances of the track with the given ID from the playlist.
	 *
	 * @param id The id to be removed
	 * @return how many instances of the track were removed
	 */
	synchronized int removeTrack(long id) {
		int numremoved = 0;
		for (int pos = 0; pos < mPlayList.size(); pos++) {
			if (mPlayList.get(pos) == id) {
				numremoved += removeTracksInternal(pos, pos);
			}
		}
		if (numremoved > 0) {
			notifyChange(CHANGED_QUEUE);
		}
		return numremoved;
	}

	/**
	 * Returns the current position in time of the currenttrack
	 *
	 * @return The current playback position in miliseconds
	 */
	long position() {
		if (mPlayer.isInitialized()) {
			return mPlayer.position();
		}
		return -1L;
	}

	/**
	 * Returns the full duration of the current track
	 *
	 * @return The duration of the current track in miliseconds
	 */
	long duration() {
		if (mPlayer.isInitialized()) {
			return mPlayer.duration();
		}
		return -1L;
	}

	/**
	 * Returns the queue
	 *
	 * @return The queue as a long[]
	 */
	synchronized long[] getQueue() {
		int len = mPlayList.size();
		long[] list = new long[len];
		for (int i = 0; i < len; i++) {
			list[i] = mPlayList.get(i);
		}
		return list;
	}

	/**
	 * True if the current track is a "favorite", false otherwise
	 */
	synchronized boolean isFavorite() {
		if (mFavoritesCache != null) {
			return mFavoritesCache.exists(getAudioId());
		}
		return false;
	}

	/**
	 * Sets the repeat mode
	 *
	 * @param repeatmode The repeat mode to use
	 */
	synchronized void setRepeatMode(int repeatmode) {
		mRepeatMode = repeatmode;
		setNextTrack();
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
		if (mShuffleMode == SHUFFLE_AUTO) {
			mShuffleMode = SHUFFLE_NORMAL;
		}
		long oldId = getAudioId();
		mPlayPos = position >= 0 ? position : nextInt(mPlayList.size() - 1);

		boolean newlist = true;
		if (mPlayList.size() == list.length) {
			newlist = false;
			for (int i = 0; i < list.length; i++) {
				if (list[i] != mPlayList.get(i)) {
					newlist = true;
					break;
				}
			}
		}
		if (newlist) {
			mPlayList.clear();
			for (long track : list)
				mPlayList.add(track);
			notifyChange(CHANGED_QUEUE);
		}
		mHistory.clear();
		openCurrentAndNext();
		if (oldId != getAudioId()) {
			notifyChange(CHANGED_META);
		}
	}

	/**
	 * Changes from the current track to the previous played track
	 */
	synchronized void prev() {
		if (mShuffleMode == SHUFFLE_NORMAL) {
			// Go to previously-played track and remove it from the history
			int histsize = mHistory.size();
			if (histsize == 0) {
				return;
			}
			mPlayPos = mHistory.removeLast();
		} else {
			if (mPlayPos > 0) {
				mPlayPos--;
			} else {
				mPlayPos = mPlayList.size() - 1;
			}
		}
		stop(false);
		openCurrentTrack();
		play();
		notifyChange(CHANGED_META);
	}

	/**
	 * Toggles the current song as a favorite.
	 */
	synchronized void toggleFavorite() {
		if (mFavoritesCache != null) {
			long trackId = getAudioId();
			// remove track if exists from the favorites
			if (mFavoritesCache.exists(trackId)) {
				mFavoritesCache.removeItem(trackId);
			} else {
				mFavoritesCache.addSongId(getAudioId(), getTrackName(), getAlbumName(), getArtistName(), getDurationMillis());
			}
		}
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
		mNextPlayPos = getNextPosition(false);
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * Queues a new list for playback
	 *
	 * @param list   The list to queue
	 * @param action The action to take
	 */
	synchronized void enqueue(long[] list, int action) {
		if (action == MOVE_NEXT && mPlayPos + 1 < mPlayList.size()) {
			addToPlayList(list, mPlayPos + 1);
			notifyChange(CHANGED_QUEUE);
		} else {
			addToPlayList(list, Integer.MAX_VALUE);
			notifyChange(CHANGED_QUEUE);
			if (action == MOVE_NOW) {
				mPlayPos = mPlayList.size() - list.length;
				openCurrentAndNext();
				play();
				notifyChange(CHANGED_META);
				return;
			}
		}
		if (mPlayPos < 0) {
			mPlayPos = 0;
			openCurrentAndNext();
			play();
			notifyChange(CHANGED_META);
		}
	}

	/**
	 *
	 */
	private void getCardId() {
		try {
			Cursor cursor = CursorFactory.makeCardCursor(this);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					mCardId = cursor.getInt(0);
				}
				cursor.close();
			}
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 */
	private void scheduleDelayedShutdown() {
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
		mShutdownScheduled = true;
	}

	/**
	 *
	 */
	private void cancelShutdown() {
		if (mShutdownScheduled) {
			mAlarmManager.cancel(mShutdownIntent);
			mShutdownScheduled = false;
		}
	}

	/**
	 * Stops playback
	 *
	 * @param goToIdle True to go to the idle state, false otherwise
	 */
	private void stop(boolean goToIdle) {
		if (mPlayer.isInitialized()) {
			mPlayer.stop();
		}
		closeCursor();
		if (goToIdle) {
			scheduleDelayedShutdown();
			mIsSupposedToBePlaying = false;
		} else {
			stopForeground(false);
		}
	}

	/**
	 * Removes the range of tracks specified from the play list. If a file
	 * within the range is the file currently being played, playback will move
	 * to the next file after the range.
	 *
	 * @param first The first file to be removed
	 * @param last  The last file to be removed
	 * @return the number of tracks deleted
	 */
	private int removeTracksInternal(int first, int last) {
		if (last < first) {
			return 0;
		} else if (first < 0) {
			first = 0;
		} else if (last >= mPlayList.size()) {
			last = mPlayList.size() - 1;
		}
		boolean gotonext = false;
		if (first <= mPlayPos && mPlayPos <= last) {
			mPlayPos = first;
			gotonext = true;
		} else if (mPlayPos > last) {
			mPlayPos -= last - first + 1;
		}
		// remove a range of tracks from playlist
		mPlayList.subList(first, last + 1).clear();
		if (gotonext) {
			if (mPlayList.isEmpty()) {
				stop(true);
				mPlayPos = -1;
				closeCursor();
			} else {
				if (mShuffleMode != SHUFFLE_NONE) {
					mPlayPos = getNextPosition(true);
				} else if (mPlayPos >= mPlayList.size()) {
					mPlayPos = 0;
				}
				boolean wasPlaying = isPlaying();
				stop(false);
				openCurrentAndNext();
				if (wasPlaying) {
					play();
				}
			}
			notifyChange(CHANGED_META);
		}
		return last - first + 1;
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
		}
		if (position > mPlayList.size()) {
			position = mPlayList.size();
		}
		for (long l : list) {
			mPlayList.add(position++, l);
		}
		if (mPlayList.isEmpty()) {
			closeCursor();
			notifyChange(CHANGED_META);
		}
	}

	/**
	 * @param trackId The track ID
	 */
	private void updateCursor(long trackId) {
		closeCursor();
		mCursor = CursorFactory.makeTrackCursor(this, trackId);
		updateAlbumCursor();
	}

	/**
	 * update track cursor
	 *
	 * @param path music file path
	 */
	private void updateCursor(String path) {
		// fixme can't find media file
		closeCursor();
		mCursor = CursorFactory.makeTrackCursor(this, path);
		updateAlbumCursor();
	}

	/**
	 *
	 */
	private void updateCursor(Uri uri) {
		closeCursor();
		mCursor = CursorFactory.makeTrackCursor(this, uri);
		updateAlbumCursor();
	}

	/**
	 *
	 */
	private void updateAlbumCursor() {
		long albumId = getAlbumId();
		if (albumId >= 0) {
			mAlbumCursor = CursorFactory.makeAlbumCursor(this, albumId);
		} else {
			mAlbumCursor = null;
		}
	}

	/**
	 *
	 */
	private void closeCursor() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		if (mAlbumCursor != null) {
			mAlbumCursor.close();
			mAlbumCursor = null;
		}
	}

	/**
	 * Called to open a new file as the current track and prepare the next for playback
	 */
	private void openCurrentTrack() {
		closeCursor();
		if (mPlayList.isEmpty()) {
			return;
		}
		stop(false);
		updateCursor(mPlayList.get(mPlayPos));
		boolean fileOpenFailed;
		long id = getTrackId();
		if (id != -1L) {
			fileOpenFailed = !openTrack(id);
		} else {
			fileOpenFailed = true;
		}
		if (fileOpenFailed || (mCursor != null && mCursor.isClosed())) {
			// if we get here then opening the file failed. We can close the
			// cursor now, because
			// we're either going to create a new one next, or stop trying
			if (mPlayList.size() > 1) {
				for (int i = 0; i < 10; i++) { // retrying 10 times until failure
					int pos = getNextPosition(false);
					if (pos < 0) {
						scheduleDelayedShutdown();
						if (mIsSupposedToBePlaying) {
							mIsSupposedToBePlaying = false;
							notifyChange(CHANGED_PLAYSTATE);
						}
						return;
					}
					mPlayPos = pos;
					stop(false);
					mPlayPos = pos;
					updateCursor(mPlayList.get(mPlayPos));
				}
			}
			scheduleDelayedShutdown();
			if (mIsSupposedToBePlaying) {
				mIsSupposedToBePlaying = false;
				notifyChange(CHANGED_PLAYSTATE);
			}
			if (BuildConfig.DEBUG) {
				Log.w(TAG, "Failed to open file for playback");
			}
		}
	}

	/**
	 * @param force True to force the player onto the track next, false otherwise.
	 * @return The next position to play.
	 */
	private int getNextPosition(boolean force) {
		// return current play position
		if (!force && mRepeatMode == REPEAT_CURRENT) {
			return Math.max(mPlayPos, 0);
		}
		switch (mShuffleMode) {
			// shuffle current tracks in the queue
			case SHUFFLE_NORMAL:
				// only add current track to history when moving to another track
				if (force && mPlayPos >= 0) {
					mHistory.add(mPlayPos);
				}
				// clear old history entries when exceeding maximum capacity
				if (mHistory.size() > MAX_HISTORY_SIZE) {
					mHistory.removeFirst();
				}
				// reset shuffle list after reaching the end or refreshing
				if (mNormalShuffleList.size() != mPlayList.size() || mShuffleIndex < 0 || mShuffleIndex >= mNormalShuffleList.size()) {
					// create a new shuffle list. if fail, prevent playing
					mShuffleIndex = 0;
					if (!makeNormalShuffleList()) {
						return -1;
					}
				}
				// get index of the new track
				return mNormalShuffleList.get(mShuffleIndex++);

			// Party shuffle
			case SHUFFLE_AUTO:
				doAutoShuffleUpdate();
				return mPlayPos + 1;

			default:
				if (mPlayPos >= mPlayList.size() - 1) {
					if (mRepeatMode == REPEAT_NONE && !force) {
						return -1;
					}
					if (mRepeatMode == REPEAT_ALL || force) {
						return 0;
					}
					return -1;
				} else {
					return mPlayPos + 1;
				}
		}
	}

	/**
	 * Sets the track track to be played
	 */
	private void setNextTrack() {
		mNextPlayPos = getNextPosition(false);
		if (mNextPlayPos >= 0) {
			long id = mPlayList.get(mNextPlayPos);
			mPlayer.setNextDataSource(Media.EXTERNAL_CONTENT_URI + "/" + id);
		} else {
			mPlayer.resetNextPlayer();
		}
	}

	/**
	 * Creates a shuffled playlist used for party mode
	 */
	private boolean makeAutoShuffleList() {
		try {
			Cursor cursor = CursorFactory.makeTrackCursor(this);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					mAutoShuffleList.clear();
					mAutoShuffleList.ensureCapacity(cursor.getColumnCount());
					do
					{
						long id = cursor.getLong(0);
						mAutoShuffleList.add(id);
					} while (cursor.moveToNext());
					return true;
				}
				cursor.close();
			}
		} catch (RuntimeException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * create a shuffle list of the current queue
	 *
	 * @return true if success, false if there aren't any tracks
	 */
	private boolean makeNormalShuffleList() {
		if (!mPlayList.isEmpty()) {
			mNormalShuffleList.clear();
			mNormalShuffleList.ensureCapacity(mPlayList.size());
			for (int index = 0; index < mPlayList.size(); index++) {
				mNormalShuffleList.add(index);
			}
			Collections.shuffle(mNormalShuffleList, mRandom);
			return true;
		}
		return false;
	}

	/**
	 * Creates the party shuffle playlist
	 */
	private void doAutoShuffleUpdate() {
		if (mPlayPos > 10) {
			removeTracks(0, mPlayPos - 9);
			notifyChange(CHANGED_QUEUE);
		}
		int toAdd = 7 - (mPlayList.size() - (mPlayPos < 0 ? -1 : mPlayPos));
		for (int i = 0; i < toAdd; i++) {
			int lookback = mHistory.size();
			int idx;
			do {
				idx = nextInt(mAutoShuffleList.size() - 1);
				lookback /= 2;
			} while (wasRecentlyUsed(idx, lookback));

			mHistory.add(idx);
			if (mHistory.size() > MAX_HISTORY_SIZE) {
				mHistory.removeFirst();
			}
			mPlayList.add(mAutoShuffleList.get(idx));
			notifyChange(CHANGED_QUEUE);
		}
	}

	/**
	 *
	 */
	private boolean wasRecentlyUsed(int idx, int lookbacksize) {
		if (lookbacksize == 0) {
			return false;
		}
		int histsize = mHistory.size();
		if (histsize < lookbacksize) {
			lookbacksize = histsize;
		}
		int maxidx = histsize - 1;
		for (int i = 0; i < lookbacksize; i++) {
			long entry = mHistory.get(maxidx - i);
			if (entry == idx) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 */
	private void releaseServiceUiAndStop() {
		if (isPlaying() || mPausedByTransientLossOfFocus || mPlayerHandler.hasMessages(MESSAGE_TRACK_ENDED)) {
			return;
		}
		stopForeground(true);
		if (!mServiceInUse) {
			saveQueue(true);
			stopSelf(mServiceStartId);
		}
	}

	/**
	 *
	 */
	private long getDurationMillis() {
		if (mCursor != null && mCursor.moveToFirst()) {
			int idx = mCursor.getColumnIndex(AudioColumns.DURATION);
			if (idx >= 0) {
				return mCursor.getLong(idx);
			}
		}
		return -1L;
	}

	/**
	 * @param interval The duration the queue
	 * @return The position of the next track to play
	 */
	private int nextInt(int interval) {
		int next;
		do {
			next = mRandom.nextInt(interval);
		} while (next == mPrevious && interval > 1 && !mPreviousNumbers.contains(next));
		mPrevious = next;
		mHistoryOfNumbers.add(mPrevious);
		mPreviousNumbers.add(mPrevious);
		// Removes old tracks and cleans up the history preparing for new tracks
		// to be added to the mapping
		if (!mHistoryOfNumbers.isEmpty() && mHistoryOfNumbers.size() >= MusicPlaybackService.MAX_HISTORY_SIZE) {
			for (int i = 0; i < Math.max(1, MusicPlaybackService.MAX_HISTORY_SIZE / 2); i++) {
				mPreviousNumbers.remove(mHistoryOfNumbers.removeFirst());
			}
		}
		return next;
	}

	/**
	 * open media file using its content ID and prepare for playback
	 *
	 * @param id content ID
	 * @return true if playback is initialized successfully
	 */
	private boolean openTrack(long id) {
		String path = Media.EXTERNAL_CONTENT_URI + "/" + id;
		updateCursor(id);
		//mPlayList.addFirst(id);
		//mPlayPos = 0;
		mPlayer.setDataSource(path);
		if (mPlayer.isInitialized()) {
			return true;
		} else {
			stop(true);
			return false;
		}
	}

	/**
	 * Opens a file and prepares it for playback
	 *
	 * @param path The path of the file to open
	 */
	private void openTrack(String path) {
		if (path != null && !path.isEmpty()) {
			// If mCursor is null, try to associate path with a database cursor
			if (mCursor == null) {
				updateCursor(path);
				long id = getTrackId();
				if (id > 0L) {
					mPlayList.addFirst(id);
					mPlayPos = 0;
				}
			}
			mPlayer.setDataSource(path);
			if (!mPlayer.isInitialized()) {
				stop(true);
			}
		}
	}

	/**
	 * Opens a file and prepares it for playback
	 *
	 * @param uri content uri of the media file to open
	 */
	private void openTrack(Uri uri) {
		if (mCursor == null) {
			updateCursor(uri);
			long id = getTrackId();
			if (id > 0L) {
				mPlayList.addFirst(id);
				mPlayPos = 0;
			}
		}
		mPlayer.setDataSource(uri.toString());
		if (!mPlayer.isInitialized()) {
			stop(true);
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
					settings.setHistory(mHistory);
				}
			}
			settings.setCursorPosition(mPlayPos);
			if (mPlayer.isInitialized()) {
				settings.setSeekPosition(mPlayer.position());
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
			if (pos < 0 || pos >= mPlayList.size()) {
				return;
			}
			mPlayPos = pos;
			synchronized (this) {
				closeCursor();
				openCurrentAndNext();
			}
			if (!mPlayer.isInitialized()) {
				return;
			}
			long seekpos = settings.getSeekPosition();
			seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);

			int repmode = settings.getRepeatMode();
			if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
				repmode = REPEAT_NONE;
			}
			mRepeatMode = repmode;

			int shufmode = settings.getShuffleMode();
			if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
				shufmode = SHUFFLE_NONE;
			}
			if (shufmode != SHUFFLE_NONE) {
				mHistory.clear();
				mHistory.addAll(settings.getTrackHistory());
			}
			if (shufmode == SHUFFLE_AUTO) {
				if (!makeAutoShuffleList()) {
					shufmode = SHUFFLE_NONE;
				}
			}
			mShuffleMode = shufmode;
		}
	}
}