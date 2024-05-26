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

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;

import androidx.appcompat.widget.AppCompatImageButton;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.views.theme.HoloSelector;
import org.nuclearfog.apollo.utils.ApolloUtils;

/**
 * A custom {@link ImageButton} that represents the "play and pause" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlayPauseButton extends AppCompatImageButton implements OnLongClickListener {

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public PlayPauseButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Theme the selector
		setBackground(new HoloSelector(context));
		// Show the cheat sheet
		setOnLongClickListener(this);
		updateState(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onLongClick(View view) {
		if (TextUtils.isEmpty(view.getContentDescription()))
			return false;
		ApolloUtils.showCheatSheet(view);
		return true;
	}

	/**
	 * Sets the correct drawable for playback.
	 */
	public void updateState(boolean play) {
		if (play) {
			setContentDescription(getResources().getString(R.string.accessibility_pause));
			setImageResource(R.drawable.btn_playback_pause);
		} else {
			setContentDescription(getResources().getString(R.string.accessibility_play));
			setImageResource(R.drawable.btn_playback_play);
		}
		requestLayout();
	}
}