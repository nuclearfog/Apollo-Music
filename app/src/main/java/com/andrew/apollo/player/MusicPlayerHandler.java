package com.andrew.apollo.player;

import static com.andrew.apollo.MusicPlaybackService.FADEDOWN;
import static com.andrew.apollo.MusicPlaybackService.FADEUP;
import static com.andrew.apollo.MusicPlaybackService.FOCUSCHANGE;
import static com.andrew.apollo.MusicPlaybackService.SERVER_DIED;
import static com.andrew.apollo.MusicPlaybackService.TRACK_ENDED;
import static com.andrew.apollo.MusicPlaybackService.TRACK_WENT_TO_NEXT;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.andrew.apollo.MusicPlaybackService;

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
			case FADEDOWN:
				mCurrentVolume -= .05f;
				if (mCurrentVolume > .2f) {
					sendEmptyMessageDelayed(FADEDOWN, 10);
				} else {
					mCurrentVolume = .2f;
				}
				service.setVolume(mCurrentVolume);
				break;

			case FADEUP:
				mCurrentVolume += .01f;
				if (mCurrentVolume < 1.0f) {
					sendEmptyMessageDelayed(FADEUP, 10);
				} else {
					mCurrentVolume = 1.0f;
				}
				service.setVolume(mCurrentVolume);
				break;

			case SERVER_DIED:
				if (service.isPlaying()) {
					service.gotoNext(true);
				} else {
					service.openCurrentAndNext();
				}
				break;

			case TRACK_WENT_TO_NEXT:
				service.onWentToNext();
				break;

			case TRACK_ENDED:
				service.onTrackEnded();
				break;

			case FOCUSCHANGE:
				switch (msg.arg1) {
					case AudioManager.AUDIOFOCUS_LOSS:
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
						service.onAudioFocusLoss(msg.arg1);
						break;

					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
						removeMessages(FADEUP);
						sendEmptyMessage(FADEDOWN);
						break;

					case AudioManager.AUDIOFOCUS_GAIN:
						if (service.onAudioFocusGain()) {
							mCurrentVolume = 0f;
						} else {
							removeMessages(FADEDOWN);
							sendEmptyMessage(FADEUP);
						}
						break;
				}
				break;
		}
	}
}