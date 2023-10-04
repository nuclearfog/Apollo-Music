package org.nuclearfog.apollo.ui.fragments.profile;

import static org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter.DISPLAY_DEFAULT_SETTING;
import static org.nuclearfog.apollo.utils.ContextMenuItems.ADD_TO_FAVORITES;
import static org.nuclearfog.apollo.utils.ContextMenuItems.ADD_TO_PLAYLIST;
import static org.nuclearfog.apollo.utils.ContextMenuItems.ADD_TO_QUEUE;
import static org.nuclearfog.apollo.utils.ContextMenuItems.DELETE;
import static org.nuclearfog.apollo.utils.ContextMenuItems.MORE_BY_ARTIST;
import static org.nuclearfog.apollo.utils.ContextMenuItems.NEW_PLAYLIST;
import static org.nuclearfog.apollo.utils.ContextMenuItems.PLAYLIST_SELECTED;
import static org.nuclearfog.apollo.utils.ContextMenuItems.PLAY_NEXT;
import static org.nuclearfog.apollo.utils.ContextMenuItems.PLAY_SELECTION;
import static org.nuclearfog.apollo.utils.ContextMenuItems.USE_AS_RINGTONE;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.loaders.FolderSongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.provider.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * decompiled from Apollo 1.6 APK
 * <p>
 * This fragment class shows tracks from a music folder
 */
public class FolderSongFragment extends ProfileFragment implements LoaderCallbacks<List<Song>> {

	/**
	 *
	 */
	public static final String REFRESH = "FolderSongFragment.refresh";

	/**
	 * context menu ID
	 */
	private static final int GROUP_ID = 0x1CABF982;

	/**
	 * ID for the loader
	 */
	private static final int LOADER_ID = 0x16A4BF2B;

	/**
	 * list view adapter with song views
	 */
	private ProfileSongAdapter mAdapter;

	/**
	 * track selected from contextmenu
	 */
	@Nullable
	private Song mSong;


	@Override
	protected void init() {
		// init adapter
		mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_DEFAULT_SETTING, false);
		setAdapter(mAdapter);
		setHasOptionsMenu(true);
		Bundle param = getArguments();
		if (param != null) {
			LoaderManager.getInstance(this).initLoader(LOADER_ID, param, this);
		}
	}


	@Override
	protected void onItemClick(View v, int position, long id) {
		MusicUtils.playAllFromUserItemClick(requireContext(), mAdapter, position);
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
			mSong = mAdapter.getItem(adapterInfo.position);
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
		} else {
			// remove selection if an error occurs
			mSong = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && mSong != null) {
			long[] ids = {mSong.getId()};

			switch (item.getItemId()) {
				default:
					return super.onContextItemSelected(item);

				case PLAY_SELECTION:
					MusicUtils.playAll(requireContext(), ids, 0, false);
					return true;

				case PLAY_NEXT:
					MusicUtils.playNext(ids);
					return true;

				case ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), ids);
					return true;

				case ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireContext()).addSongId(mSong);
					return true;

				case NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(ids).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case PLAYLIST_SELECTED:
					long playlistId = item.getIntent().getLongExtra("playlist", 0L);
					MusicUtils.addToPlaylist(requireActivity(), ids, playlistId);
					return true;

				case MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case DELETE:
					break;
			}
			MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), ids);
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
	public void onChanged(String action) {
		if (action.equals(REFRESH)) {
			// Scroll to the stop of the list before restarting the loader.
			// Otherwise, if the user has scrolled enough to move the header, it
			// becomes misplaced and needs to be reset.
			scrollToTop();
			LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), this);
		}
	}


	@Override
	public void drop(int from, int to) {
	}


	@Override
	public void remove(int which) {
	}
}