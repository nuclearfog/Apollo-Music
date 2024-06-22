package org.nuclearfog.apollo.service;

import android.os.Handler;

/**
 * Handler used to shutdown (idle) playback service after timeout
 *
 * @author nuclearfog
 */
public class ShutdownHandler extends Handler implements Runnable {

	/**
	 * Idle time before stopping the foreground notfication (1 minute)
	 */
	private static final int IDLE_DELAY = 60000;

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service
	 */
	public ShutdownHandler(MusicPlaybackService service) {
		super(service.getMainLooper());
		this.service = service;
	}


	@Override
	public void run() {
		service.releaseServiceUiAndStop();
	}

	/**
	 * start scheduled shutdown
	 */
	public void start() {
		removeCallbacks(this);
		postDelayed(this, IDLE_DELAY);
	}

	/**
	 * abort running sheduled shutdown
	 */
	public void stop() {
		removeCallbacks(this);
	}
}