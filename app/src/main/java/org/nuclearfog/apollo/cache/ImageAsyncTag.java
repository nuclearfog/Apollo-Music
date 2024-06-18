package org.nuclearfog.apollo.cache;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.worker.BitmapWorkerTask;

/**
 * A custom {@link android.view.View} Objet tag that will be attached to the
 * {@link ImageView} while the work is in progress. Contains a reference to
 * the actual worker task, so that it can be stopped if a new binding is
 * required, and makes sure that only the last started worker process can
 * bind its result, independently of the finish order.
 */
public class ImageAsyncTag implements AsyncCallback<Drawable[]> {

	/**
	 * background worker task
	 */
	private BitmapWorkerTask bitmapWorkerTask;

	private ImageView[] imageViews;

	/**
	 * key used to identify this tag
	 */
	private String mKey;

	/**
	 * Constructor of <code>AsyncDrawable</code>
	 */
	public ImageAsyncTag(ImageWorker imgWorker, @NonNull String mKey, ImageWorker.ImageType imageType, ImageView... imageViews) {
		bitmapWorkerTask = new BitmapWorkerTask(imgWorker, imageType);
		this.imageViews = imageViews;
		this.mKey = mKey;
	}


	@Override
	public void onResult(@NonNull Drawable[] drawables) {
		if (imageViews != null) {
			imageViews[0].setImageDrawable(drawables[0]);
			if (imageViews.length > 1) {
				imageViews[1].setImageDrawable(drawables[1]);
			}
		}
	}

	/**
	 * execute background task
	 */
	public void run(String artistName, String albumName, long albumId) {
		String[] param = {mKey, artistName, albumName, Long.toString(albumId)};
		bitmapWorkerTask.execute(param, this);
	}

	/**
	 * cancel worker task
	 */
	public void cancel() {
		bitmapWorkerTask.cancel();
	}

	/**
	 * @return unique tag key
	 */
	public String getTag() {
		return mKey;
	}
}