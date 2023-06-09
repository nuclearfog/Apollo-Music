/*
 * Copyright (C) 2008 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.widget.AppCompatImageButton;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.views.theme.HoloSelector;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.lang.ref.WeakReference;

/**
 * A {@link RepeatingImageButton} that will repeatedly call a 'listener' method as long
 * as the button is pressed, otherwise functions like a typecal ImageView
 */
public class RepeatingImageButton extends AppCompatImageButton implements OnClickListener {

	private static final long S_INTERVAL = 400;

	private RepeatListener mListener;
	private Repeater repeater;

	private long mStartTime;
	private int mRepeatCount;

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public RepeatingImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Theme the selector
		setBackground(new HoloSelector(context));
		setFocusable(true);
		setLongClickable(true);
		setOnClickListener(this);
		repeater = new Repeater(this);
		updateState();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.action_button_previous) {
			MusicUtils.previous(getContext());
		} else if (view.getId() == R.id.action_button_next) {
			MusicUtils.next(getContext());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean performLongClick() {
		if (mListener == null) {
			ApolloUtils.showCheatSheet(this);
		}
		mStartTime = SystemClock.elapsedRealtime();
		mRepeatCount = 0;
		post(repeater);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			/* Remove the repeater, but call the hook one more time */
			removeCallbacks(repeater);
			if (mStartTime != 0) {
				doRepeat(true);
				mStartTime = 0;
			}
		}
		return super.onTouchEvent(event);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				/*
				 * Need to call super to make long press work, but return true
				 * so that the application doesn't get the down event
				 */
				super.onKeyDown(keyCode, event);
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				/* Remove the repeater, but call the hook one more time */
				removeCallbacks(repeater);
				if (mStartTime != 0) {
					doRepeat(true);
					mStartTime = 0;
				}
		}
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Sets the listener to be called while the button is pressed and the
	 * interval in milliseconds with which it will be called.
	 *
	 * @param l The listener that will be called
	 */
	public void setRepeatListener(RepeatListener l) {
		mListener = l;
	}

	/**
	 * @param shouldRepeat If True the repeat count stops at -1, false if to add
	 *                     incrementally add the repeat count
	 */
	private void doRepeat(boolean shouldRepeat) {
		long now = SystemClock.elapsedRealtime();
		if (mListener != null) {
			mListener.onRepeat(this, now - mStartTime, shouldRepeat ? -1 : mRepeatCount++);
		}
	}

	/**
	 * Sets the correct drawable for playback.
	 */
	public void updateState() {
		if (getId() == R.id.action_button_next) {
			setImageResource(R.drawable.btn_playback_next);
		} else if (getId() == R.id.action_button_previous) {
			setImageResource(R.drawable.btn_playback_previous);
		}
	}

	public interface RepeatListener {

		/**
		 * @param v           View to be set
		 * @param duration    Duration of the long press
		 * @param repeatcount The number of repeat counts
		 */
		void onRepeat(View v, long duration, int repeatcount);
	}

	/**
	 *
	 */
	private static class Repeater implements Runnable {

		private WeakReference<RepeatingImageButton> button;

		Repeater(RepeatingImageButton button) {
			this.button = new WeakReference<>(button);
		}

		@Override
		public void run() {
			RepeatingImageButton button = this.button.get();
			if (button != null) {
				button.doRepeat(false);
				if (button.isPressed()) {
					button.postDelayed(this, S_INTERVAL);
				}
			}
		}
	}
}