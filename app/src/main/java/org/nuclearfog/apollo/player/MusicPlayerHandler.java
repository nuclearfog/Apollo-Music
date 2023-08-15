package org.nuclearfog.apollo.player;

import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_FADEDOWN;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_FADEUP;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_FOCUS_CHANGE;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_SERVER_DIED;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_TRACK_ENDED;
import static org.nuclearfog.apollo.service.MusicPlaybackService.MESSAGE_TRACK_WENT_TO_NEXT;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.service.MusicPlaybackService;

import java.lang.ref.WeakReference;

/**
 *
 */
public class MusicPlayerHandler extends Handler {

	private WeakReference<MusicPlaybackService> mService;
	private float mCurrentVolume = 1.0f;

	/**
	 * Constructor of <code>MusicPlayerHandler</code>
	 *
	 * @param service The service to use.
	 * @param looper  The thread to run on.
	 */
	public MusicPlayerHandler(MusicPlaybackService service, Looper looper) {
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
			case MESSAGE_FADEDOWN:
				mCurrentVolume -= .05f;
				if (mCurrentVolume > .2f) {
					sendEmptyMessageDelayed(MESSAGE_FADEDOWN, 10);
				} else {
					mCurrentVolume = .2f;
				}
				service.setVolume(mCurrentVolume);
				break;

			case MESSAGE_FADEUP:
				mCurrentVolume += .01f;
				if (mCurrentVolume < 1.0f) {
					sendEmptyMessageDelayed(MESSAGE_FADEUP, 10);
				} else {
					mCurrentVolume = 1.0f;
				}
				service.setVolume(mCurrentVolume);
				break;

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
						removeMessages(MESSAGE_FADEUP);
						sendEmptyMessage(MESSAGE_FADEDOWN);
						break;

					case AudioManager.AUDIOFOCUS_GAIN:
						if (service.onAudioFocusGain()) {
							mCurrentVolume = 0f;
						} else {
							removeMessages(MESSAGE_FADEDOWN);
							sendEmptyMessage(MESSAGE_FADEUP);
						}
						break;
				}
				break;
		}
	}
}