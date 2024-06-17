package org.nuclearfog.apollo.service;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

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
}