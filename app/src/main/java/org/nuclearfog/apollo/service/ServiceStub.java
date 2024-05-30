package org.nuclearfog.apollo.service;

import android.net.Uri;

import org.nuclearfog.apollo.IApolloService;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;

import java.lang.ref.WeakReference;

/**
 * callback used to communicate with activities
 */
public class ServiceStub extends IApolloService.Stub {

	private final WeakReference<MusicPlaybackService> mService;

	/**
	 * @param service callback reference
	 */
	ServiceStub(MusicPlaybackService service) {
		mService = new WeakReference<>(service);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void openFile(Uri uri) {
		MusicPlaybackService service = mService.get();
		if (service != null && uri != null) {
			service.openFile(uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(long[] list, int position) {
		MusicPlaybackService service = mService.get();
		if (mService.get() != null && list != null) {
			service.open(list, position);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void pause(boolean force) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.pause(force);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void play() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.play();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void gotoNext() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.gotoNext();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void gotoPrev() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.gotoPrev();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enqueue(long[] list, int action) {
		MusicPlaybackService service = mService.get();
		if (service != null && list != null) {
			service.enqueue(list, action);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void moveQueueItem(int from, int to) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.moveQueueItem(from, to);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.notifyChange(MusicPlaybackService.ACTION_REFRESH);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPlaying() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.isPlaying();
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getQueue() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getQueue();
		return new long[]{};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long position() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getPosition();
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void seek(long position) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.seekTo(position);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Album getCurrentAlbum() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getCurrentAlbum();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Song getCurrentTrack() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getCurrentSong();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getQueuePosition() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getQueuePosition();
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setQueuePosition(int index) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setQueuePosition(index);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getShuffleMode() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getShuffleMode();
		return MusicPlaybackService.SHUFFLE_NONE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setShuffleMode(int shufflemode) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setShuffleMode(shufflemode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRepeatMode() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getRepeatMode();
		return MusicPlaybackService.REPEAT_NONE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRepeatMode(int repeatmode) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setRepeatMode(repeatmode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearQueue() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.clearQueue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeTrack(int pos) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.removeQueueTrack(pos);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int removeTracks(long[] id) {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.removeQueueTracks(id);
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getAudioSessionId() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getAudioSessionId();
		return 0;
	}
}