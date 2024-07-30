package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Broadcast listener used to detect unmounting external storages
 *
 * @author nuclearfog
 */
public class UnmountBroadcastReceiver extends BroadcastReceiver {

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service
	 */
	public UnmountBroadcastReceiver(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
			service.onEject();
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			service.onMediaMount();
		}
	}
}