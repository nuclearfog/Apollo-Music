/*
 * Copyright (C) 2010 Daniel Nilsson Copyright (C) 2012 THe CyanogenMod Project
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.andrew.apollo.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * This class draws a panel which which will be filled with a color which can be
 * set. It can be used to show the currently selected color which you will get
 * from the {@link ColorPickerView}.
 *
 * @author Daniel Nilsson
 */
public class ColorPanelView extends View {

    /**
     * The width in pixels of the border surrounding the color panel.
     */
    private final static float BORDER_WIDTH_PX = 1;

    private static float mDensity = 1f;

    private Paint mBorderPaint;

    private Paint mColorPaint;

    private RectF mDrawingRect;

    private RectF mColorRect;

    private AlphaPatternDrawable mAlphaPattern;

    public ColorPanelView(Context context) {
        this(context, null);
    }

    public ColorPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mBorderPaint = new Paint();
        mColorPaint = new Paint();
        mDensity = getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        RectF rect = mColorRect;

        int mBorderColor = 0xff6E6E6E;
        mBorderPaint.setColor(mBorderColor);
        canvas.drawRect(mDrawingRect, mBorderPaint);

        if (mAlphaPattern != null) {
            mAlphaPattern.draw(canvas);
        }
        mColorPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, mColorPaint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawingRect = new RectF();
        mDrawingRect.left = getPaddingLeft();
        mDrawingRect.right = w - getPaddingRight();
        mDrawingRect.top = getPaddingTop();
        mDrawingRect.bottom = h - getPaddingBottom();

        setUpColorRect();
    }

    private void setUpColorRect() {
        RectF dRect = mDrawingRect;

        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = dRect.bottom - BORDER_WIDTH_PX;
        float right = dRect.right - BORDER_WIDTH_PX;

        mColorRect = new RectF(left, top, right, bottom);
        mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));
        mAlphaPattern.setBounds(Math.round(mColorRect.left), Math.round(mColorRect.top), Math.round(mColorRect.right), Math.round(mColorRect.bottom));
    }
}