package org.nuclearfog.apollo.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.AudioPreset;

/**
 * Dialog used to save preset
 *
 * @author nuclearfog
 * @see org.nuclearfog.apollo.ui.activities.AudioFxActivity
 */
public class PresetDialog extends DialogFragment implements OnClickListener, TextWatcher {

	public static final String TAG = "PresetDialog";

	private static final String KEY_PRESET = "dialog_preset";

	private AudioPreset preset;

	private EditText text;
	private Button mSaveButton;

	/**
	 * create an instance of this dialog
	 *
	 * @param preset preset configuration to show
	 */
	public static void show(FragmentManager fm, AudioPreset preset) {
		PresetDialog presetDialog;
		Bundle args = new Bundle();
		String tag = PresetDialog.TAG + ":" + preset.getName();
		Fragment dialog = fm.findFragmentByTag(tag);

		if (dialog instanceof PresetDialog) {
			presetDialog = (PresetDialog) dialog;
		} else {
			presetDialog = new PresetDialog();
		}
		args.putSerializable(KEY_PRESET, preset);
		presetDialog.setArguments(args);
		presetDialog.show(fm, tag);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		text = new EditText(getContext());
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setPositiveButton(R.string.save, this);
		builder.setNegativeButton(R.string.cancel, this);
		builder.setTitle(R.string.dialog_save_preset_title);
		builder.setView(text);
		AlertDialog dialog = builder.show();
		mSaveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);

		if (savedInstanceState != null) {
			preset = (AudioPreset) savedInstanceState.getSerializable(KEY_PRESET);
		} else if (getArguments() != null) {
			preset = (AudioPreset) getArguments().getSerializable(KEY_PRESET);
		}
		text.setLines(1);
		text.setBackgroundColor(0);
		text.setInputType(EditorInfo.TYPE_CLASS_TEXT);
		text.setPadding(30, 0, 0, 30);
		text.append(preset.getName());
		text.setHint(R.string.preset_name_hint);
		mSaveButton.setEnabled(!preset.getName().trim().isEmpty());

		text.addTextChangedListener(this);
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(KEY_PRESET, preset);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (preset.getName().isEmpty()) {
				text.setError(getString(R.string.empty_preset_name));
			} else {
				Activity activity = getActivity();
				if (activity instanceof OnPresetSaveCallback) {
					((OnPresetSaveCallback) activity).onPresetSave(preset);
				}
				text.setError(null);
				dismiss();
			}
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dismiss();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterTextChanged(Editable s) {
		mSaveButton.setEnabled(!s.toString().trim().isEmpty());
		preset.setName(s.toString());
	}

	/**
	 * callback used to send the modified preset back to activity
	 */
	public interface OnPresetSaveCallback {

		/**
		 * called to set the new preset
		 *
		 * @param preset new preset
		 */
		void onPresetSave(AudioPreset preset);
	}
}