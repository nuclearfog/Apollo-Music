/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A custom {@link ViewGroup} used to make it's children into perfect squares.
 * Useful when dealing with grid images and especially album art.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SquareView extends ViewGroup {

    /**
     * @param context The {@link Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public SquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View mChildren = getChildAt(0);
        mChildren.measure(widthMeasureSpec, heightMeasureSpec); // todo check
        int size = resolveSize(mChildren.getMeasuredWidth(), widthMeasureSpec);
        mChildren.measure(size, size);
        setMeasuredDimension(size, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int u, int r, int d) {
        getChildAt(0).layout(0, 0, r - l, d - u);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        forceLayout();
    }
}