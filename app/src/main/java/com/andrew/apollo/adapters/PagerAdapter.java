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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.AlbumFragment;
import com.andrew.apollo.ui.fragments.ArtistFragment;
import com.andrew.apollo.ui.fragments.FolderFragment;
import com.andrew.apollo.ui.fragments.GenreFragment;
import com.andrew.apollo.ui.fragments.PlaylistFragment;
import com.andrew.apollo.ui.fragments.QueueFragment;
import com.andrew.apollo.ui.fragments.RecentFragment;
import com.andrew.apollo.ui.fragments.SongFragment;
import com.andrew.apollo.ui.fragments.profile.AlbumSongFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistSongFragment;
import com.andrew.apollo.ui.fragments.profile.FavoriteSongFragment;
import com.andrew.apollo.ui.fragments.profile.FolderSongFragment;
import com.andrew.apollo.ui.fragments.profile.GenreSongFragment;
import com.andrew.apollo.ui.fragments.profile.LastAddedFragment;
import com.andrew.apollo.ui.fragments.profile.PlaylistSongFragment;
import com.andrew.apollo.ui.fragments.profile.PopularSongFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link FragmentStatePagerAdapter} class for swiping between playlists, recent,
 * artists, albums, songs, and genre {@link Fragment}s on phones.<br/>
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> fragments = new ArrayList<>(4);

    private String[] titles;

    /**
     * Constructor of <code>PagerAdapter<code>
     */
    public PagerAdapter(Context context, FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        titles = context.getResources().getStringArray(R.array.page_titles);
    }

    /**
     * Method that adds a new fragment class to the viewer (the fragment is
     * internally instantiate)
     *
     * @param fragment The full qualified name of fragment class.
     * @param params   The instantiate params.
     */
    public void add(MusicFragments fragment, @Nullable Bundle params) {
        if (params != null)
            fragment.instance.setArguments(params);
        fragments.add(fragment.instance);
        notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return fragments.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position].toUpperCase(Locale.getDefault());
    }

    /**
     * clear all fragments from adapter
     */
    public void clear() {
        fragments.clear();
        notifyDataSetChanged();
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public enum MusicFragments {

        /**
         * The playlist fragment
         */
        PLAYLIST(new PlaylistFragment()),
        /**
         * The recent fragment
         */
        RECENT(new RecentFragment()),
        /**
         * The artist fragment
         */
        ARTIST(new ArtistFragment()),
        /**
         * The album fragment
         */
        ALBUM(new AlbumFragment()),
        /**
         * The song fragment
         */
        SONG(new SongFragment()),
        /**
         * The genre fragment
         */
        GENRE(new GenreFragment()),

        /**
         * The folder fragment
         */
        FOLDER(new FolderFragment()),

        ALBUMSONG(new AlbumSongFragment()),

        GENRESONG(new GenreSongFragment()),

        ARTISTSONG(new ArtistSongFragment()),

        ARTISTALBUM(new ArtistAlbumFragment()),

        FOLDERSONG(new FolderSongFragment()),

        PLAYLISTSONG(new PlaylistSongFragment()),

        FAVORITE(new FavoriteSongFragment()),

        LASTADDED(new LastAddedFragment()),

        POPULAR(new PopularSongFragment()),

        QUEUE(new QueueFragment());

        public final Fragment instance;

        /**
         * Constructor of <code>MusicFragments</code>
         *
         * @param fragment The fragment class
         */
        MusicFragments(Fragment fragment) {
            instance = fragment;
        }
    }
}