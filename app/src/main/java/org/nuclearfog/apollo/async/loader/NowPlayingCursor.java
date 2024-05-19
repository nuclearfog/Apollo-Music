package org.nuclearfog.apollo.async.loader;

import static org.nuclearfog.apollo.utils.CursorFactory.NP_COLUMNS;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.utils.CursorFactory;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A custom {@link Cursor} used to return the queue and allow for easy dragging
 * and dropping of the items in it.
 *
 * @author nuclearfog
 */
public class NowPlayingCursor extends AbstractCursor {

	private Context mContext;

	private final ArrayList<Long> mNowPlaying = new ArrayList<>();

	private final List<Long> mCursorIndexes = new LinkedList<>();

	private int mCurPos;

	private Cursor cursor;


	public NowPlayingCursor(Context context) {
		mContext = context;
		makeNowPlayingCursor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		return mNowPlaying.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		if (oldPosition == newPosition) {
			return true;
		}
		if (mNowPlaying.isEmpty() || mCursorIndexes.isEmpty() || newPosition >= mNowPlaying.size()) {
			return false;
		}
		long id = mNowPlaying.get(newPosition);
		int cursorIndex = mCursorIndexes.indexOf(id);
		if (cursorIndex < 0)
			return false;
		cursor.moveToPosition(cursorIndex);
		mCurPos = newPosition;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getString(int column) {
		try {
			return cursor.getString(column);
		} catch (Exception ignored) {
			onChange(true);
			return "";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public short getShort(int column) {
		return cursor.getShort(column);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getInt(int column) {
		try {
			return cursor.getInt(column);
		} catch (Exception ignored) {
			onChange(true);
			return 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLong(int column) {
		try {
			return cursor.getLong(column);
		} catch (Exception ignored) {
			onChange(true);
			return 0L;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getFloat(int column) {
		return cursor.getFloat(column);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getDouble(int column) {
		return cursor.getDouble(column);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getType(int column) {
		return cursor.getType(column);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isNull(int column) {
		return cursor.isNull(column);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getColumnNames() {
		return NP_COLUMNS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deactivate() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean requery() {
		makeNowPlayingCursor();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		try {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		super.close();
	}

	/**
	 * Actually makes the queue
	 */
	private void makeNowPlayingCursor() {
		getQueue();
		if (mNowPlaying.isEmpty()) {
			cursor = null;
			return;
		}
		mCursorIndexes.clear();
		cursor = CursorFactory.makeNowPlayingCursor(mContext, mNowPlaying);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				long id = cursor.getLong(0);
				mCursorIndexes.add(id);
			} while (cursor.moveToNext());
		}
		mCurPos = -1;
		int removed = 0;
		for (long trackId : mNowPlaying) {
			int cursorIndex = mCursorIndexes.indexOf(trackId);
			if (cursorIndex < 0) {
				removed += MusicUtils.removeTrack(trackId);
			}
		}
		if (removed > 0) {
			getQueue();
			if (mNowPlaying.isEmpty()) {
				mCursorIndexes.clear();
			}
		}
	}

	/**
	 * @param which The position to remove
	 */
	public void removeItem(int which) {
		if (MusicUtils.removeTracks(which)) {
			mNowPlaying.remove(which);
			onMove(-1, mCurPos);
		}
	}

	/**
	 * get queue from MusicUtils
	 */
	private void getQueue() {
		long[] ids = MusicUtils.getQueue();
		mNowPlaying.clear();
		mNowPlaying.ensureCapacity(ids.length);
		for (long id : ids) {
			mNowPlaying.add(id);
		}
	}
}