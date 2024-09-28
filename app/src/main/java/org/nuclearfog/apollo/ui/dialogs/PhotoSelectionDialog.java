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
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.utils.ApolloUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used when the user touches the image in the header in {@link ProfileActivity}
 * . It provides an easy interface for them to choose a new image, use the old
 * image, or search Google for one.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PhotoSelectionDialog extends DialogFragment implements OnClickListener {

	public static final String TAG = "PhotoSelectionDialog";

	public static final int ARTIST = 10;
	public static final int ALBUM = 11;
	public static final int OTHER = 12;

	private static final String KEY_TITLE = "photo_title";
	private static final String KEY_TYPE = "photo_type";

	private static final int IDX_NEW = 0;
	private static final int IDX_OLD = 1;
	private static final int IDX_SEARCH = 2;
	private static final int IDX_FETCH = 3;

	private List<String> mChoices = new ArrayList<>(5);


	/**
	 * @param title The dialog title.
	 */
	public static void show(FragmentManager fm, String title, int type) {
		Bundle args = new Bundle();
		PhotoSelectionDialog photoSelectionDialog;
		Fragment dialog = fm.findFragmentByTag(TAG);

		if (dialog instanceof PhotoSelectionDialog) {
			photoSelectionDialog = (PhotoSelectionDialog) dialog;
		} else {
			photoSelectionDialog = new PhotoSelectionDialog();
		}
		args.putInt(KEY_TYPE, type);
		args.putString(KEY_TITLE, title);
		photoSelectionDialog.setArguments(args);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		int type = OTHER;
		String title = "";
		if (savedInstanceState == null) {
			savedInstanceState = getArguments();
		}
		if (savedInstanceState != null) {
			type = savedInstanceState.getInt(KEY_TYPE, OTHER);
			title = savedInstanceState.getString(KEY_TITLE, "");
		}
		switch (type) {
			case ARTIST:
				// Select a photo from the gallery
				mChoices.add(IDX_NEW, getString(R.string.new_photo));
				if (ApolloUtils.isOnline(requireContext())) {
					// Option to fetch the old artist image
					mChoices.add(IDX_OLD, getString(R.string.context_menu_fetch_artist_image));
					// Search Google for the artist name
					mChoices.add(IDX_SEARCH, getString(R.string.web_search));
				}
				break;

			case ALBUM:
				// Select a photo from the gallery
				mChoices.add(IDX_NEW, getString(R.string.new_photo));
				// Option to fetch the old album image
				mChoices.add(IDX_OLD, getString(R.string.old_photo));
				if (ApolloUtils.isOnline(requireContext())) {
					// Search Google for the album name
					mChoices.add(IDX_SEARCH, getString(R.string.web_search));
					// Option to fetch the album image
					mChoices.add(IDX_FETCH, getString(R.string.context_menu_fetch_album_art));
				}
				break;

			default:
			case OTHER:
				// Select a photo from the gallery
				mChoices.add(IDX_NEW, getString(R.string.new_photo));
				// Option to use the default image
				mChoices.add(IDX_OLD, getString(R.string.use_default));
				break;
		}
		ListAdapter adapter = new ArrayAdapter<>(requireContext(), android.R.layout.select_dialog_item, mChoices);
		return new AlertDialog.Builder(requireContext()).setTitle(title).setAdapter(adapter, this).create();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		ProfileActivity activity = (ProfileActivity) requireActivity();
		switch (which) {
			case IDX_NEW:
				activity.selectNewPhoto();
				break;
			case IDX_OLD:
				activity.selectOldPhoto();
				break;
			case IDX_FETCH:
				activity.fetchAlbumArt();
				break;
			case IDX_SEARCH:
				activity.searchWeb();
				break;
		}
	}
}