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
import com.andrew.apollo.ui.fragments.profile.FavoriteSongFragment;
import com.andrew.apollo.ui.fragments.profile.GenreSongFragment;
import com.andrew.apollo.ui.fragments.profile.LastAddedFragment;
import com.andrew.apollo.ui.fragments.profile.PlaylistSongFragment;
import com.andrew.apollo.utils.MusicUtils;

/**
 * This {@link ArrayAdapter} is used to display the songs for a particular
 * artist, album, playlist, or genre for {@link ArtistSongFragment},
 * {@link AlbumSongFragment},{@link PlaylistSongFragment},
 * {@link GenreSongFragment},{@link FavoriteSongFragment},{@link LastAddedFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileSongAdapter extends ArrayAdapter<Song> {

    /**
     * Default display setting: title/album
     */
    public static final int DISPLAY_DEFAULT_SETTING = 0x709121EE;

    /**
     * Playlist display setting: title/artist-album
     */
    public static final int DISPLAY_PLAYLIST_SETTING = 0x57909C67;

    /**
     * Album display setting: title/duration
     */
    public static final int DISPLAY_ALBUM_SETTING = 0xCCCED4CB;

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
    private static final int VIEW_TYPE_COUNT = 2;

    /**
     * Count of the view header
     */
    public static final int HEADER_COUNT = 1;

    /**
     * item layout
     */
    private static final int LAYOUT = R.layout.list_item_simple;

    /**
     * fragment layout inflater
     */
    private LayoutInflater inflater;

    /**
     * Fake header
     */
    private View mHeader;

    /**
     * Display setting for the second line in a song fragment
     */
    private int mDisplaySetting;

    /**
     * flag to set drag and drop icon
     */
    private boolean enableDnD;

    /**
     * Constructor of <code>ProfileSongAdapter</code>
     *
     * @param context The {@link Context} to use
     * @param setting defines the content of the second line
     */
    public ProfileSongAdapter(Context context, int setting, boolean enableDrag) {
        super(context, LAYOUT);
        // Used to create the custom layout
        // Cache the header
        mHeader = View.inflate(context, R.layout.faux_carousel, null);
        // Know what to put in line two
        mDisplaySetting = setting;
        //
        enableDnD = enableDrag;
        // inflater from context
        inflater = LayoutInflater.from(context);
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
            convertView = inflater.inflate(LAYOUT, parent, false);
            if (enableDnD)
                convertView.findViewById(R.id.edit_track_list_item_handle).setVisibility(View.VISIBLE);
            holder = new MusicHolder(convertView);
            // Hide the third line of text
            holder.mLineThree.setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the album
        Song song = getItem(position);
        if (song != null) {
            // Set each track name (line one)
            holder.mLineOne.setText(song.getName());
            // Set the line two
            switch (mDisplaySetting) {
                // show duration if on album fragment
                case DISPLAY_ALBUM_SETTING:
                    holder.mLineOneRight.setVisibility(View.GONE);
                    holder.mLineTwo.setText(MusicUtils.makeTimeString(getContext(), song.duration()));
                    break;

                case DISPLAY_PLAYLIST_SETTING:
                    if (song.duration() == -1) {
                        holder.mLineOneRight.setVisibility(View.GONE);
                    } else {
                        holder.mLineOneRight.setVisibility(View.VISIBLE);
                        holder.mLineOneRight.setText(MusicUtils.makeTimeString(getContext(), song.duration()));
                    }
                    String sb = song.getArtist() + " - " + song.getAlbum();
                    holder.mLineTwo.setText(sb);
                    break;

                case DISPLAY_DEFAULT_SETTING:
                default:
                    holder.mLineOneRight.setVisibility(View.VISIBLE);
                    holder.mLineOneRight.setText(MusicUtils.makeTimeString(getContext(), song.duration()));
                    holder.mLineTwo.setText(song.getAlbum());
                    break;
            }
        }
        return convertView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return super.getCount() + HEADER_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return super.getCount() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return ITEM_VIEW_TYPE_HEADER;
        return ITEM_VIEW_TYPE_MUSIC;
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
    public void insert(@Nullable Song song, int index) {
        super.insert(song, index - HEADER_COUNT);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Song getItem(int position) {
        if (position >= HEADER_COUNT)
            return super.getItem(position - HEADER_COUNT);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        if (position >= HEADER_COUNT)
            return super.getItem(position - HEADER_COUNT).getId();
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }
}