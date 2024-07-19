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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * Alert dialog used to delete tracks.
 * <p>
 * TODO: Remove albums from the recents list upon deletion.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class DeleteTracksDialog extends DialogFragment implements OnClickListener {

	public static final String NAME = "DeleteDialog";

	private static final String KEY_TITLE = NAME + "_title";

	private static final String KEY_IMAGEKEY = NAME + "_key";

	private static final String KEY_ITEMS = NAME + "_items";

	/**
	 * The item(s) to delete
	 */
	private long[] mItemList = {};
	/**
	 * The image cache
	 */
	private ImageFetcher mFetcher;

	private String key = "";
	private String title = "";

	/**
	 *
	 */
	public DeleteTracksDialog() {
	}

	/**
	 * @param title The title of the artist, album, or song to delete
	 * @param items The item(s) to delete
	 * @param key   The key used to remove items from the cache.
	 * @return A new instance of the dialog
	 */
	public static DeleteTracksDialog newInstance(String title, long[] items, String key) {
		DeleteTracksDialog dialog = new DeleteTracksDialog();
		Bundle args = new Bundle();
		args.putString(KEY_TITLE, title);
		args.putString(KEY_IMAGEKEY, key);
		args.putLongArray(KEY_ITEMS, items);
		dialog.setArguments(args);
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (getArguments() != null) {
			title = getArguments().getString(KEY_TITLE, "");
			key = getArguments().getString(KEY_IMAGEKEY, "");
			long[] mItemList = getArguments().getLongArray(KEY_ITEMS);
			if (mItemList != null) {
				this.mItemList = mItemList;
			}
		}
		String delete = getString(R.string.context_menu_delete);
		// Get the image cache key
		String dialogTitle = getString(R.string.delete_dialog_title, title);
		// Initialize the image cache
		mFetcher = ApolloUtils.getImageFetcher(requireActivity());
		// Build the dialog
		return new AlertDialog.Builder(requireContext()).setTitle(dialogTitle)
				.setMessage(R.string.cannot_be_undone)
				.setPositiveButton(delete, this)
				.setNegativeButton(R.string.cancel, this)
				.create();
	}


	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			// Remove the items from the image cache
			mFetcher.removeFromCache(key);
			// Delete the selected item(s)
			MusicUtils.deleteTracks(requireActivity(), mItemList);
			dialog.dismiss();
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dialog.dismiss();
		}
	}
}