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

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * Playlist dialog used to create, copy of rename a playlist
 *
 * @author nuclearfog
 */
public class PlaylistDialog extends DialogFragment implements TextWatcher, OnClickListener {

	private static final String NAME = "PlaylistDialog";

	/**
	 * mode used to create a new playlist
	 */
	public static final int CREATE = 14;

	/**
	 * mode used to copy a playlist
	 */
	public static final int COPY = 15;

	/**
	 * mode used to rename a playlist
	 */
	public static final int MOVE = 16;

	/**
	 * key to define what action should be performed {@link #COPY,#MOVE,#CREATE}
	 * value type is int
	 */
	private static final String PLAYLIST_MODE = "playlist_mode";

	/**
	 * key to set playlist name
	 * value type is String
	 */
	private static final String PLAYLIST_NAME = "playlist_name";

	/**
	 * key to set an ID of an existing playlist
	 * value type is long
	 */
	private static final String PLAYLIST_ID = "playlist_id";

	/**
	 * key used to set song IDs to insert in the playlist
	 * value type is long[]
	 */
	private static final String PLAYLIST_SONGS = "playlist_songs";
	/**
	 * Used to make new playlist names
	 */
	private EditText playlistName;
	/**
	 * The dialog save button
	 */
	private Button mSaveButton;

	private long playlistId;

	private int mode;

	private long[] songIds = {};

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Initialize the alert dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		// Initialize the edit text
		int padding = getResources().getDimensionPixelSize(R.dimen.list_preferred_item_padding);
		playlistName = new EditText(requireContext());
		playlistName.setLines(1);
		playlistName.setBackgroundColor(0);
		playlistName.setHint(R.string.create_playlist_prompt);
		playlistName.setPadding(padding, padding, padding, padding);
		playlistName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		// set dialog view
		builder.setView(playlistName);
		// Set the save button action
		builder.setPositiveButton(getString(R.string.save), this);
		// Set the cancel button action
		builder.setNegativeButton(getString(R.string.cancel), this);

		if (savedInstanceState == null) {
			savedInstanceState = getArguments();
		}
		if (savedInstanceState != null) {
			playlistName.append(savedInstanceState.getString(PLAYLIST_NAME, ""));
			mode = savedInstanceState.getInt(PLAYLIST_MODE);
			playlistId = savedInstanceState.getLong(PLAYLIST_ID, -1);
			if (savedInstanceState.containsKey(PLAYLIST_SONGS))
				songIds = savedInstanceState.getLongArray(PLAYLIST_SONGS);
			switch (mode) {
				case CREATE:
					builder.setTitle(R.string.new_playlist);
					break;

				case MOVE:
					builder.setTitle(R.string.rename_playlist);
					break;

				case COPY:
					builder.setTitle(R.string.copy_playlist);
					break;
			}
		}
		AlertDialog dialog = builder.show();
		// confirm button
		mSaveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
		mSaveButton.setEnabled(false);
		playlistName.addTextChangedListener(this);
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(DialogInterface dialog, int which) {
		if (which == Dialog.BUTTON_POSITIVE) {
			String name = playlistName.getText().toString();
			if (!name.trim().isEmpty()) {
				switch (mode) {
					case MOVE:
						MusicUtils.renamePlaylist(requireActivity(), playlistId, name);
						break;

					case COPY:
						songIds = MusicUtils.getSongListForPlaylist(requireContext(), playlistId);
						// fall through

					case CREATE:
						// create new playlist
						playlistId = MusicUtils.createPlaylist(requireActivity(), name);
						if (playlistId != -1) {
							MusicUtils.addToPlaylist(requireActivity(), songIds, playlistId);
						} else {
							AppMsg.makeText(requireActivity(), R.string.error_duplicate_playlistname, AppMsg.STYLE_ALERT).show();
						}
						break;
				}
				MusicUtils.refresh(getActivity());
				dismiss();
			}
		} else if (which == Dialog.BUTTON_NEGATIVE) {
			InputMethodManager iManager = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
			if (iManager != null) {
				iManager.hideSoftInputFromWindow(playlistName.getWindowToken(), 0);
			}
			MusicUtils.refresh(getActivity());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onTextChanged(CharSequence s, int start, int before, int count) {
		mSaveButton.setEnabled(!s.toString().trim().isEmpty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void afterTextChanged(Editable s) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	/**
	 * shows a dialog window
	 *
	 * @param mode    what action should be performed {@link #COPY,#MOVE,#CREATE}
	 * @param id      ID of an existing playlist to modify used to copy or move
	 * @param songIds list of song IDs to add to the playlist
	 * @param name    new name of the playlist
	 */
	public static void show(FragmentManager fm, int mode, long id, long[] songIds, String name) {
		Fragment dialogFragment = fm.findFragmentByTag(NAME);
		if (dialogFragment == null) {
			PlaylistDialog dialog = new PlaylistDialog();
			Bundle param = new Bundle();
			param.putInt(PLAYLIST_MODE, mode);
			param.putLong(PLAYLIST_ID, id);
			param.putLongArray(PLAYLIST_SONGS, songIds);
			param.putString(PLAYLIST_NAME, name);
			dialog.setArguments(param);
			dialog.show(fm, NAME);
		}
	}
}