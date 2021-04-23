package com.andrew.apollo.model;

import androidx.annotation.NonNull;

public abstract class Music {

    protected final long id;

    protected final String name;

    protected Music(long id, String name) {
        if (name != null)
            this.name = name;
        else
            this.name = "";
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public final long getId() {
        return id;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}