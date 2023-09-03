package org.nuclearfog.apollo.service;


import android.net.Uri;

import com.andrew.apollo.IApolloService;

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
	public void stop() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.stop();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void pause() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.pause();
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
	public void prev() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.prev();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void goToNext() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.gotoNext(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void goToPrev() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.goToPrev();
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
	public void toggleFavorite() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.toggleFavorite();
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
	public boolean isFavorite() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.isFavorite();
		return false;
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
	public long duration() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.duration();
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long position() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.position();
		return -1L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long seek(long position) {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.seek(position);
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getAudioId() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getAudioId();
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getArtistId() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getArtistId();
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getAlbumId() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getAlbumId();
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getArtistName() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getArtistName();
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTrackName() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getTrackName();
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlbumName() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getAlbumName();
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPath() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getPath();
		return "";
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
	public int removeTracks(int first, int last) {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.removeTracks(first, last);
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int removeTrack(long id) {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.removeTrack(id);
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMediaMountedCount() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getMediaMountedCount();
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