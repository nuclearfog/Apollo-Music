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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;

import com.andrew.apollo.R;
import com.andrew.apollo.format.Capitalize;
import com.andrew.apollo.utils.CursorFactory;
import com.andrew.apollo.utils.MusicUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com) TODO - The playlist names are
 * automatically capitalized to help when you want to play one via voice
 * actions, but it really needs to work either way. As in, capitalized
 * or not.
 */
public class CreateNewPlaylist extends BasePlaylistDialog {

    // The playlist list
    private long[] mPlaylistList = {};

    /**
     * @param list The list of tracks to add to the playlist
     * @return A new instance of this dialog.
     */
    public static CreateNewPlaylist getInstance(long[] list) {
        CreateNewPlaylist frag = new CreateNewPlaylist();
        Bundle args = new Bundle();
        args.putLongArray("playlist_list", list);
        frag.setArguments(args);
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressLint("StringFormatInvalid")
    @Override
    public void initObjects(Bundle savedInstanceState) {
        if (getArguments() != null)
            mPlaylistList = getArguments().getLongArray("playlist_list");
        if (savedInstanceState != null)
            mDefaultname = savedInstanceState.getString("defaultname");
        else
            mDefaultname = makePlaylistName();
        if (mDefaultname == null && getDialog() != null) {
            getDialog().dismiss();
            return;
        }
        String promptformat = getString(R.string.create_playlist_prompt);
        mPrompt = String.format(promptformat, mDefaultname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveClick() {
        String playlistName = mPlaylist.getText().toString();
        if (playlistName.length() > 0) {
            int playlistId = (int) MusicUtils.getIdForPlaylist(requireContext(), playlistName);
            if (playlistId >= 0) {
                MusicUtils.clearPlaylist(requireContext(), playlistId);
                MusicUtils.addToPlaylist(requireActivity(), mPlaylistList, playlistId);
            } else {
                long newId = MusicUtils.createPlaylist(requireContext(),
                        Capitalize.capitalize(playlistName));
                MusicUtils.addToPlaylist(requireActivity(), mPlaylistList, newId);
            }
            closeKeyboard();
            if (getDialog() != null) {
                getDialog().dismiss();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChangedListener() {
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