/*
 * Copyright (C) 2012 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A FrameLayout whose contents are kept beneath an
 * {@link AlphaTouchInterceptorOverlay}. If necessary, you can specify your own
 * alpha-layer and manually manage its z-order.
 */
public class FrameLayoutWithOverlay extends FrameLayout implements OnClickListener {

	private AlphaTouchInterceptorOverlay mOverlay;

	@Nullable
	private OnOverlayClickListener mListener;

	/**
	 *
	 */
	public FrameLayoutWithOverlay(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public FrameLayoutWithOverlay(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		mOverlay = new AlphaTouchInterceptorOverlay(context);
		mOverlay.setOverlayOnClickListener(this);
		addView(mOverlay);
	}

	/**
	 * After adding the View, bring the overlay to the front to ensure it's
	 * always on top.
	 */
	@Override
	public void addView(View child) {
		super.addView(child);
		mOverlay.bringToFront();
	}

	/**
	 *
	 */
	@Override
	public void onClick(View v) {
		if (mListener != null) {
			mListener.onOverlayClick(this);
		}
	}

	/**
	 * Delegate to overlay: set the View that it will use as its alpha-layer. If
	 * none is set, the overlay will use its own alpha layer. Only necessary to
	 * set this if some child views need to appear above the alpha-layer.
	 */
	protected void setAlphaLayer(View layer) {
		mOverlay.setAlphaLayer(layer);
	}

	/**
	 * Delegate to overlay: set the alpha value on the alpha layer.
	 */
	public void setAlphaLayerValue(float alpha) {
		mOverlay.setAlphaLayerValue(alpha);
	}

	/**
	 * Delegate to overlay.
	 */
	public void setOverlayOnClickListener(OnOverlayClickListener listener) {
		mListener = listener;
	}

	/**
	 * Delegate to overlay.
	 */
	public void setOverlayClickable(boolean clickable) {
		mOverlay.setOverlayClickable(clickable);
	}

	/**
	 *
	 */
	public interface OnOverlayClickListener {

		void onOverlayClick(View view);
	}
}