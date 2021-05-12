package com.andrew.apollo.loaders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apollo.utils.MusicUtils;

import java.util.Arrays;

/**
 * A custom {@link Cursor} used to return the queue and allow for easy dragging
 * and dropping of the items in it.
 */
@SuppressLint("NewApi")
public class NowPlayingCursor extends AbstractCursor {

    private static final String[] PROJECTION = {
            /* 0 */
            AudioColumns._ID,
            /* 1 */
            AudioColumns.TITLE,
            /* 2 */
            AudioColumns.ARTIST,
            /* 3 */
            AudioColumns.ALBUM,
            /* 4 */
            AudioColumns.DURATION
    };

    private Context mContext;

    private long[] mNowPlaying;

    private long[] mCursorIndexes;

    private int mSize;

    private int mCurPos;

    private Cursor mQueueCursor;

    /**
     * Constructor of <code>NowPlayingCursor</code>
     *
     * @param context The {@link Context} to use
     */
    public NowPlayingCursor(Context context) {
        mContext = context;
        makeNowPlayingCursor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (oldPosition == newPosition) {
            return true;
        }
        if (mNowPlaying == null || mCursorIndexes == null || newPosition >= mNowPlaying.length) {
            return false;
        }
        long id = mNowPlaying[newPosition];
        int cursorIndex = Arrays.binarySearch(mCursorIndexes, id);
        mQueueCursor.moveToPosition(cursorIndex);
        mCurPos = newPosition;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(int column) {
        try {
            return mQueueCursor.getString(column);
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
        return mQueueCursor.getShort(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(int column) {
        try {
            return mQueueCursor.getInt(column);
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
            return mQueueCursor.getLong(column);
        } catch (Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(int column) {
        return mQueueCursor.getFloat(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(int column) {
        return mQueueCursor.getDouble(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType(int column) {
        return mQueueCursor.getType(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNull(int column) {
        return mQueueCursor.isNull(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getColumnNames() {
        return PROJECTION;
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
            if (mQueueCursor != null) {
                mQueueCursor.close();
                mQueueCursor = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.close();
    }

    /**
     * Actually makes the queue
     */
    private void makeNowPlayingCursor() {
        mQueueCursor = null;
        mNowPlaying = MusicUtils.getQueue();
        mSize = mNowPlaying.length;
        if (mSize == 0) {
            return;
        }
        StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mSize - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        mQueueCursor = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION, selection.toString(), null, MediaStore.Audio.Media._ID);
        if (mQueueCursor == null) {
            mSize = 0;
            return;
        }
        int playlistSize = mQueueCursor.getCount();
        mCursorIndexes = new long[playlistSize];
        mQueueCursor.moveToFirst();
        int columnIndex = mQueueCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        for (int i = 0; i < playlistSize; i++) {
            mCursorIndexes[i] = mQueueCursor.getLong(columnIndex);
            mQueueCursor.moveToNext();
        }
        mQueueCursor.moveToFirst();
        mCurPos = -1;
        int removed = 0;
        for (int i = mNowPlaying.length - 1; i >= 0; i--) {
            long trackId = mNowPlaying[i];
            int cursorIndex = Arrays.binarySearch(mCursorIndexes, trackId);
            if (cursorIndex < 0) {
                removed += MusicUtils.removeTrack(trackId);
            }
        }
        if (removed > 0) {
            mNowPlaying = MusicUtils.getQueue();
            mSize = mNowPlaying.length;
            if (mSize == 0) {
                mCursorIndexes = null;
            }
        }
    }

    /**
     * @param which The position to remove
     */
    public void removeItem(int which) {
        if (MusicUtils.removeTracks(which)) {
            int i = which;
            mSize--;
            while (i < mSize) {
                mNowPlaying[i] = mNowPlaying[i + 1];
                i++;
            }
            onMove(-1, mCurPos);
        }
    }
}