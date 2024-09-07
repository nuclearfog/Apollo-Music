package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * custom MediaPlayer implementation containing two MediaPlayer to switch fast tracks
 *
 * @author nuclearfog
 */
public class MultiPlayer {

	private static final String TAG = "MultiPlayer";
	/**
	 * indicates that there is no fade in/out in progress
	 */
	private static final int NONE = 9;
	/**
	 * indicates that the current track is fading in
	 */
	private static final int FADE_IN = 10;
	/**
	 * indicates that the current track is fading out
	 */
	private static final int FADE_OUT = 11;
	/**
	 * indicates that two track are crossfading
	 */
	private static final int XFADE = 12;
	/**
	 * sampling rate of the fade effect in 1/ms
	 */
	private static final long FADE_RESOLUTION = 40;
	/**
	 * volume steps used to fade in or out
	 */
	private static final float FADE_STEPS = 0.08f;
	/**
	 * crossfade overlay of two tracks in milliseconds
	 */
	private static final long XFADE_DELAY = 1000;
	/**
	 * number of player instances used for playback
	 *
	 * @see #mPlayers
	 */
	private static final int PLAYER_INST = 2;
	/**
	 * milliseconds to wait until to retry loading track
	 */
	private static final int ERROR_RETRY = 500;

	/**
	 * thread pool used to periodically poll the current play position for crossfading
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	private MediaMetadataRetriever retriever;

	private Handler playerHandler, xfadeHandler;

	private OnPlaybackStatusCallback callback;

	@Nullable
	private Future<?> xfadeTask;
	/**
	 * mediaplayer used to switch between tracks
	 */
	private MediaPlayer[] mPlayers = new MediaPlayer[PLAYER_INST];
	/**
	 * current mediaplayer's index of {@link #mPlayers}
	 */
	private int currentPlayer = 0;
	/**
	 * true if mediaplayer is currently playing
	 */
	private volatile boolean isPlaying = false;
	/**
	 * set to true if player was initialized successfully
	 */
	private volatile boolean initialized = false;
	/**
	 * true if player continues to next track automatically
	 */
	private volatile boolean continious = true;
	/**
	 * current fade in/out status {@link #NONE,#FADE_IN,#FADE_OUT,#XFADE}
	 */
	private volatile int xfadeMode = NONE;
	/**
	 * volume of the current selected media player
	 */
	@FloatRange(from = 0.0f, to = 1.0f)
	private float volume = 0f;

	/**
	 * @param looper   playback service looper used for crossfade/error handler
	 * @param callback a callback used to inform about playback changes
	 */
	public MultiPlayer(Looper looper, OnPlaybackStatusCallback callback) {
		playerHandler = new Handler(looper);
		xfadeHandler = new Handler(looper);
		retriever = new MediaMetadataRetriever();
		this.callback = callback;
		for (int i = 0; i < mPlayers.length; i++) {
			mPlayers[i] = new MediaPlayer();
			mPlayers[i].setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setVolume(0f, 0f);
			mPlayers[i].setOnErrorListener(this::onError);
		}
	}

	/**
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if player is ready to play
	 */
	public boolean setDataSource(Context context, @NonNull Uri uri) {
		// stop current playback
		if (initialized)
			stop();
		// set source of the current selected player
		initialized = setDataSourceImpl(mPlayers[currentPlayer], context, uri);
		return initialized;
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if next data source is initialized successfully
	 */
	public boolean setNextDataSource(Context context, @Nullable Uri uri) {
		if (uri != null) {
			int nextPlayerIndex = (currentPlayer + 1) % mPlayers.length;
			continious = setDataSourceImpl(mPlayers[nextPlayerIndex], context, uri);
			return continious;
		} else {
			continious = false;
			return true;
		}
	}

	/**
	 * @return True if the player is ready to go, false otherwise
	 */
	public boolean initialized() {
		return initialized;
	}

	/**
	 * check if there is a fade transition in progress
	 *
	 * @return true if fade in/out is in progress
	 */
	public boolean busy() {
		if (!initialized)
			return false;
		return xfadeMode != NONE;
	}

	/**
	 * Starts or resumes playback.
	 *
	 * @return true if successful, false if another operation is already pending
	 */
	public boolean play() {
		if (xfadeMode == NONE) {
			isPlaying = true;
			xfadeMode = FADE_IN;
			setCrossfadeTask(true);
			return true;
		}
		return false;
	}

	/**
	 * Pauses playback. Call start() to resume.
	 *
	 * @param force true to stop playback immediately
	 */
	public void pause(boolean force) {
		MediaPlayer player = mPlayers[currentPlayer];
		try {
			if (force) {
				setCrossfadeTask(false);
				xfadeMode = NONE;
				isPlaying = false;
				volume = 0.0f;
				if (player.isPlaying()) {
					player.pause();
				}
			} else if (xfadeMode == NONE) {
				xfadeMode = FADE_OUT;
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "failed to pause player");
			stop();
		}
	}

	/**
	 * stops playback
	 */
	public void stop() {
		try {
			mPlayers[currentPlayer].stop();
			setCrossfadeTask(false);
			xfadeMode = NONE;
			isPlaying = false;
		} catch (IllegalStateException exception) {
			Log.e(TAG, "failed to stop player");
			initialized = false;
		}
	}

	/**
	 * go to next player
	 *
	 * @return true if successful, false if another operation is already pending
	 */
	public boolean next() {
		if (continious && initialized && xfadeMode == NONE) {
			xfadeMode = XFADE;
			isPlaying = true;
			setCrossfadeTask(true);
			return true;
		}
		return false;
	}

	/**
	 * Releases mediaplayer
	 */
	public void release() {
		stop();
		threadPool.shutdown();
		for (MediaPlayer player : mPlayers) {
			try {
				player.release();
			} catch (IllegalStateException exception) {
				Log.e(TAG, "failed to release player", exception);
			}
		}
	}

	/**
	 * Gets the duration of the file.
	 *
	 * @return The duration in milliseconds
	 */
	public long getDuration() {
		try {
			return mPlayers[currentPlayer].getDuration();
		} catch (IllegalStateException exception) {
			Log.e(TAG, "invalid player duration");
			return 0;
		}
	}

	/**
	 * Gets the current playback position.
	 *
	 * @return The current position in milliseconds
	 */
	public long getPosition() {
		try {
			return mPlayers[currentPlayer].getCurrentPosition();
		} catch (IllegalStateException exception) {
			Log.e(TAG, "invalid player position");
			return 0;
		}
	}

	/**
	 * Sets the current playback position.
	 *
	 * @param position The offset in milliseconds from the start to seek to
	 */
	public void setPosition(long position) {
		try {
			// limit max position to prevent conflict with fade out
			long max = getDuration() - (XFADE_DELAY * 2);
			if (max > 0) {
				if (position > max) {
					position = max;
				} else if (position < 0) {
					position = 0;
				}
				mPlayers[currentPlayer].seekTo((int) position);
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "failed to set player position: " + position + " duration:" + getDuration());
		}
	}

	/**
	 * Returns the audio session ID.
	 *
	 * @return The current audio session ID.
	 */
	public int getAudioSessionId() {
		return mPlayers[currentPlayer].getAudioSessionId();
	}

	/**
	 * check if the current selected player is playing
	 *
	 * @return true if a playback is in progress
	 */
	public boolean isPlaying() {
		return isPlaying;
	}

	/**
	 * @param player The {@link MediaPlayer} to use
	 * @param uri    The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if initialized
	 */
	private boolean setDataSourceImpl(MediaPlayer player, Context context, @NonNull Uri uri) {
		try {
			// check file if valid
			retriever.setDataSource(context, uri);
			String hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
			if (hasAudio == null || !hasAudio.equals("yes")) {
				Log.w(TAG, "invalid media file!");
				return false;
			}
			// init player
			player.reset();
			player.setDataSource(context, uri);
			player.prepare();
			return true;
		} catch (Exception err) {
			player.reset();
			Log.e(TAG, "could not open media file!");
			return false;
		}
	}

	/**
	 * called periodically while playback to detect playback changes for crossfading
	 */
	private void onCrossfadeTrack() {
		MediaPlayer current = mPlayers[currentPlayer];
		try {
			switch (xfadeMode) {
				// force crossfade between two tracks
				case XFADE:
					volume = Math.max(volume - FADE_STEPS, 0f);
					current.setVolume(volume, volume);
					if (volume == 0f) {
						gotoNext();
					}
					break;

				// fade out current track, then pause
				case FADE_OUT:
					volume = Math.max(volume - FADE_STEPS, 0f);
					current.setVolume(volume, volume);
					if (volume == 0f) {
						pause(true);
						callback.onPlaybackEnd(false);
					}
					break;

				// play and fade in current track
				case FADE_IN:
					if (!current.isPlaying()) {
						current.setVolume(0f, 0f);
						current.start();
					} else {
						volume = Math.min(volume + FADE_STEPS, 1f);
						current.setVolume(volume, volume);
						if (volume == 1f) {
							xfadeMode = NONE;
						}
					}
					break;

				// detect end of the track then cross fade to new track if any
				default:
					long diff = Math.abs(getDuration() - getPosition());
					if (diff <= XFADE_DELAY) {
						if (continious) {
							xfadeMode = XFADE;
						} else {
							xfadeMode = FADE_OUT;
						}
					}
					break;
			}
		} catch (Exception exception) {
			Log.e(TAG, "onCrossfadeTrack", exception);
			onError(current, -1, -1);
		}
	}

	/**
	 * enable/disable periodic crossfade polling
	 *
	 * @param enable true to enable crossfading
	 */
	private void setCrossfadeTask(boolean enable) {
		// set new cross fade task
		if (enable) {
			if (xfadeTask == null) {
				xfadeTask = threadPool.scheduleWithFixedDelay(new Runnable() {
					@Override
					public void run() {
						xfadeHandler.post(new Runnable() {
							@Override
							public void run() {
								onCrossfadeTrack();
							}
						});
					}
				}, FADE_RESOLUTION, FADE_RESOLUTION, TimeUnit.MILLISECONDS);
			}
		} else if (xfadeTask != null) {
			xfadeTask.cancel(true);
			xfadeTask = null;
		}

	}

	/**
	 * close current media player and select next one. Inform playback service that track changed
	 */
	private void gotoNext() {
		stop();
		isPlaying = true;
		currentPlayer = (currentPlayer + 1) % mPlayers.length;
		if (callback.onPlaybackEnd(true)) {
			play();
		} else {
			isPlaying = false;
		}
	}

	/**
	 * called if the mediaplayer reports an error
	 *
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	private boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "onError:" + what + " ," + extra);
		if (initialized) {
			setCrossfadeTask(false);
			initialized = false;
			isPlaying = false;
			xfadeMode = NONE;
			try {
				mp.reset();
			} catch (IllegalStateException e) {
				// ignore
			}
			playerHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					callback.onPlaybackError();
				}
			}, ERROR_RETRY);
			return true;
		}
		return false;
	}

	/**
	 * callback used for playbackservice
	 */
	public interface OnPlaybackStatusCallback {

		/**
		 * called if the current playback ends
		 *
		 * @param gotoNext true if player is prepared to play next track
		 * @return true to start next track
		 */
		boolean onPlaybackEnd(boolean gotoNext);

		/**
		 * called if a playback error occurs
		 */
		void onPlaybackError();
	}
}