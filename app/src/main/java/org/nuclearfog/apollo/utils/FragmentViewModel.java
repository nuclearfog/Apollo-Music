package org.nuclearfog.apollo.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * @author nuclearfog
 */
public class FragmentViewModel extends ViewModel {

	public static final String SCROLL_TOP = "scroll_to_top";
	public static final String SET_CURRENT_TRACK = "set_current_track";
	public static final String REFRESH = "refresh_tracks";

	private final MutableLiveData<String> selectedItem = new MutableLiveData<>();

	/**
	 *
	 */
	public void notify(String s) {
		selectedItem.setValue(s);
	}

	/**
	 *
	 */
	public LiveData<String> getSelectedItem() {
		return selectedItem;
	}
}