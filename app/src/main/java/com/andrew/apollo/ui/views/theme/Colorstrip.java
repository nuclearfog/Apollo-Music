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

package com.andrew.apollo.ui.views.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.andrew.apollo.utils.PreferenceUtils;

/**
 * Used as a thin strip placed just above the bottom action bar or just below
 * the top action bar.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Colorstrip extends View {

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public Colorstrip(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Theme the colorstrip
		int color = PreferenceUtils.getInstance(context).getDefaultThemeColor();
		setBackgroundColor(color);
	}
}