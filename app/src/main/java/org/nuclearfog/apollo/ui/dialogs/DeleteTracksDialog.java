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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.worker.TrackDeleteWorker;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * Alert dialog used to delete tracks.
 * used for Android versions without scoped storage
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class DeleteTracksDialog extends DialogFragment implements OnClickListener, AsyncCallback<Integer> {

	private static final String TAG = "DeleteTracksDialog";

	/**
	 * key to set the dialog title message
	 * value type is String
	 */
	private static final String KEY_TITLE = "delete_title";

	/**
	 * key to add a long array of track IDs
	 * value type is long[]
	 */
	private static final String KEY_ITEMS = "delete_items";

	private TrackDeleteWorker trackDeleteWorker;

	private long[] mItemList = {};

	/**
	 * @param title The title of the artist, album, or song to delete
	 * @param items The item(s) to delete
	 */
	public static void show(FragmentManager fm, String title, long[] items) {
		DeleteTracksDialog deleteDialog;
		Bundle args = new Bundle();
		Fragment dialog = fm.findFragmentByTag(TAG);

		if (dialog instanceof DeleteTracksDialog) {
			deleteDialog = (DeleteTracksDialog) dialog;
		} else {
			deleteDialog = new DeleteTracksDialog();
		}
		args.putString(KEY_TITLE, title);
		args.putLongArray(KEY_ITEMS, items);
		deleteDialog.setArguments(args);
		deleteDialog.show(fm, TAG);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String title = "";
		if (getArguments() != null) {
			title = getArguments().getString(KEY_TITLE, "");
			long[] mItemList = getArguments().getLongArray(KEY_ITEMS);
			if (mItemList != null) {
				this.mItemList = mItemList;
			}
		}
		String delete = getString(R.string.context_menu_delete);
		// Get the image cache key
		String dialogTitle = getString(R.string.delete_dialog_title, title);
		// Initialize the image cache
		trackDeleteWorker = new TrackDeleteWorker(requireContext());
		// Build the dialog
		return new AlertDialog.Builder(requireContext()).setTitle(dialogTitle)
				.setMessage(R.string.cannot_be_undone)
				.setPositiveButton(delete, this)
				.setNegativeButton(R.string.cancel, this)
				.create();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		trackDeleteWorker.cancel();
		super.onDestroyView();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		// prevent dialog to be dismissed automatically
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			// Delete the selected item(s)
			trackDeleteWorker.execute(ApolloUtils.toLongArray(mItemList), this);
			// prevent dialog to be dismissed after this method
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dismiss();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull Integer count) {
		Activity activity = getActivity();
		if (activity != null) {
			AppMsg.makeText(activity, R.plurals.NNNtracksdeleted, count, AppMsg.STYLE_CONFIRM).show();
			// We deleted a number of tracks, which could affect any number of
			// things in the media content domain, so update everything.
			activity.getContentResolver().notifyChange(Uri.parse("content://media"), null);
			// Notify the lists to update
			MusicUtils.refresh(activity);
			dismiss();
		}
	}
}