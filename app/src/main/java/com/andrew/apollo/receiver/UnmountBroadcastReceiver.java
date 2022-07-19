package com.andrew.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrew.apollo.MusicPlaybackService;

import java.lang.ref.WeakReference;

/**
 * Custom broadcast listener for unmounting external storage
 */
public class UnmountBroadcastReceiver extends BroadcastReceiver {

	private WeakReference<MusicPlaybackService> mReference;


	public UnmountBroadcastReceiver(MusicPlaybackService service) {
		mReference = new WeakReference<>(service);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		MusicPlaybackService mService = mReference.get();
		String action = intent.getAction();
		if (mService != null && action != null) {
			if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
				mService.onEject();
			} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
				mService.onUnmount();
			}
		}
	}
}