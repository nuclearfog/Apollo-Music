/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.model;

import android.text.TextUtils;

/**
 * A class that represents a genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Genre extends Music {

    private long[] ids;

    /**
     * Constructor of <code>Genre</code>
     *
     * @param ids       The Id of the genre
     * @param genreName The genre name
     */
    public Genre(long[] ids, String genreName) {
        super(ids[0], genreName);
        this.ids = ids;
    }

    /**
     * return a set of genre IDs for the same Genre name
     *
     * @return ID array
     */
    public long[] getGenreIds() {
        return ids;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) id;
        result = prime * result + name.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Genre other = (Genre) obj;
        if (id != other.id) {
            return false;
        }
        return TextUtils.equals(name, other.name);
    }
}