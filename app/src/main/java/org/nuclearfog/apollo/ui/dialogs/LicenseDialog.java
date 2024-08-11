package org.nuclearfog.apollo.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Dialog used to show a list of licenses of all used libraries
 *
 * @author nuclearfog
 */
public class LicenseDialog extends DialogFragment {

	private static final String TAG = "LicenseDialog";


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		WebView webView = new WebView(requireContext());
		webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		webView.loadUrl("file:///android_asset/licenses.html");
		return webView;
	}

	/**
	 * show this dialog
	 */
	public static void show(FragmentActivity activity) {
		Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
		if (fragment == null) {
			LicenseDialog dialog = new LicenseDialog();
			dialog.show(activity.getSupportFragmentManager(), TAG);
		}
	}
}