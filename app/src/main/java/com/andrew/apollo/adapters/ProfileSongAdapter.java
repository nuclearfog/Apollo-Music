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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.fragments.profile.AlbumSongFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistSongFragment;
import com.andrew.apollo.ui.fragments.profile.FavoriteFragment;
import com.andrew.apollo.ui.fragments.profile.GenreSongFragment;
import com.andrew.apollo.ui.fragments.profile.LastAddedFragment;
import com.andrew.apollo.ui.fragments.profile.PlaylistSongFragment;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;

import java.util.List;

/**
 * This {@link ArrayAdapter} is used to display the songs for a particular
 * artist, album, playlist, or genre for {@link ArtistSongFragment},
 * {@link AlbumSongFragment},{@link PlaylistSongFragment},
 * {@link GenreSongFragment},{@link FavoriteFragment},{@link LastAddedFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileSongAdapter extends ArrayAdapter<Song> {

    /**
     * Default display setting: title/album
     */
    public static final int DISPLAY_DEFAULT_SETTING = 0;

    /**
     * Playlist display setting: title/artist-album
     */
    public static final int DISPLAY_PLAYLIST_SETTING = 1;

    /**
     * Album display setting: title/duration
     */
    public static final int DISPLAY_ALBUM_SETTING = 2;

    /**
     * The header view
     */
    private static final int ITEM_VIEW_TYPE_HEADER = 0;

    /**
     * * The data in the list.
     */
    private static final int ITEM_VIEW_TYPE_MUSIC = 1;

    /**
     * Number of views (ImageView, TextView, header)
     */
    private static final int VIEW_TYPE_COUNT = 3;

    /**
     * Fake header
     */
    private final View mHeader;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Display setting for the second line in a song fragment
     */
    private final int mDisplaySetting;

    /**
     * Used to set the size of the data in the adapter
     */
    private List<Song> mCount = Lists.newArrayList();

    /**
     * Constructor of <code>ProfileSongAdapter</code>
     *
     * @param context  The {@link Context} to use
     * @param layoutId The resource Id of the view to inflate.
     * @param setting  defines the content of the second line
     */
    public ProfileSongAdapter(Context context, int layoutId, int setting) {
        super(context, 0);
        // Used to create the custom layout
        // Cache the header
        mHeader = View.inflate(context, R.layout.faux_carousel, null);
        // Get the layout Id
        mLayoutId = layoutId;
        // Know what to put in line two
        mDisplaySetting = setting;
    }

    /**
     * Constructor of <code>ProfileSongAdapter</code>
     *
     * @param context  The {@link Context} to use
     * @param layoutId The resource Id of the view to inflate.
     */
    public ProfileSongAdapter(Context context, int layoutId) {
        this(context, layoutId, DISPLAY_DEFAULT_SETTING);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Return a faux header at position 0
        if (position == 0) {
            return mHeader;
        }

        // Recycle MusicHolder's items
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

        // Retrieve the album
        Song song = getItem(position - 1);
        if (song != null) {
            // Set each track name (line one)
            holder.mLineOne.setText(song.mSongName);
            // Set the line two
            String SEPARATOR_STRING = " - ";
            switch (mDisplaySetting) {
                // show duration if on album fragment
                case DISPLAY_ALBUM_SETTING:
                    holder.mLineOneRight.setVisibility(View.GONE);
                    holder.mLineTwo.setText(MusicUtils.makeTimeString(getContext(), song.mDuration));
                    break;

                case DISPLAY_PLAYLIST_SETTING:
                    if (song.mDuration == -1) {
                        holder.mLineOneRight.setVisibility(View.GONE);
                    } else {
                        holder.mLineOneRight.setVisibility(View.VISIBLE);
                        holder.mLineOneRight.setText(MusicUtils.makeTimeString(getContext(), song.mDuration));
                    }
                    String sb = song.mArtistName + SEPARATOR_STRING + song.mAlbumName;
                    holder.mLineTwo.setText(sb);
                    break;

                case DISPLAY_DEFAULT_SETTING:
                default:
                    holder.mLineOneRight.setVisibility(View.VISIBLE);
                    holder.mLineOneRight.setText(MusicUtils.makeTimeString(getContext(), song.mDuration));
                    holder.mLineTwo.setText(song.mAlbumName);
                    break;
            }
        }
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
    public int getCount() {
        int size = mCount.size();
        return size == 0 ? 0 : size + 1;
    }

    /**
     * @param data The {@link List} used to return the count for the adapter.
     */
    public void setCount(List<Song> data) {
        mCount = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return -1;
        }
        return position - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_VIEW_TYPE_HEADER;
        }
        return ITEM_VIEW_TYPE_MUSIC;
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
    }
}