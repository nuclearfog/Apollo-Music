package org.nuclearfog.apollo.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageCache;

/**
 * Dialog used to clear the image cache
 *
 * @author nuclearfog
 */
public class CacheClearDialog extends DialogFragment {

	private static final String TAG = "CacheClearDialog";

	/**
	 * show this dialog
	 */
	public static void show(FragmentManager fm) {
		CacheClearDialog cacheClearDialog;
		Fragment dialog = fm.findFragmentByTag(TAG);

		if (dialog instanceof CacheClearDialog) {
			cacheClearDialog = (CacheClearDialog) dialog;
		} else {
			cacheClearDialog = new CacheClearDialog();
		}
		cacheClearDialog.show(fm, TAG);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(requireContext())
				.setMessage(R.string.delete_warning)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ImageCache mImageCache = ImageCache.getInstance(requireContext());
						mImageCache.clearCaches();
					}
				}).create();
	}
}