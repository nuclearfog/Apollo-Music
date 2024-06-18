package org.nuclearfog.apollo.async.worker;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.cache.ImageWorker;
import org.nuclearfog.apollo.cache.ImageWorker.ImageType;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.BitmapUtils;

import java.lang.ref.WeakReference;

/**
 * Async worker to download image artworks
 *
 * @author nuclearfog
 */
public class BitmapWorkerTask extends AsyncExecutor<String[], Drawable[]> {

	/**
	 * Default transition drawable fade time
	 */
	private static final int FADE_IN_TIME = 200;

	private WeakReference<ImageWorker> callback;
	private ImageType mImageType;


	public BitmapWorkerTask(ImageWorker worker, ImageType mImageType) {
		super(null);
		callback = new WeakReference<>(worker);
		this.mImageType = mImageType;
	}


	@Override
	protected Drawable[] doInBackground(String[] params) {
		ImageWorker worker = callback.get();
		// The result
		Bitmap bitmap = null;
		if (worker == null)
			return null;
		// First, check the disk cache for the image
		if (params[0] != null && worker.getImageCache() != null) {
			bitmap = worker.getImageCache().getCachedBitmap(params[0]);
		}
		// Define the album id now
		long mAlbumId = Long.parseLong(params[3]);
		// Second, if we're fetching artwork, check the device for the image
		if (bitmap == null && mAlbumId >= 0 && params[0] != null && worker.getImageCache() != null) {
			bitmap = worker.getImageCache().getCachedArtwork(worker.getContext(), params[0], mAlbumId);
		}
		// Third, by now we need to download the image
		if (bitmap == null && ApolloUtils.isOnline(worker.getContext())) {
			// Now define what the artist name, album name, and url are.
			String mArtistName = params[1];
			String mAlbumName = params[2] != null ? params[2] : mArtistName;
			String mUrl = worker.processImageUrl(mArtistName, mAlbumName, mImageType);
			if (mUrl != null) {
				bitmap = worker.processBitmap(mUrl);
			}
		}
		// Fourth, add the new image to the cache
		if (bitmap != null && params[0] != null && worker.getImageCache() != null) {
			worker.addBitmapToCache(params[0], bitmap);
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
		return null;
	}
}