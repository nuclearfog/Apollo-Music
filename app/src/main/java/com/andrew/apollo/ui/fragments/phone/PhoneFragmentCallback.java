package com.andrew.apollo.ui.fragments.phone;

public interface PhoneFragmentCallback {

    /**
     * reload content after change
     */
    void refresh();

    /**
     * scroll to current item
     */
    void setCurrentTrack();

}
