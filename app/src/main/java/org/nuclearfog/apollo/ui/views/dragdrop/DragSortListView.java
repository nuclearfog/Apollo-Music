/*
 * DragSortListView. A subclass of the Android ListView component that enables
 * drag and drop re-ordering of list items. Copyright 2012 Carl Bauer Licensed
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.ui.views.dragdrop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.lang.ref.WeakReference;

/**
 * ListView subclass that mediates drag and drop resorting of items.
 *
 * @author heycosmo
 */
public class DragSortListView extends ListView implements OnScrollListener {

	/**
	 * Drag flag bit. Floating View can move in the positive x direction.
	 */
	public final static int DRAG_POS_X = 0x1;
	/**
	 * Drag flag bit. Floating View can move in the negative x direction.
	 */
	public final static int DRAG_NEG_X = 0x2;
	/**
	 * Drag flag bit. Floating View can move in the positive y direction. This
	 * is subtle. What this actually means is that, if enabled, the floating
	 * View can be dragged below its starting position. Remove in favor of
	 * upper-bounding item position?
	 */
	public final static int DRAG_POS_Y = 0x4;
	/**
	 * Drag flag bit. Floating View can move in the negative y direction. This
	 * is subtle. What this actually means is that the floating View can be
	 * dragged above its starting position. Remove in favor of lower-bounding
	 * item position?
	 */
	public final static int DRAG_NEG_Y = 0x8;
	public final static int STOP = -1;
	public final static int UP = 0;
	public final static int DOWN = 1;
	/**
	 * Drag state enum.
	 */
	private final static int IDLE = 0x690DF8E8;
	private final static int STOPPED = 0x695CFF61;
	private final static int DRAGGING = 0x86B75FE;
	/**
	 * Enum telling where to cancel the ListView action when a drag-sort begins
	 */
	private static final int NO_CANCEL = 0xCF12F49;
	private static final int ON_TOUCH_EVENT = 0xA462CF1D;
	private static final int ON_INTERCEPT_TOUCH_EVENT = 0x9B5F98B2;
	/**
	 * Transparency for the floating View (XML attribute).
	 */
	private static final float M_FLOAT_ALPHA = 1.0f;
	/**
	 * Determines when a slide shuffle animation starts. That is, defines how
	 * close to the edge of the drop slot the floating View must be to initiate
	 * the slide.
	 */
	private static final float SLIDE_REGION_FRAC = 0.75f;
	/**
	 * A proposed float View location based on touch location and given deltaX
	 * and deltaY.
	 */
	private Point mFloatLoc = new Point();
	/**
	 * handler used for post() operations
	 */
	private DragSortHandler handler;
	/**
	 * Given to ListView to cancel its action when a drag-sort begins.
	 */
	private MotionEvent mCancelEvent;
	/**
	 * The View that floats above the ListView and represents the dragged item.
	 */
	@Nullable
	private View mFloatView;
	/**
	 * Sample Views ultimately used for calculating the height of ListView items
	 * that are off-screen.
	 */
	private View[] mSampleViewTypes = new View[1];
	/**
	 * The middle (in the y-direction) of the floating View.
	 */
	private int mFloatViewMid;
	/**
	 * Left edge of floating View.
	 */
	private int mFloatViewLeft;
	/**
	 * Top edge of floating View.
	 */
	private int mFloatViewTop;
	/**
	 * While drag-sorting, the current position of the floating View. If
	 * dropped, the dragged item will land in this position.
	 */
	private int mFloatPos;
	/**
	 * The amount to scroll during the next layout pass. Used only for
	 * drag-scrolling, not standard ListView scrolling.
	 */
	private int mScrollY = 0;
	/**
	 * The first expanded ListView position that helps represent the drop slot
	 * tracking the floating View.
	 */
	private int mFirstExpPos;
	/**
	 * The second expanded ListView position that helps represent the drop slot
	 * tracking the floating View. This can equal mFirstExpPos if there is no
	 * slide shuffle occurring; otherwise it is equal to mFirstExpPos + 1.
	 */
	private int mSecondExpPos;
	/**
	 * Flag set if slide shuffling is enabled.
	 */
	private boolean mAnimate = true;
	/**
	 * The user dragged from this position.
	 */
	private int mSrcPos;
	/**
	 * Offset (in x) within the dragged item at which the user picked it up (or
	 * first touched down with the digitalis).
	 */
	private int mDragDeltaX;
	/**
	 * Offset (in y) within the dragged item at which the user picked it up (or
	 * first touched down with the digitalis).
	 */
	private int mDragDeltaY;

	/**
	 * Enable/Disable item dragging
	 */
	private boolean mDragEnabled = true;
	/**
	 * Height in pixels to which the originally dragged item is collapsed during
	 * a drag-sort. Currently, this value must be greater than zero.
	 */
	private int mItemHeightCollapsed = 1;
	/**
	 * Height of the floating View. Stored for the purpose of providing the
	 * tracking drop slot.
	 */
	private int mFloatViewHeight;
	/**
	 * Convenience member. See above.
	 */
	private int mFloatViewHeightHalf;
	/**
	 * Save the given width spec for use in measuring children
	 */
	private int mWidthMeasureSpec = 0;
	/**
	 * Determines the start of the upward drag-scroll region at the top of the
	 * ListView. Specified by a fraction of the ListView height, thus screen
	 * resolution agnostic.
	 */
	private float mDragUpScrollStartFrac = 1.0f / 3.0f;
	/**
	 * Determines the start of the downward drag-scroll region at the bottom of
	 * the ListView. Specified by a fraction of the ListView height, thus screen
	 * resolution agnostic.
	 */
	private float mDragDownScrollStartFrac = 1.0f / 3.0f;
	/**
	 * The following are calculated from the above fracs.
	 */
	private int mUpScrollStartY;
	/**
	 * Calculated from above above and current ListView height.
	 */
	private float mDragUpScrollHeight;
	/**
	 * Calculated from above above and current ListView height.
	 */
	private float mDragDownScrollHeight;
	/**
	 * Current touch x.
	 */
	private int mX;
	/**
	 * Current touch y.
	 */
	private int mY;
	/**
	 * Last touch y.
	 */
	private int mLastY;
	/**
	 * Flags that determine limits on the motion of the floating View. See flags
	 * above.
	 */
	private int mDragFlags = 0;
	/**
	 * Last call to an on*TouchEvent was a call to onInterceptTouchEvent.
	 */
	private boolean mLastCallWasIntercept = false;
	/**
	 * A touch event is in progress.
	 */
	private boolean mInTouchEvent = false;
	/**
	 * A listener that receives a callback when the floating View is dropped.
	 */
	@Nullable
	private ItemChangeListener mItemChangeListener;

	private DragSortController mController;
	/**
	 * Where to cancel the ListView action when a drag-sort begins
	 */
	private int mCancelMethod = NO_CANCEL;
	/**
	 * Number between 0 and 1 indicating the relative location of a sliding item
	 * (only used if drag-sort animations are turned on). Nearly 1 means the
	 * item is at the top of the slide region (nearly full blank item is
	 * directly below).
	 */
	private float mSlideFrac = 0.0f;
	/**
	 * Needed for adjusting item heights from within layoutChildren
	 */
	private boolean mBlockLayoutRequests = false;
	private boolean mScrolling = false;
	private float mCurrFloatAlpha = M_FLOAT_ALPHA;
	private boolean mAbort;
	private long mPrevTime;
	private int scrollDir;
	private int mDownScrollStartY;
	private float mDownScrollStartYF;
	private float mUpScrollStartYF;
	private int mDragState = IDLE;

	/**
	 * Defines the scroll speed during a drag-scroll. User can provide their
	 * own; this default is a simple linear profile where scroll speed increases
	 * linearly as the floating View nears the top/bottom of the ListView.
	 */
	private DragScrollProfile mScrollProfile = new DragScrollProfile() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public float getSpeed(float w) {
			float mMaxScrollSpeed = 0.3f;
			return mMaxScrollSpeed * w;
		}
	};

	/**
	 * Watch the Adapter for data changes. Cancel a drag if coincident with a
	 * change.
	 */
	private DataSetObserver mObserver = new DataSetObserver() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onChanged() {
			if (mDragState == DRAGGING) {
				stopDrag(false);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onInvalidated() {
			if (mDragState == DRAGGING) {
				stopDrag(false);
			}
		}
	};

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public DragSortListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		handler = new DragSortHandler(this);
		mController = new DragSortController(this, R.id.edit_track_list_item_handle, DragSortController.ON_DOWN, DragSortController.FLING_RIGHT_REMOVE);
		mCancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0f, 0f, 0, 0f, 0f, 0, 0);

		setDragScrollStarts(mDragUpScrollStartFrac, mDragUpScrollStartFrac);
		mController.setRemoveEnabled(true);
		mController.setSortEnabled(true);
		mController.setBackgroundColor(PreferenceUtils.getInstance(context).getDefaultThemeColor());

		setOnTouchListener(mController);
		setOnScrollListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAdapter(ListAdapter adapter) {
		AdapterWrapper mAdapterWrapper = new AdapterWrapper(adapter);
		adapter.registerDataSetObserver(mObserver);
		super.setAdapter(mAdapterWrapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mFloatView != null) {
			if (mFirstExpPos != mSrcPos) {
				drawDivider(mFirstExpPos, canvas);
			}
			if (mSecondExpPos != mFirstExpPos && mSecondExpPos != mSrcPos) {
				drawDivider(mSecondExpPos, canvas);
			}
			int w = mFloatView.getWidth();
			int h = mFloatView.getHeight();
			int alpha = (int) (255f * mCurrFloatAlpha);
			canvas.save();
			canvas.translate(mFloatViewLeft, mFloatViewTop);
			canvas.clipRect(0, 0, w, h);
			canvas.saveLayerAlpha(0, 0, w, h, alpha, Canvas.ALL_SAVE_FLAG);
			mFloatView.draw(canvas);
			canvas.restore();
			canvas.restore();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateScrollStarts();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestLayout() {
		if (!mBlockLayoutRequests) {
			super.requestLayout();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mScrolling && visibleItemCount != 0) {
			dragView(mX, mY);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!mDragEnabled) {
			return super.onTouchEvent(ev);
		}
		boolean more = false;
		boolean lastCallWasIntercept = mLastCallWasIntercept;
		mLastCallWasIntercept = false;
		if (!lastCallWasIntercept) {
			saveTouchCoords(ev);
		}
		if (mFloatView != null) {
			switch (ev.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					stopDrag(false);
					doActionUpOrCancel();
					break;

				case MotionEvent.ACTION_MOVE:
					continueDrag((int) ev.getX(), (int) ev.getY());
					break;
			}
			more = true; // give us more!
		} else {
			// what if float view is null b/c we dropped in middle
			// of drag touch event?
			if (mDragState != STOPPED) {
				if (super.onTouchEvent(ev)) {
					more = true;
				}
			}
			int action = ev.getAction() & MotionEvent.ACTION_MASK;
			switch (action) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					doActionUpOrCancel();
					break;

				default:
					if (more) {
						mCancelMethod = ON_TOUCH_EVENT;
					}
			}
		}
		return more;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!mDragEnabled) {
			return super.onInterceptTouchEvent(ev);
		}
		saveTouchCoords(ev);
		mLastCallWasIntercept = true;
		boolean intercept = false;
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_DOWN) {
			mInTouchEvent = true;
		}
		// the following deals with calls to super.onInterceptTouchEvent
		if (mFloatView != null) {
			// super's touch event canceled in startDrag
			intercept = true;
		} else {
			if (super.onInterceptTouchEvent(ev)) {
				intercept = true;
			}
			switch (action) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					doActionUpOrCancel();
					break;

				default:
					if (intercept) {
						mCancelMethod = ON_TOUCH_EVENT;
					} else {
						mCancelMethod = ON_INTERCEPT_TOUCH_EVENT;
					}
			}
		}
		// check for startDragging
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			mInTouchEvent = false;
		}
		return intercept;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mFloatView != null) {
			if (mFloatView.isLayoutRequested()) {
				measureFloatView();
			}
		}
		mWidthMeasureSpec = widthMeasureSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void layoutChildren() {
		if (mFloatView != null) {
			mFloatView.layout(0, 0, mFloatView.getMeasuredWidth(), mFloatView.getMeasuredHeight());
			int oldFirstExpPos = mFirstExpPos;
			int oldSecondExpPos = mSecondExpPos;
			mBlockLayoutRequests = true;
			if (updatePositions()) {
				adjustAllItems();
			}
			if (mScrollY != 0) {
				doDragScroll(oldFirstExpPos, oldSecondExpPos);
			}
			mBlockLayoutRequests = false;
		}
		super.layoutChildren();
	}

	/**
	 *
	 */
	public float getFloatAlpha() {
		return mCurrFloatAlpha;
	}

	/**
	 * Usually called from a FloatViewManager. The float alpha will be reset to
	 * the xml-defined value every time a drag is stopped.
	 */
	public void setFloatAlpha(float alpha) {
		mCurrFloatAlpha = alpha;
	}

	/**
	 * Stop a drag in progress. Pass <code>true</code> if you would like to
	 * remove the dragged item from the list.
	 *
	 * @param remove Remove the dragged item from the list. Calls a registered
	 *               DropListener, if one exists.
	 */
	public void stopDrag(boolean remove) {
		if (mFloatView != null) {
			mDragState = STOPPED;
			// stop the drag
			dropFloatView(remove);
		}
	}

	/**
	 * Set the width of each drag scroll region by specifying a fraction of the
	 * ListView height.
	 *
	 * @param upperFrac Fraction of ListView height for up-scroll bound. Capped
	 *                  at 0.5f.
	 * @param lowerFrac Fraction of ListView height for down-scroll bound.
	 *                  Capped at 0.5f.
	 */
	public void setDragScrollStarts(float upperFrac, float lowerFrac) {
		mDragDownScrollStartFrac = Math.min(lowerFrac, 0.5f);
		mDragUpScrollStartFrac = Math.min(upperFrac, 0.5f);
		if (getHeight() != 0) {
			updateScrollStarts();
		}
	}

	/**
	 * Start a drag of item at <code>position</code> using the registered
	 * FloatViewManager. Calls through to
	 * {@link #startDrag(int, View, int, int, int)} after obtaining the floating
	 * View from the FloatViewManager.
	 *
	 * @param position  Item to drag.
	 * @param dragFlags Flags that restrict some movements of the floating View.
	 *                  For example, set <code>dragFlags |=
	 *                  ~{@link #DRAG_NEG_X}</code> to allow dragging the floating View in all
	 *                  directions except off the screen to the left.
	 * @param deltaX    Offset in x of the touch coordinate from the left edge of
	 *                  the floating View (i.e. touch-x minus float View left).
	 * @param deltaY    Offset in y of the touch coordinate from the top edge of
	 *                  the floating View (i.e. touch-y minus float View top).
	 * @return True if the drag was started, false otherwise. This
	 * <code>startDrag</code> will fail if we are not currently in a
	 * touch event, there is no registered FloatViewManager, or the
	 * FloatViewManager returns a null View.
	 */
	public boolean startDrag(int position, int dragFlags, int deltaX, int deltaY) {
		if (!mInTouchEvent) {
			return false;
		}
		View v = mController.onCreateFloatView(position);
		if (v == null) {
			return false;
		} else {
			return startDrag(position, v, dragFlags, deltaX, deltaY);
		}
	}

	/**
	 * Start a drag of item at <code>position</code> without using a
	 * FloatViewManager.
	 *
	 * @param position  Item to drag.
	 * @param floatView Floating View.
	 * @param dragFlags Flags that restrict some movements of the floating View.
	 *                  For example, set <code>dragFlags |=
	 *                  ~{@link #DRAG_NEG_X}</code> to allow dragging the floating View in all
	 *                  directions except off the screen to the left.
	 * @param deltaX    Offset in x of the touch coordinate from the left edge of
	 *                  the floating View (i.e. touch-x minus float View left).
	 * @param deltaY    Offset in y of the touch coordinate from the top edge of
	 *                  the floating View (i.e. touch-y minus float View top).
	 * @return True if the drag was started, false otherwise. This
	 * <code>startDrag</code> will fail if we are not currently in a
	 * touch event, <code>floatView</code> is null, or there is a drag
	 * in progress.
	 */
	public boolean startDrag(int position, View floatView, int dragFlags, int deltaX, int deltaY) {
		if (!mInTouchEvent || mFloatView != null || floatView == null) {
			return false;
		}
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
		int pos = position + getHeaderViewsCount();
		mFirstExpPos = pos;
		mSecondExpPos = pos;
		mSrcPos = pos;
		mFloatPos = pos;
		// mDragState = dragType;
		mDragState = DRAGGING;
		mDragFlags = 0;
		mDragFlags |= dragFlags;
		mFloatView = floatView;
		measureFloatView();
		// sets mFloatViewHeight
		mDragDeltaX = deltaX;
		mDragDeltaY = deltaY;
		updateFloatView(mX - mDragDeltaX, mY - mDragDeltaY);
		// set src item invisible
		View srcItem = getChildAt(mSrcPos - getFirstVisiblePosition());
		if (srcItem != null) {
			srcItem.setVisibility(View.INVISIBLE);
		}
		// once float view is created, events are no longer passed to ListView
		switch (mCancelMethod) {
			case ON_TOUCH_EVENT:
				super.onTouchEvent(mCancelEvent);
				break;

			case ON_INTERCEPT_TOUCH_EVENT:
				super.onInterceptTouchEvent(mCancelEvent);
				break;
		}
		requestLayout();
		return true;
	}

	/**
	 * This better reorder your ListAdapter! DragSortListView does not do this for you,
	 * doesn't make sense to. Make sure {@link BaseAdapter#notifyDataSetChanged()}
	 * or something like it is called in your implementation.
	 */
	public void setItemChangeListener(@Nullable ItemChangeListener listener) {
		mItemChangeListener = listener;
	}

	/**
	 * Completely custom scroll speed profile. Default increases linearly with
	 * position and is constant in time. Create your own by implementing
	 * {@link DragSortListView.DragScrollProfile}.
	 */
	public void setDragScrollProfile(@NonNull DragScrollProfile scrollProfile) {
		mScrollProfile = scrollProfile;
	}


	private void startScrolling(int dir) {
		if (!mScrolling) {
			// Debug.startMethodTracing("dslv-scroll");
			mAbort = false;
			mScrolling = true;
			mPrevTime = SystemClock.uptimeMillis();
			scrollDir = dir;
			post(handler);
		}
	}


	private void stopScrolling() {
		removeCallbacks(handler);
		mScrolling = false;
	}

	/**
	 *
	 */
	private void drawDivider(int expPosition, Canvas canvas) {
		Drawable divider = getDivider();
		int dividerHeight = getDividerHeight();
		if (divider != null && dividerHeight != 0) {
			ViewGroup expItem = (ViewGroup) getChildAt(expPosition - getFirstVisiblePosition());
			if (expItem != null && expItem.getChildAt(0) != null) {
				int t, b;
				int l = getPaddingLeft();
				int r = getWidth() - getPaddingRight();
				int childHeight = expItem.getChildAt(0).getHeight();
				if (expPosition > mSrcPos) {
					t = expItem.getTop() + childHeight;
					b = t + dividerHeight;
				} else {
					b = expItem.getBottom() - childHeight;
					t = b - dividerHeight;
				}
				divider.setBounds(l, t, r, b);
				divider.draw(canvas);
			}
		}
	}

	/**
	 *
	 */
	private void measureItemAndGetHeights(int position, View item, ItemHeights heights) {
		ViewGroup.LayoutParams lp = item.getLayoutParams();
		boolean isHeadFoot = position < getHeaderViewsCount() || position >= getCount() - getFooterViewsCount();
		int height = lp == null ? 0 : lp.height;
		if (height > 0) {
			heights.item = height;
			// get height of child, measure if we have to
			if (isHeadFoot) {
				heights.child = heights.item;
			} else if (position == mSrcPos) {
				heights.child = 0;
			} else {
				View child = ((ViewGroup) item).getChildAt(0);
				if (child != null) {
					lp = child.getLayoutParams();
					height = lp == null ? 0 : lp.height;
					if (height > 0) {
						heights.child = height;
					} else if (lp != null) {
						int hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
						int wspec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec, getListPaddingLeft() + getListPaddingRight(), lp.width);
						child.measure(wspec, hspec);
						heights.child = child.getMeasuredHeight();
					}
				}
			}
		} else {
			int hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			int wspec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec, getListPaddingLeft()
					+ getListPaddingRight(), lp == null ? ViewGroup.LayoutParams.MATCH_PARENT : lp.width);
			item.measure(wspec, hspec);
			heights.item = item.getMeasuredHeight();
			if (isHeadFoot) {
				heights.child = heights.item;
			} else if (position == mSrcPos) {
				heights.child = 0;
			} else {
				View child = ((ViewGroup) item).getChildAt(0);
				if (child != null) {
					heights.child = child.getMeasuredHeight();
				} else {
					heights.child = 0;
				}
			}
		}
	}

	/**
	 * Get the height of the given wrapped item and its child.
	 *
	 * @param position Position from which item was obtained.
	 * @param item     List item (usually obtained from
	 *                 ).
	 * @param heights  Object to fill with heights of item.
	 */
	private void getItemHeights(int position, View item, ItemHeights heights) {
		boolean isHeadFoot = position < getHeaderViewsCount() || position >= getCount() - getFooterViewsCount();
		heights.item = item.getHeight();
		if (isHeadFoot) {
			heights.child = heights.item;
		} else if (position == mSrcPos) {
			heights.child = 0;
		} else {
			heights.child = ((ViewGroup) item).getChildAt(0).getHeight();
		}
	}

	/**
	 * This function works for arbitrary positions (could be off-screen). If
	 * requested position is off-screen, this function calls
	 * <code>getView</code> to get height information.
	 *
	 * @param position ListView position.
	 * @param heights  Object to fill with heights of item at <code>position</code>.
	 */
	private void getItemHeights(int position, ItemHeights heights) {
		int first = getFirstVisiblePosition();
		int last = getLastVisiblePosition();
		if (position >= first && position <= last) {
			getItemHeights(position, getChildAt(position - first), heights);
		} else {
			ListAdapter adapter = getAdapter();
			int type = adapter.getItemViewType(position);
			// There might be a better place for checking for the following
			int typeCount = adapter.getViewTypeCount();
			if (typeCount != mSampleViewTypes.length) {
				mSampleViewTypes = new View[typeCount];
			}
			View v;
			if (type >= 0) {
				if (mSampleViewTypes[type] == null) {
					v = adapter.getView(position, null, this);
					mSampleViewTypes[type] = v;
				} else {
					v = adapter.getView(position, mSampleViewTypes[type], this);
				}
			} else {
				// type is HEADER_OR_FOOTER or IGNORE
				v = adapter.getView(position, null, this);
			}
			measureItemAndGetHeights(position, v, heights);
		}
	}

	/**
	 * Get the shuffle edge for item at position when top of item is at y-coord
	 * top
	 *
	 * @return Shuffle line between position-1 and position (for the given view
	 * of the list; that is, for when top of item at position has
	 * y-coord of given `top`). If floating View (treated as horizontal
	 * line) is dropped immediately above this line, it lands in
	 * position-1. If dropped immediately below this line, it lands in position.
	 */
	private int getShuffleEdge(int position, int top, ItemHeights heights) {
		int numHeaders = getHeaderViewsCount();
		int numFooters = getFooterViewsCount();
		// shuffle edges are defined between items that can be dragged;
		// there are N-1 of them if there are N draggable items.
		if (position <= numHeaders || position >= getCount() - numFooters) {
			return top;
		}
		int divHeight = getDividerHeight();
		int edge;
		int maxBlankHeight = mFloatViewHeight - mItemHeightCollapsed;
		if (heights == null) {
			heights = new ItemHeights();
			getItemHeights(position, heights);
		}
		// first calculate top of item given that floating View is
		// centered over src position
		int otop = top;
		if (mSecondExpPos <= mSrcPos) {
			// items are expanded on and/or above the source position
			if (position == mSecondExpPos && mFirstExpPos != mSecondExpPos) {
				if (position == mSrcPos) {
					otop = top + heights.item - mFloatViewHeight;
				} else {
					int blankHeight = heights.item - heights.child;
					otop = top + blankHeight - maxBlankHeight;
				}
			} else if (position > mSecondExpPos && position <= mSrcPos) {
				otop = top - maxBlankHeight;
			}
		} else {
			// items are expanded on and/or below the source position
			if (position > mSrcPos && position <= mFirstExpPos) {
				otop = top + maxBlankHeight;
			} else if (position == mSecondExpPos) {
				int blankHeight = heights.item - heights.child;
				otop = top + blankHeight;
			}
		}
		// otop is set
		if (position <= mSrcPos) {
			ItemHeights tmpHeights = new ItemHeights();
			getItemHeights(position - 1, tmpHeights);
			edge = otop + (mFloatViewHeight - divHeight - tmpHeights.child) / 2;
		} else {
			edge = otop + (heights.child - divHeight - mFloatViewHeight) / 2;
		}
		return edge;
	}


	private boolean updatePositions() {
		int first = getFirstVisiblePosition();
		int startPos = mFirstExpPos;
		View startView = getChildAt(startPos - first);
		if (startView == null) {
			startPos = first + getChildCount() / 2;
			startView = getChildAt(startPos - first);
		}
		int startTop = startView.getTop() + mScrollY;
		ItemHeights itemHeights = new ItemHeights();
		getItemHeights(startPos, startView, itemHeights);
		int edge = getShuffleEdge(startPos, startTop, itemHeights);
		int lastEdge = edge;
		int divHeight = getDividerHeight();
		int itemPos = startPos;
		int itemTop = startTop;
		if (mFloatViewMid < edge) {
			// scanning up for float position
			while (itemPos >= 0) {
				itemPos--;
				getItemHeights(itemPos, itemHeights);
				if (itemPos == 0) {
					edge = itemTop - divHeight - itemHeights.item;
					break;
				}
				itemTop -= itemHeights.item + divHeight;
				edge = getShuffleEdge(itemPos, itemTop, itemHeights);
				if (mFloatViewMid >= edge) {
					break;
				}
				lastEdge = edge;
			}
		} else {
			// scanning down for float position
			int count = getCount();
			while (itemPos < count) {
				if (itemPos == count - 1) {
					edge = itemTop + divHeight + itemHeights.item;
					break;
				}
				itemTop += divHeight + itemHeights.item;
				getItemHeights(itemPos + 1, itemHeights);
				edge = getShuffleEdge(itemPos + 1, itemTop, itemHeights);
				// test for hit
				if (mFloatViewMid < edge) {
					break;
				}
				lastEdge = edge;
				itemPos++;
			}
		}
		int numHeaders = getHeaderViewsCount();
		int numFooters = getFooterViewsCount();
		boolean updated = false;
		int oldFirstExpPos = mFirstExpPos;
		int oldSecondExpPos = mSecondExpPos;
		float oldSlideFrac = mSlideFrac;

		if (mAnimate) {
			int edgeToEdge = Math.abs(edge - lastEdge);
			int edgeTop, edgeBottom;
			if (mFloatViewMid < edge) {
				edgeBottom = edge;
				edgeTop = lastEdge;
			} else {
				edgeTop = edge;
				edgeBottom = lastEdge;
			}
			int slideRgnHeight = (int) (0.5f * SLIDE_REGION_FRAC * edgeToEdge);
			int slideEdgeTop = edgeTop + slideRgnHeight;
			int slideEdgeBottom = edgeBottom - slideRgnHeight;
			// Three regions
			if (mFloatViewMid < slideEdgeTop) {
				mFirstExpPos = itemPos - 1;
				mSecondExpPos = itemPos;
				mSlideFrac = 0.5f * (slideEdgeTop - mFloatViewMid) / slideRgnHeight;
			} else if (mFloatViewMid < slideEdgeBottom) {
				mFirstExpPos = itemPos;
				mSecondExpPos = itemPos;
			} else {
				mFirstExpPos = itemPos;
				mSecondExpPos = itemPos + 1;
				mSlideFrac = 0.5f * (1.0f + (edgeBottom - mFloatViewMid) / (float) slideRgnHeight);
			}
		} else {
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		}
		// correct for headers and footers
		if (mFirstExpPos < numHeaders) {
			itemPos = numHeaders;
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		} else if (mSecondExpPos >= getCount() - numFooters) {
			itemPos = getCount() - numFooters - 1;
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		}
		if (mFirstExpPos != oldFirstExpPos || mSecondExpPos != oldSecondExpPos || mSlideFrac != oldSlideFrac) {
			updated = true;
		}
		if (itemPos != mFloatPos) {
			mFloatPos = itemPos;
			updated = true;
		}
		return updated;
	}

	/**
	 *
	 */
	private void doActionUpOrCancel() {
		mCancelMethod = NO_CANCEL;
		mInTouchEvent = false;
		mDragState = IDLE;
		mCurrFloatAlpha = M_FLOAT_ALPHA;
	}


	private void saveTouchCoords(MotionEvent ev) {
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		if (action != MotionEvent.ACTION_DOWN) {
			mLastY = mY;
		}
		mX = (int) ev.getX();
		mY = (int) ev.getY();
		if (action == MotionEvent.ACTION_DOWN) {
			mLastY = mY;
		}
	}

	/**
	 *
	 */
	private void dropFloatView(boolean removeSrcItem) {
		stopScrolling();
		if (removeSrcItem) {
			if (mItemChangeListener != null) {
				mItemChangeListener.remove(mSrcPos - getHeaderViewsCount());
			}
		} else {
			if (mItemChangeListener != null && mFloatPos >= 0 && mFloatPos < getCount()) {
				int from = mSrcPos - getHeaderViewsCount();
				int to = mFloatPos - getHeaderViewsCount();
				mItemChangeListener.drop(from, to);
			}
			int firstPos = getFirstVisiblePosition();
			if (mSrcPos < firstPos) {
				// collapsed src item is off screen;
				// adjust the scroll after item heights have been fixed
				View v = getChildAt(0);
				int top = 0;
				if (v != null) {
					top = v.getTop();
				}
				setSelectionFromTop(firstPos - 1, top - getPaddingTop());
			}
		}
		mSrcPos = -1;
		mFirstExpPos = -1;
		mSecondExpPos = -1;
		mFloatPos = -1;
		removeFloatView();
	}

	/**
	 *
	 */
	private void adjustAllItems() {
		int first = getFirstVisiblePosition();
		int last = getLastVisiblePosition();
		int begin = Math.max(0, getHeaderViewsCount() - first);
		int end = Math.min(last - first, getCount() - 1 - getFooterViewsCount() - first);
		for (int i = begin; i <= end; ++i) {
			View v = getChildAt(i);
			if (v != null) {
				adjustItem(first + i, v, false);
			}
		}
	}

	/**
	 *
	 */
	private void adjustItem(int position, View v, boolean needsMeasure) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		int oldHeight = lp.height;
		int height;

		boolean isSliding = mAnimate && mFirstExpPos != mSecondExpPos;
		int maxNonSrcBlankHeight = mFloatViewHeight - mItemHeightCollapsed;
		int slideHeight = (int) (mSlideFrac * maxNonSrcBlankHeight);

		if (position == mSrcPos) {
			if (mSrcPos == mFirstExpPos) {
				if (isSliding) {
					height = slideHeight + mItemHeightCollapsed;
				} else {
					height = mFloatViewHeight;
				}
			} else if (mSrcPos == mSecondExpPos) {
				// if gets here, we know an item is sliding
				height = mFloatViewHeight - slideHeight;
			} else {
				height = mItemHeightCollapsed;
			}
		} else if (position == mFirstExpPos || position == mSecondExpPos) {
			// position is not src
			ItemHeights itemHeights = new ItemHeights();
			if (needsMeasure) {
				measureItemAndGetHeights(position, v, itemHeights);
			} else {
				getItemHeights(position, v, itemHeights);
			}
			if (position == mFirstExpPos) {
				if (isSliding) {
					height = itemHeights.child + slideHeight;
				} else {
					height = itemHeights.child + maxNonSrcBlankHeight;
				}
			} else {
				// position=mSecondExpPos
				// we know an item is sliding (b/c 2ndPos != 1stPos)
				height = itemHeights.child + maxNonSrcBlankHeight - slideHeight;
			}
		} else {
			height = ViewGroup.LayoutParams.WRAP_CONTENT;
		}
		if (height != oldHeight) {
			lp.height = height;
			v.setLayoutParams(lp);
		}
		// Adjust item gravity
		if (position == mFirstExpPos || position == mSecondExpPos) {
			if (position < mSrcPos) {
				((RelativeLayout) v).setGravity(Gravity.BOTTOM);
			} else if (position > mSrcPos) {
				((RelativeLayout) v).setGravity(Gravity.TOP);
			}
		}
		// Finally adjust item visibility
		int oldVis = v.getVisibility();
		int vis = View.VISIBLE;
		if (position == mSrcPos && mFloatView != null) {
			vis = View.INVISIBLE;
		}
		if (vis != oldVis) {
			v.setVisibility(vis);
		}
	}

	/**
	 *
	 */
	private void continueDrag(int x, int y) {
		dragView(x, y);
		requestLayout();

		int minY = Math.min(y, mFloatViewMid + mFloatViewHeightHalf);
		int maxY = Math.max(y, mFloatViewMid - mFloatViewHeightHalf);
		// get the current scroll direction
		int currentScrollDir = mScrolling ? scrollDir : STOP;

		if (minY > mLastY && minY > mDownScrollStartY && currentScrollDir != DOWN) {
			// dragged down, it is below the down scroll start and it is not
			// scrolling up
			if (currentScrollDir != STOP) {
				// moved directly from up scroll to down scroll
				stopScrolling();
			}
			// start scrolling down
			startScrolling(DOWN);
		} else if (maxY < mLastY && maxY < mUpScrollStartY && currentScrollDir != UP) {
			// dragged up, it is above the up scroll start and it is not
			// scrolling up
			if (currentScrollDir != STOP) {
				// moved directly from down scroll to up scroll
				stopScrolling();
			}
			// start scrolling up
			startScrolling(UP);
		} else if (maxY >= mUpScrollStartY && minY <= mDownScrollStartY && mScrolling) {
			// not in the upper nor in the lower drag-scroll regions but it is
			// still scrolling
			stopScrolling();
		}
	}


	private void updateScrollStarts() {
		int padTop = getPaddingTop();
		int listHeight = getHeight() - padTop - getPaddingBottom();
		mUpScrollStartYF = padTop + mDragUpScrollStartFrac * listHeight;
		mDownScrollStartYF = padTop + (1.0f - mDragDownScrollStartFrac) * listHeight;
		mUpScrollStartY = (int) mUpScrollStartYF;
		mDownScrollStartY = (int) mDownScrollStartYF;
		mDragUpScrollHeight = mUpScrollStartYF - padTop;
		mDragDownScrollHeight = padTop + listHeight - mDownScrollStartYF;
	}

	/**
	 *
	 */
	private void doDragScroll(int oldFirstExpPos, int oldSecondExpPos) {
		if (mScrollY == 0) {
			return;
		}
		int padTop = getPaddingTop();
		int listHeight = getHeight() - padTop - getPaddingBottom();
		int first = getFirstVisiblePosition();
		int last = getLastVisiblePosition();
		int movePos;
		if (mScrollY >= 0) {
			mScrollY = Math.min(listHeight, mScrollY);
			movePos = first;
		} else {
			mScrollY = Math.max(-listHeight, mScrollY);
			movePos = last;
		}
		View moveItem = getChildAt(movePos - first);
		int top = moveItem.getTop() + mScrollY;
		if (movePos == 0 && top > padTop) {
			top = padTop;
		}
		ItemHeights itemHeightsBefore = new ItemHeights();
		getItemHeights(movePos, moveItem, itemHeightsBefore);
		int moveHeightBefore = itemHeightsBefore.item;
		int moveBlankBefore = moveHeightBefore - itemHeightsBefore.child;
		ItemHeights itemHeightsAfter = new ItemHeights();
		measureItemAndGetHeights(movePos, moveItem, itemHeightsAfter);
		int moveHeightAfter = itemHeightsAfter.item;
		int moveBlankAfter = moveHeightAfter - itemHeightsAfter.child;
		if (movePos <= oldFirstExpPos) {
			if (movePos > mFirstExpPos) {
				top += mFloatViewHeight - moveBlankAfter;
			}
		} else if (movePos == oldSecondExpPos) {
			if (movePos <= mFirstExpPos) {
				top += moveBlankBefore - mFloatViewHeight;
			} else if (movePos == mSecondExpPos) {
				top += moveHeightBefore - moveHeightAfter;
			} else {
				top += moveBlankBefore;
			}
		} else {
			if (movePos <= mFirstExpPos) {
				top -= mFloatViewHeight;
			} else if (movePos == mSecondExpPos) {
				top -= moveBlankAfter;
			}
		}
		setSelectionFromTop(movePos, top - padTop);

		mScrollY = 0;
	}


	private void measureFloatView() {
		if (mFloatView != null) {
			ViewGroup.LayoutParams params = mFloatView.getLayoutParams();
			if (params == null) {
				params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			}
			int wspec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec, getListPaddingLeft() + getListPaddingRight(), params.width);
			int hspec;
			if (params.height > 0) {
				hspec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			} else {
				hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			}
			mFloatView.measure(wspec, hspec);
			mFloatViewHeight = mFloatView.getMeasuredHeight();
			mFloatViewHeightHalf = mFloatViewHeight / 2;
		}
	}

	/**
	 * Sets float View location based on suggested values and constraints set in
	 * mDragFlags.
	 */
	private void updateFloatView(int floatX, int floatY) {
		// restrict x motion
		int padLeft = getPaddingLeft();
		if ((mDragFlags & DRAG_POS_X) == 0 && floatX > padLeft) {
			mFloatViewLeft = padLeft;
		} else if ((mDragFlags & DRAG_NEG_X) == 0 && floatX < padLeft) {
			mFloatViewLeft = padLeft;
		} else {
			mFloatViewLeft = floatX;
		}
		// keep floating view from going past bottom of last header view
		int numHeaders = getHeaderViewsCount();
		int numFooters = getFooterViewsCount();
		int firstPos = getFirstVisiblePosition();
		int lastPos = getLastVisiblePosition();

		// "nHead="+numHeaders+" nFoot="+numFooters+" first="+firstPos+" last="+lastPos);
		int topLimit = getPaddingTop();
		if (firstPos < numHeaders) {
			topLimit = getChildAt(numHeaders - firstPos - 1).getBottom();
		}
		if ((mDragFlags & DRAG_NEG_Y) == 0) {
			if (firstPos <= mSrcPos) {
				topLimit = Math.max(getChildAt(mSrcPos - firstPos).getTop(), topLimit);
			}
		}
		// bottom limit is top of first footer View or
		// bottom of last item in list
		int bottomLimit = getHeight() - getPaddingBottom();
		if (lastPos >= getCount() - numFooters - 1) {
			bottomLimit = getChildAt(getCount() - numFooters - 1 - firstPos).getBottom();
		}
		if ((mDragFlags & DRAG_POS_Y) == 0) {
			if (lastPos >= mSrcPos) {
				bottomLimit = Math.min(getChildAt(mSrcPos - firstPos).getBottom(), bottomLimit);
			}
		}
		if (floatY < topLimit) {
			mFloatViewTop = topLimit;
		} else if (floatY + mFloatViewHeight > bottomLimit) {
			mFloatViewTop = bottomLimit - mFloatViewHeight;
		} else {
			mFloatViewTop = floatY;
		}
		// get y-midpoint of floating view (constrained to ListView bounds)
		mFloatViewMid = mFloatViewTop + mFloatViewHeightHalf;
	}


	private void dragView(int x, int y) {
		// proposed position
		mFloatLoc.x = x - mDragDeltaX;
		mFloatLoc.y = y - mDragDeltaY;
		Point touch = new Point(x, y);
		// let manager adjust proposed position first
		mController.onDragFloatView(touch);
		// then we override if manager gives an unsatisfactory
		// position (e.g. over a header/footer view). Also,
		// dragFlags override manager adjustments.
		updateFloatView(mFloatLoc.x, mFloatLoc.y);
	}


	private void removeFloatView() {
		if (mFloatView != null) {
			mFloatView.setVisibility(GONE);
			mController.onDestroyFloatView(mFloatView);
			mFloatView = null;
		}
	}


	private static class ItemHeights {
		int item;
		int child;
	}

	/**
	 *
	 */
	private static class DragSortHandler implements Runnable {

		private WeakReference<DragSortListView> controller;


		DragSortHandler(DragSortListView controller) {
			this.controller = new WeakReference<>(controller);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			DragSortListView mList = this.controller.get();

			if (mList == null || mList.mScrollProfile == null)
				return;
			if (mList.mAbort) {
				mList.mScrolling = false;
				return;
			}
			int first = mList.getFirstVisiblePosition();
			int last = mList.getLastVisiblePosition();
			int count = mList.getCount();
			int padTop = mList.getPaddingTop();
			int listHeight = mList.getHeight() - padTop - mList.getPaddingBottom();
			int minY = Math.min(mList.mY, mList.mFloatViewMid + mList.mFloatViewHeightHalf);
			int maxY = Math.max(mList.mY, mList.mFloatViewMid - mList.mFloatViewHeightHalf);
			// pixels per ms
			float mScrollSpeed;
			if (mList.scrollDir == UP) {
				View v = mList.getChildAt(0);
				if (v == null) {
					mList.mScrolling = false;
					return;
				} else {
					if (first == 0 && v.getTop() == padTop) {
						mList.mScrolling = false;
						return;
					}
				}
				mScrollSpeed = mList.mScrollProfile.getSpeed((mList.mUpScrollStartYF - maxY) / mList.mDragUpScrollHeight);
			} else {
				View v = mList.getChildAt(last - first);
				if (v == null) {
					mList.mScrolling = false;
					return;
				} else {
					if (last == count - 1 && v.getBottom() <= listHeight + padTop) {
						mList.mScrolling = false;
						return;
					}
				}
				mScrollSpeed = -mList.mScrollProfile.getSpeed((minY - mList.mDownScrollStartYF) / mList.mDragDownScrollHeight);
			}
			float dt = SystemClock.uptimeMillis() - mList.mPrevTime;
			// dy is change in View position of a list item; i.e. positive dy
			// means user is scrolling up (list item moves down the screen,
			// remember
			// y=0 is at top of View).
			int dy = Math.round(mScrollSpeed * dt);
			mList.mScrollY += dy;
			mList.requestLayout();
			mList.mPrevTime += Math.round(dt);
			mList.post(this);
		}
	}

	/**
	 *
	 */
	private class AdapterWrapper extends HeaderViewListAdapter {

		private ListAdapter mAdapter;


		public AdapterWrapper(ListAdapter adapter) {
			super(null, null, adapter);
			mAdapter = adapter;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			RelativeLayout view;
			View child;
			if (convertView instanceof RelativeLayout) {
				view = (RelativeLayout) convertView;
				View oldChild = view.getChildAt(0);
				try {
					child = mAdapter.getView(position, oldChild, view);
					if (child != oldChild && view.getChildCount() > 0) {
						view.removeViewAt(0);
						view.addView(child);
					}
				} catch (Exception e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
				}
			} else {
				AbsListView.LayoutParams params = new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				view = new RelativeLayout(getContext());
				view.setLayoutParams(params);
				try {
					child = mAdapter.getView(position, null, view);
					// remove from old parent if any
					if (child.getParent() instanceof ViewGroup) {
						((ViewGroup) child.getParent()).removeView(child);
					}
					view.addView(child);
				} catch (Exception e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
				}
			}
			adjustItem(position + getHeaderViewsCount(), view, true);
			return view;
		}
	}

	/**
	 * Your implementation of this has to reorder your ListAdapter! Make sure to
	 * call {@link BaseAdapter#notifyDataSetChanged()} or something like it in
	 * your implementation.
	 *
	 * @author heycosmo
	 */
	public interface ItemChangeListener {

		/**
		 * called if an item was moved to another index
		 *
		 * @param from old index of the item
		 * @param to   new index of the item
		 */
		void drop(int from, int to);

		/**
		 * called if an item was removed
		 *
		 * @param index position of the removed item
		 */
		void remove(int index);
	}

	/**
	 * Interface for controlling scroll speed as a function of touch position and time. Use
	 * {@link DragSortListView#setDragScrollProfile(DragScrollProfile)} to set custom profile.
	 *
	 * @author heycosmo
	 */
	public interface DragScrollProfile {

		/**
		 * Return a scroll speed in pixels/millisecond. Always return a positive number.
		 *
		 * @param w Normalized position in scroll region (i.e. w \in [0,1]). Small w typically means slow scrolling.
		 * @return Scroll speed at position w and time t in pixels/ms.
		 */
		float getSpeed(float w);
	}
}