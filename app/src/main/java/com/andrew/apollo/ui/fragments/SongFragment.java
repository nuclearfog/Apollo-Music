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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.SongAdapter;
import com.andrew.apollo.loaders.SongLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.AppCompatBase;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.List;

/**
 * This class is used to display all of the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>,
        OnItemClickListener, MusicStateListener {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 4;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * The adapter for the list
     */
    private SongAdapter mAdapter;

    /**
     * The list view
     */
    private ListView mListView;

    /**
     * Represents a song
     */
    private Song mSong;

    /**
     * Id of a context menu item
     */
    private long mSelectedId;

    /**
     * Song, album, and artist name used in the context menu
     */
    private String mSongName, mAlbumName, mArtistName;

    /**
     * True if the list should execute {@code #restartLoader()}.
     */
    private boolean mShouldRefresh = false;

    /**
     * empty list info
     */
    private TextView emptyText;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public SongFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Register the music status listener
        if (context instanceof AppCompatBase) {
            ((AppCompatBase) context).setMusicStateListenerListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adpater
        mAdapter = new SongAdapter(getActivity(), R.layout.list_item_simple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // init views
        View mRootView = inflater.inflate(R.layout.list_base, container, false);
        emptyText = mRootView.findViewById(R.id.list_base_empty_info);
        // setup the list view
        mListView = mRootView.findViewById(R.id.list_base);
        mListView.setAdapter(mAdapter);
        mListView.setRecyclerListener(new RecycleHolder());
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
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
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the position of the selected item
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int mSelectedPosition = info.position;
        // Create a new song
        mSong = mAdapter.getItem(mSelectedPosition);
        if (mSong != null) {
            mSelectedId = mSong.mSongId;
            mSongName = mSong.mSongName;
            mAlbumName = mSong.mAlbumName;
            mArtistName = mSong.mArtistName;
        }
        // Play the song
        menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
        // Play next
        menu.add(GROUP_ID, FragmentMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
        // Add the song to the queue
        menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
        // Add the song to a playlist
        SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(getActivity(), GROUP_ID, subMenu, true);
        // View more content by the song artist
        menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
        // Make the song a ringtone
        menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
        // Delete the song
        menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(new long[]{mSelectedId}, 0, false);
                    return true;

                case FragmentMenuItems.PLAY_NEXT:
                    MusicUtils.playNext(new long[]{
                            mSelectedId
                    });
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(getActivity(), new long[]{
                            mSelectedId
                    });
                    return true;

                case FragmentMenuItems.ADD_TO_FAVORITES:
                    FavoritesStore.getInstance(getActivity()).addSongId(mSelectedId, mSongName, mAlbumName, mArtistName);
                    return true;

                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(new long[]{mSelectedId}).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case FragmentMenuItems.PLAYLIST_SELECTED:
                    long mPlaylistId = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(requireActivity(), new long[]{mSelectedId}, mPlaylistId);
                    return true;

                case FragmentMenuItems.MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getActivity(), mArtistName);
                    return true;

                case FragmentMenuItems.USE_AS_RINGTONE:
                    MusicUtils.setRingtone(requireContext(), mSelectedId);
                    return true;

                case FragmentMenuItems.DELETE:
                    mShouldRefresh = true;
                    DeleteDialog.newInstance(mSong.mSongName, new long[]{mSelectedId}, null).show(getParentFragmentManager(), "DeleteDialog");
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
        return new SongLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        // Start fresh
        mAdapter.unload();
        // Check for any errors
        if (data.isEmpty()) {
            // Set the empty text
            mListView.setEmptyView(emptyText);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            // Add the data to the adpater
            for (Song song : data)
                mAdapter.add(song);
            // Build the cache
            mAdapter.buildCache();
            emptyText.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * Scrolls the list to the currently playing song when the user touches the
     * header in the {@link TitlePageIndicator}.
     */
    public void scrollToCurrentSong() {
        int currentSongPosition = getItemPositionBySong();

        if (currentSongPosition != 0) {
            mListView.setSelection(currentSongPosition);
        }
    }

    /**
     * @return The position of an item in the list based on the name of the
     * currently playing song.
     */
    private int getItemPositionBySong() {
        long trackId = MusicUtils.getCurrentAudioId();
        if (mAdapter == null) {
            return 0;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Song song = mAdapter.getItem(i);
            if (song != null && song.mSongId == trackId) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Restarts the loader.
     */
    public void refresh() {
        LoaderManager.getInstance(this).restartLoader(LOADER, null, this);
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
        // Nothing to do
    }
}