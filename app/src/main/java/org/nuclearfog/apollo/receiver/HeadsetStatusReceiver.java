package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Broadcast receiver class used to detect when headphones are disconnected
 * When disconnecting the playback will be stopped
 *
 * @author nuclearfog
 */
public class HeadsetStatusReceiver extends BroadcastReceiver {

	private MusicPlaybackService service;

	/**
	 * @param service callback to playbackservice to stop playback
	 */
	public HeadsetStatusReceiver(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			service.pause(true);
		}
	}
}