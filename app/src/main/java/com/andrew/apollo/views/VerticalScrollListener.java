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

package com.andrew.apollo.views;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

@SuppressLint("NewApi")
public class VerticalScrollListener implements OnScrollListener {

    /* Used to determine the off set to scroll the header */
    private final ScrollableHeader mHeader;

    private final ProfileTabCarousel mTabCarousel;

    private final int mPageIndex;

    public VerticalScrollListener(ScrollableHeader header, ProfileTabCarousel carousel, int pageIndex) {
        mHeader = header;
        mTabCarousel = carousel;
        mPageIndex = pageIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (mTabCarousel == null || mTabCarousel.isTabCarouselIsAnimating()) {
            return;
        }

        View top = view.getChildAt(firstVisibleItem);
        if (top == null) {
            return;
        }

        if (firstVisibleItem != 0) {
            mTabCarousel.moveToYCoordinate(mPageIndex, -mTabCarousel.getAllowedVerticalScrollLength());
            return;
        }

        float y = view.getChildAt(firstVisibleItem).getY();
        float amtToScroll = Math.max(y, -mTabCarousel.getAllowedVerticalScrollLength());
        mTabCarousel.moveToYCoordinate(mPageIndex, amtToScroll);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mHeader != null) {
            mHeader.onScrollStateChanged(scrollState);
        }
    }

    /**
     * Defines the header to be scrolled.
     */
    public interface ScrollableHeader {

        /* Used the pause the disk cache while scrolling */
        void onScrollStateChanged(int scrollState);
    }
}