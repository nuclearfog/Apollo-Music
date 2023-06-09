package org.nuclearfog.apollo.ui.dialogs;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * Dialog to copy an existing playlist
 *
 * @author nuclearfog
 */
public class PlaylistCopyDialog extends BasePlaylistDialog {

	public static final String NAME = "PlaylistCopyDialog";

	private static final String KEY_ID = "copy-id";

	private static final String KEY_DEFAULT_NAME = "defaultname";

	/**
	 * ID of the playlist to copy
	 */
	private long copyId;


	public static PlaylistCopyDialog getInstance(long id) {
		PlaylistCopyDialog instance = new PlaylistCopyDialog();
		Bundle args = new Bundle();
		args.putLong(KEY_ID, id);
		instance.setArguments(args);
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSaveInstanceState(Bundle outcicle) {
		outcicle.putString(KEY_DEFAULT_NAME, mPlaylist.getText().toString());
		outcicle.putLong(KEY_ID, copyId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressLint("StringFormatInvalid")
	public void initObjects(Bundle savedInstanceState) {
		// get ID of the playlist to copy
		if (savedInstanceState != null) {
			copyId = savedInstanceState.getLong(KEY_ID);
		} else if (getArguments() != null) {
			copyId = getArguments().getLong(KEY_ID);
		}
		// get playlist name
		String mOriginalName = getPlaylistNameFromId(copyId);
		if (savedInstanceState != null) {
			mDefaultname = savedInstanceState.getString(KEY_DEFAULT_NAME);
		} else {
			mDefaultname = mOriginalName;
		}
		// check for valid information
		if (copyId >= 0 && mOriginalName != null && mDefaultname != null) {
			String promptformat = getString(R.string.create_playlist_prompt);
			mPrompt = String.format(promptformat, mOriginalName, mDefaultname);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSaveClick() {
		String playlistName = mPlaylist.getText().toString();
		long id = MusicUtils.getIdForPlaylist(requireContext(), playlistName);
		if (playlistName.isEmpty()) {
			Toast.makeText(requireContext(), R.string.error_empty_playlistname, Toast.LENGTH_SHORT).show();
		} else if (id >= 0) {
			Toast.makeText(requireContext(), R.string.error_duplicate_playlistname, Toast.LENGTH_SHORT).show();
		} else {
			long newId = MusicUtils.createPlaylist(requireContext(), StringUtils.capitalize(playlistName));
			long[] songIds = MusicUtils.getSongListForPlaylist(requireContext(), copyId);
			MusicUtils.addToPlaylist(requireActivity(), songIds, newId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTextChangedListener() {
	}
}