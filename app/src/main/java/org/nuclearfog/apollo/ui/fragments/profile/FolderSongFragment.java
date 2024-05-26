package org.nuclearfog.apollo.ui.fragments.profile;

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

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * decompiled from Apollo 1.6 APK
 * <p>
 * This fragment class shows tracks from a music folder
 *
 * @author nuclearfog
 */
public class FolderSongFragment extends ProfileFragment implements AsyncCallback<List<Song>> {

	/**
	 * context menu ID
	 */
	private static final int GROUP_ID = 0x1CABF982;

	/**
	 * list view adapter with song views
	 */
	private ProfileSongAdapter mAdapter;
	private FolderSongLoader mLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Song mSong;
	private String foldername = "";


	@Override
	protected void init(Bundle param) {
		// init loader
		mLoader = new FolderSongLoader(requireContext());
		// init adapter
		mAdapter = new ProfileSongAdapter(requireContext(), ProfileSongAdapter.DISPLAY_DEFAULT_SETTING, false);
		// set adapter
		setAdapter(mAdapter);
		setHasOptionsMenu(true);
		// init loader
		if (param != null) {
			foldername = param.getString("folder_path", "");
			mLoader.execute(foldername, this);
		}
	}


	@Override
	public void onDestroy() {
		mLoader.cancel();
		super.onDestroy();
	}


	@Override
	protected void onItemClick(View v, int position, long id) {
		MusicUtils.playAllFromUserItemClick(requireActivity(), mAdapter, position);
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
			menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
			// play the song
			menu.add(GROUP_ID, ContextMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
			// Add the song to the queue
			menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
			// get more tracks by artist
			menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
			// set selected track as ringtone
			menu.add(GROUP_ID, ContextMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			// delete track
			menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			// Add the song to a playlist
			SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
			MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, true);
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
				case ContextMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(requireActivity(), ids, 0, false);
					return true;

				case ContextMenuItems.PLAY_NEXT:
					MusicUtils.playNext(requireActivity(), ids);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), ids);
					return true;

				case ContextMenuItems.ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireContext()).addFavorite(mSong);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(ids).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					long playlistId = item.getIntent().getLongExtra("playlist", -1L);
					if (playlistId != -1L) {
						MusicUtils.addToPlaylist(requireActivity(), ids, playlistId);
					}
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), ids);
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Song> songs) {
		if (isAdded()) {
			// start fresh
			mAdapter.clear();
			// add items to adapter
			for (Song song : songs) {
				mAdapter.add(song);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refresh() {
		mAdapter.clear();
		mLoader.execute(foldername, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void moveToCurrent() {
		Song song = MusicUtils.getCurrentTrack(requireActivity());
		if (song != null) {
			for (int pos = 0; pos < mAdapter.getCount(); pos++) {
				if (mAdapter.getItemId(pos) == song.getId()) {
					scrollTo(pos);
					break;
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drop(int from, int to) {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int which) {
		// not used
	}
}