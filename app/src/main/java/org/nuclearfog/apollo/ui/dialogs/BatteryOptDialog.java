package org.nuclearfog.apollo.ui.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;

/**
 * Dialog used to show user battery optimization settings
 *
 * @author nuclearfog
 */
public class BatteryOptDialog extends DialogFragment implements OnClickListener {

	private static final String TAG = "BatteryOptDialog";

	/**
	 *
	 */
	public static void show(FragmentManager fm) {
		BatteryOptDialog batteryDialog;
		Fragment dialog = fm.findFragmentByTag(TAG);

		if (dialog instanceof BatteryOptDialog) {
			batteryDialog = (BatteryOptDialog) dialog;
		} else {
			batteryDialog = new BatteryOptDialog();
		}
		batteryDialog.show(fm, TAG);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return new AlertDialog.Builder(requireContext())
				.setTitle(R.string.dialog_disable_bat_opt_title)
				.setMessage(R.string.dialog_disable_bat_opt_message)
				.setPositiveButton(R.string.dialog_disable_bat_opt_confirm, this)
				.setNegativeButton(R.string.dialog_disable_bat_opt_ignore, this)
				.create();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			ApolloUtils.redirectToBatteryOptimization(requireActivity());
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			PreferenceUtils.getInstance(requireContext()).setIgnoreBatteryOptimization();
		}
	}
}