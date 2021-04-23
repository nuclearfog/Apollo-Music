package com.andrew.apollo.ui.fragments.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
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
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.widgets.ProfileTabCarousel;
import com.andrew.apollo.widgets.VerticalScrollListener;

import java.io.File;
import java.util.List;

public class FolderSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>>, AdapterView.OnItemClickListener {

    private ProfileSongAdapter mAdapter;

    private String mAlbumName;

    private String mArtistName;

    private ListView mListView;

    private ProfileTabCarousel mProfileTabCarousel;

    private long mSelectedId;

    private Song mSong;

    private String mSongName;

    private void refresh() {
        mListView.setSelection(0);
        LoaderManager.getInstance(this).restartLoader(0, getArguments(), this);
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
    public boolean onContextItemSelected(MenuItem paramMenuItem) {
        if (paramMenuItem.getGroupId() == 14) {
            long l;
            switch (paramMenuItem.getItemId()) {
                default:
                    return super.onContextItemSelected(paramMenuItem);

                case 1:
                    MusicUtils.playAll(new long[]{mSelectedId}, 0, false);
                    return true;

                case 16:
                    MusicUtils.playNext(new long[]{mSelectedId});
                    return true;

                case 2:
                    MusicUtils.addToQueue(requireContext(), new long[]{this.mSelectedId});
                    return true;

                case 4:
                    FavoritesStore.getInstance(requireContext()).addSongId(mSelectedId, mSongName, mAlbumName, mArtistName);
                    return true;

                case 5:
                    CreateNewPlaylist.getInstance(new long[]{this.mSelectedId}).show(getParentFragmentManager(), "CreatePlaylist");
                    return true;

                case 7:
                    l = paramMenuItem.getIntent().getLongExtra("playlist", 0L);
                    MusicUtils.addToPlaylist(requireActivity(), new long[]{mSelectedId}, l);
                    return true;

                case 8:
                    NavUtils.openArtistProfile(requireActivity(), mArtistName);
                    return true;

                case 12:
                    MusicUtils.setRingtone(requireContext(), mSelectedId);
                    return true;

                case 9:
                    break;
            }
            DeleteDialog.newInstance(mSong.getName(), new long[]{mSelectedId}, null).show(getParentFragmentManager(), "DeleteDialog");
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        this.mAdapter = new ProfileSongAdapter(requireContext(), R.layout.list_item_simple, 1);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu paramContextMenu, @NonNull View paramView, ContextMenuInfo paramContextMenuInfo) {
        super.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
        int position = ((AdapterContextMenuInfo) paramContextMenuInfo).position - 1;
        mSong = mAdapter.getItem(position);
        if (mSong != null) {
            mSelectedId = mSong.getId();
            mSongName = mSong.getName();
            mAlbumName = mSong.getAlbum();
            mArtistName = mSong.getArtist();
        }
        paramContextMenu.add(14, 1, 0, R.string.context_menu_play_selection);
        paramContextMenu.add(14, 16, 0, R.string.context_menu_play_next);
        paramContextMenu.add(14, 2, 0, R.string.add_to_queue);
        SubMenu subMenu = paramContextMenu.addSubMenu(14, 3, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(requireActivity(), 14, subMenu, true);
        paramContextMenu.add(14, 8, 0, R.string.context_menu_more_by_artist);
        paramContextMenu.add(14, 12, 0, R.string.context_menu_use_as_ringtone);
        paramContextMenu.add(14, 9, 0, R.string.context_menu_delete);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int paramInt, @Nullable Bundle paramBundle) {
        String foldername = null;
        if (paramBundle != null)
            foldername = paramBundle.getString("folder_path");
        if (foldername == null)
            foldername = "";
        return new FolderSongLoader(requireContext(), new File(foldername));
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
}