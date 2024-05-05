package org.nuclearfog.apollo.service;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

/**
 * callback class used by media buttons to control playback
 * todo add more button methods
 *
 * @author nuclearfog
 */
public class MediaButtonCallback extends MediaSessionCompat.Callback {

	private MusicPlaybackService service;

	public MediaButtonCallback(MusicPlaybackService service) {
		this.service = service;
	}

	@Override
	public void onPlay() {
		service.play();
	}

	@Override
	public void onPause() {
		service.pause(false);
	}

	@Override
	public void onStop() {
		service.stop();
	}

	@Override
	public void onSkipToNext() {
		service.gotoNext();
	}

	@Override
	public void onSkipToPrevious() {
		service.gotoPrev();
	}

	@Override
	public void onSeekTo(long pos) {
		service.seekTo(pos);
	}

	@Override
	public void onPlayFromUri(Uri uri, Bundle extras) {
		service.openFile(uri);
	}
}