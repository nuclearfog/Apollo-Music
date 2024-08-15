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

import static android.graphics.PorterDuff.Mode.MULTIPLY;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * A custom {@link AppCompatImageButton} that represents the "repeat" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class RepeatButton extends AppCompatImageButton implements OnLongClickListener {

	/**
	 * Highlight color
	 */
	private int color = Color.WHITE;

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public RepeatButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Set the selector
		ThemeUtils mTheme = new ThemeUtils(context);
		mTheme.setBackgroundColor(this);
		// Show the cheat sheet
		setOnLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onLongClick(@NonNull View view) {
		if (TextUtils.isEmpty(view.getContentDescription())) {
			return false;
		} else {
			ApolloUtils.showCheatSheet(view);
			return true;
		}
	}

	/**
	 * sets the highlight color
	 *
	 * @param color ARGB color value
	 */
	public void setColor(int color) {
		this.color = color;
	}

	/**
	 * Sets the correct drawable for the repeat state.
	 *
	 * @param repeatMode repeat mode state {@link MusicUtils#REPEAT_NONE,MusicUtils#REPEAT_CURRENT,MusicUtils#REPEAT_ALL}
	 */
	public void updateRepeatState(int repeatMode) {
		switch (repeatMode) {
			case MusicUtils.REPEAT_ALL:
				setContentDescription(getContext().getString(R.string.accessibility_repeat_all));
				Drawable button = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_repeat_all);
				if (button != null)
					button.setColorFilter(new PorterDuffColorFilter(color, MULTIPLY));
				setImageDrawable(button);
				break;

			case MusicUtils.REPEAT_CURRENT:
				setContentDescription(getContext().getString(R.string.accessibility_repeat_one));
				button = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_repeat_one);
				if (button != null)
					button.setColorFilter(new PorterDuffColorFilter(color, MULTIPLY));
				setImageDrawable(button);
				break;

			default:
			case MusicUtils.REPEAT_NONE:
				setContentDescription(getContext().getString(R.string.accessibility_repeat));
				button = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_repeat);
				setImageDrawable(button);
				break;
		}
	}
}