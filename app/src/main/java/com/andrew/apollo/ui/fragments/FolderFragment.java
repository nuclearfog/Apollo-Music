package com.andrew.apollo.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.FolderAdapter;
import com.andrew.apollo.loaders.FolderLoader;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment.BrowserCallback;
import com.andrew.apollo.utils.MusicUtils;

import java.io.File;
import java.util.List;

import static com.andrew.apollo.ui.activities.ProfileActivity.PAGE_FOLDERS;

/**
 * decompiled from Apollo 1.6 APK
 */
public class FolderFragment extends Fragment implements LoaderCallbacks<List<File>>,
        OnItemClickListener, BrowserCallback {

    private static final int GROUP_ID = 6;

    private static final int ADD_QUEUE = 1;

    private static final int SELECTION = 2;

    private FolderAdapter mAdapter;

    /**
     * current folder to search for tracks
     */
    private File mFolder;

    /**
     * IDs of all tracks of the folder
     */
    private long[] mSongList = {};

    private TextView emptyholder;

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        // Init views
        View mRootView = paramLayoutInflater.inflate(R.layout.list_base, paramViewGroup, false);
        ListView mListView = mRootView.findViewById(R.id.list_base);
        emptyholder = mRootView.findViewById(R.id.list_base_empty_info);
        // set listview
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(emptyholder);
        mListView.setRecyclerListener(new RecycleHolder());
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        return mRootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle paramBundle) {
        super.onActivityCreated(paramBundle);
        setHasOptionsMenu(true);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }


    @Override
    public boolean onContextItemSelected(@NonNull MenuItem paramMenuItem) {
        if (paramMenuItem.getGroupId() == GROUP_ID) {
            switch (paramMenuItem.getItemId()) {
                case SELECTION:
                    MusicUtils.playAll(mSongList, 0, false);

                    // fallthrough
                case ADD_QUEUE:
                    MusicUtils.addToQueue(requireContext(), mSongList);
                    return true;
            }
        }
        return super.onContextItemSelected(paramMenuItem);
    }


    @Override
    public void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        mAdapter = new FolderAdapter(requireContext(), R.layout.list_item_detailed);
    }


    @Override
    public void onCreateContextMenu(@NonNull ContextMenu paramContextMenu, @NonNull View paramView, ContextMenuInfo paramContextMenuInfo) {
        super.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
        if (paramContextMenuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo adapterContextMenuInfo = (AdapterContextMenuInfo) paramContextMenuInfo;
            mFolder = mAdapter.getItem(adapterContextMenuInfo.position);
            mSongList = MusicUtils.getSongListForFolder(requireContext(), mFolder);
            paramContextMenu.add(GROUP_ID, SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            paramContextMenu.add(GROUP_ID, ADD_QUEUE, Menu.NONE, R.string.add_to_queue);
        }
    }


    @NonNull
    @Override
    public Loader<List<File>> onCreateLoader(int paramInt, Bundle paramBundle) {
        return new FolderLoader(requireContext());
    }


    @Override
    public void onItemClick(AdapterView<?> paramAdapterView, View paramView, int paramInt, long paramLong) {
        mFolder = mAdapter.getItem(paramInt);
        Bundle bundle = new Bundle();
        bundle.putLong("id", 0L);
        bundle.putString("name", mFolder.getName());
        bundle.putString("mime_type", PAGE_FOLDERS);
        bundle.putString("folder_path", mFolder.toString());
        Intent intent = new Intent(requireContext(), ProfileActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }


    @Override
    public void onLoadFinished(@NonNull Loader<List<File>> paramLoader, List<File> paramList) {
        // Clear list
        mAdapter.clear();
        if (paramList.isEmpty()) {
            emptyholder.setVisibility(View.VISIBLE);
        } else {
            emptyholder.setVisibility(View.INVISIBLE);
            for (File file : paramList)
                mAdapter.add(file);
            mAdapter.buildCache();
        }
    }


    @Override
    public void onLoaderReset(@NonNull Loader<List<File>> paramLoader) {
        mAdapter.clear();
    }


    @Override
    public void refresh() {
        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }


    @Override
    public void scrollToCurrent() {
    }
}