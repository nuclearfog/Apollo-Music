package org.nuclearfog.apollo.ui.dialogs;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Playlist;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * @author nuclearfog
 */
public class DeletePlaylistDialog extends DialogFragment implements OnClickListener {

	private static final String KEY_PLAYLIST = "playlist";

	private Playlist playlist;

	/**
	 *
	 */
	public DeletePlaylistDialog() {
	}

	/**
	 * @return A new instance of the dialog
	 */
	public static DeletePlaylistDialog newInstance(Playlist playlist) {
		DeletePlaylistDialog dialog = new DeletePlaylistDialog();
		Bundle args = new Bundle();
		args.putSerializable(KEY_PLAYLIST, playlist);
		dialog.setArguments(args);
		return dialog;
	}


	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String name = "";
		if (getArguments() != null) {
			playlist = (Playlist) getArguments().getSerializable(KEY_PLAYLIST);
			if (playlist != null) {
				name = playlist.getName();
			}
		}
		return new AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.delete_dialog_title, name))
				.setPositiveButton(R.string.context_menu_delete, this).setNegativeButton(R.string.cancel, this)
				.setMessage(R.string.cannot_be_undone).show();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (playlist != null) {
				Uri mUri = ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, playlist.getId());
				int deleted = requireActivity().getContentResolver().delete(mUri, null, null);
				if (deleted > 0) {
					AppMsg.makeText(requireActivity(), R.string.info_removed_playlist, AppMsg.STYLE_CONFIRM).show();
					MusicUtils.refresh(requireActivity());
				} else {
					AppMsg.makeText(requireActivity(), R.string.error_delete_playlist, AppMsg.STYLE_ALERT).show();
				}
			}
			dialog.dismiss();
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dialog.dismiss();
		}
	}
}
