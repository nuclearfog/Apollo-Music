package com.andrew.apollo.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * custom implementation of a vertical seekbar
 *
 * @author nuclerfog
 */
public class VerticalSeekBar extends SeekBar {

	public VerticalSeekBar(Context context) {
		this(context, null);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.seekBarStyle);
		// rotate view to vertical
		setRotation(270.0f);
	}

	@Override
	protected synchronized void onMeasure(int w, int h) {
		super.onMeasure(w, h);
		// fix: set width same as height
		setMeasuredDimension(h, h);
	}
}