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

package org.nuclearfog.apollo.ui.views;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.views.FrameLayoutWithOverlay.OnOverlayClickListener;
import org.nuclearfog.apollo.utils.AnimatorUtils;

/**
 * A custom {@link HorizontalScrollView} that displays up to two "tabs" in the
 * {@link ProfileActivity}. If the second tab is visible, a fraction of it will
 * overflow slightly onto the screen.
 */
public class ProfileTabCarousel extends HorizontalScrollView implements OnClickListener, OnTouchListener, OnOverlayClickListener, OnGlobalLayoutListener, AnimatorListener {

	/**
	 * Number of tabs
	 */
	private static final int TAB_COUNT = 2;

	/**
	 * First tab index
	 */
	private static final int TAB_INDEX_FIRST = 0;

	/**
	 * Second tab index
	 */
	private static final int TAB_INDEX_SECOND = 1;

	/**
	 * Alpha layer to be set on the lable view
	 */
	private static final float MAX_ALPHA = 0.6f;

	/**
	 * Y coordinate of the tab at the given index was selected
	 */
	private static final float[] mYCoordinateArray = new float[TAB_COUNT];

	/**
	 * Tab width as defined as a fraction of the screen width
	 */
	private final float tabWidthScreenWidthFraction;

	/**
	 * Tab height as defined as a fraction of the screen width
	 */
	private final float tabHeightScreenWidthFraction;

	/**
	 * Height of the tab label
	 */
	private final int mTabDisplayLabelHeight;

	/**
	 * Height in pixels of the shadow under the tab carousel
	 */
	private final int mTabShadowHeight;

	/**
	 * The last scrolled position
	 */
	private int mLastScrollPosition = 0;

	/**
	 * Allowed horizontal scroll duration
	 */
	private int mAllowedHorizontalScrollLength = Integer.MIN_VALUE;

	/**
	 * Allowed vertical scroll duration
	 */
	private int mAllowedVerticalScrollLength = Integer.MIN_VALUE;

	/**
	 * Current tab index
	 */
	private int mCurrentTab = TAB_INDEX_FIRST;

	/**
	 * Factor to scale scroll-amount sent to listeners
	 */
	private float mScrollScaleFactor = 1.0f;

	private boolean mScrollToCurrentTab = false;
	private boolean mTabCarouselIsAnimating;
	private boolean mEnableSwipe;
	private CarouselTab mFirstTab, mSecondTab;
	private Listener mListener;

	/**
	 *
	 */
	public ProfileTabCarousel(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public ProfileTabCarousel(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		View view = LayoutInflater.from(context).inflate(R.layout.profile_tab_carousel, this, false);
		mFirstTab = view.findViewById(R.id.profile_tab_carousel_tab_one);
		mSecondTab = view.findViewById(R.id.profile_tab_carousel_tab_two);
		addView(view);
		updateAlphaLayers();

		mTabDisplayLabelHeight = context.getResources().getDimensionPixelSize(R.dimen.profile_photo_shadow_height);
		mTabShadowHeight = context.getResources().getDimensionPixelSize(R.dimen.profile_carousel_label_height);
		tabWidthScreenWidthFraction = context.getResources().getFraction(R.fraction.tab_width_screen_percentage, 1, 1);
		tabHeightScreenWidthFraction = context.getResources().getFraction(R.fraction.tab_height_screen_percentage, 1, 1);
		// disable scroll effects which can cause bugs
		setHorizontalScrollBarEnabled(false);
		setHorizontalFadingEdgeEnabled(false);
		setOverScrollMode(OVER_SCROLL_NEVER);

		mFirstTab.getPhoto().setOnClickListener(this);
		mFirstTab.setOverlayOnClickListener(this);
		mSecondTab.setOverlayOnClickListener(this);
		setOnTouchListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
		int tabWidth = Math.round(tabWidthScreenWidthFraction * screenWidth);
		int tabHeight = Math.round(screenWidth * tabHeightScreenWidthFraction) + mTabShadowHeight;
		mAllowedHorizontalScrollLength = tabWidth * TAB_COUNT - screenWidth;
		if (mAllowedHorizontalScrollLength == 0) {
			mScrollScaleFactor = 1.0f;
		} else {
			mScrollScaleFactor = (float) screenWidth / mAllowedHorizontalScrollLength;
		}
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			if (mEnableSwipe) {
				// Add 1 dip of separation between the tabs
				int seperatorPixels = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()) + 0.5f);
				int width = MeasureSpec.makeMeasureSpec(TAB_COUNT * tabWidth + seperatorPixels, MeasureSpec.EXACTLY);
				int height = MeasureSpec.makeMeasureSpec(tabHeight, MeasureSpec.EXACTLY);
				child.measure(width, height);
			} else {
				int width = MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY);
				int height = MeasureSpec.makeMeasureSpec(tabHeight, MeasureSpec.EXACTLY);
				child.measure(width, height);
			}
		}
		mAllowedVerticalScrollLength = tabHeight - mTabDisplayLabelHeight - mTabShadowHeight;
		setMeasuredDimension(resolveSize(screenWidth, widthMeasureSpec), resolveSize(tabHeight, heightMeasureSpec));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (mScrollToCurrentTab) {
			mScrollToCurrentTab = false;
			getViewTreeObserver().addOnGlobalLayoutListener(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onScrollChanged(int x, int y, int oldX, int oldY) {
		super.onScrollChanged(x, y, oldX, oldY);
		if (mLastScrollPosition != x) {
			int scaledL = (int) (x * mScrollScaleFactor);
			int oldScaledL = (int) (oldX * mScrollScaleFactor);
			mListener.onScrollChanged(scaledL, oldScaledL);
			mLastScrollPosition = x;
			updateAlphaLayers();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.profile_tab_photo) {
			mListener.onAlbumArtSelected();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mListener.onTouchDown();
				return true;

			case MotionEvent.ACTION_UP:
				mListener.onTouchUp();
				return true;
		}
		return super.onTouchEvent(event);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean mInterceptTouch = super.onInterceptTouchEvent(ev);
		if (mInterceptTouch) {
			mListener.onTouchDown();
		}
		return mInterceptTouch;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onGlobalLayout() {
		/* Layout pass done, unregister for further events */
		getViewTreeObserver().removeOnGlobalLayoutListener(this);
		scrollTo(mCurrentTab == TAB_INDEX_FIRST ? 0 : mAllowedHorizontalScrollLength, 0);
		updateAlphaLayers();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onOverlayClick(View view) {
		if (view.getId() == R.id.profile_tab_carousel_tab_one) {
			mListener.onTabSelected(TAB_INDEX_FIRST);
		} else if (view.getId() == R.id.profile_tab_carousel_tab_two) {
			mListener.onTabSelected(TAB_INDEX_SECOND);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationCancel(Animator animation) {
		mTabCarouselIsAnimating = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationEnd(Animator animation) {
		mTabCarouselIsAnimating = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationRepeat(Animator animation) {
		mTabCarouselIsAnimating = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationStart(Animator animation) {
		mTabCarouselIsAnimating = true;
	}

	/**
	 * Reset the carousel to the start position (i.e. because new data will be
	 * loaded in for a different contact).
	 */
	public void reset() {
		scrollTo(0, 0);
		setCurrentTab(TAB_INDEX_FIRST);
		moveToYCoordinate(TAB_INDEX_FIRST, 0);
	}

	/**
	 * Restore the Y position of this view to the last manually requested value.
	 * This can be done after the parent has been re-laid out again, where this
	 * view's position could have been lost if the view laid outside its
	 * parent's bounds.
	 */
	public void restoreYCoordinate(int duration, int tabIndex) {
		float storedYCoordinate = getStoredYCoordinateForTab(tabIndex);
		AnimatorUtils.translate(this, storedYCoordinate, duration, this);
	}

	/**
	 * Request that the view move to the given Y coordinate. Also store the Y
	 * coordinate as the last requested Y coordinate for the given tabIndex.
	 */
	public void moveToYCoordinate(int tabIndex, float y) {
		storeYCoordinate(tabIndex, y);
		restoreYCoordinate(0, tabIndex);
	}

	/**
	 * Store this information as the last requested Y coordinate for the given
	 * tabIndex.
	 */
	public void storeYCoordinate(int tabIndex, float y) {
		mYCoordinateArray[tabIndex] = y;
	}

	/**
	 * Returns the stored Y coordinate of this view the last time the user was
	 * on the selected tab given by tabIndex.
	 */
	public float getStoredYCoordinateForTab(int tabIndex) {
		return mYCoordinateArray[tabIndex];
	}

	/**
	 * Returns the number of pixels that this view can be scrolled horizontally.
	 */
	public int getAllowedHorizontalScrollLength() {
		return mAllowedHorizontalScrollLength;
	}

	/**
	 * Returns the number of pixels that this view can be scrolled vertically
	 * while still allowing the tab labels to still show.
	 */
	public int getAllowedVerticalScrollLength() {
		return mAllowedVerticalScrollLength;
	}

	/**
	 * Sets the correct alpha layers over the tabs.
	 */
	private void updateAlphaLayers() {
		float alpha = mLastScrollPosition * MAX_ALPHA / mAllowedHorizontalScrollLength;
		alpha = AlphaTouchInterceptorOverlay.clamp(alpha, 0.0f, 1.0f);
		mFirstTab.setAlphaLayerValue(alpha);
		mSecondTab.setAlphaLayerValue(MAX_ALPHA - alpha);
	}

	/**
	 * set image of the album art
	 *
	 * @param uri path to the local imagefile
	 */
	public void setAlbumArt(Uri uri) {
		mFirstTab.getPhoto().setImageURI(uri);
	}

	/**
	 * @return The {@link ImageView} used to set the album art.
	 */
	public ImageView getAlbumArt() {
		return mFirstTab.getAlbumArt();
	}

	/**
	 * @return True if the carousel is currently animating, false otherwise
	 */
	public boolean isTabCarouselIsAnimating() {
		return mTabCarouselIsAnimating;
	}

	/**
	 * Updates the tab selection.
	 */
	public void setCurrentTab(int position) {
		CarouselTab selected, deselected;

		switch (position) {
			case TAB_INDEX_FIRST:
				selected = mFirstTab;
				deselected = mSecondTab;
				break;

			case TAB_INDEX_SECOND:
				selected = mSecondTab;
				deselected = mFirstTab;
				break;

			default:
				return;
		}
		selected.setSelected(true);
		selected.showSelectedState();
		selected.setOverlayClickable(false);
		deselected.setSelected(false);
		deselected.showDeselectedState();
		deselected.setOverlayClickable(true);
		mCurrentTab = position;
	}

	/**
	 * Set the given {@link Listener} to handle carousel events.
	 */
	public void setListener(Listener listener) {
		mListener = listener;
	}

	/**
	 * Sets the artist image header
	 *
	 * @param artistName The artist name used to find the cached artist image
	 *                   and used to find the last album played by the artist
	 */
	public void setArtistProfileHeader(String artistName) {
		mFirstTab.setLabel(getResources().getString(R.string.page_songs));
		mSecondTab.setLabel(getResources().getString(R.string.page_albums));
		mFirstTab.setArtistPhoto(artistName);
		mSecondTab.setArtistAlbumPhoto(artistName);
		mEnableSwipe = true;
	}

	/**
	 * Sets the album image header
	 *
	 * @param albumName  The key used to find the cached album art
	 * @param artistName The artist name used to find the cached artist image
	 */
	public void setAlbumProfileHeader(String albumName, String artistName) {
		mFirstTab.setLabel(getResources().getString(R.string.page_songs));
		mFirstTab.setAlbumPhoto(albumName, artistName);

		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * Sets the playlist or genre image header
	 *
	 * @param profileName The key used to find the cached image for a playlist or genre
	 */
	public void setPlaylistOrGenreProfileHeader(String profileName) {
		mFirstTab.setDefault();
		mFirstTab.setLabel(getResources().getString(R.string.page_songs));
		mFirstTab.setPlaylistOrGenrePhoto(profileName);
		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * Used to fetch for the album art via Last.fm.
	 *
	 * @param albumName  The name of the album in the profile the user is viewing.
	 * @param artistName The name of the album artist in the profile the user is viewing.
	 */
	public void fetchAlbumPhoto(String albumName, String artistName) {
		mFirstTab.fetchAlbumPhoto(albumName, artistName);
	}

	/**
	 * Interface for callbacks invoked when the user interacts with the
	 * carousel.
	 */
	public interface Listener {

		void onTouchDown();

		void onTouchUp();

		void onScrollChanged(int l, int oldl);

		void onTabSelected(int position);

		void onAlbumArtSelected();
	}
}