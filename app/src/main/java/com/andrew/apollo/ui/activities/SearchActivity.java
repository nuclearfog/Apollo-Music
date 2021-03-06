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

package com.andrew.apollo.ui.activities;

import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.content.res.ResourcesCompat;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.MusicHolder;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.format.PrefixHighlighter;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeUtils;

import java.util.Locale;

import static com.andrew.apollo.utils.MusicUtils.mService;

/**
 * Provides the search interface for Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchActivity extends AppCompatBase implements LoaderCallbacks<Cursor>,
        OnScrollListener, OnQueryTextListener, OnItemClickListener, ServiceConnection {
    /**
     * Grid view column count. ONE - list, TWO - normal grid
     */
    private static final int ONE = 1, TWO = 2;

    /**
     * The service token
     */
    private ServiceToken mToken;

    /**
     * The query
     */
    private String mFilterString;

    /**
     * Grid view
     */
    private GridView mGridView;

    private TextView emptyText;

    /**
     * List view adapter
     */
    private SearchAdapter mAdapter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init view
        mGridView = findViewById(R.id.grid_search);
        emptyText = findViewById(R.id.grid_search_empty_info);
        View background = findViewById(R.id.grid_search_container);

        // Initialze the theme resources
        ThemeUtils mResources = new ThemeUtils(this);
        // Set the overflow style
        mResources.setOverflowStyle(this);
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);
        background.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.pager_background, null));
        // Get the query
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        mFilterString = !TextUtils.isEmpty(query) ? query : null;
        // Action bar subtitle
        mResources.setSubtitle("\"" + mFilterString + "\"");
        // Initialize the adapter
        mAdapter = new SearchAdapter(this);
        // Set the prefix
        mAdapter.setPrefix(mFilterString);
        // Bind the data
        mGridView.setAdapter(mAdapter);
        // Recycle the data
        mGridView.setRecyclerListener(new RecycleHolder());
        // Seepd up scrolling
        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);
        if (ApolloUtils.isLandscape(this)) {
            mGridView.setNumColumns(TWO);
        } else {
            mGridView.setNumColumns(ONE);
        }
        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String query = intent.getStringExtra(SearchManager.QUERY);
        mFilterString = !TextUtils.isEmpty(query) ? query : null;
        // Set the prefix
        mAdapter.setPrefix(mFilterString);
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(this, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mService != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(mFilterString));
        String[] projection = new String[]{
                BaseColumns._ID, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Media.TITLE, "data1", "data2"
        };
        return new CursorLoader(this, uri, projection, null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.isClosed() || data.getCount() <= 0) {
            mGridView.setEmptyView(emptyText);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            // Swap the new cursor in. (The framework will take care of closing the
            // old cursor once we return.)
            mAdapter.swapCursor(data);
            emptyText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public View getContentView() {
        return View.inflate(this, R.layout.list_search, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mAdapter.setPauseDiskCache(true);
        } else {
            mAdapter.setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            return false;
        }
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mFilterString = !TextUtils.isEmpty(newText) ? newText : null;
        // Set the prefix
        mAdapter.setPrefix(mFilterString);
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return;
        }
        // Get the MIME type
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

        // If it's an artist, open the artist profile
        if ("artist".equals(mimeType)) {
            NavUtils.openArtistProfile(this, cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)));
        } else if ("album".equals(mimeType)) {
            // If it's an album, open the album profile
            NavUtils.openAlbumProfile(this,
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)));
        } else if (position >= 0 && id >= 0) {
            // If it's a song, play it and leave
            long[] list = new long[]{id};
            MusicUtils.playAll(list, 0, false);
        }
        // Close it up
        cursor.close();
        // All done
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Nothing to do
    }

    /**
     * Used to populate the list view with the search results.
     */
    private static final class SearchAdapter extends CursorAdapter {

        /**
         * Number of views (ImageView and TextView)
         */
        private static final int VIEW_TYPE_COUNT = 2;

        /**
         * Image cache and image fetcher
         */
        private final ImageFetcher mImageFetcher;

        /**
         * Highlights the query
         */
        private final PrefixHighlighter mHighlighter;

        /**
         * The prefix that's highlighted
         */
        private char[] mPrefix;

        /**
         * Constructor for <code>SearchAdapter</code>
         *
         * @param context The {@link Context} to use.
         */
        public SearchAdapter(AppCompatActivity context) {
            super(context, null, false);
            // Initialize the cache & image fetcher
            mImageFetcher = ApolloUtils.getImageFetcher(context);
            // Create the prefix highlighter
            mHighlighter = new PrefixHighlighter(context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bindView(View convertView, Context context, Cursor cursor) {
            /* Recycle ViewHolder's items */
            MusicHolder holder = (MusicHolder) convertView.getTag();
            if (holder == null) {
                holder = new MusicHolder(convertView);
                convertView.setTag(holder);
            }
            // Get the MIME type
            String mimetype = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
            if (mimetype.equals("artist")) {
                holder.mImage.setScaleType(ScaleType.CENTER_CROP);
                // Get the artist name
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                holder.mLineOne.setText(artist);
                // Get the album count
                int albumCount = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
                holder.mLineTwo.setText(MusicUtils.makeLabel(context, R.plurals.Nalbums, albumCount));
                // Get the song count
                int songCount = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));
                holder.mLineThree.setText(MusicUtils.makeLabel(context, R.plurals.Nsongs, songCount));
                // Asynchronously load the artist image into the adapter
                mImageFetcher.loadArtistImage(artist, holder.mImage);
                // Highlght the query
                mHighlighter.setText(holder.mLineOne, artist, mPrefix);
            } else if (mimetype.equals("album")) {
                holder.mImage.setScaleType(ScaleType.FIT_XY);
                // Get the Id of the album
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
                // Get the album name
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
                holder.mLineOne.setText(album);
                // Get the artist name
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
                holder.mLineTwo.setText(artist);
                // Asynchronously load the album images into the adapter
                mImageFetcher.loadAlbumImage(artist, album, id, holder.mImage);
                // Asynchronously load the artist image into the adapter
                mImageFetcher.loadArtistImage(artist, holder.mBackground);
                // Highlght the query
                mHighlighter.setText(holder.mLineOne, album, mPrefix);
            } else if (mimetype.startsWith("audio/") || mimetype.equals("application/ogg") || mimetype.equals("application/x-ogg")) {
                holder.mImage.setScaleType(ScaleType.FIT_XY);
                holder.mImage.setImageResource(R.drawable.header_temp);
                // Get the track name
                String track = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                holder.mLineOne.setText(track);
                // Get the album name
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                holder.mLineTwo.setText(album);
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                // Asynchronously load the artist image into the adapter
                mImageFetcher.loadArtistImage(artist, holder.mBackground);
                holder.mLineThree.setText(artist);
                // Highlght the query
                mHighlighter.setText(holder.mLineOne, track, mPrefix);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return View.inflate(context, R.layout.list_item_detailed, null);
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

        /**
         * @param pause True to temporarily pause the disk cache, false
         *              otherwise.
         */
        public void setPauseDiskCache(boolean pause) {
            if (mImageFetcher != null) {
                mImageFetcher.setPauseDiskCache(pause);
            }
        }

        /**
         * @param prefix The query to filter.
         */
        public void setPrefix(CharSequence prefix) {
            if (!TextUtils.isEmpty(prefix)) {
                mPrefix = prefix.toString().toUpperCase(Locale.getDefault()).toCharArray();
            } else {
                mPrefix = null;
            }
        }
    }
}