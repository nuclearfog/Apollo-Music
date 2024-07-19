package org.nuclearfog.apollo.ui.fragments;

import static org.nuclearfog.apollo.Config.FOLDER;
import static org.nuclearfog.apollo.Config.ID;
import static org.nuclearfog.apollo.Config.MIME_TYPE;
import static org.nuclearfog.apollo.Config.NAME;
import static org.nuclearfog.apollo.ui.activities.ProfileActivity.PAGE_FOLDERS;

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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.FolderLoader;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.model.Folder;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.adapters.listview.FolderAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * decompiled from Apollo 1.6 APK
 *
 * @author nuclearfog
 */
public class FolderFragment extends Fragment implements AsyncCallback<List<Folder>>, OnItemClickListener, Observer<String> {

	/**
	 * context menu group ID
	 */
	private static final int GROUP_ID = 0x1E42C9C7;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;

	/**
	 * listview adapter for music folder view
	 */
	private FolderAdapter mAdapter;

	/**
	 * viewmodel used for communication with hosting activity
	 */
	private FragmentViewModel viewModel;

	/**
	 * app settings
	 */
	private PreferenceUtils preference;

	/**
	 * context menu selection
	 */
	@Nullable
	private Folder selectedFolder;
	private FolderLoader folderLoader;
	private FolderSongLoader folderSongLoader;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle extras) {
		// Init views
		View mRootView = inflater.inflate(R.layout.list_base, container, false);
		ListView mList = mRootView.findViewById(R.id.list_base);
		TextView emptyholder = mRootView.findViewById(R.id.list_base_empty_info);
		//
		preference = PreferenceUtils.getInstance(requireContext());
		mAdapter = new FolderAdapter(requireContext());
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		folderLoader = new FolderLoader(requireContext());
		folderSongLoader = new FolderSongLoader(requireContext());
		//
		setHasOptionsMenu(true);
		// set listview
		mList.setAdapter(mAdapter);
		mList.setEmptyView(emptyholder);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// start loader
		folderLoader.execute(null, this);
		return mRootView;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		folderLoader.cancel();
		folderSongLoader.cancel();
		super.onDestroyView();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		if (info instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo adapterContextMenuInfo = (AdapterContextMenuInfo) info;
			selectedFolder = mAdapter.getItem(adapterContextMenuInfo.position);
			if (selectedFolder != null) {
				menu.add(GROUP_ID, ContextMenuItems.PLAY_FOLDER, Menu.NONE, R.string.context_menu_play_selection);
				menu.add(GROUP_ID, ContextMenuItems.ADD_FOLDER_QUEUE, Menu.NONE, R.string.add_to_queue);
				// hide artist from list
				if (selectedFolder.isVisible()) {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_FOLDER, Menu.NONE, R.string.context_menu_hide_folder);
				} else {
					menu.add(GROUP_ID, ContextMenuItems.HIDE_FOLDER, Menu.NONE, R.string.context_menu_unhide_folder);
				}
			}
		} else {
			// remove selection
			selectedFolder = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && selectedFolder != null) {
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_FOLDER:
					folderSongLoader.execute(selectedFolder.getPath(), onPlaySongs);
					return true;

				case ContextMenuItems.ADD_FOLDER_QUEUE:
					folderSongLoader.execute(selectedFolder.getPath(), onAddToQueue);
					return true;

				case ContextMenuItems.HIDE_FOLDER:
					MusicUtils.excludeFolder(requireContext(), selectedFolder);
					MusicUtils.refresh(requireActivity());
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		Folder mFolder = mAdapter.getItem(position);
		Bundle bundle = new Bundle();
		bundle.putLong(ID, -1L);
		bundle.putString(NAME, mFolder.getName());
		bundle.putString(MIME_TYPE, PAGE_FOLDERS);
		bundle.putString(FOLDER, mFolder.getPath());
		Intent intent = new Intent(requireActivity(), ProfileActivity.class);
		intent.putExtras(bundle);
		requireActivity().startActivity(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Folder> folders) {
		if (isAdded()) {
			// Clear list
			mAdapter.clear();
			// add data to the adapter
			for (Folder folder : folders) {
				if (preference.getExcludeTracks() || folder.isVisible()) {
					mAdapter.add(folder);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		if (action.equals(MusicBrowserPhoneFragment.REFRESH)) {
			folderLoader.execute(null, this);
		}
	}

	/**
	 * play loaded songs
	 */
	private void onPlaySongs(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(requireActivity(), ids, 0, false);
	}

	/**
	 * add loaded songs to queue
	 */
	private void onAddToQueue(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.addToQueue(requireActivity(), ids);
	}
}