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

package com.andrew.apollo.ui.fragments.phone;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.adapters.PagerAdapter.MusicFragments;
import com.andrew.apollo.ui.fragments.AlbumFragment;
import com.andrew.apollo.ui.fragments.ArtistFragment;
import com.andrew.apollo.ui.fragments.SongFragment;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.SortOrder;
import com.andrew.apollo.utils.ThemeUtils;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitlePageIndicator.OnCenterItemClickListener;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 * s for phones.
 * <p>
 * #note  The reason the sort orders are taken care of in this fragment rather
 * than the individual fragments is to keep from showing all of the menu
 * items on tablet interfaces. That being said, I have a tablet interface
 * worked out, but I'm going to keep it in the Play Store version of
 * Apollo for a couple of weeks or so before merging it with CM.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class MusicBrowserPhoneFragment extends Fragment implements OnCenterItemClickListener {

    /**
     * Pager
     */
    private ViewPager mViewPager;

    /**
     * VP's adapter
     */
    private PagerAdapter mPagerAdapter;

    /**
     * Theme resources
     */
    private ThemeUtils mResources;

    private PreferenceUtils mPreferences;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public MusicBrowserPhoneFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // The View for the fragment's UI
        View rootView = inflater.inflate(R.layout.fragment_music_browser_phone, container, false);

        // Initialize the adapter
        mPagerAdapter = new PagerAdapter(requireActivity());
        for (MusicFragments mFragment : MusicFragments.values()) {
            mPagerAdapter.add(mFragment.getFragmentClass(), null);
        }

        // Initialize the ViewPager
        mViewPager = rootView.findViewById(R.id.fragment_home_phone_pager);
        // Attach the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mPreferences.getStartPage());

        // Initialize the TPI
        TitlePageIndicator pageIndicator = rootView.findViewById(R.id.fragment_home_phone_pager_titles);
        // Attach the ViewPager
        pageIndicator.setViewPager(mViewPager);
        // Scroll to the current artist, album, or song
        pageIndicator.setOnCenterItemClickListener(this);
        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Initialze the theme resources
        mResources = new ThemeUtils(requireActivity());
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        // Save the last page the use was on
        mPreferences.setStartPage(mViewPager.getCurrentItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem favorite = menu.findItem(R.id.menu_favorite);
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_action_favorite);
        mResources.setFavoriteIcon(favorite);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Favorite action
        inflater.inflate(R.menu.favorite, menu);
        // Shuffle all
        inflater.inflate(R.menu.shuffle, menu);
        // Sort orders
        if (isRecentPage()) {
            inflater.inflate(R.menu.view_as, menu);
        } else if (isArtistPage()) {
            inflater.inflate(R.menu.artist_sort_by, menu);
            inflater.inflate(R.menu.view_as, menu);
        } else if (isAlbumPage()) {
            inflater.inflate(R.menu.album_sort_by, menu);
            inflater.inflate(R.menu.view_as, menu);
        } else if (isSongPage()) {
            inflater.inflate(R.menu.song_sort_by, menu);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(getActivity());
                return true;

            case R.id.menu_favorite:
                // Toggle the current track as a favorite and update the menu
                // item
                MusicUtils.toggleFavorite();
                requireActivity().invalidateOptionsMenu();
                return true;

            case R.id.menu_sort_by_az:
                if (isArtistPage()) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_za:
                if (isArtistPage()) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_artist:
                if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_album:
                if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_year:
                if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_duration:
                if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_number_of_songs:
                if (isArtistPage()) {
                    mPreferences
                            .setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                    getAlbumFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_number_of_albums:
                if (isArtistPage()) {
                    mPreferences
                            .setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                    getArtistFragment().refresh();
                }
                return true;

            case R.id.menu_sort_by_filename:
                if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                    getSongFragment().refresh();
                }
                return true;

            case R.id.menu_view_as_simple:
                if (isRecentPage()) {
                    mPreferences.setRecentLayout("simple");
                } else if (isArtistPage()) {
                    mPreferences.setArtistLayout("simple");
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumLayout("simple");
                }
                NavUtils.goHome(requireActivity());
                return true;

            case R.id.menu_view_as_detailed:
                if (isRecentPage()) {
                    mPreferences.setRecentLayout("detailed");
                } else if (isArtistPage()) {
                    mPreferences.setArtistLayout("detailed");
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumLayout("detailed");
                }
                NavUtils.goHome(requireActivity());
                return true;

            case R.id.menu_view_as_grid:
                if (isRecentPage()) {
                    mPreferences.setRecentLayout("grid");
                } else if (isArtistPage()) {
                    mPreferences.setArtistLayout("grid");
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumLayout("grid");
                }
                NavUtils.goHome(requireActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCenterItemClick(int position) {
        // If on the artist fragment, scrolls to the current artist
        if (position == 2) {
            getArtistFragment().scrollToCurrentArtist();
            // If on the album fragment, scrolls to the current album
        } else if (position == 3) {
            getAlbumFragment().scrollToCurrentAlbum();
            // If on the song fragment, scrolls to the current song
        } else if (position == 4) {
            getSongFragment().scrollToCurrentSong();
        }
    }

    private boolean isArtistPage() {
        return mViewPager.getCurrentItem() == 2;
    }

    private ArtistFragment getArtistFragment() {
        return (ArtistFragment) mPagerAdapter.getFragment(2);
    }

    private boolean isAlbumPage() {
        return mViewPager.getCurrentItem() == 3;
    }

    private AlbumFragment getAlbumFragment() {
        return (AlbumFragment) mPagerAdapter.getFragment(3);
    }

    private boolean isSongPage() {
        return mViewPager.getCurrentItem() == 4;
    }

    private SongFragment getSongFragment() {
        return (SongFragment) mPagerAdapter.getFragment(4);
    }

    private boolean isRecentPage() {
        return mViewPager.getCurrentItem() == 1;
    }
}