package com.andrew.apollo.cache;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.BitmapUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

/**
 * A custom {@link android.view.View} Objet tag that will be attached to the
 * {@link ImageView} while the work is in progress. Contains a reference to
 * the actual worker task, so that it can be stopped if a new binding is
 * required, and makes sure that only the last started worker process can
 * bind its result, independently of the finish order.
 */
public class ImageAsyncTag {

	/**
	 * Default transition drawable fade time
	 */
	private static final int FADE_IN_TIME = 200;

	/**
	 * background worker task
	 */
	private BitmapWorkerTask bitmapWorkerTask;

	/**
	 * key used to identify this tag
	 */
	private String mKey;

	/**
	 * Constructor of <code>AsyncDrawable</code>
	 */
	public ImageAsyncTag(ImageWorker imgWorker, @NonNull String mKey, ImageWorker.ImageType imageType, ImageView... imageViews) {
		bitmapWorkerTask = new ImageAsyncTag.BitmapWorkerTask(imgWorker, imageType, imageViews);
		this.mKey = mKey;
	}

	/**
	 * execute background task
	 */
	public void run(String artistName, String albumName, long albumId) {
		try {
			bitmapWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mKey, artistName, albumName, Long.toString(albumId));
		} catch (RejectedExecutionException e) {
			// Executor has exhausted queue space, show default artwork
			e.printStackTrace();
		}
	}

	/**
	 * cancel worker task
	 */
	public void cancel() {
		bitmapWorkerTask.cancel(true);
	}

	/**
	 * @return unique tag key
	 */
	public String getTag() {
		return mKey;
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
		private ImageWorker.ImageType mImageType;

		/**
		 * Constructor of <code>BitmapWorkerTask</code>
		 *
		 * @param imageView  The {@link ImageView} to use.
		 * @param mImageType The type of image URL to fetch for.
		 */
		private BitmapWorkerTask(ImageWorker imageWorker, ImageWorker.ImageType mImageType, ImageView[] imageView) {
			super();
			callback = new WeakReference<>(imageWorker);
			mImageReference = new WeakReference<>(imageView);
			imageView[0].setBackgroundResource(R.drawable.default_artwork);
			this.mImageType = mImageType;
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