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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ProfileSongAdapter;
import com.andrew.apollo.loaders.FolderSongLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ProfileActivity.FragmentCallback;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.widgets.ProfileTabCarousel;
import com.andrew.apollo.widgets.VerticalScrollListener;

import java.util.List;

import static com.andrew.apollo.adapters.ProfileSongAdapter.DISPLAY_DEFAULT_SETTING;
import static com.andrew.apollo.menu.FragmentMenuItems.ADD_TO_FAVORITES;
import static com.andrew.apollo.menu.FragmentMenuItems.ADD_TO_PLAYLIST;
import static com.andrew.apollo.menu.FragmentMenuItems.ADD_TO_QUEUE;
import static com.andrew.apollo.menu.FragmentMenuItems.DELETE;
import static com.andrew.apollo.menu.FragmentMenuItems.MORE_BY_ARTIST;
import static com.andrew.apollo.menu.FragmentMenuItems.NEW_PLAYLIST;
import static com.andrew.apollo.menu.FragmentMenuItems.PLAYLIST_SELECTED;
import static com.andrew.apollo.menu.FragmentMenuItems.PLAY_NEXT;
import static com.andrew.apollo.menu.FragmentMenuItems.PLAY_SELECTION;
import static com.andrew.apollo.menu.FragmentMenuItems.USE_AS_RINGTONE;

/**
 * decompiled from Apollo 1.6 APK
 * <p>
 * This fragment class shows tracks from a music folder
 */
public class FolderSongFragment extends Fragment implements LoaderCallbacks<List<Song>>,
        OnItemClickListener, FragmentCallback {

    /**
     * context menu ID
     */
    private static final int GROUP_ID = 0x1CABF982;

    /**
     * ID for the loader
     */
    private static final int LOADER_ID = 0x16A4BF2B;

    /**
     *
     */
    private ProfileTabCarousel mProfileTabCarousel;

    /**
     * fragment list view
     */
    private ListView mList;

    /**
     * list view adapter with song views
     */
    private ProfileSongAdapter mAdapter;

    /**
     * track selected from contextmenu
     */
    @Nullable
    private Song selectedSong;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        mProfileTabCarousel = activity.findViewById(R.id.activity_profile_base_tab_carousel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_DEFAULT_SETTING, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle extras) {
        // root view of the fragment
        View view = inflater.inflate(R.layout.list_base, parent, false);
        // empty info
        TextView emptyInfo = view.findViewById(R.id.list_base_empty_info);
        // list view of the fragment
        mList = view.findViewById(R.id.list_base);
        // set song adapter
        mList.setAdapter(mAdapter);
        // Set empty list info
        mList.setEmptyView(emptyInfo);
        mList.setRecyclerListener(new RecycleHolder());
        mList.setOnCreateContextMenuListener(this);
        mList.setOnItemClickListener(this);
        mList.setOnScrollListener(new VerticalScrollListener(null, mProfileTabCarousel, 0));
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        savedInstanceState = getArguments();
        if (savedInstanceState != null) {
            LoaderManager.getInstance(this).initLoader(LOADER_ID, savedInstanceState, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle extras) {
        Bundle bundle;
        super.onSaveInstanceState(extras);
        if (getArguments() != null) {
            bundle = getArguments();
        } else {
            bundle = new Bundle();
        }
        extras.putAll(bundle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        if (info instanceof AdapterContextMenuInfo) {
            // Get the position of the selected item
            AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) info;
            // set selected track
            selectedSong = mAdapter.getItem(adapterInfo.position);
            // Play the song
            menu.add(GROUP_ID, PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            // play the song
            menu.add(GROUP_ID, PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
            // Add the song to the queue
            menu.add(GROUP_ID, ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
            // get more tracks by artist
            menu.add(GROUP_ID, MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
            // set selected track as ringtone
            menu.add(GROUP_ID, USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
            // delete track
            menu.add(GROUP_ID, DELETE, Menu.NONE, R.string.context_menu_delete);
            // Add the song to a playlist
            SubMenu subMenu = menu.addSubMenu(GROUP_ID, ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
            MusicUtils.makePlaylistMenu(requireActivity(), GROUP_ID, subMenu, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == GROUP_ID && selectedSong != null) {
            long[] ids = {selectedSong.getId()};

            switch (item.getItemId()) {
                default:
                    return super.onContextItemSelected(item);

                case PLAY_SELECTION:
                    MusicUtils.playAll(ids, 0, false);
                    return true;

                case PLAY_NEXT:
                    MusicUtils.playNext(ids);
                    return true;

                case ADD_TO_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), ids);
                    return true;

                case ADD_TO_FAVORITES:
                    FavoritesStore.getInstance(requireContext()).addSongId(selectedSong);
                    return true;

                case NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(ids).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case PLAYLIST_SELECTED:
                    long playlistId = item.getIntent().getLongExtra("playlist", 0L);
                    MusicUtils.addToPlaylist(requireActivity(), ids, playlistId);
                    return true;

                case MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(requireActivity(), selectedSong.getArtist());
                    return true;

                case USE_AS_RINGTONE:
                    MusicUtils.setRingtone(requireActivity(), selectedSong.getId());
                    return true;

                case DELETE:
                    break;
            }
            MusicUtils.openDeleteDialog(requireActivity(), selectedSong.getName(), ids);
            refresh();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle extras) {
        String foldername;
        if (extras != null)
            foldername = extras.getString("folder_path", "");
        else
            foldername = "";
        return new FolderSongLoader(requireContext(), foldername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, @NonNull List<Song> data) {
        // disable loader
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
        // start fresh
        mAdapter.clear();
        // add items to adapter
        for (Song song : data) {
            mAdapter.add(song);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        mAdapter.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
        MusicUtils.playAllFromUserItemClick(mAdapter, position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        // Scroll to the stop of the list before restarting the loader.
        // Otherwise, if the user has scrolled enough to move the header, it
        // becomes misplaced and needs to be reset.
        mList.setSelection(0);
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), this);
    }
}