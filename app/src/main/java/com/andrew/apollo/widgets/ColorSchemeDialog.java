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

package com.andrew.apollo.widgets;

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

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.widgets.ColorPickerView.OnColorChangedListener;

import java.util.Locale;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ColorSchemeDialog extends AlertDialog implements OnColorChangedListener, OnClickListener {

    private final int mCurrentColor;

    private final OnColorChangedListener mListener = this;

    private ColorPickerView mColorPicker;
    private Button mNewColor;
    private View mRootView;
    private EditText mHexValue;

    /**
     * Constructor of <code>ColorSchemeDialog</code>
     *
     * @param context The {@link Context} to use.
     */
    public ColorSchemeDialog(Context context) {
        super(context);
        if (getWindow() != null)
            getWindow().setFormat(PixelFormat.RGBA_8888);
        mCurrentColor = PreferenceUtils.getInstance(context).getDefaultThemeColor();
        setUp(mCurrentColor);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ApolloUtils.removeHardwareAccelerationSupport(mColorPicker);
    }

    /*
     * (non-Javadoc)
     * @see com.andrew.apollo.widgets.ColorPickerView.OnColorChangedListener#
     * onColorChanged(int)
     */
    @Override
    public void onColorChanged(int color) {
        if (mHexValue != null) {
            mHexValue.setText(padLeft(Integer.toHexString(color).toUpperCase(Locale.getDefault())));
        }
        mNewColor.setBackgroundColor(color);
    }


    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.color_scheme_dialog_preset_one) {
            mColorPicker.setColor(getColor(R.color.holo_green));
        } else if (vid == R.id.color_scheme_dialog_preset_two) {
            mColorPicker.setColor(getColor(R.color.holo_green_light));
        } else if (vid == R.id.color_scheme_dialog_preset_three) {
            mColorPicker.setColor(getColor(R.color.holo_orange_dark));
        } else if (vid == R.id.color_scheme_dialog_preset_four) {
            mColorPicker.setColor(getColor(R.color.holo_orange_light));
        } else if (vid == R.id.color_scheme_dialog_preset_five) {
            mColorPicker.setColor(getColor(R.color.holo_purple));
        } else if (vid == R.id.color_scheme_dialog_preset_six) {
            mColorPicker.setColor(getColor(R.color.holo_red_light));
        } else if (vid == R.id.color_scheme_dialog_preset_seven) {
            mColorPicker.setColor(getColor(R.color.white));
        } else if (vid == R.id.color_scheme_dialog_preset_eight) {
            mColorPicker.setColor(getColor(R.color.black));
        } else if (vid == R.id.color_scheme_dialog_old_color) {
            mColorPicker.setColor(mCurrentColor);
        }
        mListener.onColorChanged(getColor());
    }


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
     * Initialzes the presets and color picker
     *
     * @param color The color to use.
     */
    private void setUp(int color) {
        mRootView = View.inflate(getContext(), R.layout.color_scheme_dialog, null);

        mColorPicker = mRootView.findViewById(R.id.color_picker_view);
        Button mOldColor = mRootView.findViewById(R.id.color_scheme_dialog_old_color);
        mOldColor.setOnClickListener(this);
        mNewColor = mRootView.findViewById(R.id.color_scheme_dialog_new_color);
        setUpPresets(R.id.color_scheme_dialog_preset_one);
        setUpPresets(R.id.color_scheme_dialog_preset_two);
        setUpPresets(R.id.color_scheme_dialog_preset_three);
        setUpPresets(R.id.color_scheme_dialog_preset_four);
        setUpPresets(R.id.color_scheme_dialog_preset_five);
        setUpPresets(R.id.color_scheme_dialog_preset_six);
        setUpPresets(R.id.color_scheme_dialog_preset_seven);
        setUpPresets(R.id.color_scheme_dialog_preset_eight);
        mHexValue = mRootView.findViewById(R.id.color_scheme_dialog_hex_value);
        mHexValue.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mColorPicker.setColor(Color.parseColor("#"
                            + mHexValue.getText().toString().toUpperCase(Locale.getDefault())));
                    mNewColor.setBackgroundColor(Color.parseColor("#"
                            + mHexValue.getText().toString().toUpperCase(Locale.getDefault())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /* Nothing to do */
            }

            @Override
            public void afterTextChanged(Editable s) {
                /* Nothing to do */
            }
        });

        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setBackgroundColor(color);
        mColorPicker.setColor(color, true);

        setTitle(R.string.color_picker_title);
        setView(mRootView);
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

    /**
     * @param which The Id of the preset color
     */
    private void setUpPresets(int which) {
        Button preset = mRootView.findViewById(which);
        if (preset != null) {
            preset.setOnClickListener(this);
        }
    }
}