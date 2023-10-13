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

package org.nuclearfog.apollo.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.CursorFactory;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com) TODO - The playlist names are
 * automatically capitalized to help when you want to play one via voice
 * actions, but it really needs to work either way. As in, capitalized
 * or not.
 */
public class PlaylistCreateDialog extends BasePlaylistDialog {

	public static final String NAME = "CreatePlaylist";

	private static final String KEY_LIST = "playlist_list";

	private static final String KEY_DEFAULT_NAME = "defaultname";

	// The playlist list
	private long[] mPlaylistList = {};

	/**
	 * @param list The list of tracks to add to the playlist
	 * @return A new instance of this dialog.
	 */
	public static PlaylistCreateDialog getInstance(long[] list) {
		PlaylistCreateDialog frag = new PlaylistCreateDialog();
		Bundle args = new Bundle();
		args.putLongArray(KEY_LIST, list);
		frag.setArguments(args);
		return frag;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSaveInstanceState(Bundle outcicle) {
		outcicle.putString(KEY_DEFAULT_NAME, mPlaylist.getText().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("StringFormatInvalid")
	@Override
	protected void initObjects(Bundle savedInstanceState) {
		if (getArguments() != null) {
			long[] mPlaylistList = getArguments().getLongArray(KEY_LIST);
			if (mPlaylistList != null) {
				this.mPlaylistList = mPlaylistList;
			}
		}
		if (savedInstanceState != null) {
			mDefaultname = savedInstanceState.getString(KEY_DEFAULT_NAME);
		} else {
			mDefaultname = makePlaylistName();
		}
		if (mDefaultname == null && getDialog() != null) {
			getDialog().dismiss();
		} else {
			String promptformat = getString(R.string.create_playlist_prompt);
			mPrompt = String.format(promptformat, mDefaultname);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return
	 */
	@Override
	protected void onSaveClick() {
		if (mPlaylist.length() > 0) {
			String playlistName = mPlaylist.getText().toString();
			long playlistId = MusicUtils.getIdForPlaylist(requireContext(), playlistName);
			if (playlistId != -1L) {
				// save to existing playlist
				// fixme scoped storage does not allow modifying foreign playlists
				MusicUtils.clearPlaylist(requireContext(), playlistId);
				MusicUtils.addToPlaylist(requireActivity(), mPlaylistList, playlistId);
			} else {
				// create new playlist
				long newId = MusicUtils.createPlaylist(requireContext(), StringUtils.capitalize(playlistName));
				MusicUtils.addToPlaylist(requireActivity(), mPlaylistList, newId);
			}
		} else {
			Toast.makeText(requireContext(), R.string.error_empty_playlistname, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onTextChangedListener() {
		String playlistName = mPlaylist.getText().toString();
		mSaveButton = mPlaylistDialog.getButton(Dialog.BUTTON_POSITIVE);
		if (mSaveButton == null) {
			return;
		}
		if (playlistName.trim().length() == 0) {
			mSaveButton.setEnabled(false);
		} else {
			mSaveButton.setEnabled(true);
			if (MusicUtils.getIdForPlaylist(requireContext(), playlistName) >= 0) {
				mSaveButton.setText(R.string.overwrite);
			} else {
				mSaveButton.setText(R.string.save);
			}
		}
	}

	/**
	 * generate default playlist name without conflicting existing names
	 *
	 * @return generated playlist name
	 */
	private String makePlaylistName() {
		Cursor cursor = CursorFactory.makePlaylistCursor(requireContext());
		if (cursor != null) {
			// get all available playlist names
			String[] playlists = new String[cursor.getCount()];
			cursor.moveToFirst();
			for (int i = 0; i < playlists.length && !cursor.isAfterLast(); i++) {
				playlists[i] = cursor.getString(1);
				cursor.moveToNext();
			}
			cursor.close();
			// search for conflicts and increase number suffix
			int num = 1;
			boolean conflict;
			String suggestedname;
			String template = getString(R.string.new_playlist_name_template);
			do {
				conflict = false;
				suggestedname = String.format(template, num++);
				for (String playlist : playlists) {
					if (suggestedname.equals(playlist)) {
						conflict = true;
						break;
					}
				}
			} while (conflict);
			return suggestedname;
		}
		return "";
	}
}