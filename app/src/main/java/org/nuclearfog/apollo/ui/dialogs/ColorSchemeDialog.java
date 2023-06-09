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

package org.nuclearfog.apollo.ui.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.views.ColorPickerView;

import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.Locale;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ColorSchemeDialog extends AlertDialog implements ColorPickerView.OnColorChangedListener, OnClickListener, TextWatcher {

	private ColorPickerView mColorPicker;
	private Button mNewColor;
	private EditText mHexValue;

	private int mCurrentColor;

	/**
	 * Constructor of <code>ColorSchemeDialog</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public ColorSchemeDialog(Context context) {
		super(context);
		if (getWindow() != null)
			getWindow().setFormat(PixelFormat.RGBA_8888);
		setTitle(R.string.color_picker_title);
		View mRootView = View.inflate(getContext(), R.layout.color_scheme_dialog, null);
		setView(mRootView);

		mColorPicker = mRootView.findViewById(R.id.color_picker_view);
		mNewColor = mRootView.findViewById(R.id.color_scheme_dialog_new_color);
		mHexValue = mRootView.findViewById(R.id.color_scheme_dialog_hex_value);
		Button button1 = mRootView.findViewById(R.id.color_scheme_dialog_preset_one);
		Button button2 = mRootView.findViewById(R.id.color_scheme_dialog_preset_two);
		Button button3 = mRootView.findViewById(R.id.color_scheme_dialog_preset_three);
		Button button4 = mRootView.findViewById(R.id.color_scheme_dialog_preset_four);
		Button button5 = mRootView.findViewById(R.id.color_scheme_dialog_preset_five);
		Button button6 = mRootView.findViewById(R.id.color_scheme_dialog_preset_six);
		Button button7 = mRootView.findViewById(R.id.color_scheme_dialog_preset_seven);
		Button button8 = mRootView.findViewById(R.id.color_scheme_dialog_preset_eight);
		Button mOldColor = mRootView.findViewById(R.id.color_scheme_dialog_old_color);

		mCurrentColor = PreferenceUtils.getInstance(context).getDefaultThemeColor();
		mOldColor.setBackgroundColor(mCurrentColor);
		mColorPicker.setColor(mCurrentColor, true);

		mColorPicker.setOnColorChangedListener(this);
		mHexValue.addTextChangedListener(this);
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);
		button3.setOnClickListener(this);
		button4.setOnClickListener(this);
		button5.setOnClickListener(this);
		button6.setOnClickListener(this);
		button7.setOnClickListener(this);
		button8.setOnClickListener(this);
		mOldColor.setOnClickListener(this);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		ApolloUtils.removeHardwareAccelerationSupport(mColorPicker);
	}

	@Override
	public void onColorChanged(int color) {
		if (mHexValue != null) {
			mHexValue.setText(padLeft(Integer.toHexString(color).toUpperCase(Locale.getDefault())));
		}
		mNewColor.setBackgroundColor(color);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.color_scheme_dialog_preset_one) {
			mColorPicker.setColor(getColor(R.color.holo_green));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_two) {
			mColorPicker.setColor(getColor(R.color.holo_green_light));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_three) {
			mColorPicker.setColor(getColor(R.color.holo_orange_dark));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_four) {
			mColorPicker.setColor(getColor(R.color.holo_orange_light));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_five) {
			mColorPicker.setColor(getColor(R.color.holo_purple));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_six) {
			mColorPicker.setColor(getColor(R.color.holo_red_light));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_seven) {
			mColorPicker.setColor(getColor(R.color.white));
		} else if (v.getId() == R.id.color_scheme_dialog_preset_eight) {
			mColorPicker.setColor(getColor(R.color.black));
		} else if (v.getId() == R.id.color_scheme_dialog_old_color) {
			mColorPicker.setColor(mCurrentColor);
		}
		onColorChanged(getColor());
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		try {
			int color = Color.parseColor("#" + mHexValue.getText().toString().toUpperCase(Locale.getDefault()));
			mColorPicker.setColor(color);
			mNewColor.setBackgroundColor(color);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	/**
	 *
	 */
	private String padLeft(String string) {
		if (string.length() >= 8) {
			return string;
		}
		StringBuilder result = new StringBuilder();
		for (int i = string.length(); i < 8; i++) {
			result.append((char) 0);
		}
		result.append(string);
		return result.toString();
	}

	/**
	 * @param color The color resource.
	 * @return A new color from Apollo's resources.
	 */
	private int getColor(int color) {
		return getContext().getResources().getColor(color);
	}

	/**
	 * @return {@link ColorPickerView}'s current color
	 */
	public int getColor() {
		return mColorPicker.getColor();
	}
}