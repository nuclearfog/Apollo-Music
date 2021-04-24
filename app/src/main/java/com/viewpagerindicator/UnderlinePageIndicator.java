/*
 * Copyright (C) 2012 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viewpagerindicator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;

import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;

/**
 * Draws a line for each page. The current page line is colored differently
 * than the unselected page lines.
 */
public class UnderlinePageIndicator extends View implements PageIndicator {

    private static final int INVALID_POINTER = -1;
    private static final int FADE_FRAME_MS = 30;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Runnable mFadeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mFades) return;

            int alpha = Math.max(mPaint.getAlpha() - mFadeBy, 0);
            mPaint.setAlpha(alpha);
            invalidate();
            if (alpha > 0) {
                postDelayed(this, FADE_FRAME_MS);
            }
        }
    };
    private boolean mFades;
    private int mFadeDelay;
    private int mFadeBy;

    private ViewPager mViewPager;
    private int mScrollState;
    private int mCurrentPage;
    private float mPositionOffset;
    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;

    public UnderlinePageIndicator(Context context) {
        this(context, null);
    }

    public UnderlinePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiUnderlinePageIndicatorStyle);
    }

    public UnderlinePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (isInEditMode()) return;

        Resources res = getResources();

        //Load defaults from resources
        boolean defaultFades = res.getBoolean(R.bool.default_underline_indicator_fades);
        int defaultFadeDelay = res.getInteger(R.integer.default_underline_indicator_fade_delay);
        int defaultFadeLength = res.getInteger(R.integer.default_underline_indicator_fade_length);
        int defaultSelectedColor = PreferenceUtils.getInstance(context).getDefaultThemeColor();

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UnderlinePageIndicator, defStyle, 0);

        setFades(a.getBoolean(R.styleable.UnderlinePageIndicator_fades, defaultFades));
        setSelectedColor(a.getColor(R.styleable.UnderlinePageIndicator_selectedColor, defaultSelectedColor));
        setFadeDelay(a.getInteger(R.styleable.UnderlinePageIndicator_fadeDelay, defaultFadeDelay));
        setFadeLength(a.getInteger(R.styleable.UnderlinePageIndicator_fadeLength, defaultFadeLength));

        Drawable background = a.getDrawable(R.styleable.UnderlinePageIndicator_android_background);
        if (background != null) {
            setBackground(background);
        }

        a.recycle();
        mTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
    }

    public void setFades(boolean fades) {
        if (fades != mFades) {
            mFades = fades;
            if (fades) {
                post(mFadeRunnable);
            } else {
                removeCallbacks(mFadeRunnable);
                mPaint.setAlpha(0xFF);
                invalidate();
            }
        }
    }


    public void setFadeDelay(int fadeDelay) {
        mFadeDelay = fadeDelay;
    }


    public void setFadeLength(int fadeLength) {
        mFadeBy = 0xFF / (fadeLength / FADE_FRAME_MS);
    }


    public void setSelectedColor(int selectedColor) {
        mPaint.setColor(selectedColor);
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewPager == null || mViewPager.getAdapter() == null) {
            return;
        }
        int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }
        if (mCurrentPage >= count) {
            setCurrentItem(count - 1);
            return;
        }
        int paddingLeft = getPaddingLeft();
        float pageWidth = (getWidth() - paddingLeft - getPaddingRight()) / (1f * count);
        float left = paddingLeft + pageWidth * (mCurrentPage + mPositionOffset);
        float right = left + pageWidth;
        float top = getPaddingTop();
        float bottom = getHeight() - getPaddingBottom();
        canvas.drawRect(left, top, right, bottom, mPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }
        if ((mViewPager == null) || mViewPager.getAdapter() == null || (mViewPager.getAdapter().getCount() == 0)) {
            return false;
        }

        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mLastMotionX = ev.getX();
                break;

            case MotionEvent.ACTION_MOVE: {
                int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                float x = ev.getX(activePointerIndex);
                float deltaX = x - mLastMotionX;

                if (!mIsDragging) {
                    if (Math.abs(deltaX) > mTouchSlop) {
                        mIsDragging = true;
                    }
                }

                if (mIsDragging) {
                    mLastMotionX = x;
                    if (mViewPager.isFakeDragging() || mViewPager.beginFakeDrag()) {
                        mViewPager.fakeDragBy(deltaX);
                    }
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    int count = mViewPager.getAdapter().getCount();
                    int width = getWidth();
                    float halfWidth = width / 2f;
                    float sixthWidth = width / 6f;

                    if ((mCurrentPage > 0) && (ev.getX() < halfWidth - sixthWidth)) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager.setCurrentItem(mCurrentPage - 1);
                        }
                        return true;
                    } else if ((mCurrentPage < count - 1) && (ev.getX() > halfWidth + sixthWidth)) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager.setCurrentItem(mCurrentPage + 1);
                        }
                        return true;
                    }
                }

                mIsDragging = false;
                mActivePointerId = INVALID_POINTER;
                if (mViewPager.isFakeDragging()) mViewPager.endFakeDrag();
                break;

            case ACTION_POINTER_DOWN: {
                int index = ev.getActionIndex();
                mLastMotionX = ev.getX(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case ACTION_POINTER_UP:
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                break;
        }

        return true;
    }


    private void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mViewPager.setCurrentItem(item);
        mCurrentPage = item;
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
        mPositionOffset = positionOffset;
        if (mFades) {
            if (positionOffsetPixels > 0) {
                removeCallbacks(mFadeRunnable);
                mPaint.setAlpha(0xFF);
            } else if (mScrollState != ViewPager.SCROLL_STATE_DRAGGING) {
                postDelayed(mFadeRunnable, mFadeDelay);
            }
        }
        invalidate();
    }

    @Override
    public void onPageSelected(int position) {
        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position;
            mPositionOffset = 0;
            invalidate();
            mFadeRunnable.run();
        }
    }


    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        @SuppressWarnings("UnusedDeclaration")
        public static Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }
    }
}
