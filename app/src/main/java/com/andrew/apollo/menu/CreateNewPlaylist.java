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
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import com.andrew.apollo.R;
import com.andrew.apollo.format.Capitalize;
import com.andrew.apollo.utils.MusicUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com) TODO - The playlist names are
 * automatically capitalized to help when you want to play one via voice
 * actions, but it really needs to work either way. As in, capitalized
 * or not.
 */
public class CreateNewPlaylist extends BasePlaylistDialog {

    // The playlist list
    private long[] mPlaylistList = new long[]{};

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
            makePlaylistName();
        if (mDefaultname == null && getDialog() != null) {
            getDialog().dismiss();
            return;
        }
        String promptformat = getString(R.string.create_playlist_prompt);
        mPrompt = String.format(promptformat, mDefaultname);
    }

    @Override
    public void onSaveClick() {
        String playlistName = mPlaylist.getText().toString();
        if (playlistName.length() > 0) {
            int playlistId = (int) MusicUtils.getIdForPlaylist(requireContext(), playlistName);
            if (playlistId >= 0) {
                MusicUtils.clearPlaylist(requireContext(), playlistId);
                MusicUtils.addToPlaylist(requireActivity(), mPlaylistList, playlistId);
            } else {
                long newId = MusicUtils.createPlaylist(getActivity(),
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

    private void makePlaylistName() {
        String template = getString(R.string.new_playlist_name_template);
        int num = 1;
        String[] projection = new String[]{MediaStore.Audio.Playlists.NAME};
        ContentResolver resolver = requireActivity().getContentResolver();
        String selection = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, projection,
                selection, null, MediaStore.Audio.Playlists.NAME);
        if (cursor == null) {
            return;
        }

        String suggestedname;
        suggestedname = String.format(template, num++);
        boolean done = false;
        while (!done) {
            done = true;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String playlistname = cursor.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
    }
}