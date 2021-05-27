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
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists.Members;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ProfileSongAdapter;
import com.andrew.apollo.dragdrop.DragSortListView;
import com.andrew.apollo.dragdrop.DragSortListView.DragScrollProfile;
import com.andrew.apollo.dragdrop.DragSortListView.DropListener;
import com.andrew.apollo.dragdrop.DragSortListView.RemoveListener;
import com.andrew.apollo.loaders.PlaylistSongLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ProfileActivity.FragmentCallback;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.widgets.VerticalScrollListener;

import java.util.List;

import static com.andrew.apollo.adapters.ProfileSongAdapter.DISPLAY_PLAYLIST_SETTING;
import static com.andrew.apollo.adapters.ProfileSongAdapter.HEADER_COUNT;

/**
 * This class is used to display all of the songs from a particular playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>,
        OnItemClickListener, DropListener, RemoveListener, DragScrollProfile, FragmentCallback {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 0x37B5704;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER_ID = 0x61AF9DC4;

    /**
     * selection to remove track with given ID
     */
    private static final String DELETE_SELECT = Members.AUDIO_ID + "=?";

    /**
     * The adapter for the list
     */
    private ProfileSongAdapter mAdapter;

    /**
     * Represents a song
     */
    @Nullable
    private Song mSong;

    /**
     * The Id of the playlist the songs belong to
     */
    private long mPlaylistId;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public PlaylistSongFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        activity.findViewById(R.id.activity_profile_base_tab_carousel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adapter
        mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_PLAYLIST_SETTING, true);
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
        DragSortListView mListView = rootView.findViewById(R.id.list_base);
        // setup empty text
        emptyInfo.setText(R.string.empty_playlist);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Set empty list info
        mListView.setEmptyView(emptyInfo);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        mListView.setOnCreateContextMenuListener(this);
        // Play the selected song
        mListView.setOnItemClickListener(this);
        // Set the drop listener
        mListView.setDropListener(this);
        // Set the swipe to remove listener
        mListView.setRemoveListener(this);
        // Quick scroll while dragging
        mListView.setDragScrollProfile(this);
        // To help make scrolling smooth
        mListView.setOnScrollListener(new VerticalScrollListener(null, null, 0));
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
            mPlaylistId = arguments.getLong(Config.ID);
            LoaderManager.getInstance(this).initLoader(LOADER_ID, arguments, this);
        }
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
        if (menuInfo instanceof AdapterContextMenuInfo) {
            // Get the position of the selected item
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            // Creat a new song
            mSong = mAdapter.getItem(info.position);
            // Play the song
            menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            // Play next
            menu.add(GROUP_ID, FragmentMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
            // Add the song to the queue
            menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
            // Add the song to a playlist
            SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
            MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, true);
            // View more content by the song artist
            menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
            // Make the song a ringtone
            menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
            // Remove the song from playlist
            menu.add(GROUP_ID, FragmentMenuItems.REMOVE_FROM_PLAYLIST, Menu.NONE, R.string.context_menu_remove_from_playlist);
            // Delete the song
            menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
        }
    }


    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == GROUP_ID && mSong != null) {
            long[] trackId = {mSong.getId()};

            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(trackId, 0, false);
                    return true;

                case FragmentMenuItems.PLAY_NEXT:
                    MusicUtils.playNext(trackId);
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), trackId);
                    return true;

                case FragmentMenuItems.ADD_TO_FAVORITES:
                    FavoritesStore.getInstance(requireContext()).addSongId(mSong);
                    return true;

                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(trackId).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case FragmentMenuItems.PLAYLIST_SELECTED:
                    long playlistId = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(requireActivity(), trackId, playlistId);
                    return true;

                case FragmentMenuItems.MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
                    return true;

                case FragmentMenuItems.USE_AS_RINGTONE:
                    MusicUtils.setRingtone(requireActivity(), mSong.getId());
                    return true;

                case FragmentMenuItems.DELETE:
                    MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackId);
                    return true;

                case FragmentMenuItems.REMOVE_FROM_PLAYLIST:
                    mAdapter.remove(mSong);
                    MusicUtils.removeFromPlaylist(requireContext(), mSong.getId(), mPlaylistId);
                    LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
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
        MusicUtils.playAllFromUserItemClick(mAdapter, position);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new PlaylistSongLoader(requireContext(), mPlaylistId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, @NonNull List<Song> data) {
        // disable loader
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
        // Start fresh
        mAdapter.clear();
        // Add the data to the adpater
        for (Song song : data) {
            mAdapter.add(song);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getSpeed(float w) {
        if (w > 0.8f)
            return mAdapter.getCount() / 0.001f;
        return 10.0f * w;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(int which) {
        mSong = mAdapter.getItem(which);
        if (mSong != null) {
            ContentResolver resolver = requireActivity().getContentResolver();
            Uri uri = Members.getContentUri("external", mPlaylistId);
            String[] args = {Long.toString(mSong.getId())};
            resolver.delete(uri, DELETE_SELECT, args);

            mAdapter.remove(mSong);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(int from, int to) {
        if (from == 0 || to == 0 || from == to) {
            // no changes detected, revert layout changes
            mAdapter.notifyDataSetChanged();
        } else {
            // update adapter
            Song mSong = mAdapter.getItem(from);
            mAdapter.remove(mSong);
            mAdapter.insert(mSong, to);
            // move playlist track
            ContentResolver resolver = requireActivity().getContentResolver();
            Members.moveItem(resolver, mPlaylistId, from - HEADER_COUNT, to - HEADER_COUNT);
        }
    }


    @Override
    public void refresh() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), this);
    }
}