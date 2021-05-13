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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.andrew.apollo.adapters.MusicHolder.DataHolder;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.fragments.QueueFragment;
import com.andrew.apollo.ui.fragments.SongFragment;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.ArrayList;

/**
 * This {@link ArrayAdapter} is used to display all of the songs on a user's
 * device for {@link SongFragment}. It is also used to show the queue in
 * {@link QueueFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongAdapter extends ArrayAdapter<Song> {

    /**
     * color mask to set background  transparency of the selected item
     */
    private static final int TRANSPARENCY_MASK = 0x3f000000;

    /**
     * The resource Id of the layout to inflate
     */
    private int mLayoutId;

    /**
     * Used to cache the song info
     */
    private ArrayList<DataHolder> mData = new ArrayList<>();

    private int nowplayingPos = -1;

    /**
     * Constructor of <code>SongAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public SongAdapter(Context context, int layoutId) {
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
            // Hide the third line of text
            holder.mLineThree.setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }
        // Retrieve the data holder
        DataHolder dataHolder = mData.get(position);
        // Set each song name (line one)
        holder.mLineOne.setText(dataHolder.mLineOne);
        // Set the song duration (line one, right)
        holder.mLineOneRight.setText(dataHolder.mLineOneRight);
        // Set the album name (line two)
        holder.mLineTwo.setText(dataHolder.mLineTwo);

        if (holder.mBackground != null) {
            // set background of the current track
            if (position == nowplayingPos) {
                PreferenceUtils prefs = PreferenceUtils.getInstance(parent.getContext());
                int backgroundColor = TRANSPARENCY_MASK | (prefs.getDefaultThemeColor() & 0xffffff);
                holder.mBackground.setBackgroundColor(backgroundColor);
            } else {
                holder.mBackground.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        return convertView;
    }


    @Override
    public long getItemId(int position) {
        Song song = getItem(position);
        if (song != null)
            return song.getId();
        return position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }


    @Override
    public void clear() {
        super.clear();
        mData.clear();
    }

    /**
     * moves the track item to another position
     *
     * @param from index where the track is located
     * @param to   index where the track should be moved
     */
    @MainThread
    public void moveTrack(int from, int to) {
        if (from != to) {
            Song mSong = getItem(from);
            remove(mSong);
            insert(mSong, to);
            if (nowplayingPos == from) {
                nowplayingPos = to;
            } else {
                if (from < nowplayingPos && to >= nowplayingPos)
                    nowplayingPos--;
                else if (from > nowplayingPos && to <= nowplayingPos)
                    nowplayingPos++;
            }
            notifyDataSetChanged();
            buildCache();
        }
    }

    /**
     * set current track ID
     *
     * @param pos position of the current track
     */
    public void setCurrentTrackPos(int pos) {
        nowplayingPos = pos;
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData.clear();
        mData.ensureCapacity(getCount());
        for (int i = 0; i < getCount(); i++) {
            // Build the song
            Song song = getItem(i);
            if (song != null) {
                // Build the data holder
                DataHolder holder = new DataHolder();
                // Song Id
                holder.mItemId = song.getId();
                // Song names (line one)
                holder.mLineOne = song.getName();
                // Song duration (line one, right)
                holder.mLineOneRight = MusicUtils.makeTimeString(getContext(), song.duration());
                // Artist names (line two)
                holder.mLineTwo = song.getArtist();
                mData.add(holder);
            }
        }
    }
}