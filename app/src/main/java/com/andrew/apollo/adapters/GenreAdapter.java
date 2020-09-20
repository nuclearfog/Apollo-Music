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

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.MusicHolder.DataHolder;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.ui.fragments.GenreFragment;

/**
 * This {@link ArrayAdapter} is used to display all of the genres on a user's
 * device for {@link GenreFragment} .
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreAdapter extends ArrayAdapter<Genre> {

    /**
     * Number of views (TextView)
     */
    private static final int VIEW_TYPE_COUNT = 1;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Used to cache the genre info
     */
    private DataHolder[] mData;

    /**
     * Constructor of <code>GenreAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public GenreAdapter(final Context context, final int layoutId) {
        super(context, 0);
        // Get the layout Id
        mLayoutId = layoutId;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            // Hide the second and third lines of text
            holder.mLineTwo.setVisibility(View.GONE);
            holder.mLineThree.setVisibility(View.GONE);
            // Make line one slightly larger
            holder.mLineOne.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.text_size_large));
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the data holder
        DataHolder dataHolder = mData[position];

        // Set each genre name (line one)
        holder.mLineOne.setText(dataHolder.mLineOne);
        return convertView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData = new DataHolder[getCount()];
        for (int i = 0; i < getCount(); i++) {
            // Build the artist
            final Genre genre = getItem(i);

            if (genre != null) {
                // Build the data holder
                mData[i] = new DataHolder();
                // Genre Id
                mData[i].mItemId = genre.mGenreId;
                // Genre names (line one)
                mData[i].mLineOne = genre.mGenreName;
            }
        }
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
        mData = null;
    }
}