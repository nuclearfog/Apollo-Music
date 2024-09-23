/*
 * Copyright 2012 Evgeny Shishkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nuclearfog.apollo.ui.appmsg;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * In-layout notifications. Based on {@link android.widget.Toast} notifications
 * and article by Cyril Mottier (<a href="http://android.cyrilmottier.com/?p=773">...</a>).
 *
 * @author e.shishkin
 */
public class AppMsg {

	/**
	 * Show the view or text notification for a short period of time. This time
	 * could be user-definable. This is the default.
	 */
	public static final int LENGTH_SHORT = 3000;

	/**
	 * Show the view or text notification for a long period of time. This time
	 * could be user-definable.
	 */
	public static final int LENGTH_LONG = 5000;

	/**
	 * Show the text notification for a long period of time with a negative style.
	 */
	public static final Style STYLE_ALERT = new Style(LENGTH_LONG, R.color.alert);

	/**
	 * Show the text notification for a short period of time with a positive style.
	 */
	public static final Style STYLE_CONFIRM = new Style(LENGTH_SHORT, R.color.confirm);

	private Activity activity;
	private LayoutParams mLayoutParams;
	private View mView;

	private int mDuration = LENGTH_SHORT;

	/**
	 *
	 */
	private AppMsg(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Make a {@link AppMsg} that just contains a text view.
	 *
	 * @param count number used for plurals
	 * @param id    ID of the plural resource
	 */
	public static AppMsg makeText(Activity activity, @PluralsRes int id, @IntRange(from = 0) int count, Style style) {
		String text = StringUtils.makeLabel(activity, id, count);
		return makeText(activity, text, style);
	}

	/**
	 * Make a {@link AppMsg} that just contains a text view.
	 *
	 * @param id ID of the string resource
	 */
	public static AppMsg makeText(Activity activity, @StringRes int id, Style style) {
		String text = activity.getString(id);
		return makeText(activity, text, style);
	}

	/**
	 * Make a {@link AppMsg} that just contains a text view.
	 *
	 * @param text The text to show. Can be formatted text.
	 */
	public static AppMsg makeText(Activity activity, CharSequence text, Style style) {
		AppMsg result = new AppMsg(activity);
		View view = View.inflate(activity, R.layout.app_msg, null);
		TextView tv = view.findViewById(android.R.id.message);

		view.setBackgroundResource(style.background);
		tv.setText(text);

		result.mView = view;
		result.mDuration = style.duration;
		return result;
	}

	/**
	 * Show the view for the specified duration.
	 */
	public void show() {
		MsgManager manager = MsgManager.getInstance();
		manager.add(this);
	}

	/**
	 * @return <code>true</code> if the {@link AppMsg} is being displayed, else <code>false</code>.
	 */
	boolean isShowing() {
		return mView != null && mView.getParent() != null;
	}


	void addContentView(View v, LayoutParams params) {
		activity.addContentView(v, params);
	}


	Context getContext() {
		return activity.getApplicationContext();
	}

	/**
	 * Return the view.
	 */
	View getView() {
		return mView;
	}

	/**
	 * Return the duration.
	 */
	int getDuration() {
		return mDuration;
	}

	/**
	 * Gets the crouton's layout parameters, constructing a default if necessary.
	 *
	 * @return the layout parameters
	 */
	LayoutParams getLayoutParams() {
		if (mLayoutParams == null) {
			mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		}
		return mLayoutParams;
	}

	/**
	 * The style for a {@link AppMsg}.
	 *
	 * @author e.shishkin
	 */
	private static class Style {

		private int duration;
		private int background;

		/**
		 * Construct an {@link AppMsg.Style} object.
		 *
		 * @param duration How long to display the message. Either {@link #LENGTH_SHORT} or {@link #LENGTH_LONG}
		 * @param resId    resource for AppMsg background
		 */
		private Style(int duration, int resId) {
			this.duration = duration;
			this.background = resId;
		}


		@Override
		public boolean equals(Object o) {
			if (!(o instanceof AppMsg.Style)) {
				return false;
			}
			Style style = (Style) o;
			return style.duration == duration && style.background == background;
		}
	}
}