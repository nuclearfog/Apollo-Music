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

package org.nuclearfog.apollo.ui.drawables;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * A themeable {@link StateListDrawable}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class HoloSelector extends StateListDrawable {

	/**
	 * Button states
	 */
	private static final int[][] STATES = {
			{android.R.attr.state_focused},
			{android.R.attr.state_pressed},
			{}
	};

	/**
	 * @param color color when the button is pressed
	 */
	public HoloSelector(int color) {
		// Focused
		addState(STATES[0], new ColorDrawable(color));
		// Pressed
		addState(STATES[1], new ColorDrawable(color));
		// Default
		addState(STATES[2], new ColorDrawable(Color.TRANSPARENT));
		setExitFadeDuration(400);
	}
}