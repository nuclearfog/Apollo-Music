package org.nuclearfog.apollo.ui.fragments;

/**
 * callback interface to update fragments
 *
 * @author nuclearfog
 */
public interface FragmentCallback {

	/**
	 * reload content after change
	 */
	void refresh();

	/**
	 * scroll to current item
	 */
	void setCurrentTrack();
}