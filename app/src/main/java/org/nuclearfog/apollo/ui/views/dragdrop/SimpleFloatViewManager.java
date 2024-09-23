package org.nuclearfog.apollo.ui.views.dragdrop;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Simple implementation of the FloatViewManager class. Uses list items as they
 * appear in the ListView to create the floating View.
 */
public class SimpleFloatViewManager {

	private ListView mListView;
	private Bitmap mFloatBitmap;

	private int mFloatBGColor = Color.BLACK;

	/**
	 *
	 */
	public SimpleFloatViewManager(ListView lv) {
		mListView = lv;
	}

	/**
	 *
	 */
	public void setBackgroundColor(int color) {
		mFloatBGColor = color;
	}

	/**
	 * This simple implementation creates a Bitmap copy of the list item
	 * currently shown at ListView <code>position</code>.
	 */
	public View onCreateFloatView(int position) {
		View child = mListView.getChildAt(position + mListView.getHeaderViewsCount() - mListView.getFirstVisiblePosition());
		if (child != null) {
			child.setPressed(false);
			child.setDrawingCacheEnabled(true);
			mFloatBitmap = Bitmap.createBitmap(child.getDrawingCache());
			child.setDrawingCacheEnabled(false);

			ImageView iv = new ImageView(mListView.getContext());
			iv.setBackgroundColor(mFloatBGColor);
			iv.setPadding(0, 0, 0, 0);
			iv.setImageBitmap(mFloatBitmap);
			return iv;
		}
		return null;
	}

	/**
	 * Removes the Bitmap from the ImageView created in onCreateFloatView() and
	 * tells the system to recycle it.
	 */
	protected void onDestroyFloatView(View floatView) {
		if (floatView instanceof ImageView)
			((ImageView) floatView).setImageDrawable(null);
		mFloatBitmap.recycle();
		mFloatBitmap = null;
	}
}