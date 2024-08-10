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

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.views.ColorPickerView;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.Locale;

/**
 * Dialog showing color picker to set the system accent color
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ColorSchemeDialog extends DialogFragment implements ColorPickerView.OnColorChangedListener, OnClickListener, TextWatcher {

	private static final String TAG = "ColorSchemeDialog";

	private ColorPickerView mColorPicker;
	private Button mNewColor;
	private EditText mHexValue;

	private PreferenceUtils mPreferences;


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceUtils.getInstance(inflater.getContext());
		View mRootView = View.inflate(getContext(), R.layout.color_scheme_dialog, null);
		mColorPicker = mRootView.findViewById(R.id.color_picker_view);
		mNewColor = mRootView.findViewById(R.id.color_scheme_dialog_new_color);
		mHexValue = mRootView.findViewById(R.id.color_scheme_dialog_hex_value);
		Button confirm = mRootView.findViewById(R.id.color_scheme_dialog_apply);
		Button cancel = mRootView.findViewById(R.id.color_scheme_dialog_cancel);
		Button button1 = mRootView.findViewById(R.id.color_scheme_dialog_preset_1);
		Button button2 = mRootView.findViewById(R.id.color_scheme_dialog_preset_2);
		Button button3 = mRootView.findViewById(R.id.color_scheme_dialog_preset_3);
		Button button4 = mRootView.findViewById(R.id.color_scheme_dialog_preset_4);
		Button button5 = mRootView.findViewById(R.id.color_scheme_dialog_preset_5);
		Button button6 = mRootView.findViewById(R.id.color_scheme_dialog_preset_6);
		Button button7 = mRootView.findViewById(R.id.color_scheme_dialog_preset_7);
		Button button8 = mRootView.findViewById(R.id.color_scheme_dialog_preset_8);
		Button mOldColor = mRootView.findViewById(R.id.color_scheme_dialog_old_color);

		mOldColor.setBackgroundColor(mPreferences.getDefaultThemeColor());
		mColorPicker.setColor(mPreferences.getDefaultThemeColor());
		onColorChanged(mPreferences.getDefaultThemeColor());

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
		confirm.setOnClickListener(this);
		cancel.setOnClickListener(this);

		return mRootView;
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
		if (v.getId() == R.id.color_scheme_dialog_preset_1) {
			mColorPicker.setColorResource(R.color.holo_green);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_2) {
			mColorPicker.setColorResource(R.color.holo_green_light);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_3) {
			mColorPicker.setColorResource(R.color.holo_orange_dark);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_4) {
			mColorPicker.setColorResource(R.color.holo_orange_light);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_5) {
			mColorPicker.setColorResource(R.color.holo_purple);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_6) {
			mColorPicker.setColorResource(R.color.holo_red_light);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_7) {
			mColorPicker.setColorResource(R.color.white);
		} else if (v.getId() == R.id.color_scheme_dialog_preset_8) {
			mColorPicker.setColorResource(R.color.black);
		} else if (v.getId() == R.id.color_scheme_dialog_old_color) {
			mColorPicker.setColor(mPreferences.getDefaultThemeColor());
		} else if (v.getId() == R.id.color_scheme_dialog_cancel) {
			dismiss();
			return;
		} else if (v.getId() == R.id.color_scheme_dialog_apply) {
			mPreferences.setDefaultThemeColor(mColorPicker.getColor());
			NavUtils.goHome(getActivity());
			dismiss();
			return;
		}
		onColorChanged(mColorPicker.getColor());
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		try {
			int color = Color.parseColor("#" + mHexValue.getText().toString().toUpperCase(Locale.getDefault()));
			mColorPicker.setColor(color);
			mNewColor.setBackgroundColor(color);
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
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
	 * show this dialog
	 */
	public static void show(FragmentActivity activity) {
		Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
		if (fragment == null) {
			ColorSchemeDialog dialog = new ColorSchemeDialog();
			dialog.show(activity.getSupportFragmentManager(), TAG);
		}
	}
}