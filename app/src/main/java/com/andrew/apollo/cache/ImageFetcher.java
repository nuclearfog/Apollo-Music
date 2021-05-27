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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.andrew.apollo.Config;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.lastfm.Album;
import com.andrew.apollo.lastfm.Artist;
import com.andrew.apollo.lastfm.ImageSize;
import com.andrew.apollo.lastfm.MusicEntry;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher extends ImageWorker {
    /**
     *
     */
    public static final int IO_BUFFER_SIZE_BYTES = 1024;
    /**
     *
     */
    private static final int DEFAULT_MAX_IMAGE_HEIGHT = 1024;
    /**
     *
     */
    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;
    /**
     * size of the artist/album art of the notification image
     */
    private static final int NOTIFICATION_SIZE = 200;
    /**
     * location folder name of the image cache
     */
    private static final String DEFAULT_HTTP_CACHE_DIR = "http"; //$NON-NLS-1$

    private static final ImageSize[] QUALITY = {
            ImageSize.MEGA, ImageSize.EXTRALARGE, ImageSize.LARGE,
            ImageSize.MEDIUM, ImageSize.SMALL, ImageSize.UNKNOWN};

    private static ImageFetcher sInstance = null;

    /**
     * Creates a new instance of {@link ImageFetcher}.
     *
     * @param context The {@link Context} to use.
     */
    private ImageFetcher(Context context) {
        super(context);
    }

    /**
     * Used to create a singleton of the image fetcher
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static ImageFetcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }


    private static String getBestImage(MusicEntry e) {
        for (ImageSize q : QUALITY) {
            String url = e.getImageURL(q);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * Download a {@link Bitmap} from a URL, write it to a disk and return the
     * File pointer. This implementation uses a simple disk cache.
     *
     * @param context   The context to use
     * @param urlString The URL to fetch
     * @return A {@link File} pointing to the fetched bitmap
     */
    public static File downloadBitmapToFile(Context context, String urlString, String uniqueName) {
        File cacheDir = ImageCache.getDiskCacheDir(context, uniqueName);

        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        HttpsURLConnection urlConnection = null;
        BufferedOutputStream out = null;

        try {
            File tempFile = File.createTempFile("bitmap", null, cacheDir); //$NON-NLS-1$

            URL url = new URL(urlString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            if (urlConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                return null;
            }
            InputStream in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE_BYTES);
            out = new BufferedOutputStream(new FileOutputStream(tempFile), IO_BUFFER_SIZE_BYTES);

            int oneByte;
            while ((oneByte = in.read()) != -1) {
                out.write(oneByte);
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Decode and sample down a {@link Bitmap} from a file to the requested
     * width and height.
     *
     * @param filename The full path of the file to decode
     * @return A {@link Bitmap} sampled down from the original with the same
     * aspect ratio and dimensions that are equal to or greater than the
     * requested width and height
     */
    public static Bitmap decodeSampledBitmapFromFile(String filename) {

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, DEFAULT_MAX_IMAGE_WIDTH,
                DEFAULT_MAX_IMAGE_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    /**
     * Calculate an inSampleSize for use in a
     * {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This
     * implementation calculates the closest inSampleSize that will result in
     * the final decoded bitmap having a width and height equal to or larger
     * than the requested width and height. This implementation does not ensure
     * a power of 2 is returned for inSampleSize which can be faster when
     * decoding but results in a larger bitmap which isn't as useful for caching
     * purposes.
     *
     * @param options   An options object with out* params already populated (run
     *                  through a decode* method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        /* Raw height and width of image */
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            float totalPixels = width * height;

            /* More than 2x the requested pixels we'll sample down further */
            float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Generates key used by album art cache. It needs both album name and artist name
     * to let to select correct image for the case when there are two albums with the
     * same artist.
     *
     * @param albumName  The album name the cache key needs to be generated.
     * @param artistName The artist name the cache key needs to be generated.
     */
    @Nullable
    public static String generateAlbumCacheKey(String albumName, String artistName) {
        if (albumName == null || artistName == null) {
            return null;
        }
        String result = albumName + "_" + artistName + "_" + Config.ALBUM_ART_SUFFIX;
        // encode string to avoid illegal characters in filenames
        return Base64.encodeToString(result.getBytes(), Base64.DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Bitmap processBitmap(String url) {
        if (url == null) {
            return null;
        }
        File file = downloadBitmapToFile(mContext, url, DEFAULT_HTTP_CACHE_DIR);
        if (file != null) {
            // Return a sampled down version
            Bitmap bitmap = decodeSampledBitmapFromFile(file.toString());
            file.delete();
            return bitmap;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String processImageUrl(String artistName, String albumName, ImageType imageType) {
        switch (imageType) {
            case ARTIST:
                if (!TextUtils.isEmpty(artistName) && PreferenceUtils.getInstance(mContext).downloadMissingArtistImages()) {
                    Artist artist = Artist.getInfo(artistName);
                    if (artist != null) {
                        return getBestImage(artist);
                    }
                }
                break;

            case ALBUM:
                if (!TextUtils.isEmpty(artistName) && !TextUtils.isEmpty(albumName)
                        && PreferenceUtils.getInstance(mContext).downloadMissingArtwork()) {
                    Artist correction = Artist.getCorrection(artistName);
                    if (correction != null) {
                        Album album = Album.getInfo(correction.getName(), albumName);
                        if (album != null) {
                            return getBestImage(album);
                        }
                    }
                }
                break;
        }
        return null;
    }

    /**
     * Used to fetch album images.
     */
    public void loadAlbumImage(String artistName, String albumName, long albumId, ImageView imageView) {
        loadImage(generateAlbumCacheKey(albumName, artistName), artistName, albumName, albumId, imageView,
                ImageType.ALBUM);
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentArtwork(ImageView imageView) {
        loadImage(generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName()),
                MusicUtils.getArtistName(), MusicUtils.getAlbumName(), MusicUtils.getCurrentAlbumId(),
                imageView, ImageType.ALBUM);
    }

    /**
     * Used to fetch artist images.
     */
    public void loadArtistImage(String key, ImageView imageView) {
        // fixme last FM does not return artist images anymore so try to download an album artwork instead
        loadImage(key, key, null, -1, imageView, ImageType.ALBUM);
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(boolean pause) {
        if (mImageCache != null) {
            mImageCache.setPauseDiskCache(pause);
        }
    }

    /**
     * @param key The key used to find the image to remove
     */
    public void removeFromCache(String key) {
        if (mImageCache != null) {
            mImageCache.removeFromCache(key);
        }
    }

    /**
     * @param key The key used to find the image to return
     */
    public Bitmap getCachedBitmap(String key) {
        if (mImageCache != null) {
            return mImageCache.getCachedBitmap(key);
        }
        return getDefaultArtwork();
    }

    /**
     * @param keyAlbum  The key (album name) used to find the album art to return
     * @param keyArtist The key (artist name) used to find the album art to return
     */
    public Bitmap getCachedArtwork(String keyAlbum, String keyArtist) {
        long id = MusicUtils.getIdForAlbum(mContext, keyAlbum, keyArtist);
        return getCachedArtwork(keyAlbum, keyArtist, id);
    }

    /**
     * @param keyAlbum  The key (album name) used to find the album art to return
     * @param keyArtist The key (artist name) used to find the album art to return
     * @param keyId     The key (album id) used to find the album art to return
     */
    public Bitmap getCachedArtwork(String keyAlbum, String keyArtist, long keyId) {
        if (mImageCache != null) {
            String key = generateAlbumCacheKey(keyAlbum, keyArtist);
            return mImageCache.getCachedArtwork(mContext, key, keyId);
        }
        return getDefaultArtwork();
    }

    /**
     * Finds cached or downloads album art. Used in {@link MusicPlaybackService}
     * to set the current album art in the notification and lock screen
     *
     * @param albumName  The name of the current album
     * @param albumId    The ID of the current album
     * @param artistName The album artist in case we should have to download
     *                   missing artwork
     * @return The album art as an {@link Bitmap}
     */
    @Nullable
    public Bitmap getArtwork(String albumName, long albumId, String artistName) {
        // Check the disk cache
        Bitmap artwork = null;
        if (mImageCache != null) {
            if (albumName != null) {
                artwork = mImageCache.getBitmapFromDiskCache(generateAlbumCacheKey(albumName, artistName));
            }
            if (artwork == null && albumId >= 0) {
                // Check for local artwork
                artwork = mImageCache.getArtworkFromFile(mContext, albumId);
            }
        }
        if (artwork == null) {
            artwork = getDefaultArtwork();
        }
        if (artwork == null) {
            return null;
        }
        // scale down image
        return Bitmap.createScaledBitmap(artwork, NOTIFICATION_SIZE, NOTIFICATION_SIZE, false);
    }


    private Bitmap getDefaultArtwork() {
        Drawable bitmap = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.default_artwork, null);
        if (bitmap != null)
            return ((BitmapDrawable) bitmap).getBitmap();
        return null;
    }
}