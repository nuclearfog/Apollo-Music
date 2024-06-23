package org.nuclearfog.apollo.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

/**
 * callback class used by media buttons to control playback
 *
 * @author nuclearfog
 */
public class MediaButtonCallback extends MediaSessionCompat.Callback {

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service
	 */
	public MediaButtonCallback(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlay() {
		service.play();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		service.pause(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		service.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSkipToNext() {
		service.gotoNext();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSkipToPrevious() {
		service.gotoPrev();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSeekTo(long pos) {
		service.seekTo(pos);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlayFromUri(Uri uri, Bundle extras) {
		service.openFile(uri);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSetShuffleMode(int shuffleMode) {
		switch (shuffleMode) {
			case PlaybackStateCompat.SHUFFLE_MODE_INVALID:
			case PlaybackStateCompat.SHUFFLE_MODE_NONE:
				service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
				break;

			case PlaybackStateCompat.SHUFFLE_MODE_ALL:
				service.setShuffleMode(MusicPlaybackService.SHUFFLE_AUTO);
				break;

			case PlaybackStateCompat.SHUFFLE_MODE_GROUP:
				service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSetRepeatMode(int repeatMode) {
		switch (repeatMode) {
			case PlaybackStateCompat.REPEAT_MODE_NONE:
			case PlaybackStateCompat.REPEAT_MODE_INVALID:
				service.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
				break;

			case PlaybackStateCompat.REPEAT_MODE_ONE:
				service.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
				break;

			case PlaybackStateCompat.REPEAT_MODE_ALL:
			case PlaybackStateCompat.REPEAT_MODE_GROUP:
				service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
		String intentAction = mediaButtonEvent.getAction();
		if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (service.isPlaying()) {
					service.pause(false);
				} else {
					service.play();
				}
				return true;
			}
		}
		return false;
	}
}