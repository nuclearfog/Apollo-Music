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
import com.andrew.apollo.adapters.MusicHolder.DataHolder;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import java.util.ArrayList;

/**
 * This {@link ArrayAdapter} is used to display all of the albums on a user's
 * device for {@link com.andrew.apollo.ui.fragments.RecentFragment} and {@link com.andrew.apollo.ui.fragments.AlbumFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumAdapter extends ArrayAdapter<Album> {

    /**
     * Number of views (ImageView and TextView)
     */
    private static final int VIEW_TYPE_COUNT = 2;

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
     * Sets the album art on click listener to start playing them album when
     * touched.
     */
    private boolean mTouchPlay = false;

    /**
     * Used to cache the album info
     */
    private ArrayList<DataHolder> mData = new ArrayList<>();

    /**
     * Constructor of <code>AlbumAdapter</code>
     *
     * @param context  The {@link Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public AlbumAdapter(FragmentActivity context, int layoutId) {
        super(context, layoutId);
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
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }
        // Retrieve the data holder
        DataHolder dataHolder = mData.get(position);
        // Set each album name (line one)
        holder.mLineOne.setText(dataHolder.mLineOne);
        // Set the artist name (line two)
        holder.mLineTwo.setText(dataHolder.mLineTwo);
        // Asynchronously load the album images into the adapter
        mImageFetcher.loadAlbumImage(dataHolder.mLineTwo, dataHolder.mLineOne, dataHolder.mItemId, holder.mImage);
        // List view only items
        if (mLoadExtraData) {
            // Set the number of songs (line three)
            holder.mLineThree.setText(dataHolder.mLineThree);
            // Asynchronously load the artist image on the background view
            mImageFetcher.loadArtistImage(dataHolder.mLineTwo, holder.mBackground);
        }
        if (mTouchPlay) {
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
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }


    @Override
    public void clear() {
        super.clear();
        mData.clear();
    }

    /**
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData.clear();
        mData.ensureCapacity(getCount());
        for (int i = 0; i < getCount(); i++) {
            // Build the album
            Album album = getItem(i);
            if (album != null) {
                // Build the data holder
                DataHolder holder = new DataHolder();
                // Album Id
                holder.mItemId = album.getId();
                // Album names (line one)
                holder.mLineOne = album.getName();
                // Album artist names (line two)
                holder.mLineTwo = album.getArtist();
                // Number of songs for each album (line three)
                holder.mLineThree = MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, album.getTrackCount());
                mData.add(holder);
            }
        }
    }

    /**
     * Starts playing an album if the user touches the artwork in the list.
     *
     * @param albumCover The {@link ImageView} holding the album
     * @param position   The position of the album to play.
     */
    private void playAlbum(ImageView albumCover, final int position) {
        albumCover.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Album album = getItem(position);
                if (album != null) {
                    long id = album.getId();
                    long[] list = MusicUtils.getSongListForAlbum(getContext(), id);
                    MusicUtils.playAll(list, 0, false);
                }
            }
        });
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
     * @param extra True to load line three and the background image, false
     *              otherwise.
     */
    public void setLoadExtraData(boolean extra) {
        mLoadExtraData = extra;
        setTouchPlay(true);
    }

    /**
     * @param play True to play the album when the artwork is touched, false
     *             otherwise.
     */
    public void setTouchPlay(boolean play) {
        mTouchPlay = play;
    }
}