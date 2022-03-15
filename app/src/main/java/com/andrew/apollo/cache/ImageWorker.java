/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.cache;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.BitmapUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a {@link Bitmap} to an {@link ImageView}. It handles things like using a
 * memory and disk cache, running the work in a background thread and setting a
 * placeholder image.
 */
public abstract class ImageWorker {

    /**
     * Default transition drawable fade time
     */
    private static final int FADE_IN_TIME = 200;

    /**
     * The resources to use
     */
    private Resources mResources;

    /**
     * Layer drawable used to cross fade the result from the worker
     */
    private Drawable[] mArrayDrawable;

    /**
     * The Context to use
     */
    protected Context mContext;

    /**
     * Disk and memory caches
     */
    protected ImageCache mImageCache;

    /**
     * Constructor of <code>ImageWorker</code>
     *
     * @param context The {@link Context} to use
     */
    protected ImageWorker(Context context) {
        mContext = context.getApplicationContext();
        mResources = mContext.getResources();
        // Create the transparent layer for the transition drawable
        ColorDrawable mCurrentDrawable = new ColorDrawable(mResources.getColor(R.color.transparent));
        // A transparent image (layer 0) and the new result (layer 1)
        mArrayDrawable = new Drawable[2];
        mArrayDrawable[0] = mCurrentDrawable;
        // XXX The second layer is set in the worker task.
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public static boolean executePotentialWork(Object data, ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            Object bitmapData = bitmapWorkerTask.mKey;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        return true;
    }

    /**
     * Used to determine if the current image drawable has an instance of
     * {@link BitmapWorkerTask}
     *
     * @param imageView Any {@link ImageView}.
     * @return Retrieve the currently active work task (if any) associated with
     * this {@link ImageView}. null if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * Set the {@link ImageCache} object to use with this ImageWorker.
     *
     * @param cacheCallback new {@link ImageCache} object.
     */
    public void setImageCache(ImageCache cacheCallback) {
        mImageCache = cacheCallback;
    }

    /**
     * flush() is called to synchronize up other methods that are accessing the
     * cache first
     */
    public void flush() {
        if (mImageCache != null) {
            mImageCache.flush();
        }
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (mImageCache != null) {
            mImageCache.addBitmapToCache(key, bitmap);
        }
    }

    /**
     * Called to fetch the artist or ablum art.
     *
     * @param key        The unique identifier for the image.
     * @param artistName The artist name for the Last.fm API.
     * @param albumName  The album name for the Last.fm API.
     * @param albumId    The album art index, to check for missing artwork.
     * @param imageviews The {@link ImageView} used to set the cached {@link Bitmap}.
     *                   a second image is optional and will be used to add blurring effect
     * @param imageType  The type of image URL to fetch for.
     */
    @SuppressWarnings("SameParameterValue")
    protected void loadImage(String key, String artistName, String albumName, long albumId, ImageType imageType, ImageView... imageviews) {
        if (key != null && mImageCache != null && imageviews.length > 0) {
            // First, check the cache for the image
            Bitmap lruBitmap = mImageCache.getBitmapFromMemCache(key);
            if (lruBitmap != null) {
                // Bitmap found in memory cache
                imageviews[0].setImageBitmap(lruBitmap);
                // add blurring to the second image if defined
                if (imageviews.length > 1) {
                    Bitmap blur = BitmapUtils.createBlurredBitmap(lruBitmap);
                    imageviews[1].setImageBitmap(blur);
                }
            }
            // check storage for image or download
            else if (executePotentialWork(key, imageviews[0]) && !mImageCache.isDiskCachePaused()) {
                // Otherwise run the worker task
                BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(this, imageType, imageviews);
                AsyncDrawable asyncDrawable = new AsyncDrawable(bitmapWorkerTask);
                imageviews[0].setImageDrawable(asyncDrawable);
                try {
                    bitmapWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key, artistName, albumName, String.valueOf(albumId));
                } catch (RejectedExecutionException e) {
                    // Executor has exhausted queue space, show default artwork
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the final {@link Bitmap}. This will be executed in
     * a background thread and be long running.
     *
     * @param key The key to identify which image to process, as provided by
     * @return The processed {@link Bitmap}.
     */
    protected abstract Bitmap processBitmap(String key);

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the URL needed to fetch the final {@link Bitmap}.
     *
     * @param artistName The artist name param used in the Last.fm API.
     * @param albumName  The album name param used in the Last.fm API.
     * @param imageType  The type of image URL to fetch for.
     * @return The image URL for an artist image or album image.
     */
    protected abstract String processImageUrl(String artistName, String albumName, ImageType imageType);

    /**
     * Used to define what type of image URL to fetch for, artist or album.
     */
    public enum ImageType {
        ARTIST, ALBUM
    }

    /**
     * A custom {@link BitmapDrawable} that will be attached to the
     * {@link ImageView} while the work is in progress. Contains a reference to
     * the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can
     * bind its result, independently of the finish order.
     */
    private static final class AsyncDrawable extends ColorDrawable {

        private WeakReference<BitmapWorkerTask> mBitmapWorkerTaskReference;

        /**
         * Constructor of <code>AsyncDrawable</code>
         */
        public AsyncDrawable(BitmapWorkerTask mBitmapWorkerTask) {
            super(Color.TRANSPARENT);
            mBitmapWorkerTaskReference = new WeakReference<>(mBitmapWorkerTask);
        }

        /**
         * @return The {@link BitmapWorkerTask} associated with this drawable
         */
        @Nullable
        public BitmapWorkerTask getBitmapWorkerTask() {
            return mBitmapWorkerTaskReference.get();
        }
    }

    /**
     * The actual {@link AsyncTask} that will process the image.
     */
    private static class BitmapWorkerTask extends AsyncTask<String, Void, Drawable[]> {

        /**
         * callback reference to update image
         */
        private WeakReference<ImageWorker> callback;

        /**
         * The {@link ImageView} used to set the result
         */
        private WeakReference<ImageView[]> mImageReference;

        /**
         * Type of URL to download
         */
        private ImageType mImageType;

        /**
         * The key used to store cached entries
         */
        private String mKey;

        /**
         * Constructor of <code>BitmapWorkerTask</code>
         *
         * @param imageView The {@link ImageView} to use.
         * @param imageType The type of image URL to fetch for.
         */
        public BitmapWorkerTask(ImageWorker callback, ImageType imageType, ImageView... imageView) {
            super();
            imageView[0].setBackgroundResource(R.drawable.default_artwork);
            mImageReference = new WeakReference<>(imageView);
            this.callback = new WeakReference<>(callback);
            mImageType = imageType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Drawable[] doInBackground(String... params) {
            try {
                ImageWorker worker = callback.get();
                if (worker == null)
                    return null;

                // Define the key
                mKey = params[0];

                // The result
                Bitmap bitmap = null;

                // First, check the disk cache for the image
                if (mKey != null && worker.mImageCache != null && !isCancelled()) {
                    bitmap = worker.mImageCache.getCachedBitmap(mKey);
                }

                // Define the album id now
                long mAlbumId = Long.parseLong(params[3]);

                // Second, if we're fetching artwork, check the device for the image
                if (bitmap == null && mAlbumId >= 0 && mKey != null && !isCancelled() && worker.mImageCache != null) {
                    bitmap = worker.mImageCache.getCachedArtwork(worker.mContext, mKey, mAlbumId);
                }

                // Third, by now we need to download the image
                if (bitmap == null && ApolloUtils.isOnline(worker.mContext) && !isCancelled()) {
                    // Now define what the artist name, album name, and url are.
                    String mArtistName = params[1];
                    String mAlbumName = params[2] != null ? params[2] : mArtistName;
                    String mUrl = worker.processImageUrl(mArtistName, mAlbumName, mImageType);
                    if (mUrl != null) {
                        bitmap = worker.processBitmap(mUrl);
                    }
                }

                // Fourth, add the new image to the cache
                if (bitmap != null && mKey != null && worker.mImageCache != null) {
                    worker.addBitmapToCache(mKey, bitmap);
                }

                // Add the second layer to the translation drawable
                if (bitmap != null) {
                    BitmapDrawable layerTwo = new BitmapDrawable(worker.mResources, bitmap);
                    layerTwo.setFilterBitmap(false);
                    layerTwo.setDither(false);
                    worker.mArrayDrawable[1] = layerTwo;
                    TransitionDrawable result = new TransitionDrawable(worker.mArrayDrawable);
                    result.setCrossFadeEnabled(true);
                    result.startTransition(FADE_IN_TIME);

                    Bitmap blur = BitmapUtils.createBlurredBitmap(bitmap);
                    BitmapDrawable layerBlur = new BitmapDrawable(worker.mResources, blur);

                    return new Drawable[]{result, layerBlur};
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(Drawable[] result) {
            ImageView[] imageviews = mImageReference.get();
            if (result != null && imageviews != null) {
                imageviews[0].setImageDrawable(result[0]);
                if (imageviews.length > 1) {
                    imageviews[1].setImageDrawable(result[1]);
                }
            }
        }
    }
}