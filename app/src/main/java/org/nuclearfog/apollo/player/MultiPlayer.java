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
	 * sampling rate of the fade effect
	 */
	private static final long FADE_RESOLUTION = 50;

	/**
	 * volume steps used to fade in or out
	 */
	private static final float FADE_STEPS = 0.1f;

	/**
	 * crossfade overlay of two tracks in milliseconds
	 */
	private static final long XFADE_DELAY = 1000;

	/**
	 * volume threshold to disable playback
	 */
	private static final float FADE_THRESHOLD = 0.01f;

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
	private MediaPlayer[] mPlayers = new MediaPlayer[3];
	/**
	 * volume of the current mediaplayer
	 */
	private float currentVolume = 0.0f;
	/**
	 * current mediaplayer's index
	 */
	private int currentPlayer = 0;
	/**
	 * set to true if player was initialized successfully
	 */
	private boolean initialized = false;
	/**
	 * current fade in/out status {@link #NONE,#FADE_IN,#FADE_OUT,#XFADE}
	 */
	private int xfadeMode = NONE;

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
	public void setNextDataSource(@NonNull Uri uri) {
		int nextPlayerIndex;
		// if there is a crossfade pending, use the mediaplayer after next
		if (xfadeMode == XFADE)
			nextPlayerIndex = (currentPlayer + 2) % mPlayers.length;
		else
			nextPlayerIndex = (currentPlayer + 1) % mPlayers.length;
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
			setCrossfadeTask(true);
			mp.setVolume(currentVolume, currentVolume);
			mp.start();
			xfadeMode = FADE_IN;
		}
	}

	/**
	 * Pauses playback. Call start() to resume.
	 *
	 * @param force true to stop playback immediately
	 */
	public void pause(boolean force) {
		MediaPlayer mp = mPlayers[currentPlayer];
		if (force) {
			if (mp.isPlaying())
				mp.pause();
			setCrossfadeTask(false);
		} else {
			xfadeMode = FADE_OUT;
		}
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
	 */
	public void next() {
		MediaPlayer mp = mPlayers[(currentPlayer + 1) % mPlayers.length];
		if (!mp.isPlaying()) {
			mp.setVolume(0f, 0f);
			mp.start();
		}
		xfadeMode = XFADE;
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
		MediaPlayer current = mPlayers[currentPlayer];
		MediaPlayer next = mPlayers[(currentPlayer + 1) % mPlayers.length];
		// crossfade current and next playback
		if ((getPosition() > XFADE_DELAY && diff < XFADE_DELAY)) {
			// calc volume for current and next player
			float volume = Math.max((float) diff / XFADE_DELAY, 0f);
			float invert = 1f - volume;
			// fade down current player
			current.setVolume(volume, volume);
			// fade up next player
			next.setVolume(invert, invert);
			// start next player
			if (!next.isPlaying()) {
				next.start();
			}
		}
		// force crossfade to next track
		else if (xfadeMode == XFADE) {
			currentVolume = Math.max(currentVolume - FADE_STEPS, 0f);
			float invert = 1f - currentVolume;
			current.setVolume(currentVolume, currentVolume);
			next.setVolume(invert, invert);
			if (currentVolume < FADE_THRESHOLD) {
				onCompletion(current);
				currentVolume = 1f;
				xfadeMode = NONE;
			}
		}
		// fade out current playback
		else if (xfadeMode == FADE_OUT) {
			currentVolume = Math.max(currentVolume - FADE_STEPS, 0f);
			current.setVolume(currentVolume, currentVolume);
			if (currentVolume < FADE_THRESHOLD)  {
				xfadeMode = NONE;
				pause(true);
			}
		}
		// fade in curent playback
		else if (xfadeMode == FADE_IN) {
			currentVolume = Math.min(currentVolume + FADE_STEPS, 1f);
			current.setVolume(currentVolume, currentVolume);
			if (currentVolume == 1f)  {
				xfadeMode = NONE;
			}
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