package org.nuclearfog.apollo.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.AudioPreset;

/**
 * @author nuclearfog
 */
public class PresetDialog extends DialogFragment implements OnClickListener, TextWatcher {

	public static final String TAG = "PresetDialog";

	private static final String KEY_PRESET = "dialog_preset";

	private AudioPreset preset;


	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null)
			preset = (AudioPreset) savedInstanceState.getSerializable(KEY_PRESET);
		else if (getArguments() != null)
			preset = (AudioPreset) getArguments().getSerializable(KEY_PRESET);
		EditText text = new EditText(getContext());
		text.setText(preset.getName());
		text.addTextChangedListener(this);
		return new AlertDialog.Builder(requireContext())
				.setPositiveButton(R.string.save, this)
				.setNegativeButton(R.string.cancel, this)
				.setTitle(R.string.dialog_save_preset_title)
				.setView(text)
				.create();
	}


	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(KEY_PRESET, preset);
	}


	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			Activity activity = getActivity();
			if (activity instanceof OnPresetSaveCallback) {
				((OnPresetSaveCallback)activity).onPresetSave(preset);
			}
			dismiss();
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			dismiss();
		}
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}


	@Override
	public void afterTextChanged(Editable s) {
		preset.setName(s.toString());
	}


	public static PresetDialog newInstance(AudioPreset preset) {
		PresetDialog dialog = new PresetDialog();
		Bundle args = new Bundle();
		args.putSerializable(KEY_PRESET, preset);
		dialog.setArguments(args);
		return dialog;
	}


	public interface OnPresetSaveCallback {

		void onPresetSave(AudioPreset preset);
	}
}