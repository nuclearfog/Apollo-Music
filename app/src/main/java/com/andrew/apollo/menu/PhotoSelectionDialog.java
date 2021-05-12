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

package com.andrew.apollo.menu;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.ApolloUtils;

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

    private static final int IDX_NEW = 0;

    private static final int IDX_OLD = 1;

    private static final int IDX_SEARCH = 2;

    private static final int IDX_FETCH = 3;

    private static ProfileType mProfileType;

    private List<String> mChoices = new ArrayList<>(5);

    private PhotoSelectionDialog() {
    }

    /**
     * @param title The dialog title.
     * @return A new instance of the dialog.
     */
    public static PhotoSelectionDialog newInstance(String title, ProfileType type) {
        PhotoSelectionDialog frag = new PhotoSelectionDialog();
        Bundle args = new Bundle();
        args.putString(Config.NAME, title);
        frag.setArguments(args);
        mProfileType = type;
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        switch (mProfileType) {
            case ARTIST:
                // Select a photo from the gallery
                mChoices.add(IDX_NEW, getString(R.string.new_photo));
                if (ApolloUtils.isOnline(requireContext())) {
                    // Option to fetch the old artist image
                    mChoices.add(IDX_OLD, getString(R.string.context_menu_fetch_artist_image));
                    // Search Google for the artist name
                    mChoices.add(IDX_SEARCH, getString(R.string.google_search));
                }
                break;

            case ALBUM:
                // Select a photo from the gallery
                mChoices.add(IDX_NEW, getString(R.string.new_photo));
                // Option to fetch the old album image
                mChoices.add(IDX_OLD, getString(R.string.old_photo));
                if (ApolloUtils.isOnline(requireContext())) {
                    // Search Google for the album name
                    mChoices.add(IDX_SEARCH, getString(R.string.google_search));
                    // Option to fetch the album image
                    mChoices.add(IDX_FETCH, getString(R.string.context_menu_fetch_album_art));
                }
                break;

            case OTHER:
                // Select a photo from the gallery
                mChoices.add(IDX_NEW, getString(R.string.new_photo));
                // Option to use the default image
                mChoices.add(IDX_OLD, getString(R.string.use_default));
                break;
        }
        // Dialog item Adapter
        String title = getArguments() != null ? getArguments().getString(Config.NAME) : "";
        ListAdapter adapter = new ArrayAdapter<>(requireContext(), android.R.layout.select_dialog_item, mChoices);
        return new AlertDialog.Builder(requireContext()).setTitle(title).setAdapter(adapter, this).create();
    }


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
                activity.googleSearch();
                break;
        }
    }

    /**
     * Easily detect the MIME type
     */
    public enum ProfileType {
        ARTIST, ALBUM, OTHER
    }
}