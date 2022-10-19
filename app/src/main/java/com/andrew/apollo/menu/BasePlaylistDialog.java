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

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

/**
 * A simple base class for the playlist dialogs.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BasePlaylistDialog extends DialogFragment implements TextWatcher, OnClickListener {

	/**
	 * The actual dialog
	 */
	protected AlertDialog mPlaylistDialog;
	/**
	 * Used to make new playlist names
	 */
	protected EditText mPlaylist;
	/**
	 * The dialog save button
	 */
	protected Button mSaveButton;
	/**
	 * The dialog prompt
	 */
	protected String mPrompt = "";
	/**
	 * The default edit text text
	 */
	protected String mDefaultname = "";

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Initialize the alert dialog
		mPlaylistDialog = new AlertDialog.Builder(requireContext()).create();
		// Initialize the edit text
		mPlaylist = new EditText(requireContext());
		// To show the "done" button on the soft keyboard
		mPlaylist.setSingleLine(true);
		// All caps
		mPlaylist.setInputType(mPlaylist.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		// Set the save button action
		mPlaylistDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.save), this);
		// Set the cancel button action
		mPlaylistDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), this);

		initObjects(savedInstanceState);
		mPlaylistDialog.setTitle(mPrompt);
		mPlaylistDialog.setView(mPlaylist);
		mPlaylist.setText(mDefaultname);
		mPlaylist.setSelection(mDefaultname.length());
		mPlaylist.addTextChangedListener(this);
		mPlaylistDialog.show();
		return mPlaylistDialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(DialogInterface dialog, int which) {
		if (dialog == mPlaylistDialog) {
			if (which == Dialog.BUTTON_POSITIVE) {
				onSaveClick();
				MusicUtils.refresh();
				dialog.dismiss();
			} else if (which == Dialog.BUTTON_NEGATIVE) {
				closeKeyboard();
				MusicUtils.refresh();
				dialog.dismiss();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onTextChanged(CharSequence s, int start, int before, int count) {
		onTextChangedListener();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void afterTextChanged(Editable s) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	/**
	 * Closes the soft keyboard
	 */
	protected void closeKeyboard() {
		InputMethodManager iManager = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
		if (iManager != null) {
			iManager.hideSoftInputFromWindow(mPlaylist.getWindowToken(), 0);
		}
	}

	/**
	 * Initializes the prompt and default name
	 */
	public abstract void initObjects(Bundle savedInstanceState);

	/**
	 * Called when the save button of our {@link AlertDialog} is pressed
	 */
	public abstract void onSaveClick();

	/**
	 * Called in our {@link TextWatcher} during a text change
	 */
	public abstract void onTextChangedListener();
}