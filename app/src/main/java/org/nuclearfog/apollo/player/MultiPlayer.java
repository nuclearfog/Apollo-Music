package org.nuclearfog.apollo.player;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

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
public class MultiPlayer implements OnPreparedListener, OnErrorListener, OnCompletionListener {

	/**
	 * sampling rate of the fade effect
	 */
	private static final long FADE_RESOLUTION = 50;

	/**
	 * volume steps used to fade in or out
	 */
	private static final float FADE_STEPS = 0.1f;

	/**
	 * crossfade overlapping of two tracks in milliseconds
	 */
	private static final long XFADE_DELAY = 1000;

	private static final ScheduledExecutorService THREAD_POOL = Executors.newScheduledThreadPool(1);

	private WeakReference<MusicPlaybackService> mService;
	private Handler playerHandler, xfadeHandler;
	private PreferenceUtils mPreferences;

	@Nullable
	private Future<?> xfadeTask;
	/**
	 * mediaplayer used to switch between tracks
	 */
	private MediaPlayer[] mPlayers = new MediaPlayer[2];
	/**
	 * volume of the current mediaplayer
	 */
	private float currentVolume = 0.0f;
	/**
	 * current mediaplayer's index
	 */
	private int currentPlayer = 0;
	/**
	 * flag used to fade-out current track
	 */
	private boolean isPaused = false;
	/**
	 * indicates if mediaplayer is ready to play
	 */
	private boolean initialized = false;

	/**
	 * Constructor of <code>MultiPlayer</code>
	 */
	public MultiPlayer(MusicPlaybackService service) {
		mService = new WeakReference<>(service);
		playerHandler = new Handler(service.getMainLooper());
		mPreferences = PreferenceUtils.getInstance(service);
		xfadeHandler = new Handler(service.getMainLooper());
		for (int i = 0 ; i < mPlayers.length ; i++) {
			mPlayers[i] = createPlayer();
			mPlayers[i].setOnPreparedListener(this);
			mPlayers[i].setOnCompletionListener(this);
			mPlayers[i].setOnErrorListener(this);
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setVolume(currentVolume, currentVolume);
		}
	}


	@Override
	public void onPrepared(MediaPlayer mp) {
		initialized = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			initialized = false;
			mp.reset();
			playerHandler.sendMessageDelayed(playerHandler.obtainMessage(MusicPlaybackService.MESSAGE_SERVER_DIED), 2000);
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (mp == mPlayers[currentPlayer]) {
			// switch to next player
			mPlayers[currentPlayer].reset();
			currentPlayer = (currentPlayer + 1) % mPlayers.length;
			//
			playerHandler.sendEmptyMessage(MusicPlaybackService.MESSAGE_TRACK_WENT_TO_NEXT);
		} else {
			playerHandler.sendEmptyMessage(MusicPlaybackService.MESSAGE_TRACK_ENDED);
		}
	}

	/**
	 * @param uri The path of the file, or the http/rtsp URL of the stream
	 *            you want to play
	 */
	public void setDataSource(@NonNull Uri uri) {
		setDataSourceImpl(mPlayers[currentPlayer], uri);
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream
	 *            you want to play
	 */
	public void setNextDataSource(@NonNull Uri uri) {
		try {
			int nextPlayerIndex = (currentPlayer + 1) % mPlayers.length;
			setDataSourceImpl(mPlayers[nextPlayerIndex], uri);
		} catch (Exception err) {
			if (BuildConfig.DEBUG) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Sets the handler
	 *
	 * @param handler The handler to use
	 */
	public void setHandler(Handler handler) {
		playerHandler = handler;
	}

	/**
	 * @return True if the player is ready to go, false otherwise
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Starts or resumes playback.
	 */
	public void play() {
		if (!mPlayers[currentPlayer].isPlaying()) {
			mPlayers[currentPlayer].start();
		}
		setCrossfadeTask(true);
		currentVolume = 0.0f;
		isPaused = false;
	}

	/**
	 * Pauses playback. Call start() to resume.
	 */
	public void pause(boolean force) {
		isPaused = true;
		if (force) {
			mPlayers[currentPlayer].pause();
			setCrossfadeTask(false);
		}
	}

	/**
	 * stops playback
	 */
	public void stop() {
		isPaused = true;
		setCrossfadeTask(false);
		mPlayers[currentPlayer].stop();
	}

	/**
	 * Releases resources associated with this MediaPlayer object.
	 */
	public void release() {
		stop();
		for (MediaPlayer mp : mPlayers) {
			mp.release();
		}
		THREAD_POOL.shutdown();
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
	 * Gets the current playback position.
	 *
	 * @param whereto The offset in milliseconds from the start to seek to
	 */
	public void seek(long whereto) {
		mPlayers[currentPlayer].seekTo((int) whereto);
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
	 */
	private void setDataSourceImpl(MediaPlayer player, @NonNull Uri uri) {
		MusicPlaybackService musicService = mService.get();
		if (musicService != null) {
			try {
				player.reset();
				player.setDataSource(musicService.getApplicationContext(), uri);
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.prepare();
			} catch (Exception err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
			if (mPreferences.isExternalAudioFxPrefered() && !mPreferences.isAudioFxEnabled()) {
				Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
				intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
				intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, MusicPlaybackService.APOLLO_PACKAGE_NAME);
				musicService.sendBroadcast(intent);
			}
		}
	}

	/**
	 * check for track status/position and change volume or playback status
	 */
	private void crossfadeTrack() {
		long diff = ((getDuration() - getPosition()) / FADE_RESOLUTION) * FADE_RESOLUTION;
		// crossfade
		if (getPosition() > XFADE_DELAY && diff < XFADE_DELAY) {
			float volume = (float) diff / XFADE_DELAY;
			float invert = 1.0f - volume;
			mPlayers[currentPlayer].setVolume(volume, volume);
			MediaPlayer nextPlayer = mPlayers[(currentPlayer + 1) % mPlayers.length];
			if (!nextPlayer.isPlaying()) {
				nextPlayer.start();
			}
			nextPlayer.setVolume(invert, invert);
		}
		// fade out
		else if (isPaused) {
			if (currentVolume > 0.0f) {
				currentVolume -= FADE_STEPS;
			} else {
				currentVolume = 0.0f;
				pause(true);
			}
			mPlayers[currentPlayer].setVolume(currentVolume, currentVolume);
		}
		// fade in
		else {
			if (currentVolume < 1.0f) {
				currentVolume += FADE_STEPS;
			} else {
				currentVolume = 1.0f;
			}
			mPlayers[currentPlayer].setVolume(currentVolume, currentVolume);
		}
	}

	/**
	 * enable/disable periodic crossfading
	 *
	 * @param enable true to enable crossfading
	 */
	private void setCrossfadeTask(boolean enable) {
		if (enable) {
			xfadeTask = THREAD_POOL.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					xfadeHandler.post(new Runnable() {
						@Override
						public void run() {
							crossfadeTrack();
						}
					});
				}
			}, FADE_RESOLUTION, FADE_RESOLUTION, TimeUnit.MILLISECONDS);
		} else {
			if (xfadeTask != null) {
				xfadeTask.cancel(true);
			}
		}
	}
}