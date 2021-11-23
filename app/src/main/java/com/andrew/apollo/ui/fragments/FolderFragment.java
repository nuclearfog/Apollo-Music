package com.andrew.apollo.ui.fragments;

import static com.andrew.apollo.Config.FOLDER;
import static com.andrew.apollo.Config.ID;
import static com.andrew.apollo.Config.MIME_TYPE;
import static com.andrew.apollo.Config.NAME;
import static com.andrew.apollo.ui.activities.ProfileActivity.PAGE_FOLDERS;

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
import com.andrew.apollo.ui.fragments.phone.PhoneFragmentCallback;
import com.andrew.apollo.utils.MusicUtils;

import java.io.File;
import java.util.List;

/**
 * decompiled from Apollo 1.6 APK
 */
public class FolderFragment extends Fragment implements LoaderCallbacks<List<File>>,
        OnItemClickListener, PhoneFragmentCallback {

    /**
     * context menu group ID
     */
    private static final int GROUP_ID = 0x1E42C9C7;

    /**
     * context menu item
     */
    private static final int ADD_QUEUE = 0x67A7B3EB;

    /**
     * context menu item
     */
    private static final int SELECTION = 0x718EDAAE;

    /**
     * ID of the loader
     */
    private static final int LOADER_ID = 0xE1E246AA;

    /**
     * IDs of all tracks of the folder
     */
    @NonNull
    private long[] selectedFolderSongs = {};

    /**
     * listview adapter for music folder view
     */
    private FolderAdapter mAdapter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(@Nullable Bundle extras) {
        super.onCreate(extras);
        mAdapter = new FolderAdapter(requireContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle extras) {
        // Init views
        View mRootView = inflater.inflate(R.layout.list_base, container, false);
        ListView mList = mRootView.findViewById(R.id.list_base);
        TextView emptyholder = mRootView.findViewById(R.id.list_base_empty_info);
        // set listview
        mList.setAdapter(mAdapter);
        mList.setEmptyView(emptyholder);
        mList.setRecyclerListener(new RecycleHolder());
        mList.setOnCreateContextMenuListener(this);
        mList.setOnItemClickListener(this);
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        if (info instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo adapterContextMenuInfo = (AdapterContextMenuInfo) info;
            File mFolder = mAdapter.getItem(adapterContextMenuInfo.position);
            selectedFolderSongs = MusicUtils.getSongListForFolder(requireContext(), mFolder.toString());
            menu.add(GROUP_ID, SELECTION, Menu.NONE, R.string.context_menu_play_selection);
            menu.add(GROUP_ID, ADD_QUEUE, Menu.NONE, R.string.add_to_queue);
        } else {
            // remove selection
            selectedFolderSongs = new long[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case SELECTION:
                    MusicUtils.playAll(selectedFolderSongs, 0, false);
                    return true;

                case ADD_QUEUE:
                    MusicUtils.addToQueue(requireActivity(), selectedFolderSongs);
                    return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        File mFolder = mAdapter.getItem(position);
        Bundle bundle = new Bundle();
        bundle.putLong(ID, 0L);
        bundle.putString(NAME, mFolder.getName());
        bundle.putString(MIME_TYPE, PAGE_FOLDERS);
        bundle.putString(FOLDER, mFolder.toString());
        Intent intent = new Intent(requireContext(), ProfileActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Loader<List<File>> onCreateLoader(int id, Bundle args) {
        return new FolderLoader(requireContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<File>> loader, @NonNull List<File> data) {
        // stop loader
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
        // Clear list
        mAdapter.clear();
        // add data to the adapter
        for (File file : data) {
            mAdapter.add(file);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(@NonNull Loader<List<File>> loader) {
        // Clear the data in the adapter
        mAdapter.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentTrack() {
        // do nothing
    }
}