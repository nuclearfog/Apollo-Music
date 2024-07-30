package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * this class updates the current play status from {@link MusicPlaybackService} to an activity
 *
 * @author nuclearfog
 */
public class PlaybackStatusReceiver extends BroadcastReceiver {

	/**
	 * callback reference
	 */
	private PlayStatusListener callback;


	/**
	 * @param callback callback listener to update information
	 */
	public PlaybackStatusReceiver(PlayStatusListener callback) {
		this.callback = callback;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			switch (action) {
				case MusicPlaybackService.CHANGED_META:
					callback.onMetaChange();
					break;

				case MusicPlaybackService.CHANGED_PLAYSTATE:
					callback.onStateChange();
					break;

				case MusicPlaybackService.CHANGED_REPEATMODE:
				case MusicPlaybackService.CHANGED_SHUFFLEMODE:
					callback.onModeChange();
					break;

				case MusicPlaybackService.ACTION_REFRESH:
					callback.refresh();
					break;
			}
		}
	}

	/**
	 * callback listener for status change
	 */
	public interface PlayStatusListener {

		/**
		 * called when meta information changes
		 */
		void onMetaChange();

		/**
		 * called when playstate changes
		 */
		void onStateChange();

		/**
		 * called when mode changes between repeat and shuffle
		 */
		void onModeChange();

		/**
		 *
		 */
		void refresh();
	}
}