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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.theme.HoloSelector;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

/**
 * A custom {@link ImageButton} that represents the "repeat" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressLint("AppCompatCustomView")
public class RepeatButton extends ImageButton implements OnClickListener, OnLongClickListener {

    /**
     * Repeat one theme resource
     */
    private static final String REPEAT_ALL = "btn_playback_repeat_all";

    /**
     * Repeat one theme resource
     */
    private static final String REPEAT_CURRENT = "btn_playback_repeat_one";

    /**
     * Repeat one theme resource
     */
    private static final String REPEAT_NONE = "btn_playback_repeat";

    /**
     * The resources to use.
     */
    private final ThemeUtils mResources;

    /**
     * Highlight color
     */
    private int color = -1;

    /**
     * @param context The {@link Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public RepeatButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // Initialze the theme resources
        mResources = new ThemeUtils(context);
        // Set the selector
        setBackgroundDrawable(new HoloSelector(context));
        // Control playback (cycle repeat modes)
        setOnClickListener(this);
        // Show the cheat sheet
        setOnLongClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        MusicUtils.cycleRepeat();
        updateRepeatState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(final View view) {
        if (TextUtils.isEmpty(view.getContentDescription())) {
            return false;
        } else {
            ApolloUtils.showCheatSheet(view);
            return true;
        }
    }

    /**
     * sets the highlight color
     *
     * @param color ARGB color value
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Sets the correct drawable for the repeat state.
     */
    public void updateRepeatState() {
        String info;
        Drawable button;
        switch (MusicUtils.getRepeatMode()) {
            case MusicPlaybackService.REPEAT_ALL:
                info = getResources().getString(R.string.accessibility_repeat_all);
                button = mResources.getDrawable(REPEAT_ALL);
                button.setColorFilter(new PorterDuffColorFilter(color, MULTIPLY));
                break;

            case MusicPlaybackService.REPEAT_CURRENT:
                info = getResources().getString(R.string.accessibility_repeat_one);
                button = mResources.getDrawable(REPEAT_CURRENT);
                button.setColorFilter(new PorterDuffColorFilter(color, MULTIPLY));
                break;

            default:
            case MusicPlaybackService.REPEAT_NONE:
                info = getResources().getString(R.string.accessibility_repeat);
                button = mResources.getDrawable(REPEAT_NONE);
                break;
        }
        setContentDescription(info);
        setImageDrawable(button);
    }
}