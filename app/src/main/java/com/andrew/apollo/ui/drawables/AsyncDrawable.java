package com.andrew.apollo.ui.drawables;

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
import com.andrew.apollo.cache.ImageWorker;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.BitmapUtils;

import java.lang.ref.WeakReference;

/**
 * A custom {@link BitmapDrawable} that will be attached to the
 * {@link ImageView} while the work is in progress. Contains a reference to
 * the actual worker task, so that it can be stopped if a new binding is
 * required, and makes sure that only the last started worker process can
 * bind its result, independently of the finish order.
 */
public class AsyncDrawable extends ColorDrawable {

    /**
     * Default transition drawable fade time
     */
    private static final int FADE_IN_TIME = 200;

    private BitmapWorkerTask mBitmapWorkerTask;

    /**
     * Constructor of <code>AsyncDrawable</code>
     */
    public AsyncDrawable(BitmapWorkerTask mBitmapWorkerTask) {
        super(Color.TRANSPARENT);
        this.mBitmapWorkerTask = mBitmapWorkerTask;
    }

    /**
     * @return The {@link BitmapWorkerTask} associated with this drawable
     */
    @Nullable
    public BitmapWorkerTask getBitmapWorkerTask() {
        return mBitmapWorkerTask;
    }

    /**
     * The actual {@link AsyncTask} that will process the image.
     */
    public static class BitmapWorkerTask extends AsyncTask<String, Void, Drawable[]> {

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
        private ImageWorker.ImageType mImageType;

        /**
         * The key used to store cached entries
         */
        public final String tag;

        /**
         * Constructor of <code>BitmapWorkerTask</code>
         *
         * @param imageView  The {@link ImageView} to use.
         * @param mImageType The type of image URL to fetch for.
         */
        public BitmapWorkerTask(ImageWorker imageWorker, String tag, ImageWorker.ImageType mImageType, ImageView[] imageView) {
            super();
            callback = new WeakReference<>(imageWorker);
            mImageReference = new WeakReference<>(imageView);
            imageView[0].setBackgroundResource(R.drawable.default_artwork);
            this.mImageType = mImageType;
            this.tag = tag;
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
                String mKey = params[0];

                // The result
                Bitmap bitmap = null;

                // First, check the disk cache for the image
                if (mKey != null && worker.getImageCache() != null && !isCancelled()) {
                    bitmap = worker.getImageCache().getCachedBitmap(mKey);
                }

                // Define the album id now
                long mAlbumId = Long.parseLong(params[3]);

                // Second, if we're fetching artwork, check the device for the image
                if (bitmap == null && mAlbumId >= 0 && mKey != null && !isCancelled() && worker.getImageCache() != null) {
                    bitmap = worker.getImageCache().getCachedArtwork(worker.getContext(), mKey, mAlbumId);
                }

                // Third, by now we need to download the image
                if (bitmap == null && ApolloUtils.isOnline(worker.getContext()) && !isCancelled()) {
                    // Now define what the artist name, album name, and url are.
                    String mArtistName = params[1];
                    String mAlbumName = params[2] != null ? params[2] : mArtistName;
                    String mUrl = worker.processImageUrl(mArtistName, mAlbumName, mImageType);
                    if (mUrl != null) {
                        bitmap = worker.processBitmap(mUrl);
                    }
                }

                // Fourth, add the new image to the cache
                if (bitmap != null && mKey != null && worker.getImageCache() != null) {
                    worker.addBitmapToCache(mKey, bitmap);
                }

                // Add the second layer to the translation drawable
                if (bitmap != null) {
                    Drawable layerOne = new ColorDrawable(worker.getContext().getResources().getColor(R.color.transparent));
                    BitmapDrawable layerTwo = new BitmapDrawable(worker.getContext().getResources(), bitmap);
                    layerTwo.setFilterBitmap(false);
                    layerTwo.setDither(false);
                    TransitionDrawable result = new TransitionDrawable(new Drawable[]{layerOne, layerTwo});
                    result.setCrossFadeEnabled(true);
                    result.startTransition(FADE_IN_TIME);

                    Bitmap blur = BitmapUtils.createBlurredBitmap(bitmap);
                    BitmapDrawable layerBlur = new BitmapDrawable(worker.getContext().getResources(), blur);
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