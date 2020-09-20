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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class holds the memory and disk bitmap caches.
 */
public final class ImageCache {

    private static final String TAG = ImageCache.class.getSimpleName();

    /**
     * The {@link Uri} used to retrieve album art
     */
    private static final Uri mArtworkUri;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float MEM_CACHE_DIVIDER = 0.25f;

    /**
     * Default disk cache size 10MB
     */
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10;

    /**
     * Compression settings when writing images to disk cache
     */
    private static final CompressFormat COMPRESS_FORMAT = CompressFormat.JPEG;

    /**
     * Disk cache index to read from
     */
    private static final int DISK_CACHE_INDEX = 0;

    /**
     * Image compression quality
     */
    private static final int COMPRESS_QUALITY = 98;
    private static ImageCache sInstance;

    static {
        mArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    private final Object mPauseLock = new Object();
    /**
     * Used to temporarily pause the disk cache while scrolling
     */
    public boolean mPauseDiskAccess = false;
    /**
     * LRU cache
     */
    private MemoryCache mLruCache;
    /**
     * Disk LRU cache
     */
    private DiskLruCache mDiskCache;

    /**
     * Constructor of <code>ImageCache</code>
     *
     * @param context The {@link Context} to use
     */
    public ImageCache(Context context) {
        init(context);
    }

    /**
     * Used to create a singleton of {@link ImageCache}
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static ImageCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageCache(context);
        }
        return sInstance;
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}
     * , if not found a new one is created using the supplied params and saved
     * to a {@link RetainFragment}
     *
     * @param activity The calling {@link androidx.fragment.app.FragmentActivity}
     * @return An existing retained ImageCache object or a new one if one did
     * not exist
     */
    public static ImageCache findOrCreateCache(FragmentActivity activity) {

        // Search for, or create an instance of the non-UI RetainFragment
        RetainFragment retainFragment = findOrCreateRetainFragment(activity.getSupportFragmentManager());

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache cache = (ImageCache) retainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (cache == null) {
            cache = getInstance(activity);
            retainFragment.setObject(cache);
        }
        return cache;
    }

    /**
     * Locate an existing instance of this {@link Fragment} or if not found,
     * create and add it using {@link FragmentManager}
     *
     * @param fm The {@link FragmentManager} to use
     * @return The existing instance of the {@link Fragment} or the new instance
     * if just created
     */
    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        // Check to see if we have retained the worker fragment
        RetainFragment retainFragment = (RetainFragment) fm.findFragmentByTag(TAG);

        // If not retained, we need to create and add it
        if (retainFragment == null) {
            retainFragment = new RetainFragment();
            fm.beginTransaction().add(retainFragment, TAG).commit();
        }
        return retainFragment;
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param context    The {@link Context} to use
     * @param uniqueName A unique directory name to append to the cache
     *                   directory
     * @return The cache directory
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // getExternalCacheDir(context) returns null if external storage is not ready
        String cachePath = getExternalCacheDir(context) != null
                ? getExternalCacheDir(context).getPath()
                : context.getCacheDir().getPath();
        return new File(cachePath, uniqueName);
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link Context} to use
     * @return The external cache directory
     */
    public static File getExternalCacheDir(Context context) {
        return context.getExternalCacheDir();
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    public static long getUsableSpace(File path) {
        return path.getUsableSpace();
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     *
     * @param key The key used to store the file
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    /**
     * http://stackoverflow.com/questions/332079
     *
     * @param bytes The bytes to convert.
     * @return A {@link String} converted from the bytes of a hashable key used
     * to store a filename on the disk, to hex digits.
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    /**
     * Initialize the cache, providing all parameters.
     *
     * @param context The {@link Context} to use
     */
    private void init(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Initialize the disk cache in a background thread
                    initDiskCache(context);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }).start();
        // Set up the memory cache
        initLruCache(context);
    }

    /**
     * Initializes the disk cache. Note that this includes disk access so this
     * should not be executed on the main/UI thread. By default an ImageCache
     * does not initialize the disk cache when it is created, instead you should
     * call initDiskCache() to initialize it on a background thread.
     *
     * @param context The {@link Context} to use
     */
    private synchronized void initDiskCache(Context context) {
        // Set up disk cache
        if (mDiskCache == null || mDiskCache.isClosed()) {
            File diskCacheDir = getDiskCacheDir(context, TAG);
            if (!diskCacheDir.exists()) {
                diskCacheDir.mkdirs();
            }
            if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
                try {
                    mDiskCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets up the Lru cache
     *
     * @param context The {@link Context} to use
     */
    @SuppressLint("NewApi")
    public void initLruCache(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int lruCacheSize = Math.round(MEM_CACHE_DIVIDER * activityManager.getMemoryClass() * 1024 * 1024);
        mLruCache = new MemoryCache(lruCacheSize);
        // Release some memory as needed
        context.registerComponentCallbacks(new ComponentCallbacks2() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void onTrimMemory(int level) {
                if (level >= TRIM_MEMORY_MODERATE) {
                    evictAll();
                } else if (level >= TRIM_MEMORY_BACKGROUND) {
                    mLruCache.trimToSize(mLruCache.size() / 2);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onLowMemory() {
                // Nothing to do
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {
                // Nothing to do
            }
        });
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param data   The key used to store the image
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }

        // Add to memory cache
        addBitmapToMemCache(data, bitmap);

        // Add to disk cache
        if (mDiskCache != null) {
            String key = hashKeyForDisk(data);
            OutputStream out = null;
            try {
                DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
                if (snapshot == null) {
                    DiskLruCache.Editor editor = mDiskCache.edit(key);
                    if (editor != null) {
                        out = editor.newOutputStream(DISK_CACHE_INDEX);
                        bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, out);
                        editor.commit();
                        out.close();
                        flush();
                    }
                } else {
                    snapshot.getInputStream(DISK_CACHE_INDEX).close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "addBitmapToCache - " + e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "addBitmapToCache - " + e);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Log.e(TAG, "addBitmapToCache - " + e);
                }
            }
        }
    }

    /**
     * Called to add a new image to the memory cache
     *
     * @param data   The key identifier
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToMemCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }
        // Add to memory cache
        if (getBitmapFromMemCache(data) == null) {
            mLruCache.put(data, bitmap);
        }
    }

    /**
     * Fetches a cached image from the memory cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public Bitmap getBitmapFromMemCache(String data) {
        if (data == null) {
            return null;
        }
        if (mLruCache != null) {
            return mLruCache.get(data);
        }
        return null;
    }

    /**
     * Fetches a cached image from the disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public Bitmap getBitmapFromDiskCache(String data) {
        if (data == null) {
            return null;
        }

        // Check in the memory cache here to avoid going to the disk cache less
        // often
        if (getBitmapFromMemCache(data) != null) {
            return getBitmapFromMemCache(data);
        }

        waitUntilUnpaused();
        String key = hashKeyForDisk(data);
        if (mDiskCache != null) {
            InputStream inputStream = null;
            try {
                DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
                if (snapshot != null) {
                    inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            return bitmap;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getBitmapFromDiskCache - " + e);
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Tries to return a cached image from memory cache before fetching from the
     * disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public Bitmap getCachedBitmap(String data) {
        if (data == null) {
            return null;
        }
        Bitmap cachedImage = getBitmapFromMemCache(data);
        if (cachedImage == null) {
            cachedImage = getBitmapFromDiskCache(data);
        }
        if (cachedImage != null) {
            addBitmapToMemCache(data, cachedImage);
            return cachedImage;
        }
        return null;
    }

    /**
     * Tries to return the album art from memory cache and disk cache, before
     * calling {@code #getArtworkFromFile(Context, String)} again
     *
     * @param context The {@link Context} to use
     * @param data    The name of the album art
     * @param id      The ID of the album to find artwork for
     * @return The artwork for an album
     */
    public Bitmap getCachedArtwork(Context context, String data, long id) {
        if (context == null || data == null) {
            return null;
        }
        Bitmap cachedImage = getCachedBitmap(data);
        if (cachedImage == null && id >= 0) {
            cachedImage = getArtworkFromFile(context, id);
        }
        if (cachedImage != null) {
            addBitmapToMemCache(data, cachedImage);
            return cachedImage;
        }
        return null;
    }

    /**
     * Used to fetch the artwork for an album locally from the user's device
     *
     * @param context The {@link Context} to use
     * @return The artwork for an album
     */
    public Bitmap getArtworkFromFile(Context context, long albumId) {
        if (albumId < 0) {
            return null;
        }
        Bitmap artwork = null;
        waitUntilUnpaused();
        try {
            Uri uri = ContentUris.withAppendedId(mArtworkUri, albumId);

            ParcelFileDescriptor fileDescr = context.getContentResolver().openFileDescriptor(uri, "r");
            if (fileDescr != null) {
                FileDescriptor fileDescriptor = fileDescr.getFileDescriptor();
                artwork = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (IllegalStateException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (OutOfMemoryError e) {
            //e.printStackTrace();
            evictAll();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return artwork;
    }

    /**
     * flush() is called to synchronize up other methods that are accessing the
     * cache first
     */
    public void flush() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mDiskCache != null) {
                    try {
                        if (!mDiskCache.isClosed()) {
                            mDiskCache.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "flush - " + e);
                    }
                }
            }
        }).start();
    }

    /**
     * Clears the disk and memory caches
     */
    public void clearCaches() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Clear the disk cache
                try {
                    if (mDiskCache != null) {
                        mDiskCache.delete();
                        mDiskCache = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "clearCaches - " + e);
                }
                // Clear the memory cache
                evictAll();
            }
        }).start();
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void close() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mDiskCache != null) {
                    try {
                        if (!mDiskCache.isClosed()) {
                            mDiskCache.close();
                            mDiskCache = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "close - " + e);
                    }
                }
            }
        }).start();
    }

    /**
     * Evicts all of the items from the memory cache and lets the system know
     * now would be a good time to garbage collect
     */
    public void evictAll() {
        if (mLruCache != null) {
            mLruCache.evictAll();
        }
        System.gc();
    }

    /**
     * @param key The key used to identify which cache entries to delete.
     */
    public void removeFromCache(String key) {
        if (key == null) {
            return;
        }
        // Remove the Lru entry
        if (mLruCache != null) {
            mLruCache.remove(key);
        }

        try {
            // Remove the disk entry
            if (mDiskCache != null) {
                mDiskCache.remove(hashKeyForDisk(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "remove - " + e);
        }
        flush();
    }

    /**
     * Used to temporarily pause the disk cache while the user is scrolling to
     * improve scrolling.
     *
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(boolean pause) {
        synchronized (mPauseLock) {
            if (mPauseDiskAccess != pause) {
                mPauseDiskAccess = pause;
                if (!pause) {
                    mPauseLock.notify();
                }
            }
        }
    }

    private void waitUntilUnpaused() {
        synchronized (mPauseLock) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                while (mPauseDiskAccess) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        // ignored, we'll start waiting again
                    }
                }
            }
        }
    }

    /**
     * @return True if the user is scrolling, false otherwise.
     */
    public boolean isDiskCachePaused() {
        return mPauseDiskAccess;
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over
     * configuration changes. In this sample it will be used to retain an
     * {@link ImageCache} object.
     */
    public static class RetainFragment extends Fragment {

        /**
         * The object to be stored
         */
        private Object mObject;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Get the stored object
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }

        /**
         * Store a single object in this {@link Fragment}
         *
         * @param object The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }
    }

    /**
     * Used to cache images via {@link LruCache}.
     */
    public static class MemoryCache extends LruCache<String, Bitmap> {

        /**
         * Constructor of <code>MemoryCache</code>
         *
         * @param maxSize The allowed size of the {@link LruCache}
         */
        public MemoryCache(int maxSize) {
            super(maxSize);
        }

        /**
         * Get the size in bytes of a bitmap.
         */
        public static int getBitmapSize(Bitmap bitmap) {
            return bitmap.getByteCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int sizeOf(String paramString, Bitmap paramBitmap) {
            return getBitmapSize(paramBitmap);
        }
    }
}