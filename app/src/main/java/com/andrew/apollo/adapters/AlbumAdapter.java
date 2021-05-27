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

package com.andrew.apollo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

/**
 * This {@link ArrayAdapter} is used to display all of the albums on a user's
 * device for {@link com.andrew.apollo.ui.fragments.RecentFragment}
 * and {@link com.andrew.apollo.ui.fragments.AlbumFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumAdapter extends ArrayAdapter<Album> {

    /**
     * fragment layout inflater
     */
    private LayoutInflater inflater;

    /**
     * The resource Id of the layout to inflate
     */
    private int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private ImageFetcher mImageFetcher;

    /**
     * Determines if the grid or list should be the default style
     */
    private boolean mLoadExtraData = false;

    /**
     * Constructor of <code>AlbumAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public AlbumAdapter(FragmentActivity context, int layoutId) {
        super(context, layoutId);
        inflater = context.getLayoutInflater();
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }
        // Retrieve the data holder
        Album album = getItem(position);
        if (album != null) {
            // Set each album name (line one)
            holder.mLineOne.setText(album.getName());
            // Set the artist name (line two)
            holder.mLineTwo.setText(album.getArtist());
            // Asynchronously load the album images into the adapter
            mImageFetcher.loadAlbumImage(album.getArtist(), album.getName(), album.getId(), holder.mImage);
            // List view only items
            if (mLoadExtraData) {
                // Set the number of songs (line three)
                String count = MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, album.getTrackCount());
                holder.mLineThree.setText(count);
                // register album art click listener
                ApolloUtils.registerItemViewListener(holder.mImage, parent, position, album.getId());
            }
        }
        return convertView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        Album album = getItem(position);
        if (album != null)
            return album.getId();
        return super.getItemId(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(boolean pause) {
        if (mImageFetcher != null) {
            mImageFetcher.setPauseDiskCache(pause);
        }
    }

    /**
     * Flushes the disk cache.
     */
    public void flush() {
        mImageFetcher.flush();
    }

    /**
     * enable extra information
     */
    public void setLoadExtraData() {
        mLoadExtraData = true;
    }
}