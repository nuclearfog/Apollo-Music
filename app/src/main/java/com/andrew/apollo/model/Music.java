package com.andrew.apollo.model;

import androidx.annotation.NonNull;

/**
 * item super class for music information
 */
public abstract class Music {

    protected final long id;

    protected final String name;

    /**
     *
     */
    protected Music(long id, String name) {
        if (name != null)
            this.name = name;
        else
            this.name = "";
        this.id = id;
    }

    /**
     * get name of the item like artist name or track name
     *
     * @return name of the item
     */
    public String getName() {
        return name;
    }

    /**
     * get ID of the item like artist, album or song ID
     *
     * @return ID of the item
     */
    public final long getId() {
        return id;
    }


    @NonNull
    @Override
    public String toString() {
        return name;
    }
}