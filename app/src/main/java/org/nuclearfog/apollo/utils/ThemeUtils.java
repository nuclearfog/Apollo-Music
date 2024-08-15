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

package org.nuclearfog.apollo.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;

import org.nuclearfog.apollo.R;

/**
 * In order to implement the theme chooser for Apollo, this class returns a
 * {@link Resources} object that can be used like normal. In other words, when
 * {@code getDrawable()} or {@code getColor()} is called, the object returned is
 * from the current theme package name and because all of the theme resource
 * identifiers are the same as all of Apollo's resources a little less code is
 * used to implement the theme chooser.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeUtils {

	/**
	 * Custom action bar layout
	 */
	private final View mActionBarLayout;

	/**
	 * The theme resources.
	 */
	private Resources resources;

	/**
	 * Constructor for <code>ThemeUtils</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public ThemeUtils(Context context) {
		resources = context.getResources();
		// Inflate the custom layout
		mActionBarLayout = View.inflate(context, R.layout.action_bar, null);
	}

	/**
	 * Sets the {@link MenuItem} icon for the favorites action.
	 *
	 * @param favorite The favorites action.
	 * @param enable   true to enable favorite icon
	 */
	public void setFavoriteIcon(MenuItem favorite, boolean enable) {
		Drawable favIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_action_favorite, null);
		if (favIcon != null) {
			if (enable) {
				favIcon.mutate().setColorFilter(resources.getColor(R.color.favorite_selected), PorterDuff.Mode.SRC_IN);
			}
			favorite.setIcon(favIcon);
		}
	}

	/**
	 * Builds a custom layout and applies it to the action bar, then themes the
	 * background, title, and subtitle.
	 *
	 * @param actionBar The {@link ActionBar} to use.
	 * @param titleID   The title for the action bar
	 */
	public void themeActionBar(ActionBar actionBar, @StringRes int titleID) {
		int backgroundColor = ResourcesCompat.getColor(resources, R.color.action_bar, null);
		int textColor = ResourcesCompat.getColor(resources, R.color.action_bar_title, null);
		String title = resources.getString(titleID);
		// Set the custom layout
		actionBar.setCustomView(mActionBarLayout);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		// Theme the action bar background
		Drawable background = new ColorDrawable(backgroundColor);
		actionBar.setBackgroundDrawable(background);
		// Theme the title
		TextView actionBarTitle = mActionBarLayout.findViewById(R.id.action_bar_title);
		actionBarTitle.setTextColor(textColor);
		// Set the title
		actionBarTitle.setText(title);
	}

	/**
	 * Themes the action bar subtitle
	 *
	 * @param subtitle The subtitle to use
	 */
	public void setSubtitle(@NonNull String subtitle) {
		TextView actionBarSubtitle = mActionBarLayout.findViewById(R.id.action_bar_subtitle);
		actionBarSubtitle.setVisibility(View.VISIBLE);
		// Theme the subtitle
		int color = ResourcesCompat.getColor(resources, R.color.action_bar_subtitle, null);
		actionBarSubtitle.setTextColor(color);
		// Set the subtitle
		actionBarSubtitle.setText(subtitle);
	}

	/**
	 * set view background
	 *
	 * @param v view to set the default background
	 */
	public void setBackground(View v) {
		v.setBackgroundResource(R.drawable.pager_background);
	}
}