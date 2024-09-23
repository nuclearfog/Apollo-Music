package org.nuclearfog.apollo.ui.views.dragdrop;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

/**
 * Class that starts and stops item drags on a {@link DragSortListView} based on
 * touch gestures. This class also inherits from {@link SimpleFloatViewManager},
 * which provides basic float View creation. An instance of this class is meant
 * to be passed to the methods  and of your {@link DragSortListView} instance.
 */
public class DragSortController extends SimpleFloatViewManager implements View.OnTouchListener, GestureDetector.OnGestureListener {

	public final static int ON_DOWN = 0;
	public final static int ON_DRAG = 1;
	public final static int ON_LONG_PRESS = 2;
	public final static int FLING_RIGHT_REMOVE = 0;
	public final static int FLING_LEFT_REMOVE = 1;
	public final static int SLIDE_RIGHT_REMOVE = 2;
	public final static int SLIDE_LEFT_REMOVE = 3;
	public final static int MISS = -1;

	private GestureDetector mDetector;
	private GestureDetector mFlingRemoveDetector;
	private DragSortListView mDslv;

	private float mOrigFloatAlpha;
	private int mTouchSlop;
	private int mRemoveMode;
	private int mItemX;
	private int mItemY;
	private int mCurrX;
	private int mCurrY;
	private int mDragHandleId;
	private int mDragInitMode;

	private int[] mTempLoc = new int[2];
	private int mHitPos = MISS;
	private boolean mSortEnabled = true;
	private boolean mRemoveEnabled = false;
	private boolean mDragging = false;

	/**
	 * By default, sorting is enabled, and removal is disabled.
	 *
	 * @param listView     The DSLV instance
	 * @param dragHandleId The resource id of the View that represents the drag handle in a list item.
	 */
	public DragSortController(@NonNull DragSortListView listView, int dragHandleId, int dragInitMode, int removeMode) {
		super(listView);
		mDslv = listView;
		mDetector = new GestureDetector(listView.getContext(), this);
		mFlingRemoveDetector = new GestureDetector(listView.getContext(), this);
		mTouchSlop = ViewConfiguration.get(listView.getContext()).getScaledTouchSlop();
		mDragHandleId = dragHandleId;
		mRemoveMode = removeMode;
		mDragInitMode = dragInitMode;

		mFlingRemoveDetector.setIsLongpressEnabled(false);
		mOrigFloatAlpha = listView.getFloatAlpha();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		mDetector.onTouchEvent(ev);
		if (mRemoveEnabled && mDragging && (mRemoveMode == FLING_RIGHT_REMOVE || mRemoveMode == FLING_LEFT_REMOVE)) {
			mFlingRemoveDetector.onTouchEvent(ev);
		}
		int mAction = ev.getAction() & MotionEvent.ACTION_MASK;
		switch (mAction) {
			case MotionEvent.ACTION_DOWN:
				mCurrX = (int) ev.getX();
				mCurrY = (int) ev.getY();
				break;

			case MotionEvent.ACTION_UP:
				if (mRemoveEnabled) {
					int x = (int) ev.getX();
					int thirdW = mDslv.getWidth() / 3;
					int twoThirdW = mDslv.getWidth() - thirdW;
					if ((mRemoveMode == SLIDE_RIGHT_REMOVE && x > twoThirdW)
							|| (mRemoveMode == SLIDE_LEFT_REMOVE && x < thirdW)) {
						mDslv.stopDrag(true);
					}
				}

			case MotionEvent.ACTION_CANCEL:
				mDragging = false;
				break;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onDown(MotionEvent ev) {
		mHitPos = dragHandleHitPosition(ev);
		if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
			startDrag(mHitPos, (int) ev.getX() - mItemX, (int) ev.getY() - mItemY);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (mHitPos != MISS && mDragInitMode == ON_DRAG && !mDragging) {
			int x1 = (int) e1.getX();
			int y1 = (int) e1.getY();
			int x2 = (int) e2.getX();
			int y2 = (int) e2.getY();
			boolean start = false;
			if (mRemoveEnabled && mSortEnabled) {
				start = true;
			} else if (mRemoveEnabled) {
				start = Math.abs(x2 - x1) > mTouchSlop;
			} else if (mSortEnabled) {
				start = Math.abs(y2 - y1) > mTouchSlop;
			}
			if (start) {
				startDrag(mHitPos, x2 - mItemX, y2 - mItemY);
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLongPress(MotionEvent e) {
		if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
			mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (mRemoveEnabled) {
			float mFlingSpeed = 500f;
			switch (mRemoveMode) {
				case FLING_RIGHT_REMOVE:
					if (velocityX > mFlingSpeed) {
						mDslv.stopDrag(true);
					}
					break;

				case FLING_LEFT_REMOVE:
					if (velocityX < -mFlingSpeed) {
						mDslv.stopDrag(true);
					}
					break;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent ev) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onShowPress(MotionEvent ev) {
	}

	/**
	 * Enable/Disable list item sorting. Disabling is useful if only item
	 * removal is desired. Prevents drags in the vertical direction.
	 *
	 * @param enabled Set <code>true</code> to enable list item sorting.
	 */
	public void setSortEnabled(boolean enabled) {
		mSortEnabled = enabled;
	}

	/**
	 * Enable/Disable item removal without affecting remove mode.
	 */
	public void setRemoveEnabled(boolean enabled) {
		mRemoveEnabled = enabled;
	}

	/**
	 * Overrides to provide fading when slide removal is enabled.
	 */
	public void onDragFloatView(Point touch) {
		if (mRemoveEnabled) {
			int x = touch.x;
			if (mRemoveMode == SLIDE_RIGHT_REMOVE) {
				int width = mDslv.getWidth();
				int thirdWidth = width / 3;
				float alpha;
				if (x < thirdWidth) {
					alpha = 1.0f;
				} else if (x < width - thirdWidth) {
					alpha = (width - thirdWidth - x) / ((float) thirdWidth);
				} else {
					alpha = 0.0f;
				}
				mDslv.setFloatAlpha(mOrigFloatAlpha * alpha);
			} else if (mRemoveMode == SLIDE_LEFT_REMOVE) {
				int width = mDslv.getWidth();
				int thirdWidth = width / 3;
				float alpha;
				if (x < thirdWidth) {
					alpha = 0.0f;
				} else if (x < width - thirdWidth) {
					alpha = (x - thirdWidth) / ((float) thirdWidth);
				} else {
					alpha = 1.0f;
				}
				mDslv.setFloatAlpha(mOrigFloatAlpha * alpha);
			}
		}
	}

	/**
	 * Checks for the touch of an item's drag handle specified by
	 * setDragHandleId(int), and returns that item's position if a
	 * drag handle touch was detected.
	 *
	 * @param ev The ACTION_DOWN MotionEvent.
	 * @return The list position of the item whose drag handle was touched; MISS if unsuccessful.
	 */
	private int dragHandleHitPosition(MotionEvent ev) {
		int x = (int) ev.getX();
		int y = (int) ev.getY();

		int touchPos = mDslv.pointToPosition(x, y);
		int numHeaders = mDslv.getHeaderViewsCount();
		int numFooters = mDslv.getFooterViewsCount();
		int count = mDslv.getCount();

		if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders && touchPos < (count - numFooters)) {
			View item = mDslv.getChildAt(touchPos - mDslv.getFirstVisiblePosition());
			int rawX = (int) ev.getRawX();
			int rawY = (int) ev.getRawY();
			View dragBox = item.findViewById(mDragHandleId);
			if (dragBox != null) {
				dragBox.getLocationOnScreen(mTempLoc);
				if (rawX > mTempLoc[0] && rawY > mTempLoc[1]
						&& rawX < mTempLoc[0] + dragBox.getWidth()
						&& rawY < mTempLoc[1] + dragBox.getHeight()) {
					mItemX = item.getLeft();
					mItemY = item.getTop();
					return touchPos;
				}
			}
		}
		return MISS;
	}

	/**
	 * Sets flags to restrict certain motions of the floating View based on
	 * DragSortController settings (such as remove mode). Starts the drag on the DragSortListView.
	 *
	 * @param position The list item position (includes headers).
	 * @param deltaX   Touch x-coord minus left edge of floating View.
	 * @param deltaY   Touch y-coord minus top edge of floating View.
	 */
	private void startDrag(int position, int deltaX, int deltaY) {
		int mDragFlags = 0;
		if (mSortEnabled) {
			mDragFlags |= DragSortListView.DRAG_POS_Y | DragSortListView.DRAG_NEG_Y;
		}
		if (mRemoveEnabled) {
			if (mRemoveMode == FLING_RIGHT_REMOVE) {
				mDragFlags |= DragSortListView.DRAG_POS_X;
			} else if (mRemoveMode == FLING_LEFT_REMOVE) {
				mDragFlags |= DragSortListView.DRAG_NEG_X;
			}
		}
		mDragging = mDslv.startDrag(position - mDslv.getHeaderViewsCount(), mDragFlags, deltaX, deltaY);
	}
}