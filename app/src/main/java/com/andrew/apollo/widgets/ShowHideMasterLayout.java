/*
 * Copyright 2012 Google Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.widgets;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * A layout that supports the Show/Hide pattern for portrait tablet layouts. See
 * <a href=
 * "http://developer.android.com/design/patterns/multi-pane-layouts.html#orientation"
 * >Android Design &gt; Patterns &gt; Multi-pane Layouts & gt; Compound Views
 * and Orientation Changes</a> for more details on this pattern. This layout
 * should normally be used in association with the Up button. Specifically, show
 * the master pane using {@link #showMaster(boolean, int)} when the Up button is
 * pressed. If the master pane is visible, defer to normal Up behavior.
 * <p>
 * TODO: swiping should be more tactile and actually follow the user's finger.
 * <p>
 * Requires API level 11
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ShowHideMasterLayout extends ViewGroup implements Animator.AnimatorListener {

    /**
     * A flag for {@link #showMaster(boolean, int)} indicating that the change
     * in visibility should not be animated.
     */
    public static final int FLAG_IMMEDIATE = 0x1;

    private View sMasterView;

    private View mDetailView;

    private GestureDetector mGestureDetector;

    private Runnable mShowMasterCompleteRunnable;

    private boolean mFirstShow = true;

    private boolean mMasterVisible = true;

    private boolean mIsAnimating;

    /* The last measured master width, including its margins */
    private int mTranslateAmount;

    private OnGestureListener mGestureListener = new SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            ViewConfiguration mViewConfig = ViewConfiguration.get(getContext());
            float mAbsVelocityX = Math.abs(velocityX);
            float mAbsVelocityY = Math.abs(velocityY);
            if (!mMasterVisible && velocityX > 0
                    && mAbsVelocityX >= mAbsVelocityY
                    && mAbsVelocityX > mViewConfig.getScaledMinimumFlingVelocity()
                    && mAbsVelocityX < mViewConfig.getScaledMaximumFlingVelocity()) {
                showMaster(true, 0);
                return true;
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };

    public ShowHideMasterLayout(Context context) {
        super(context);
        init();
    }

    public ShowHideMasterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShowHideMasterLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mGestureDetector = new GestureDetector(getContext(), mGestureListener);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mCount = getChildCount();

        /* Measure once to find the maximum child size */
        int sMaxHeight = 0;
        int sMaxWidth = 0;
        int mChildState = 0;

        for (int i = 0; i < mCount; i++) {
            View mChild = getChildAt(i);
            if (mChild.getVisibility() == GONE) {
                continue;
            }
            measureChildWithMargins(mChild, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams mLayoutParams = (MarginLayoutParams) mChild.getLayoutParams();
            sMaxWidth = Math.max(sMaxWidth, mChild.getMeasuredWidth() + mLayoutParams.leftMargin + mLayoutParams.rightMargin);
            sMaxHeight = Math.max(sMaxHeight, mChild.getMeasuredHeight() + mLayoutParams.topMargin + mLayoutParams.bottomMargin);
            mChildState = combineMeasuredStates(mChildState, mChild.getMeasuredState());
        }

        /* Account for padding too */
        sMaxWidth += getPaddingLeft() + getPaddingRight();
        sMaxHeight += getPaddingLeft() + getPaddingRight();

        /* Check against our minimum height and width */
        sMaxHeight = Math.max(sMaxHeight, getSuggestedMinimumHeight());
        sMaxWidth = Math.max(sMaxWidth, getSuggestedMinimumWidth());

        /* Set our own measured size */
        setMeasuredDimension(
                resolveSizeAndState(sMaxWidth, widthMeasureSpec, mChildState),
                resolveSizeAndState(sMaxHeight, heightMeasureSpec, mChildState << MEASURED_HEIGHT_STATE_SHIFT));

        /* Measure children for them to set their measured dimensions */
        for (int i = 0; i < mCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            MarginLayoutParams mLayoutParams = (MarginLayoutParams) child.getLayoutParams();
            int mChildWidthMeasureSpec;
            int mChildHeightMeasureSpec;

            if (mLayoutParams.width == LayoutParams.MATCH_PARENT) {
                mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth()
                        - getPaddingLeft() - getPaddingRight() - mLayoutParams.leftMargin
                        - mLayoutParams.rightMargin, MeasureSpec.EXACTLY);
            } else {
                mChildWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft()
                                + getPaddingRight() + mLayoutParams.leftMargin + mLayoutParams.rightMargin,
                        mLayoutParams.width);
            }

            if (mLayoutParams.height == LayoutParams.MATCH_PARENT) {
                mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight()
                        - getPaddingTop() - getPaddingBottom() - mLayoutParams.topMargin
                        - mLayoutParams.bottomMargin, MeasureSpec.EXACTLY);
            } else {
                mChildHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                        getPaddingTop() + getPaddingBottom() + mLayoutParams.topMargin
                                + mLayoutParams.bottomMargin, mLayoutParams.height);
            }

            child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        updateChildReferences();

        if (sMasterView == null || mDetailView == null) {
            return;
        }

        int sMasterWidth = sMasterView.getMeasuredWidth();
        MarginLayoutParams sMasterLp = (MarginLayoutParams) sMasterView.getLayoutParams();
        MarginLayoutParams mDetailLp = (MarginLayoutParams) mDetailView.getLayoutParams();
        mTranslateAmount = sMasterWidth + sMasterLp.leftMargin + sMasterLp.rightMargin;

        sMasterView.layout(l + sMasterLp.leftMargin, t + sMasterLp.topMargin, l
                + sMasterLp.leftMargin + sMasterWidth, b - sMasterLp.bottomMargin);

        mDetailView.layout(l + mDetailLp.leftMargin + mTranslateAmount, t + mDetailLp.topMargin, r
                - mDetailLp.rightMargin + mTranslateAmount, b - mDetailLp.bottomMargin);

        /* Update translationX values */
        if (!mIsAnimating) {
            float mTranslationX = mMasterVisible ? 0 : -mTranslateAmount;
            sMasterView.setTranslationX(mTranslationX);
            mDetailView.setTranslationX(mTranslationX);
        }
    }

    private void updateChildReferences() {
        int mChildCount = getChildCount();
        sMasterView = mChildCount > 0 ? getChildAt(0) : null;
        mDetailView = mChildCount > 1 ? getChildAt(1) : null;
    }

    /**
     * Calls {@link #showMaster(boolean, int, Runnable)} with a null runnable.
     */
    public void showMaster(boolean show, int flags) {
        showMaster(show, flags, null);
    }

    /**
     * Shows or hides the master pane.
     *
     * @param show             Whether or not to show the master pane.
     * @param flags            {@link #FLAG_IMMEDIATE} to show/hide immediately, or 0 to
     *                         animate.
     * @param completeRunnable An optional runnable to run when any animations
     *                         related to this are complete.
     */
    public void showMaster(boolean show, int flags, Runnable completeRunnable) {
        if (!mFirstShow && mMasterVisible == show) {
            return;
        }

        mShowMasterCompleteRunnable = completeRunnable;
        mFirstShow = false;
        mMasterVisible = show;
        updateChildReferences();

        if (sMasterView == null || mDetailView == null) {
            return;
        }

        float mTranslationX = show ? 0 : -mTranslateAmount;

        if ((flags & FLAG_IMMEDIATE) != 0) {
            sMasterView.setTranslationX(mTranslationX);
            mDetailView.setTranslationX(mTranslationX);
            if (mShowMasterCompleteRunnable != null) {
                mShowMasterCompleteRunnable.run();
                mShowMasterCompleteRunnable = null;
            }
        } else {
            long mDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            /* Animate if we have Honeycomb APIs, don't animate otherwise */
            mIsAnimating = true;
            AnimatorSet mAnimatorSet = new AnimatorSet();
            sMasterView.setLayerType(LAYER_TYPE_HARDWARE, null);
            mDetailView.setLayerType(LAYER_TYPE_HARDWARE, null);
            mAnimatorSet.play(ObjectAnimator.ofFloat(sMasterView, "translationX", mTranslationX).setDuration(mDuration))
                    .with(ObjectAnimator.ofFloat(mDetailView, "translationX", mTranslationX).setDuration(mDuration));
            mAnimatorSet.addListener(this);
            mAnimatorSet.start();
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // Really bad hack... we really shouldn't do this.
        // super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mMasterVisible) {
            mGestureDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && sMasterView != null && mMasterVisible) {
            if (event.getX() > mTranslateAmount) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mMasterVisible && mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && sMasterView != null && mMasterVisible) {
            if (event.getX() > mTranslateAmount) {
                showMaster(false, 0);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        mIsAnimating = false;
        sMasterView.setLayerType(LAYER_TYPE_NONE, null);
        mDetailView.setLayerType(LAYER_TYPE_NONE, null);
        requestLayout();
        if (mShowMasterCompleteRunnable != null) {
            mShowMasterCompleteRunnable.run();
            mShowMasterCompleteRunnable = null;
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        mIsAnimating = false;
        sMasterView.setLayerType(LAYER_TYPE_NONE, null);
        mDetailView.setLayerType(LAYER_TYPE_NONE, null);
        requestLayout();
        if (mShowMasterCompleteRunnable != null) {
            mShowMasterCompleteRunnable.run();
            mShowMasterCompleteRunnable = null;
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
        /* Nothing to do */
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        /* Nothing to do */
    }
}