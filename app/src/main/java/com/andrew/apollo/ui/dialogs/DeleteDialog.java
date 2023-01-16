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

package com.andrew.apollo.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

/**
 * Alert dialog used to delete tracks.
 * <p>
 * TODO: Remove albums from the recents list upon deletion.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class DeleteDialog extends DialogFragment implements OnClickListener {

	public static final String NAME = "DeleteDialog";

	/**
	 * The item(s) to delete
	 */
	private long[] mItemList;
	/**
	 * The image cache
	 */
	private ImageFetcher mFetcher;

	private String key = "";
	private String title = "";

	/**
	 * Empty constructor as per the {@link androidx.fragment.app.Fragment} documentation
	 */
	public DeleteDialog(String title, String key, long[] ids) {
		if (key != null)
			this.key = key;
		if (title != null)
			this.title = title;
		mItemList = ids;
	}

	/**
	 * @param title The title of the artist, album, or song to delete
	 * @param items The item(s) to delete
	 * @param key   The key used to remove items from the cache.
	 * @return A new instance of the dialog
	 */
	public static DeleteDialog newInstance(String title, long[] items, String key) {
		return new DeleteDialog(title, key, items);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
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
			if (getActivity() instanceof DeleteDialogCallback) {
				((DeleteDialogCallback) getActivity()).onDelete();
			}
			dialog.dismiss();
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dialog.dismiss();
		}
	}

	/**
	 *
	 */
	public interface DeleteDialogCallback {
		void onDelete();
	}
}