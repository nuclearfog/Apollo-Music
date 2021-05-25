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

package com.andrew.apollo.ui.fragments.profile;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ArtistAlbumAdapter;
import com.andrew.apollo.loaders.ArtistAlbumLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ProfileActivity.FragmentCallback;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.widgets.ProfileTabCarousel;
import com.andrew.apollo.widgets.VerticalScrollListener;
import com.andrew.apollo.widgets.VerticalScrollListener.ScrollableHeader;

import java.util.List;

/**
 * This class is used to display all of the albums from a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Album>>,
        OnItemClickListener, FragmentCallback, ScrollableHeader {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 0x6CEDC429;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0x6D4DD8EA;

    /**
     * The adapter for the grid
     */
    private ArtistAlbumAdapter mAdapter;
    /**
     * The list view
     */
    private ListView mList;
    /**
     * Album song list
     */
    @Nullable
    private long[] mAlbumList;
    /**
     * Represents an album
     */
    @Nullable
    private Album mAlbum;
    /**
     * Profile header
     */
    private ProfileTabCarousel mProfileTabCarousel;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public ArtistAlbumFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        mProfileTabCarousel = activity.findViewById(R.id.acivity_profile_base_tab_carousel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adpater
        mAdapter = new ArtistAlbumAdapter(requireActivity(), R.layout.list_item_detailed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The View for the fragment's UI
        View rootView = inflater.inflate(R.layout.list_base, container, false);
        // empty info
        TextView emptyInfo = rootView.findViewById(R.id.list_base_empty_info);
        // Initialize the list
        mList = rootView.findViewById(R.id.list_base);
        // Set the data behind the grid
        mList.setAdapter(mAdapter);
        // Set empty list info
        mList.setEmptyView(emptyInfo);
        // Release any references to the recycled Views
        mList.setRecyclerListener(new RecycleHolder());
        // disable fast scroll
        mList.setFastScrollEnabled(false);
        // Listen for ContextMenus to be created
        mList.setOnCreateContextMenuListener(this);
        // Show the songs from the selected album
        mList.setOnItemClickListener(this);
        // To help make scrolling smooth
        mList.setOnScrollListener(new VerticalScrollListener(this, mProfileTabCarousel, 1));
        return rootView;
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
        Bundle arguments = getArguments();
        if (arguments != null) {
            LoaderManager.getInstance(this).initLoader(LOADER, arguments, this);
        }
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
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
        mAlbum = mAdapter.getItem(info.position - 1);
        // Create a list of the album's songs
        if (mAlbum != null) {
            mAlbumList = MusicUtils.getSongListForAlbum(requireContext(), mAlbum.getId());
            // Play the album
            menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            // Add the album to the queue
            menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
            // Add the album to a playlist
            SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
            MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
            // Delete the album
            menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Avoid leaking context menu selections
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    if (mAlbumList != null)
                        MusicUtils.playAll(mAlbumList, 0, false);
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), mAlbumList);
                    return true;

                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(mAlbumList).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case FragmentMenuItems.PLAYLIST_SELECTED:
                    if (mAlbumList != null) {
                        long id = item.getIntent().getLongExtra("playlist", 0);
                        MusicUtils.addToPlaylist(requireActivity(), mAlbumList, id);
                    }
                    return true;

                case FragmentMenuItems.DELETE:
                    if (mAlbum != null)
                        MusicUtils.openDeleteDialog(requireActivity(), mAlbum.getName(), mAlbumList);
                    return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position > 0) {
            Album album = mAdapter.getItem(position - 1);
            if (album != null) {
                NavUtils.openAlbumProfile(requireActivity(), album.getName(), album.getArtist(), album.getId());
                requireActivity().finish();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Album>> onCreateLoader(int id, @Nullable Bundle args) {
        long artistId = args != null ? args.getLong(Config.ID) : 0;
        return new ArtistAlbumLoader(requireActivity(), artistId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Album>> loader, @NonNull List<Album> data) {
        // Start fresh
        mAdapter.clear();
        if (data.isEmpty()) {
            mList.getEmptyView().setVisibility(View.VISIBLE);
        } else {
            mList.getEmptyView().setVisibility(View.INVISIBLE);
            // Add the data to the adpater
            for (Album album : data) {
                mAdapter.add(album);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
        // Clear the data in the adapter
        mAdapter.clear();
    }

    /**
     * Restarts the loader.
     */
    @Override
    public void refresh() {
        // Scroll to the stop of the list before restarting the loader.
        // Otherwise, if the user has scrolled enough to move the header, it
        // becomes misplaced and needs to be reset.
        mList.setSelection(0);
        mAdapter.notifyDataSetChanged();
        LoaderManager.getInstance(this).restartLoader(LOADER, getArguments(), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mAdapter.setPauseDiskCache(true);
        } else {
            mAdapter.setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }
}