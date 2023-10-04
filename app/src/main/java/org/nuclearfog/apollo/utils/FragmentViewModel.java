package org.nuclearfog.apollo.utils;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.nuclearfog.apollo.BuildConfig;

/**
 * ViewModel class used to communicate between activities and fragments
 *
 * @author nuclearfog
 */
public class FragmentViewModel extends ViewModel {

	private static final String TAG = "FragmentViewModel";


	private final MutableLiveData<String> selectedItem = new MutableLiveData<>();

	/**
	 * send notification string to registered fragments
	 *
	 * @param s notification string
	 */
	public void notify(String s) {
		selectedItem.setValue(s);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, s);
		}
	}

	/**
	 *
	 */
	public LiveData<String> getSelectedItem() {
		return selectedItem;
	}
}