package com.andrew.apollo.loaders;

import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

/**
 * <a href="http://code.google.com/p/android/issues/detail?id=14944">Issue
 * 14944</a>
 *
 * @author Alexander Blom
 */
public abstract class WrappedAsyncTaskLoader<D> extends AsyncTaskLoader<D> {

    private D mData;

    /**
     * Constructor of <code>WrappedAsyncTaskLoader</code>
     *
     * @param context The {@link Context} to use.
     */
    public WrappedAsyncTaskLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliverResult(D data) {
        if (!isReset()) {
            mData = data;
            super.deliverResult(data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        } else if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible
        cancelLoad();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();
        mData = null;
    }
}
