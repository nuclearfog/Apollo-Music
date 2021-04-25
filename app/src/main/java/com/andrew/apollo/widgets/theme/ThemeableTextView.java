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

package com.andrew.apollo.widgets.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.andrew.apollo.R;

/**
 * A custom {@link AppCompatTextView} that is made themeable for developers. It allows a
 * custom font and color to be set, otherwise functions like normal. Because
 * different text views may required different colors to be set, the resource
 * name each can be set in the XML via the attribute .
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeableTextView extends androidx.appcompat.widget.AppCompatTextView {

    /**
     * @param context The {@link Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public ThemeableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Retrieve styles attributes
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThemeableTextView, 0, 0);
        // Get the theme resource name
        int color = typedArray.getColor(R.styleable.ThemeableTextView_themeResource, 0xffffffff);
        setTextColor(color);
        // Recyle the attrs
        typedArray.recycle();
    }
}