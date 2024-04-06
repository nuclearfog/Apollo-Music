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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists;
import android.widget.Toast;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * Alert dialog used to rename playlits.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistRenameDialog extends BasePlaylistDialog {

	public static final String NAME = "PlaylistRenameDialog";

	private static final String KEY_ID = NAME + "_rename_id";

	private static final String KEY_DEFAULT_NAME = NAME + "_default_name";

	/**
	 * ID of the playlist to rename
	 */
	private long mRenameId;

	/**
	 *
	 */
	public PlaylistRenameDialog() {
	}

	/**
	 * @param id The Id of the playlist to rename
	 * @return A new instance of this dialog.
	 */
	public static PlaylistRenameDialog getInstance(long id) {
		PlaylistRenameDialog frag = new PlaylistRenameDialog();
		Bundle args = new Bundle();
		args.putLong(KEY_ID, id);
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
	@Override
	@SuppressLint("StringFormatInvalid")
	protected void initObjects(Bundle savedInstanceState) {
		String mOriginalName = null;
		if (getArguments() != null) {
			mRenameId = getArguments().getLong(KEY_ID);
			mOriginalName = getPlaylistNameFromId(mRenameId);
		}
		if (savedInstanceState != null) {
			mDefaultname = savedInstanceState.getString(KEY_DEFAULT_NAME);
		} else {
			mDefaultname = mOriginalName;
		}
		// check for valid information
		if (mRenameId >= 0 && mOriginalName != null && mDefaultname != null) {
			String promptformat = getString(R.string.create_playlist_prompt);
			mPrompt = String.format(promptformat, mOriginalName, mDefaultname);
		} else if (getDialog() != null) {
			getDialog().dismiss();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveClick() {
		String playlistName = mPlaylist.getText().toString();
		long id = MusicUtils.getIdForPlaylist(requireContext(), playlistName);
		if (playlistName.isEmpty()) {
			Toast.makeText(requireContext(), R.string.error_empty_playlistname, Toast.LENGTH_SHORT).show();
		} else if (id >= 0) {
			Toast.makeText(requireContext(), R.string.error_duplicate_playlistname, Toast.LENGTH_SHORT).show();
		} else {
			// seting new name
			ContentValues values = new ContentValues(1);
			values.put(Playlists.NAME, StringUtils.capitalize(playlistName));
			// update old playlist
			Uri uri = ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, mRenameId);
			ContentResolver resolver = requireActivity().getContentResolver();
			resolver.update(uri, values, null, null);
		}
	}


	@Override
	protected void onTextChangedListener() {
		String playlistName = mPlaylist.getText().toString();
		if (mSaveButton == null) {
			return;
		}
		if (playlistName.trim().isEmpty()) {
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
}