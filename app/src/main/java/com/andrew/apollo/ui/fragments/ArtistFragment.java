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

package com.andrew.apollo.ui.fragments;

import static com.andrew.apollo.utils.PreferenceUtils.ARTIST_LAYOUT;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ArtistAdapter;
import com.andrew.apollo.loaders.ArtistLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ActivityBase;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistFragment extends Fragment implements LoaderCallbacks<List<Artist>>,
        OnScrollListener, OnItemClickListener, MusicStateListener, FragmentCallback {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 0x793F54E4;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER_ID = 0x1137083;

    /**
     * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
     */
    private static final int ONE = 1, TWO = 2, FOUR = 4;

    /**
     * The adapter for the grid
     */
    private ArtistAdapter mAdapter;

    /**
     * The grid view
     */
    private GridView mList;

    /**
     * app preferences
     */
    private PreferenceUtils preference;

    /**
     * Artist song list
     */
    @NonNull
    private long[] mArtistList = {};

    /**
     * Represents an artist
     */
    @Nullable
    private Artist mArtist;

    /**
     * True if the list should execute {@code #restartLoader()}.
     */
    private boolean mShouldRefresh = false;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public ArtistFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // init app settings
        preference = PreferenceUtils.getInstance(context);
        // Register the music status listener
        if (context instanceof ActivityBase) {
            ((ActivityBase) context).setMusicStateListenerListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize views
        View mRootView = inflater.inflate(R.layout.grid_base, container, false);
        TextView emptyHolder = mRootView.getRootView().findViewById(R.id.grid_base_empty_info);
        mList = mRootView.findViewById(R.id.grid_base);
        // init list
        initList();
        // setup list view
        mList.setAdapter(mAdapter);
        // setup empty view
        mList.setEmptyView(emptyHolder);
        // Release any references to the recycled Views
        mList.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        mList.setOnCreateContextMenuListener(this);
        // Show the albums and songs from the selected artist
        mList.setOnItemClickListener(this);
        // To help make scrolling smooth
        mList.setOnScrollListener(this);
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        mAdapter.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (menuInfo instanceof AdapterContextMenuInfo) {
            // Get the position of the selected item
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            // Create a new model
            mArtist = mAdapter.getItem(info.position);
            if (mArtist != null) {
                // Create a list of the artist's songs
                mArtistList = MusicUtils.getSongListForArtist(requireContext(), mArtist.getId());
                // Play the artist
                menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
                // Add the artist to the queue
                menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
                // Add the artist to a playlist
                SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
                MusicUtils.makePlaylistMenu(requireActivity(), GROUP_ID, subMenu, false);
                // Delete the artist
                menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
            }
        } else {
            // remove selection
            mArtist = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Avoid leaking context menu selections
        if (item.getGroupId() == GROUP_ID && mArtist != null) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(mArtistList, 0, true);
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), mArtistList);
                    return true;

                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(mArtistList).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case FragmentMenuItems.PLAYLIST_SELECTED:
                    long id = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(requireActivity(), mArtistList, id);
                    return true;

                case FragmentMenuItems.DELETE:
                    mShouldRefresh = true;
                    String artist = mArtist.getName();
                    MusicUtils.openDeleteDialog(requireActivity(), artist, mArtistList);
                    return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mAdapter.setPauseDiskCache(true);
        } else {
            mAdapter.setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, @NonNull View view, int position, long id) {
        if (view.getId() == R.id.image) {
            long[] list = MusicUtils.getSongListForArtist(getContext(), id);
            MusicUtils.playAll(list, 0, false);
        } else {
            Artist selectedArtist = mAdapter.getItem(position);
            if (selectedArtist != null) {
                NavUtils.openArtistProfile(requireActivity(), selectedArtist.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
        return new ArtistLoader(requireActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Artist>> loader, @NonNull List<Artist> data) {
        // disable loader
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
        // Start fresh
        mAdapter.clear();
        // Add the data to the adapter
        for (Artist artist : data) {
            mAdapter.add(artist);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Artist>> loader) {
        // Clear the data in the adapter
        mAdapter.clear();
    }

    /**
     * Restarts the loader.
     */
    @Override
    public void refresh() {
        initList();
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }


    @Override
    public void setCurrentTrack() {
        if (mList != null && mAdapter != null) {
            long artistId = MusicUtils.getCurrentArtistId();
            for (int i = 0; i < mAdapter.getCount(); i++) {
                Artist artist = mAdapter.getItem(i);
                if (artist != null && artist.getId() == artistId) {
                    mList.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        if (mShouldRefresh) {
            LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
        }
        mShouldRefresh = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMetaChanged() {
        // Nothing to do
    }

    /**
     * initialize adapter & list
     */
    private void initList() {
        if (preference.isSimpleLayout(ARTIST_LAYOUT)) {
            mAdapter = new ArtistAdapter(requireActivity(), R.layout.list_item_normal);
        } else if (preference.isDetailedLayout(ARTIST_LAYOUT)) {
            mAdapter = new ArtistAdapter(requireActivity(), R.layout.list_item_detailed);
        } else {
            mAdapter = new ArtistAdapter(requireActivity(), R.layout.grid_item_normal);
        }
        if (preference.isSimpleLayout(ARTIST_LAYOUT)) {
            mList.setNumColumns(ONE);
        } else if (preference.isDetailedLayout(ARTIST_LAYOUT)) {
            mAdapter.setLoadExtraData();
            if (ApolloUtils.isLandscape(requireContext())) {
                mList.setNumColumns(TWO);
            } else {
                mList.setNumColumns(ONE);
            }
        } else {
            if (ApolloUtils.isLandscape(requireContext())) {
                mList.setNumColumns(FOUR);
            } else {
                mList.setNumColumns(TWO);
            }
        }
        // set adapter and empty view for the list
        mList.setAdapter(mAdapter);
    }
}