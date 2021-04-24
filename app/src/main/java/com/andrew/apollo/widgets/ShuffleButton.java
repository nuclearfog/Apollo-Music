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
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.widgets.theme.HoloSelector;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ShuffleButton extends ImageButton implements OnClickListener, OnLongClickListener {

    /**
     * highlight color
     */
    private int color = -1;

    /**
     * @param context The {@link Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public ShuffleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Theme the selector
        setBackground(new HoloSelector(context));
        // Control playback (cycle shuffle)
        setOnClickListener(this);
        // Show the cheat sheet
        setOnLongClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        MusicUtils.cycleShuffle();
        updateShuffleState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(View view) {
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
     * Sets the correct drawable for the shuffle state.
     */
    public void updateShuffleState() {
        String info;
        Drawable button;
        switch (MusicUtils.getShuffleMode()) {
            case MusicPlaybackService.SHUFFLE_NORMAL:
            case MusicPlaybackService.SHUFFLE_AUTO:
                info = getResources().getString(R.string.accessibility_shuffle_all);
                button = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_shuffle_all);
                if (button != null)
                    button.setColorFilter(new PorterDuffColorFilter(color, MULTIPLY));
                break;

            default:
            case MusicPlaybackService.SHUFFLE_NONE:
                info = getResources().getString(R.string.accessibility_shuffle);
                button = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_shuffle);
                break;
        }
        setContentDescription(info);
        setImageDrawable(button);
    }
}