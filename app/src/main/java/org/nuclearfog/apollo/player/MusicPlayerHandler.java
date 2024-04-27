package org.nuclearfog.apollo.player;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_FOCUS_CHANGE;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_SERVER_DIED;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_TRACK_ENDED;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_TRACK_WENT_TO_NEXT;

import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.service.MusicPlaybackService;

import java.lang.ref.WeakReference;

/**
 * Player handler for {@link MusicPlaybackService}
 *
 * @author nuclearfog
 */
public class MusicPlayerHandler extends Handler {

	/**
	 *
	 */
	private static final String HANDLER_NAME = "MusicPlayerHandler";


	private WeakReference<MusicPlaybackService> mService;


	private MusicPlayerHandler(MusicPlaybackService service, Looper looper) {
		super(looper);
		mService = new WeakReference<>(service);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleMessage(@NonNull Message msg) {
		MusicPlaybackService service = mService.get();
		if (service == null) {
			return;
		}
		switch (msg.what) {
			case MESSAGE_SERVER_DIED:
				if (service.isPlaying()) {
					service.gotoNext(true);
				} else {
					service.openCurrentAndNext();
				}
				break;

			case MESSAGE_TRACK_WENT_TO_NEXT:
				service.onWentToNext();
				break;

			case MESSAGE_TRACK_ENDED:
				service.onTrackEnded();
				break;

			case MESSAGE_FOCUS_CHANGE:
				switch (msg.arg1) {
					case AudioManager.AUDIOFOCUS_LOSS:
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
						service.onAudioFocusLoss(msg.arg1);
						break;

					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
						service.pause(false);
						break;

					case AudioManager.AUDIOFOCUS_GAIN:
						service.onAudioFocusGain();
						break;
				}
				break;
		}
	}

	/**
	 * initialize MusicPlayerHandler class
	 */
	public static MusicPlayerHandler init(MusicPlaybackService service) {
		HandlerThread thread = new HandlerThread(HANDLER_NAME, THREAD_PRIORITY_BACKGROUND);
		thread.start();
		return new MusicPlayerHandler(service, thread.getLooper());
	}
}