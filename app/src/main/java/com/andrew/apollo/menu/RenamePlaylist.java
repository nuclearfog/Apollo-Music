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
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists;

import com.andrew.apollo.R;
import com.andrew.apollo.format.Capitalize;
import com.andrew.apollo.utils.MusicUtils;

/**
 * Alert dialog used to rename playlits.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RenamePlaylist extends BasePlaylistDialog {

    private static final String CONDITION = Playlists._ID + "=?";

    private static final String[] PROJECTION = {Playlists.NAME};

    private long mRenameId;

    /**
     * @param id The Id of the playlist to rename
     * @return A new instance of this dialog.
     */
    public static RenamePlaylist getInstance(Long id) {
        RenamePlaylist frag = new RenamePlaylist();
        Bundle args = new Bundle();
        args.putLong("rename", id);
        frag.setArguments(args);
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
        outcicle.putLong("rename", mRenameId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressLint("StringFormatInvalid")
    public void initObjects(Bundle savedInstanceState) {

        if (savedInstanceState != null)
            mRenameId = savedInstanceState.getLong("rename");
        else if (getArguments() != null)
            mRenameId = getArguments().getLong("rename", -1);
        String mOriginalName = getPlaylistNameFromId(mRenameId);
        mDefaultname = savedInstanceState != null ? savedInstanceState.getString("defaultname") : mOriginalName;
        if ((mRenameId < 0 || mOriginalName == null || mDefaultname == null) && getDialog() != null) {
            getDialog().dismiss();
            return;
        }
        String promptformat = getString(R.string.create_playlist_prompt);
        mPrompt = String.format(promptformat, mOriginalName, mDefaultname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveClick() {
        String playlistName = mPlaylist.getText().toString();
        if (!playlistName.isEmpty()) {
            // seting new name
            ContentValues values = new ContentValues(1);
            values.put(Playlists.NAME, Capitalize.capitalize(playlistName));
            // update old playlist
            Uri uri = ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, mRenameId);
            ContentResolver resolver = requireActivity().getContentResolver();
            resolver.update(uri, values, null, null);
            // close keyboard after dialog end
            closeKeyboard();
            if (getDialog() != null) {
                getDialog().dismiss();
            }
        }
    }

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
     * @param id The Id of the playlist
     * @return The name of the playlist
     */
    private String getPlaylistNameFromId(long id) {
        String[] param = {String.valueOf(id)};
        ContentResolver resolver = requireActivity().getContentResolver();
        Cursor cursor = resolver.query(Playlists.EXTERNAL_CONTENT_URI, PROJECTION, CONDITION, param, Playlists.NAME);

        String playlistName = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                playlistName = cursor.getString(0);
            }
            cursor.close();
        }
        return playlistName;
    }
}