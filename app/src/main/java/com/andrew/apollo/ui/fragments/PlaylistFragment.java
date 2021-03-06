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

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PlaylistAdapter;
import com.andrew.apollo.loaders.PlaylistLoader;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.AppCompatBase;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.MusicUtils;

import java.util.List;

import static com.andrew.apollo.ui.activities.ProfileActivity.PAGE_FAVORIT;
import static com.andrew.apollo.ui.activities.ProfileActivity.PAGE_LAST_ADDED;

/**
 * This class is used to display all of the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistFragment extends Fragment implements LoaderCallbacks<List<Playlist>>,
        OnItemClickListener, MusicStateListener {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = 0;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * The adapter for the list
     */
    private PlaylistAdapter mAdapter;

    /**
     * Represents a playlist
     */
    private Playlist mPlaylist;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public PlaylistFragment() {
    }

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
        // Create the adpater
        mAdapter = new PlaylistAdapter(requireContext(), R.layout.list_item_simple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // init views
        View rootView = inflater.inflate(R.layout.list_base, container, false);
        ListView mListView = rootView.findViewById(R.id.list_base);
        // setup list view
        mListView.setAdapter(mAdapter);
        mListView.setRecyclerListener(new RecycleHolder());
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        return rootView;
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
        if (menuInfo instanceof AdapterContextMenuInfo) {
            // Get the position of the selected item
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            int mPosition = info.position;
            // Create a new playlist
            mPlaylist = mAdapter.getItem(mPosition);
            // Play the playlist
            menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            // Add the playlist to the queue
            menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
            // Delete and rename (user made playlists)
            if (info.position > 1) {
                menu.add(GROUP_ID, FragmentMenuItems.RENAME_PLAYLIST, Menu.NONE, R.string.context_menu_rename_playlist);
                menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    if (info.position == 0) {
                        MusicUtils.playFavorites(getActivity());
                    } else if (info.position == 1) {
                        MusicUtils.playLastAdded(getActivity());
                    } else {
                        MusicUtils.playPlaylist(getActivity(), mPlaylist.mPlaylistId);
                    }
                    return true;

                case FragmentMenuItems.ADD_TO_QUEUE:
                    long[] list;
                    if (info.position == 0) {
                        list = MusicUtils.getSongListForFavorites(getActivity());
                    } else if (info.position == 1) {
                        list = MusicUtils.getSongListForLastAdded(getActivity());
                    } else {
                        list = MusicUtils.getSongListForPlaylist(requireContext(), mPlaylist.mPlaylistId);
                    }
                    MusicUtils.addToQueue(getActivity(), list);
                    return true;

                case FragmentMenuItems.RENAME_PLAYLIST:
                    RenamePlaylist.getInstance(mPlaylist.mPlaylistId).show(getParentFragmentManager(), "RenameDialog");
                    return true;

                case FragmentMenuItems.DELETE:
                    buildDeleteDialog().show();
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
        Bundle bundle = new Bundle();
        mPlaylist = mAdapter.getItem(position);
        String playlistName = null;
        // Favorites list
        if (position == 0) {
            playlistName = getString(R.string.playlist_favorites);
            bundle.putString(Config.MIME_TYPE, PAGE_FAVORIT);
            // Last added
        } else if (position == 1) {
            playlistName = getString(R.string.playlist_last_added);
            bundle.putString(Config.MIME_TYPE, PAGE_LAST_ADDED);
        } else if (mPlaylist != null) {
            // User created
            playlistName = mPlaylist.mPlaylistName;
            bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
            bundle.putLong(Config.ID, mPlaylist.mPlaylistId);
        }
        bundle.putString(Config.NAME, playlistName);
        // Create the intent to launch the profile activity
        Intent intent = new Intent(requireContext(), ProfileActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Playlist>> onCreateLoader(int id, Bundle args) {
        return new PlaylistLoader(requireContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Playlist>> loader, List<Playlist> data) {
        // Check for any errors
        if (!data.isEmpty()) {
            // Start fresh
            mAdapter.unload();
            // Add the data to the adpater
            for (Playlist playlist : data) {
                mAdapter.add(playlist);
            }
            // Build the cache
            mAdapter.buildCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Playlist>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartLoader() {
        // Refresh the list when a playlist is deleted or renamed
        LoaderManager.getInstance(this).restartLoader(LOADER, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMetaChanged() {
        // Nothing to do
    }

    /**
     * Create a new {@link AlertDialog} for easy playlist deletion
     *
     * @return A new {@link AlertDialog} used to delete playlists
     */
    private AlertDialog buildDeleteDialog() {
        return new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_dialog_title, mPlaylist.mPlaylistName))
                .setPositiveButton(R.string.context_menu_delete, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri mUri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mPlaylist.mPlaylistId);
                        requireActivity().getContentResolver().delete(mUri, null, null);
                        MusicUtils.refresh();
                    }
                }).setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setMessage(R.string.cannot_be_undone).create();
    }
}