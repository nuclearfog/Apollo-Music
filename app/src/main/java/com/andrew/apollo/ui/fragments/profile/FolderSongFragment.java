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
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
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
 */
public class FolderSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>,
        AdapterView.OnItemClickListener, FragmentCallback {

    /**
     * context menu ID
     */
    private static final int GROUP_ID = 0x1CABF982;

    private ProfileSongAdapter mAdapter;

    private ListView mListView;

    private ProfileTabCarousel mProfileTabCarousel;

    /**
     * track selected from contextmenu
     */
    @Nullable
    private Song selectedSong;


    @Override
    public void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        this.mAdapter = new ProfileSongAdapter(requireContext(), R.layout.list_item_simple, 1);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle paramBundle) {
        super.onActivityCreated(paramBundle);
        setHasOptionsMenu(true);
        paramBundle = getArguments();
        if (paramBundle != null) {
            LoaderManager.getInstance(this).initLoader(0, paramBundle, this);
        }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        mProfileTabCarousel = activity.findViewById(R.id.acivity_profile_base_tab_carousel);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle paramBundle) {
        Bundle bundle;
        super.onSaveInstanceState(paramBundle);
        if (getArguments() != null) {
            bundle = getArguments();
        } else {
            bundle = new Bundle();
        }
        paramBundle.putAll(bundle);
    }


    @Override
    public void onCreateContextMenu(@NonNull ContextMenu paramContextMenu, @NonNull View paramView, ContextMenuInfo paramContextMenuInfo) {
        super.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
        int position = ((AdapterContextMenuInfo) paramContextMenuInfo).position - 1;
        selectedSong = mAdapter.getItem(position);
        paramContextMenu.add(GROUP_ID, PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
        paramContextMenu.add(GROUP_ID, PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
        paramContextMenu.add(GROUP_ID, ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
        paramContextMenu.add(GROUP_ID, MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
        paramContextMenu.add(GROUP_ID, USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
        paramContextMenu.add(GROUP_ID, DELETE, Menu.NONE, R.string.context_menu_delete);
        SubMenu subMenu = paramContextMenu.addSubMenu(GROUP_ID, ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(requireActivity(), GROUP_ID, subMenu, true);
    }


    @Override
    public boolean onContextItemSelected(MenuItem paramMenuItem) {
        if (paramMenuItem.getGroupId() == GROUP_ID && selectedSong != null) {
            long[] ids = {selectedSong.getId()};

            switch (paramMenuItem.getItemId()) {
                default:
                    return super.onContextItemSelected(paramMenuItem);

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
                    long playlistId = paramMenuItem.getIntent().getLongExtra("playlist", 0L);
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


    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int paramInt, @Nullable Bundle paramBundle) {
        String foldername = null;
        if (paramBundle != null)
            foldername = paramBundle.getString("folder_path");
        if (foldername == null)
            foldername = "";
        return new FolderSongLoader(requireContext(), foldername);
    }


    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.list_base, paramViewGroup, false);
        mListView = view.findViewById(R.id.list_base);
        mListView.setAdapter(mAdapter);
        mListView.setRecyclerListener(new RecycleHolder());
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(new VerticalScrollListener(null, mProfileTabCarousel, 0));
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setFastScrollEnabled(false);
        return view;
    }


    @Override
    public void onItemClick(AdapterView<?> paramAdapterView, View paramView, int paramInt, long paramLong) {
        MusicUtils.playAllFromUserItemClick(mAdapter, paramInt);
    }


    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> paramLoader, List<Song> paramList) {
        if (!paramList.isEmpty()) {
            mAdapter.clear();
            for (Song song : paramList)
                mAdapter.add(song);
            mAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> paramLoader) {
        mAdapter.clear();
    }


    @Override
    public void refresh() {
        mListView.setSelection(0);
        LoaderManager.getInstance(this).restartLoader(0, getArguments(), this);
    }
}