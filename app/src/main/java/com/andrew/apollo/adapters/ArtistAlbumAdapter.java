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
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

/**
 * This {@link ArrayAdapter} is used to display the albums for a particular
 * artist for {@link ArtistAlbumFragment} .
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAlbumAdapter extends ArrayAdapter<Album> {

    /**
     * The header view
     */
    private static final int ITEM_VIEW_TYPE_HEADER = 0;

    /**
     * * The data in the list.
     */
    private static final int ITEM_VIEW_TYPE_MUSIC = 1;

    /**
     * Number of views (ImageView, TextView, header)
     */
    private static final int VIEW_TYPE_COUNT = 2;

    /**
     * count of header views
     */
    private static final int HEADER_COUNT = 1;

    /**
     * layout resource
     */
    private static final int LAYOUT = R.layout.list_item_detailed;

    /**
     * fragment layout inflater
     */
    private LayoutInflater inflater;

    /**
     * Fake header
     */
    private View mHeader;

    /**
     * Image cache and image fetcher
     */
    private ImageFetcher mImageFetcher;

    /**
     * Constructor of <code>ArtistAlbumAdapter</code>
     *
     * @param context The {@link Context} to use
     */
    public ArtistAlbumAdapter(FragmentActivity context) {
        super(context, 0);
        // Used to create the custom layout
        // Cache the header
        mHeader = View.inflate(context, R.layout.faux_carousel, null);
        // Initialize the cache & image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(context);
        // layout inflater from fragment
        inflater = context.getLayoutInflater();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Return a faux header at position 0
        if (position == 0) {
            return mHeader;
        }
        // Recycle MusicHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(LAYOUT, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }
        // Retrieve the album
        Album album = getItem(position);
        if (album != null) {
            // Set each album name (line one)
            holder.mLineOne.setText(album.getName());
            // Set the number of songs (line two)
            holder.mLineTwo.setText(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, album.getTrackCount()));
            // Set the album year (line three)
            holder.mLineThree.setText(album.getRelease());
            // Asynchronously load the album images into the adapter
            mImageFetcher.loadAlbumImage(album.getArtist(), album.getName(), album.getId(), holder.mImage);
            // register album art click listener
            ApolloUtils.registerItemViewListener(holder.mImage, parent, position, album.getId());
        }
        return convertView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return HEADER_COUNT + super.getCount();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Album getItem(int position) {
        if (position >= HEADER_COUNT)
            return super.getItem(position - HEADER_COUNT);
        return null;
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
    public boolean isEmpty() {
        return super.getCount() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return ITEM_VIEW_TYPE_HEADER;
        return ITEM_VIEW_TYPE_MUSIC;
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
}