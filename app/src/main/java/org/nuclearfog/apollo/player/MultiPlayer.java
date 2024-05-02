package org.nuclearfog.apollo.player;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * custom MediaPlayer implementation containing two MediaPlayer to switch fast tracks
 *
 * @author nuclearfog
 */
@SuppressLint("UnsafeOptInUsageError")
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
	 * @see #mPlayers
	 */
	private static final int PLAYER_INST = 3;

	/**
	 * thread pool used to periodically poll the current play position for crossfading
	 */
	private static final ScheduledExecutorService THREAD_POOL = Executors.newScheduledThreadPool(3);

	private WeakReference<MusicPlaybackService> mService;
	private Handler playerHandler, xfadeHandler;
	private PreferenceUtils mPreferences;

	@Nullable
	private Future<?> xfadeTask;
	/**
	 * mediaplayer used to switch between tracks
	 */
	private MediaPlayer[] mPlayers = new MediaPlayer[PLAYER_INST];
	/**
	 * current mediaplayer's index of {@link #mPlayers}
	 */
	@IntRange(from = 0, to = PLAYER_INST - 1)
	private int currentPlayer = 0;
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
	@FloatRange(from=0.0f, to=1.0f)
	private float volume = 0f;

	/**
	 * @param service reference used to communicate to playbackservice
	 */
	public MultiPlayer(MusicPlaybackService service) {
		mService = new WeakReference<>(service);
		playerHandler = new Handler(service.getMainLooper());
		xfadeHandler = new Handler(service.getMainLooper());
		mPreferences = PreferenceUtils.getInstance(service);
		for (int i = 0; i < mPlayers.length; i++) {
			mPlayers[i] = createPlayer();
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setVolume(0f, 0f);
			mPlayers[i].setOnErrorListener(this::onError);
		}
	}

	/**
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 */
	public void setDataSource(@NonNull Uri uri) {
		initialized = setDataSourceImpl(mPlayers[currentPlayer], uri);
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 */
	public void setNextDataSource(@Nullable Uri uri) {
		if (uri != null) {
			int nextPlayerIndex;
			// if there is a crossfade pending, use the mediaplayer after next
			if (xfadeMode == NONE) {
				nextPlayerIndex = (currentPlayer + 1) % mPlayers.length;
			} else {
				nextPlayerIndex = (currentPlayer + 2) % mPlayers.length;
			}
			setDataSourceImpl(mPlayers[nextPlayerIndex], uri);
			continious = true;
		} else {
			continious = false;
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
	 * @return true if successful, false if another operation is already pending
	 */
	public boolean pause(boolean force) {
		MediaPlayer player = mPlayers[currentPlayer];
		if (force) {
			xfadeMode = NONE;
			setCrossfadeTask(false);
			if (player.isPlaying()) {
				player.pause();
			}
			return true;
		} else if (xfadeMode == NONE) {
			xfadeMode = FADE_OUT;
			return true;
		}
		return false;
	}

	/**
	 * stops playback
	 */
	public void stop() {
		xfadeMode = NONE;
		setCrossfadeTask(false);
		mPlayers[currentPlayer].stop();
	}

	/**
	 * go to next player
	 *
	 * @return true if successful, false if another operation is already pending
	 */
	public boolean next() {
		if (xfadeMode == NONE) {
			xfadeMode = XFADE;
			setCrossfadeTask(true);
			return true;
		}
		return false;
	}

	/**
	 * Releases resources associated with this MediaPlayer object.
	 */
	public void release() {
		stop();
		THREAD_POOL.shutdown();
		for (MediaPlayer player : mPlayers) {
			player.release();
		}
	}

	/**
	 * Gets the duration of the file.
	 *
	 * @return The duration in milliseconds
	 */
	public long getDuration() {
		return mPlayers[currentPlayer].getDuration();
	}

	/**
	 * Gets the current playback position.
	 *
	 * @return The current position in milliseconds
	 */
	public long getPosition() {
		return mPlayers[currentPlayer].getCurrentPosition();
	}

	/**
	 * Sets the current playback position.
	 *
	 * @param position The offset in milliseconds from the start to seek to
	 */
	public void setPosition(long position) {
		mPlayers[currentPlayer].seekTo((int) position);
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
		return mPlayers[currentPlayer].isPlaying();
	}

	/**
	 * create and configure MediaPlayer instance
	 *
	 * @return player
	 */
	private MediaPlayer createPlayer() {
		MediaPlayer player = new MediaPlayer();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AudioAttributes attr = new AudioAttributes.Builder()
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.setUsage(AudioAttributes.USAGE_MEDIA).build();
			player.setAudioAttributes(attr);
		}
		return player;
	}

	/**
	 * @param player The {@link MediaPlayer} to use
	 * @param uri    The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if initialized
	 */
	private boolean setDataSourceImpl(MediaPlayer player, @NonNull Uri uri) {
		Service service = mService.get();
		if (service != null) {
			try {
				player.reset();
				player.setDataSource(service, uri);
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.prepare();
			} catch (Exception err) {
				Log.e(TAG, "failed to set data source!");
				player.reset();
				return false;
			}
			// send session ID to external equalizer if set
			if (mPreferences.isExternalAudioFxPrefered() && !mPreferences.isAudioFxEnabled()) {
				Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
				intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getAudioSessionId());
				intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
				service.sendBroadcast(intent);
			}
			return true;
		}
		return false;
	}

	/**
	 * called periodically while playback to detect playback changes for crossfading
	 */
	private void onCrossfadeTrack() {
		MediaPlayer current = mPlayers[currentPlayer];
		switch (xfadeMode) {
			// force crossfade between two tracks
			case XFADE:
				if (!current.isPlaying()) {
					xfadeMode = NONE;
					onCompletion();
					break;
				}

			// fade out current track, then pause
			case FADE_OUT:
				volume = Math.max(volume - FADE_STEPS, 0f);
				current.setVolume(volume, volume);
				if (volume == 0f) {
					current.pause();
					if (xfadeMode == FADE_OUT) {
						xfadeMode = NONE;
					}
				}
				break;

			// play and fade in current track
			case FADE_IN:
				volume = Math.min(volume + FADE_STEPS, 1f);
				current.setVolume(volume, volume);
				if (!current.isPlaying()) {
					current.start();
				}
				if (volume == 1f) {
					xfadeMode = NONE;
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
	}

	/**
	 * enable/disable periodic crossfade polling
	 *
	 * @param enable true to enable crossfading
	 */
	private void setCrossfadeTask(boolean enable) {
		// remove old task if running
		if (xfadeTask != null) {
			xfadeTask.cancel(true);
			xfadeTask = null;
		}
		// set new cross fade task
		if (enable) {
			xfadeTask = THREAD_POOL.scheduleAtFixedRate(new Runnable() {
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
	}

	/**
	 * close current media player and select next one. Inform playback service that track changed
	 */
	private void onCompletion() {
		MediaPlayer current = mPlayers[currentPlayer];
		MusicPlaybackService service = mService.get();
		if (continious) {
			current.stop();
			// select next media player
			currentPlayer = (currentPlayer + 1) % mPlayers.length;
			// notify playback service that track went to next
			if (service != null) {
				service.onWentToNext();
			}
			play();
		} else {
			// notify playback service that the track ended
			if (service != null) {
				service.onTrackEnded();
			}
			stop();
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
			mp.stop();
			initialized = false;
			xfadeMode = NONE;
			final MusicPlaybackService service = mService.get();
			if (service != null) {
				playerHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (service.isPlaying()) {
							service.gotoNext(true);
						} else {
							service.openCurrentAndNext();
						}
					}
				}, 2000);
			}
			return true;
		}
		return false;
	}
}