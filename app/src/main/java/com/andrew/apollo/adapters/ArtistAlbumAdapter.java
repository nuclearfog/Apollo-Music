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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;

import java.util.List;

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
    private static final int VIEW_TYPE_COUNT = 3;

    /**
     * Fake header
     */
    private View mHeader;

    /**
     * The resource Id of the layout to inflate
     */
    private int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private ImageFetcher mImageFetcher;

    /**
     * Used to set the size of the data in the adapter
     */
    private List<Album> mCount = Lists.newArrayList();

    /**
     * Constructor of <code>ArtistAlbumAdapter</code>
     *
     * @param context  The {@link Context} to use
     * @param layoutId The resource Id of the view to inflate.
     */
    public ArtistAlbumAdapter(FragmentActivity context, int layoutId) {
        super(context, 0);
        // Used to create the custom layout
        // Cache the header
        mHeader = View.inflate(context, R.layout.faux_carousel, null);
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(context);
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
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            // Remove the background layer
            holder.mOverlay.setBackgroundColor(0);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the album
        Album album = getItem(position - 1);

        if (album != null) {
            String albumName = album.mAlbumName;
            // Set each album name (line one)
            holder.mLineOne.setText(albumName);
            // Set the number of songs (line two)
            holder.mLineTwo.setText(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, album.mSongNumber));
            // Set the album year (line three)
            holder.mLineThree.setText(album.mYear);
            // Asynchronously load the album images into the adapter
            mImageFetcher.loadAlbumImage(album.mArtistName, albumName, album.mAlbumId, holder.mImage);
            // Play the album when the artwork is touched
            playAlbum(holder.mImage, position);
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
        int size = mCount.size();
        return size == 0 ? 0 : size + 1;
    }

    /**
     * @param data The {@link List} used to return the count for the adapter.
     */
    public void setCount(List<Album> data) {
        mCount = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return -1;
        }
        return position - 1;
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
        if (position == 0) {
            return ITEM_VIEW_TYPE_HEADER;
        }
        return ITEM_VIEW_TYPE_MUSIC;
    }

    /**
     * Starts playing an album if the user touches the artwork in the list.
     *
     * @param album    The {@link ImageView} holding the album
     * @param position The position of the album to play.
     */
    private void playAlbum(ImageView album, final int position) {
        album.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Album album = getItem(position - 1);
                if (album != null) {
                    long id = album.mAlbumId;
                    long[] list = MusicUtils.getSongListForAlbum(getContext(), id);
                    MusicUtils.playAll(list, 0, false);
                }
            }
        });
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
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