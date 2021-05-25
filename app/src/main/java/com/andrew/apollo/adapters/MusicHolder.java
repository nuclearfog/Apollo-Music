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

package com.andrew.apollo.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrew.apollo.R;

/**
 * Used to efficiently cache and recyle the {@link View}s used in the artist,
 * album, song, playlist, and genre adapters.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class MusicHolder {

    /**
     * This is the overlay on top of the background artist, playlist, or genre
     * image
     */
    public final RelativeLayout mOverlay;

    /**
     * This is the artist or album image
     */
    public final ImageView mImage;

    /**
     * This is the first line displayed in the list or grid
     * <p>
     * {@code #getView()} of a specific adapter for more detailed info
     */
    public final TextView mLineOne;

    /**
     * This is displayed on the right side of the first line in the list or grid
     * <p>
     * {@code #getView()} of a specific adapter for more detailed info
     */
    public final TextView mLineOneRight;

    /**
     * This is the second line displayed in the list or grid
     * <p>
     * {@code #getView()} of a specific adapter for more detailed info
     */
    public final TextView mLineTwo;

    /**
     * This is the third line displayed in the list or grid
     * <p>
     * {@code #getView()} of a specific adapter for more detailed info
     */
    public final TextView mLineThree;

    /**
     * Constructor of <code>ViewHolder</code>
     */
    public MusicHolder(View view) {
        super();
        // Initialize mOverlay
        mOverlay = view.findViewById(R.id.image_background);

        // Initialize mImage
        mImage = view.findViewById(R.id.image);

        // Initialize mLineOne
        mLineOne = view.findViewById(R.id.line_one);

        // Initialize mLineOneRight
        mLineOneRight = view.findViewById(R.id.line_one_right);

        // Initialize mLineTwo
        mLineTwo = view.findViewById(R.id.line_two);

        // Initialize mLineThree
        mLineThree = view.findViewById(R.id.line_three);
    }
}