package org.nuclearfog.apollo.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(requireContext())
				.setMessage(R.string.delete_warning)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ImageCache mImageCache = ImageCache.getInstance(requireContext());
						mImageCache.clearCaches();
					}
				})
				.setNegativeButton(R.string.cancel, null).create();
	}

	/**
	 * show this dialog
	 */
	public static void show(FragmentActivity fragmentActivity) {
		Fragment fragment = fragmentActivity.getSupportFragmentManager().findFragmentByTag(TAG);
		if (fragment == null) {
			CacheClearDialog dialog = new CacheClearDialog();
			dialog.show(fragmentActivity.getSupportFragmentManager(), TAG);
		}
	}
}