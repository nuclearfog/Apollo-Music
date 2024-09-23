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

package org.nuclearfog.apollo.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.ui.drawables.AlphaPatternDrawable;

/**
 * Displays a color picker to the user and allow them to select a color. A
 * slider for the alpha channel is also available. Enable it by setting
 * setAlphaSliderVisible(boolean) to true.
 *
 * @author Daniel Nilsson
 */
public class ColorPickerView extends View {

	/**
	 * The width in pixels of the border surrounding all color panels.
	 */
	private final static float BORDER_WIDTH_PX = 1;
	private final static int PANEL_SAT_VAL = 0;
	private final static int PANEL_HUE = 1;
	private final static int PANEL_ALPHA = 2;
	private final static int SLIDER_COLOR = 0xff1c1c1c;
	private final static String TEXT_ALPHA_SLIDER = "Alpha";

	/**
	 * The width in dp of the hue panel.
	 */
	private float HUE_PANEL_WIDTH = 30f;

	/**
	 * The height in dp of the alpha panel
	 */
	private float ALPHA_PANEL_HEIGHT = 20f;

	/**
	 * The distance in dp between the different color panels.
	 */
	private float PANEL_SPACING = 10f;

	/**
	 * The radius in dp of the color palette tracker circle.
	 */
	private float PALETTE_CIRCLE_TRACKER_RADIUS = 5f;

	/**
	 * The dp which the tracker of the hue or alpha panel will extend outside of
	 * its bounds.
	 */
	private float RECTANGLE_TRACKER_OFFSET = 2f;

	/**
	 * To remember which panel that has the "focus" when processing hardware
	 * button data.
	 */
	private int mLastTouchedPanel = PANEL_SAT_VAL;
	/**
	 * Offset from the edge we must have or else the finger tracker will get
	 * clipped when it is drawn outside of the view.
	 */
	private float mDrawingOffset;

	@Nullable
	private OnColorChangedListener mListener;
	@Nullable
	private AlphaPatternDrawable mAlphaPattern;
	@Nullable
	private RectF mDrawingRect, mSatValRect, mHueRect, mAlphaRect;
	@Nullable
	private Shader mValShader, mHueShader;
	@Nullable
	private Point mStartTouchPoint;

	private Paint mSatValPaint = new Paint();
	private Paint mSatValTrackerPaint = new Paint();
	private Paint mHuePaint = new Paint();
	private Paint mHueTrackerPaint = new Paint();
	private Paint mAlphaPaint = new Paint();
	private Paint mAlphaTextPaint = new Paint();
	private Paint mBorderPaint = new Paint();

	private int mAlpha = 0xff;
	private float mHue = 360f;
	private float mSat = 0f;
	private float mVal = 0f;
	private float mDensity;
	private int mBorderColor = 0xff6E6E6E;
	private boolean mShowAlphaPanel = false;

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context) {
		this(context, null);
	}

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// init
		mDensity = getContext().getResources().getDisplayMetrics().density;
		PALETTE_CIRCLE_TRACKER_RADIUS *= mDensity;
		RECTANGLE_TRACKER_OFFSET *= mDensity;
		HUE_PANEL_WIDTH *= mDensity;
		ALPHA_PANEL_HEIGHT *= mDensity;
		PANEL_SPACING = PANEL_SPACING * mDensity;
		mDrawingOffset = calculateRequiredOffset();
		// init paint tools
		mSatValTrackerPaint.setStyle(Style.STROKE);
		mSatValTrackerPaint.setStrokeWidth(2f * mDensity);
		mSatValTrackerPaint.setAntiAlias(true);
		mHueTrackerPaint.setColor(SLIDER_COLOR);
		mHueTrackerPaint.setStyle(Style.STROKE);
		mHueTrackerPaint.setStrokeWidth(2f * mDensity);
		mHueTrackerPaint.setAntiAlias(true);
		mAlphaTextPaint.setColor(SLIDER_COLOR);
		mAlphaTextPaint.setTextSize(14f * mDensity);
		mAlphaTextPaint.setAntiAlias(true);
		mAlphaTextPaint.setTextAlign(Align.CENTER);
		mAlphaTextPaint.setFakeBoldText(true);
		// Needed for receiving track ball motion events.
		setFocusable(true);
		setFocusableInTouchMode(true);
	}


	@Override
	protected void onDraw(Canvas canvas) {
		if (mDrawingRect != null && mDrawingRect.width() > 0 && mDrawingRect.height() > 0) {
			drawSatValPanel(canvas);
			drawHuePanel(canvas);
			drawAlphaPanel(canvas);
		}
	}


	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		boolean update = false;
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			switch (mLastTouchedPanel) {
				case PANEL_SAT_VAL:
					float sat = mSat + x / 50f;
					float val = mVal - y / 50f;
					if (sat < 0f) {
						sat = 0f;
					} else if (sat > 1f) {
						sat = 1f;
					}
					if (val < 0f) {
						val = 0f;
					} else if (val > 1f) {
						val = 1f;
					}
					mSat = sat;
					mVal = val;
					update = true;
					break;

				case PANEL_HUE:
					float hue = mHue - y * 10f;
					if (hue < 0f) {
						hue = 0f;
					} else if (hue > 360f) {
						hue = 360f;
					}
					mHue = hue;
					update = true;
					break;

				case PANEL_ALPHA:
					if (mShowAlphaPanel && mAlphaRect != null) {
						int alpha = Math.round(mAlpha - x * 10.0f);
						if (alpha < 0) {
							alpha = 0;
						} else if (alpha > 0xff) {
							alpha = 0xff;
						}
						mAlpha = alpha;
						update = true;
					}
					break;
			}
		}
		if (update) {
			if (mListener != null) {
				mListener.onColorChanged(getColor());
			}
			invalidate();
			return true;
		}
		return super.onTrackballEvent(event);
	}


	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean update = false;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mStartTouchPoint = new Point(Math.round(event.getX()), Math.round(event.getY()));
				update = moveTrackersIfNeeded(event);
				break;

			case MotionEvent.ACTION_MOVE:
				update = moveTrackersIfNeeded(event);
				break;

			case MotionEvent.ACTION_UP:
				mStartTouchPoint = null;
				update = moveTrackersIfNeeded(event);
				break;
		}
		if (update) {
			if (mListener != null) {
				mListener.onColorChanged(getColor());
			}
			invalidate();
			return true;
		}
		return super.onTouchEvent(event);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width;
		int height;
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
		int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);
		widthAllowed = chooseWidth(widthMode, widthAllowed);
		heightAllowed = chooseHeight(heightMode, heightAllowed);

		if (!mShowAlphaPanel) {
			height = Math.round(widthAllowed - PANEL_SPACING - HUE_PANEL_WIDTH);
			// If calculated height (based on the width) is more than the
			// allowed height.
			if (height > heightAllowed) {
				height = heightAllowed;
				width = Math.round(height + PANEL_SPACING + HUE_PANEL_WIDTH);
			} else {
				width = widthAllowed;
			}
		} else {
			width = Math.round(heightAllowed - ALPHA_PANEL_HEIGHT + HUE_PANEL_WIDTH);
			if (width > widthAllowed) {
				width = widthAllowed;
				height = Math.round(widthAllowed - HUE_PANEL_WIDTH + ALPHA_PANEL_HEIGHT);
			} else {
				height = heightAllowed;
			}
		}
		setMeasuredDimension(width, height);
	}


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mDrawingRect = new RectF();
		mDrawingRect.left = mDrawingOffset + getPaddingLeft();
		mDrawingRect.right = w - mDrawingOffset - getPaddingRight();
		mDrawingRect.top = mDrawingOffset + getPaddingTop();
		mDrawingRect.bottom = h - mDrawingOffset - getPaddingBottom();
		setUpSatValRect();
		setUpHueRect();
		setUpAlphaRect();
	}

	/**
	 * Set a OnColorChangedListener to get notified when the color selected by
	 * the user has changed.
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener) {
		mListener = listener;
	}

	/**
	 * Get the current color this view is showing.
	 *
	 * @return the current color.
	 */
	public int getColor() {
		float[] color = new float[]{mHue, mSat, mVal};
		return Color.HSVToColor(mAlpha, color);
	}

	/**
	 * Set the color the view should show.
	 *
	 * @param color The color that should be selected.
	 */
	public void setColor(int color) {
		int alpha = Color.alpha(color);
		int red = Color.red(color);
		int blue = Color.blue(color);
		int green = Color.green(color);

		float[] hsv = new float[3];

		Color.RGBToHSV(red, green, blue, hsv);

		mAlpha = alpha;
		mHue = hsv[0];
		mSat = hsv[1];
		mVal = hsv[2];

		invalidate();
	}

	/**
	 * set the color resource
	 */
	public void setColorResource(@ColorRes int colorRes) {
		int color = getResources().getColor(colorRes);
		setColor(color);
	}


	private float calculateRequiredOffset() {
		float offset = Math.max(PALETTE_CIRCLE_TRACKER_RADIUS, RECTANGLE_TRACKER_OFFSET);
		offset = Math.max(offset, BORDER_WIDTH_PX * mDensity);
		return offset * 1.5f;
	}


	private int[] buildHueColorArray() {
		int[] hue = new int[361];
		int count = 0;
		for (int i = hue.length - 1; i >= 0; i--, count++) {
			float[] color = new float[]{i, 1f, 1f};
			hue[count] = Color.HSVToColor(color);
		}
		return hue;
	}


	private void drawSatValPanel(Canvas canvas) {
		mBorderPaint.setColor(mBorderColor);
		if (mSatValRect != null && mDrawingRect != null) {
			canvas.drawRect(mDrawingRect.left, mDrawingRect.top, mSatValRect.right + BORDER_WIDTH_PX, mSatValRect.bottom + BORDER_WIDTH_PX, mBorderPaint);
			if (mValShader == null) {
				mValShader = new LinearGradient(mSatValRect.left, mSatValRect.top, mSatValRect.left, mSatValRect.bottom, 0xffffffff, 0xff000000, TileMode.CLAMP);
			}
			int rgb = Color.HSVToColor(new float[]{mHue, 1f, 1f});
			Shader mSatShader = new LinearGradient(mSatValRect.left, mSatValRect.top, mSatValRect.right, mSatValRect.top, 0xffffffff, rgb, TileMode.CLAMP);
			ComposeShader mShader = new ComposeShader(mValShader, mSatShader, PorterDuff.Mode.MULTIPLY);
			mSatValPaint.setShader(mShader);

			canvas.drawRect(mSatValRect, mSatValPaint);
			Point p = satValToPoint(mSat, mVal);

			mSatValTrackerPaint.setColor(0xff000000);
			canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS - mDensity, mSatValTrackerPaint);
			mSatValTrackerPaint.setColor(0xffdddddd);
			canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS, mSatValTrackerPaint);
		}
	}


	private void drawHuePanel(Canvas canvas) {
		mBorderPaint.setColor(mBorderColor);
		if (mHueRect != null) {
			canvas.drawRect(mHueRect.left - BORDER_WIDTH_PX, mHueRect.top - BORDER_WIDTH_PX, mHueRect.right + BORDER_WIDTH_PX, mHueRect.bottom + BORDER_WIDTH_PX, mBorderPaint);
			if (mHueShader == null) {
				mHueShader = new LinearGradient(mHueRect.left, mHueRect.top, mHueRect.left, mHueRect.bottom, buildHueColorArray(), null, TileMode.CLAMP);
				mHuePaint.setShader(mHueShader);
			}
			canvas.drawRect(mHueRect, mHuePaint);

			float rectHeight = 4 * mDensity / 2;
			Point p = hueToPoint(mHue);
			RectF r = new RectF();
			r.left = mHueRect.left - RECTANGLE_TRACKER_OFFSET;
			r.right = mHueRect.right + RECTANGLE_TRACKER_OFFSET;
			r.top = p.y - rectHeight;
			r.bottom = p.y + rectHeight;
			canvas.drawRoundRect(r, 2, 2, mHueTrackerPaint);
		}
	}


	private void drawAlphaPanel(Canvas canvas) {
		if (!mShowAlphaPanel || mAlphaRect == null || mAlphaPattern == null) {
			return;
		}
		RectF rect = mAlphaRect;
		mBorderPaint.setColor(mBorderColor);
		canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, mBorderPaint);
		mAlphaPattern.draw(canvas);

		float[] hsv = new float[]{mHue, mSat, mVal};
		int color = Color.HSVToColor(hsv);
		int acolor = Color.HSVToColor(0, hsv);
		float rectWidth = 4 * mDensity / 2;

		Shader mAlphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, color, acolor, TileMode.CLAMP);
		mAlphaPaint.setShader(mAlphaShader);
		canvas.drawRect(rect, mAlphaPaint);
		canvas.drawText(TEXT_ALPHA_SLIDER, rect.centerX(), rect.centerY() + 4 * mDensity, mAlphaTextPaint);

		Point p = alphaToPoint(mAlpha);
		RectF r = new RectF();
		r.left = p.x - rectWidth;
		r.right = p.x + rectWidth;
		r.top = rect.top - RECTANGLE_TRACKER_OFFSET;
		r.bottom = rect.bottom + RECTANGLE_TRACKER_OFFSET;
		canvas.drawRoundRect(r, 2, 2, mHueTrackerPaint);
	}


	private Point hueToPoint(float hue) {
		Point point = new Point();
		if (mHueRect != null) {
			float height = mHueRect.height();
			point.y = Math.round(height - hue * height / 360f + mHueRect.top);
			point.x = Math.round(mHueRect.left);
		}
		return point;
	}


	private Point satValToPoint(float sat, float val) {
		Point point = new Point();
		if (mSatValRect != null) {
			point.x = Math.round(sat * mSatValRect.width() + mSatValRect.left);
			point.y = Math.round((1f - val) * mSatValRect.height() + mSatValRect.top);
		}
		return point;
	}


	private Point alphaToPoint(int alpha) {
		Point point = new Point();
		if (mAlphaRect != null) {
			float width = mAlphaRect.width();
			point.x = Math.round(width - alpha * width / 0xff + mAlphaRect.left);
			point.y = Math.round(mAlphaRect.top);
		}
		return point;
	}


	private float[] pointToSatVal(float x, float y) {
		float[] result = new float[2];
		if (mSatValRect != null) {
			float width = mSatValRect.width();
			float height = mSatValRect.height();
			if (x < mSatValRect.left) {
				x = 0f;
			} else if (x > mSatValRect.right) {
				x = width;
			} else {
				x = x - mSatValRect.left;
			}
			if (y < mSatValRect.top) {
				y = 0f;
			} else if (y > mSatValRect.bottom) {
				y = height;
			} else {
				y = y - mSatValRect.top;
			}
			if (width != 0) {
				result[0] = 1.f / width * x;
			}
			if (height != 0) {
				result[1] = 1.f - 1.f / height * y;
			}
		}
		return result;
	}


	private float pointToHue(float y) {
		if (mHueRect != null) {
			float height = mHueRect.height();
			if (y < mHueRect.top) {
				y = 0f;
			} else if (y > mHueRect.bottom) {
				y = height;
			} else {
				y = y - mHueRect.top;
			}
			if (height != 0) {
				return 360f - y * 360f / height;
			}
		}
		return 0f;
	}


	private int pointToAlpha(int x) {
		if (mAlphaRect != null) {
			int width = Math.round(mAlphaRect.width());
			if (x < mAlphaRect.left) {
				x = 0;
			} else if (x > mAlphaRect.right) {
				x = width;
			} else {
				x = x - Math.round(mAlphaRect.left);
			}
			return 0xff - x * 0xff / width;
		}
		return 0;
	}


	private boolean moveTrackersIfNeeded(MotionEvent event) {
		if (mStartTouchPoint == null || mHueRect == null || mSatValRect == null) {
			return false;
		}
		boolean update = false;
		int startX = mStartTouchPoint.x;
		int startY = mStartTouchPoint.y;

		if (mHueRect.contains(startX, startY)) {
			mLastTouchedPanel = PANEL_HUE;
			mHue = pointToHue(event.getY());
			update = true;
		} else if (mSatValRect.contains(startX, startY)) {
			mLastTouchedPanel = PANEL_SAT_VAL;
			float[] result = pointToSatVal(event.getX(), event.getY());
			mSat = result[0];
			mVal = result[1];
			update = true;
		} else if (mAlphaRect != null && mAlphaRect.contains(startX, startY)) {
			mLastTouchedPanel = PANEL_ALPHA;
			mAlpha = pointToAlpha(Math.round(event.getX()));
			update = true;
		}
		return update;
	}


	private int chooseWidth(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return getPrefferedWidth();
		}
	}


	private int chooseHeight(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return getPrefferedHeight();
		}
	}


	private int getPrefferedWidth() {
		int width = getPrefferedHeight();
		if (mShowAlphaPanel) {
			width -= Math.round(PANEL_SPACING + ALPHA_PANEL_HEIGHT);
		}
		return Math.round(width + HUE_PANEL_WIDTH + PANEL_SPACING);

	}


	private int getPrefferedHeight() {
		int height = Math.round(200 * mDensity);
		if (mShowAlphaPanel) {
			height += Math.round(PANEL_SPACING + ALPHA_PANEL_HEIGHT);
		}
		return height;
	}


	private void setUpSatValRect() {
		if (mDrawingRect != null) {
			float panelSide = mDrawingRect.height() - BORDER_WIDTH_PX * 2;
			if (mShowAlphaPanel) {
				panelSide -= PANEL_SPACING + ALPHA_PANEL_HEIGHT;
			}
			float left = mDrawingRect.left + BORDER_WIDTH_PX;
			float top = mDrawingRect.top + BORDER_WIDTH_PX;
			float bottom = top + panelSide;
			float right = left + panelSide;
			mSatValRect = new RectF(left, top, right, bottom);
		}
	}


	private void setUpHueRect() {
		if (mDrawingRect != null) {
			float left = mDrawingRect.right - HUE_PANEL_WIDTH + BORDER_WIDTH_PX;
			float top = mDrawingRect.top + BORDER_WIDTH_PX;
			float bottom = mDrawingRect.bottom - BORDER_WIDTH_PX - (mShowAlphaPanel ? PANEL_SPACING + ALPHA_PANEL_HEIGHT : 0);
			float right = mDrawingRect.right - BORDER_WIDTH_PX;
			mHueRect = new RectF(left, top, right, bottom);
		}
	}


	private void setUpAlphaRect() {
		if (mShowAlphaPanel && mDrawingRect != null) {
			float left = mDrawingRect.left + BORDER_WIDTH_PX;
			float top = mDrawingRect.bottom - ALPHA_PANEL_HEIGHT + BORDER_WIDTH_PX;
			float bottom = mDrawingRect.bottom - BORDER_WIDTH_PX;
			float right = mDrawingRect.right - BORDER_WIDTH_PX;
			mAlphaRect = new RectF(left, top, right, bottom);
			mAlphaPattern = new AlphaPatternDrawable(Math.round(5.0f * mDensity));
			mAlphaPattern.setBounds(Math.round(mAlphaRect.left), Math.round(mAlphaRect.top), Math.round(mAlphaRect.right), Math.round(mAlphaRect.bottom));
		}
	}

	/**
	 *
	 */
	public interface OnColorChangedListener {

		void onColorChanged(int color);
	}
}