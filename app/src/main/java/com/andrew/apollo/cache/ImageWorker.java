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
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.andrew.apollo.utils.BitmapUtils;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a {@link Bitmap} to an {@link ImageView}. It handles things like using a
 * memory and disk cache, running the work in a background thread and setting a
 * placeholder image.
 */
public abstract class ImageWorker {

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
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public static boolean executePotentialWork(String key, ImageView imageView) {
        if (imageView != null) {
            Object drawable = imageView.getTag();
            if (drawable instanceof ImageAsyncTag) {
                ImageAsyncTag asyncDrawable = (ImageAsyncTag) drawable;
                if (!asyncDrawable.getTag().equals(key)) {
                    // cancel worker to load a new image
                    asyncDrawable.cancel();
                } else {
                    // The same work is already in progress
                    return false;
                }
            }
        }
        return true;
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
     * @return reference to the image cache
     */
    @Nullable
    public ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * @return application context
     */
    public Context getContext() {
        return mContext;
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
                ImageAsyncTag asyncTag = new ImageAsyncTag(this, key, imageType, imageviews);
                imageviews[0].setTag(asyncTag);
                asyncTag.run(artistName, albumName, albumId);
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
    public abstract Bitmap processBitmap(String key);

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the URL needed to fetch the final {@link Bitmap}.
     *
     * @param artistName The artist name param used in the Last.fm API.
     * @param albumName  The album name param used in the Last.fm API.
     * @param imageType  The type of image URL to fetch for.
     * @return The image URL for an artist image or album image.
     */
    public abstract String processImageUrl(String artistName, String albumName, ImageType imageType);

    /**
     * Used to define what type of image URL to fetch for, artist or album.
     */
    public enum ImageType {
        ARTIST, ALBUM
    }
}