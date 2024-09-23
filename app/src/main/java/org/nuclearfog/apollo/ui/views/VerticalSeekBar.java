package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * custom implementation of a vertical seekbar
 *
 * @author nuclerfog
 */
public class VerticalSeekBar extends AppCompatSeekBar {

	/**
	 *
	 */
	public VerticalSeekBar(@NonNull Context context) {
		this(context, null);
	}

	/**
	 *
	 */
	public VerticalSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs, android.R.attr.seekBarStyle);
		// rotate view to vertical
		setRotation(270.0f);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void onMeasure(int w, int h) {
		super.onMeasure(w, h);
		// fix: set width same as height
		setMeasuredDimension(h, h);
	}
}