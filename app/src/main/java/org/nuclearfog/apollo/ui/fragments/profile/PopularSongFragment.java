package org.nuclearfog.apollo.ui.fragments.profile;

import static org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter.DISPLAY_PLAYLIST_SETTING;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.PopularSongsLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.ui.adapters.listview.ProfileSongAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistCreateDialog;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * This fragment class shows the most playes tracks
 *
 * @author nuclearfog
 */
public class PopularSongFragment extends ProfileFragment implements AsyncCallback<List<Song>> {

	/**
	 * context menu ID
	 */
	private static final int GROUP_ID = 0xC46D92C;

	/**
	 * The adapter for the list
	 */
	private ProfileSongAdapter mAdapter;

	private PopularSongsLoader mLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Song mSong;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void init(Bundle param) {
		mLoader = new PopularSongsLoader(requireContext());
		// sets empty list text
		mAdapter = new ProfileSongAdapter(requireContext(), DISPLAY_PLAYLIST_SETTING, false);
		setAdapter(mAdapter);
		setEmptyText(R.string.empty_recents);
		// start loader
		mLoader.execute(null, this);
	}


	@Override
	public void onDestroy() {
		mLoader.cancel();
		super.onDestroy();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onItemClick(View v, int pos, long id) {
		// play all tracks
		MusicUtils.playAllFromUserItemClick(requireContext(), mAdapter, pos);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
			// Get the position of the selected item
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			// Creat a new song
			mSong = mAdapter.getItem(info.position);
			// Play the song
			menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
			// Play next
			menu.add(GROUP_ID, ContextMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
			// Add the song to the queue
			menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
			// Add option to remove track from popular
			menu.add(GROUP_ID, ContextMenuItems.REMOVE_FROM_POPULAR, Menu.NONE, R.string.remove_from_popular);
			// Add the song to a playlist
			SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
			MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
			// View more content by the song artist
			menu.add(GROUP_ID, ContextMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
			// Make the song a ringtone
			menu.add(GROUP_ID, ContextMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			// Delete the song
			menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
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
			long[] trackId = {mSong.getId()};
			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(requireContext(), trackId, 0, false);
					return true;

				case ContextMenuItems.PLAY_NEXT:
					MusicUtils.playNext(trackId);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), trackId);
					return true;

				case ContextMenuItems.ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireContext()).addItem(mSong);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					PlaylistCreateDialog.getInstance(trackId).show(getParentFragmentManager(), PlaylistCreateDialog.NAME);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					long mPlaylistId = item.getIntent().getLongExtra("playlist", -1L);
					MusicUtils.addToPlaylist(requireActivity(), trackId, mPlaylistId);
					return true;

				case ContextMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case ContextMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case ContextMenuItems.REMOVE_FROM_POPULAR:
					PopularStore.getInstance(requireContext()).removeItem(mSong.getId());
					mAdapter.remove(mSong);
					return true;

				case ContextMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackId);
					mLoader.execute(null, this);
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void moveToCurrent() {
		Song song = MusicUtils.getCurrentTrack();
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
	protected void refresh() {
		mAdapter.clear();
		mLoader.execute(null, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Song> songs) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adpater
			for (Song song : songs) {
				mAdapter.add(song);
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