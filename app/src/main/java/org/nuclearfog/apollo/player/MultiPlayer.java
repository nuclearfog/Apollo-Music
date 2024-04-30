package org.nuclearfog.apollo.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.ContentDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.metadata.MetadataOutput;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.video.VideoRendererEventListener;
import androidx.media3.extractor.DefaultExtractorsFactory;

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
public class MultiPlayer implements Player.Listener {

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
	private ExoPlayer[] mPlayers = new ExoPlayer[PLAYER_INST];
	/**
	 * current mediaplayer's index of {@link #mPlayers}
	 */
	@IntRange(from=0, to=PLAYER_INST - 1)
	private int currentPlayer = 0;
	/**
	 * set to true if player was initialized successfully
	 */
	private boolean initialized = false;
	/**
	 * true if player continues to next track automatically
	 */
	private boolean continious = true;
	/**
	 * current fade in/out status {@link #NONE,#FADE_IN,#FADE_OUT,#XFADE}
	 */
	private int xfadeMode = NONE;

	/**
	 * Constructor of <code>MultiPlayer</code>
	 */
	public MultiPlayer(MusicPlaybackService service) {
		mService = new WeakReference<>(service);
		playerHandler = new Handler(service.getMainLooper());
		xfadeHandler = new Handler(service.getMainLooper());
		mPreferences = PreferenceUtils.getInstance(service);
		for (int i = 0 ; i < mPlayers.length ; i++) {
			mPlayers[i] = createPlayer(service.getApplicationContext());
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setVolume(0f);
			mPlayers[i].addListener(this);
		}
	}


	@Override
	public void onPlayerError(@NonNull PlaybackException error) {
		Log.e(TAG, "onError:" + error.getErrorCodeName());
		if (initialized) {
			mPlayers[currentPlayer].stop();
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
		if ((Math.abs(getDuration() - getPosition())) <= XFADE_DELAY)
			return true;
		return xfadeMode != NONE;
	}

	/**
	 * Starts or resumes playback.
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
	 */
	public boolean pause(boolean force) {
		ExoPlayer player = mPlayers[currentPlayer];
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
		for (ExoPlayer player : mPlayers) {
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
	private ExoPlayer createPlayer(Context context) {
		return new ExoPlayer.Builder(context, new RenderersFactory() {
			@NonNull
			@Override
			public Renderer[] createRenderers(@NonNull Handler eventHandler, @NonNull VideoRendererEventListener videoRendererEventListener,
											  @NonNull AudioRendererEventListener audioRendererEventListener, @NonNull TextOutput textRendererOutput,
											  @NonNull MetadataOutput metadataRendererOutput) {
				return new Renderer[]{new MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener)};
			}
		}).build();
	}

	/**
	 * @param player The {@link ExoPlayer} to use
	 * @param uri    The path of the file, or the http/rtsp URL of the stream you want to play
	 *
	 * @return true if initialized
	 */
	private boolean setDataSourceImpl(ExoPlayer player, @NonNull Uri uri) {
		Context context = mService.get();
		if (context != null) {
			try {
				DataSource.Factory dataSourceFactory = new DataSource.Factory() {
					@NonNull
					@Override
					public DataSource createDataSource() {
						return new ContentDataSource(context);
					}
				};
				MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory()).createMediaSource(MediaItem.fromUri(uri));
				player.setMediaSource(mediaSource);
				player.prepare();
			} catch (Exception err) {
				Log.e(TAG, "failed to set data source!");
				return false;
			}
			// send session ID to external equalizer if set
			if (mPreferences.isExternalAudioFxPrefered() && !mPreferences.isAudioFxEnabled()) {
				Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
				intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getAudioSessionId());
				intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
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
		ExoPlayer current = mPlayers[currentPlayer];
		switch (xfadeMode) {
			case XFADE:
				if (!current.isPlaying()) {
					xfadeMode = NONE;
					onCompletion();
					break;
				}

			case FADE_OUT:
				float currentVolume = Math.max(current.getVolume() - FADE_STEPS, 0f);
				current.setVolume(currentVolume);
				if (currentVolume == 0f) {
					current.pause();
					if (xfadeMode == FADE_OUT) {
						xfadeMode = NONE;
					}
				}
				break;

			case FADE_IN:
				currentVolume = Math.min(current.getVolume() + FADE_STEPS, 1f);
				current.setVolume(currentVolume);
				if (!current.isPlaying()) {
					current.play();
				}
				if (currentVolume == 1f) {
					xfadeMode = NONE;
				}
				break;

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

	/**
	 * close current media player and select next one. Inform playback service that track changed
	 */
	private void onCompletion() {
		ExoPlayer current = mPlayers[currentPlayer];
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
}