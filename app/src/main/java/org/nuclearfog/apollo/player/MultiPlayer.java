package org.nuclearfog.apollo.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
public class MultiPlayer implements OnErrorListener, OnCompletionListener {

	private static final String TAG = "MultiPlayer";

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

	/**
	 * thread pool used to periodically poll the current play position for crossfading
	 */
	private static final ScheduledExecutorService THREAD_POOL = Executors.newScheduledThreadPool(3);

	private WeakReference<Context> mContext;
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
	public MultiPlayer(Service service) {
		mContext = new WeakReference<>(service.getApplicationContext());
		playerHandler = new Handler(service.getMainLooper());
		xfadeHandler = new Handler(service.getMainLooper());
		mPreferences = PreferenceUtils.getInstance(service);
		for (int i = 0 ; i < mPlayers.length ; i++) {
			mPlayers[i] = createPlayer();
			mPlayers[i].setOnCompletionListener(this);
			mPlayers[i].setOnErrorListener(this);
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setVolume(currentVolume, currentVolume);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			mp.reset();
			initialized = false;
			playerHandler.sendMessageDelayed(playerHandler.obtainMessage(MusicPlaybackService.MESSAGE_SERVER_DIED), 2000);
			Log.d(TAG, "onError:" + what + "," + extra);
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
		initialized = setDataSourceImpl(mPlayers[currentPlayer], uri);
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream
	 *            you want to play
	 */
	public void setNextDataSource(@NonNull Uri uri) {
		int nextPlayerIndex = (currentPlayer + 1) % mPlayers.length;
		setDataSourceImpl(mPlayers[nextPlayerIndex], uri);
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
		MediaPlayer mp = mPlayers[currentPlayer];
		if (!mp.isPlaying()) {
			currentVolume = 0.0f;
			isPaused = false;
			setCrossfadeTask(true);
			mp.setVolume(currentVolume, currentVolume);
			mp.start();
		}
	}

	/**
	 * Pauses playback. Call start() to resume.
	 */
	public void pause(boolean force) {
		MediaPlayer mp = mPlayers[currentPlayer];
		isPaused = true;
		if (force) {
			if (mp.isPlaying())
				mp.pause();
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
		THREAD_POOL.shutdown();
		for (MediaPlayer mp : mPlayers) {
			mp.release();
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
	 *
	 * @return true if initialized
	 */
	private boolean setDataSourceImpl(MediaPlayer player, @NonNull Uri uri) {
		Context context = mContext.get();
		if (context != null) {
			try {
				player.reset();
				player.setDataSource(context, uri);
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
				intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, MusicPlaybackService.APOLLO_PACKAGE_NAME);
				context.sendBroadcast(intent);
			}
			return true;
		}
		return false;
	}

	/**
	 * called periodically while playback to detect playback changes for crossfading
	 */
	private void onCrossfadeTrack() {
		long diff = getDuration() - getPosition();
		// crossfade current and next playback
		if (getPosition() > XFADE_DELAY && diff < XFADE_DELAY) {
			MediaPlayer nextPlayer = mPlayers[(currentPlayer + 1) % mPlayers.length];
			// calc volume for current and next player
			float volume = (float) diff / XFADE_DELAY;
			float invert = 1.0f - volume;
			// fade down current player
			mPlayers[currentPlayer].setVolume(volume, volume);
			// fade up next player
			nextPlayer.setVolume(invert, invert);
			// start next player
			if (!nextPlayer.isPlaying()) {
				nextPlayer.start();
			}
		}
		// fade out current playback
		else if (isPaused) {
			if (currentVolume > 0.0f) {
				currentVolume -= FADE_STEPS;
			} else {
				currentVolume = 0.0f;
				pause(true);
			}
			mPlayers[currentPlayer].setVolume(currentVolume, currentVolume);
		}
		// fade in curent playback
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
}