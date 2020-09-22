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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.AlbumAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.loaders.RecentLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.AppCompatBase;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.andrew.apollo.utils.PreferenceUtils.RECENT_LAYOUT;

/**
 * This class is used to display all of the recently listened to albums by the
 * user.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentFragment extends Fragment implements LoaderCallbacks<List<Album>>,
        OnScrollListener, OnItemClickListener, MusicStateListener {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 1;

    /**
     * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
     */
    private static final int ONE = 1, TWO = 2, FOUR = 4;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * Fragment UI
     */
    private View mRootView;

    /**
     * The adapter for the grid
     */
    private AlbumAdapter mAdapter;

    /**
     * The grid view
     */
    private GridView mGridView;

    /**
     * The list view
     */
    private ListView mListView;

    /**
     * Album song list
     */
    private long[] mAlbumList;

    /**
     * Represents an album
     */
    private Album mAlbum;

    private PreferenceUtils pref;

    /**
     * True if the list should execute {@code #restartLoader()}.
     */
    private boolean mShouldRefresh = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatBase) {
            // Register the music status listener
            ((AppCompatBase) context).setMusicStateListenerListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceUtils.getInstance(getActivity());
        int layout;
        if (isSimpleLayout()) {
            layout = R.layout.list_item_normal;
        } else if (isDetailedLayout()) {
            layout = R.layout.list_item_detailed;
        } else {
            layout = R.layout.grid_items_normal;
        }
        mAdapter = new AlbumAdapter(requireActivity(), layout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The View for the fragment's UI
        if (isSimpleLayout()) {
            mRootView = inflater.inflate(R.layout.list_base, container, false);
            initListView();
        } else {
            mRootView = inflater.inflate(R.layout.grid_base, container, false);
            initGridView();
        }
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        LoaderManager.getInstance(this).initLoader(LOADER, null, this);
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
        // Get the position of the selected item
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        // Create a new album
        mAlbum = mAdapter.getItem(info.position);
        // Create a list of the album's songs
        mAlbumList = MusicUtils.getSongListForAlbum(requireContext(), mAlbum.mAlbumId);
        // Play the album
        menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
        // Add the album to the queue
        menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
        // Add the album to a playlist
        SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(requireActivity(), GROUP_ID, subMenu, false);
        // View more content by the album artist
        menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
        // Remove the album from the list
        menu.add(GROUP_ID, FragmentMenuItems.REMOVE_FROM_RECENT, Menu.NONE, R.string.context_menu_remove_from_recent);
        // Delete the album
        menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Avoid leaking context menu selections
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(mAlbumList, 0, false);
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), mAlbumList);
                    return true;

                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(mAlbumList).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case FragmentMenuItems.MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(requireActivity(), mAlbum.mArtistName);
                    return true;

                case FragmentMenuItems.PLAYLIST_SELECTED:
                    long id = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(requireActivity(), mAlbumList, id);
                    return true;

                case FragmentMenuItems.REMOVE_FROM_RECENT:
                    mShouldRefresh = true;
                    RecentStore.getInstance(requireActivity()).removeItem(mAlbum.mAlbumId);
                    MusicUtils.refresh();
                    return true;

                case FragmentMenuItems.DELETE:
                    mShouldRefresh = true;
                    String album = mAlbum.mAlbumName;
                    DeleteDialog.newInstance(album, mAlbumList,
                            ImageFetcher.generateAlbumCacheKey(album, mAlbum.mArtistName))
                            .show(getParentFragmentManager(), "DeleteDialog");
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAlbum = mAdapter.getItem(position);
        NavUtils.openAlbumProfile(getActivity(), mAlbum.mAlbumName, mAlbum.mArtistName, mAlbum.mAlbumId);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Album>> onCreateLoader(int id, Bundle args) {
        return new RecentLoader(requireActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Album>> loader, List<Album> data) {
        // Check for any errors
        if (data.isEmpty()) {
            // Set the empty text
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.empty_recent);
            empty.setTextColor(pref.getDefaultThemeColor(requireContext()));
            if (isSimpleLayout()) {
                mListView.setEmptyView(empty);
            } else {
                mGridView.setEmptyView(empty);
            }
        } else {
            // Start fresh
            mAdapter.unload();
            // Add the data to the adpater
            for (Album album : data) {
                mAdapter.add(album);
            }
            // Build the cache
            mAdapter.buildCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
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
            LoaderManager.getInstance(this).restartLoader(LOADER, null, this);
        }
        mShouldRefresh = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMetaChanged() {
        LoaderManager.getInstance(this).restartLoader(LOADER, null, this);
    }

    /**
     * Sets up various helpers for both the list and grid
     *
     * @param list The list or grid
     */
    private void initAbsListView(AbsListView list) {
        // Release any references to the recycled Views
        list.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        list.setOnCreateContextMenuListener(this);
        // Show the albums and songs from the selected artist
        list.setOnItemClickListener(this);
        // To help make scrolling smooth
        list.setOnScrollListener(this);
    }

    /**
     * Sets up the list view
     */
    private void initListView() {
        // Initialize the grid
        mListView = mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Set up the helpers
        initAbsListView(mListView);
        mAdapter.setTouchPlay(true);
    }

    /**
     * Sets up the grid view
     */
    private void initGridView() {
        // Initialize the grid
        mGridView = mRootView.findViewById(R.id.grid_base);
        // Set the data behind the grid
        mGridView.setAdapter(mAdapter);
        // Set up the helpers
        initAbsListView(mGridView);
        if (ApolloUtils.isLandscape(requireContext())) {
            if (isDetailedLayout()) {
                mAdapter.setLoadExtraData(true);
                mGridView.setNumColumns(TWO);
            } else {
                mGridView.setNumColumns(FOUR);
            }
        } else {
            if (isDetailedLayout()) {
                mAdapter.setLoadExtraData(true);
                mGridView.setNumColumns(ONE);
            } else {
                mGridView.setNumColumns(TWO);
            }
        }
    }

    private boolean isSimpleLayout() {
        return pref.isSimpleLayout(RECENT_LAYOUT);
    }

    private boolean isDetailedLayout() {
        return pref.isDetailedLayout(RECENT_LAYOUT);
    }
}