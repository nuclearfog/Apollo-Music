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

package com.andrew.apollo.views.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;

import com.andrew.apollo.utils.PreferenceUtils;

/**
 * A themeable {@link StateListDrawable}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
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
     * Constructor for <code>HoloSelector</code>
     *
     * @param context The {@link Context} to use.
     */
    public HoloSelector(Context context) {
        int holoColor = PreferenceUtils.getInstance(context).getDefaultThemeColor();
        // Focused
        addState(STATES[0], new ColorDrawable(holoColor));
        // Pressed
        addState(STATES[1], new ColorDrawable(holoColor));
        // Default
        addState(STATES[2], new ColorDrawable(Color.TRANSPARENT));
        setExitFadeDuration(400);
    }
}