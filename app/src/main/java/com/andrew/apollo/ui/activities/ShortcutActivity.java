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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.format.Capitalize;
import com.andrew.apollo.loaders.AsyncHandler;
import com.andrew.apollo.loaders.SearchLoader;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;

import java.util.ArrayList;
import java.util.List;

import static com.andrew.apollo.Config.MIME_TYPE;
import static com.andrew.apollo.loaders.LastAddedLoader.ORDER;
import static com.andrew.apollo.loaders.LastAddedLoader.PROJECTION;
import static com.andrew.apollo.loaders.LastAddedLoader.SELECTION;
import static com.andrew.apollo.ui.activities.ProfileActivity.PAGE_FAVORIT;
import static com.andrew.apollo.utils.MusicUtils.mService;

/**
 * This class is opened when the user touches a Home screen shortcut or album
 * art in an app-wdget, and then carries out the proper action. It is also
 * responsible for processing voice queries and playing the spoken artist,
 * album, song, playlist, or genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ShortcutActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * If true, this class will begin playback and open
     * {@link AudioPlayerActivity}, false will close the class after playback,
     * which is what happens when a user starts playing something from an
     * app-widget
     */
    public static final String OPEN_AUDIO_PLAYER = null;
    /**
     * Used with the loader and voice queries
     */
    private ArrayList<Song> mSong = Lists.newArrayList();
    /**
     * Service token
     */
    private ServiceToken mToken;
    /**
     * Gather the intent action and extras
     */
    private Intent mIntent;
    /**
     * The list of songs to play
     */
    private long[] mList;
    /**
     * Used to shuffle the tracks or play them in order
     */
    private boolean mShouldShuffle;
    /**
     * Search query from a voice action
     */
    private String mVoiceQuery;
    /**
     * Uses the query from a voice search to try and play a song, then album,
     * then artist. If all of those fail, it checks for playlists and genres via
     * a  #mPlaylistGenreQuery.
     */
    private LoaderCallbacks<List<Song>> mSongAlbumArtistQuery = new LoaderCallbacks<List<Song>>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            return new SearchLoader(ShortcutActivity.this, mVoiceQuery);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
            // If the user searched for a playlist or genre, this list will
            // return empty
            if (data.isEmpty()) {
                // Before running the playlist loader, try to play the
                // "Favorites" playlist
                if (isFavorite()) {
                    MusicUtils.playFavorites(ShortcutActivity.this);
                }
                // Finish up
                allDone();
                return;
            }

            // Start fresh
            mSong.clear();
            // Add the data to the adpater
            mSong.addAll(data);

            // What's about to happen is similar to the above process. Apollo
            // runs a
            // series of checks to see if anything comes up. When it does, it
            // assumes (pretty accurately) that it should begin to play that
            // thing.
            // The fancy search query used in {@link SearchLoader} is the key to
            // this. It allows the user to perform very specific queries. i.e.
            // "Listen to Ethio

            String song = mSong.get(0).mSongName;
            String album = mSong.get(0).mAlbumName;
            String artist = mSong.get(0).mArtistName;
            // This tripes as the song, album, and artist Id
            long id = mSong.get(0).mSongId;
            // First, try to play a song
            if (mList == null && song != null) {
                mList = new long[]{
                        id
                };
            } else
                // Second, try to play an album
                if (mList == null && album != null) {
                    mList = MusicUtils.getSongListForAlbum(ShortcutActivity.this, id);
                } else
                    // Third, try to play an artist
                    if (mList == null && artist != null) {
                        mList = MusicUtils.getSongListForArtist(ShortcutActivity.this, id);
                    }
            // Finish up
            allDone();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
            // Clear the data
            mSong.clear();
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);
        // Intiialize the intent
        mIntent = getIntent();
        // Get the voice search query
        mVoiceQuery = Capitalize.capitalize(mIntent.getStringExtra(SearchManager.QUERY));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
        // Check for a voice query
        if (mIntent.getAction() != null && mIntent.getAction().equals(Config.PLAY_FROM_SEARCH)) {
            LoaderManager.getInstance(this).initLoader(0, null, mSongAlbumArtistQuery);
        } else if (mService != null) {
            AsyncHandler.post(new Runnable() {
                @Override
                public void run() {
                    String requestedMimeType = "";
                    if (mIntent.getExtras() != null)
                        requestedMimeType = mIntent.getExtras().getString(MIME_TYPE);

                    // First, check the artist MIME type
                    if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(requestedMimeType)) {

                        // Shuffle the artist track list
                        mShouldShuffle = true;

                        // Get the artist song list
                        mList = MusicUtils.getSongListForArtist(ShortcutActivity.this, getId());
                    } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(requestedMimeType)) {

                        // Shuffle the album track list
                        mShouldShuffle = true;

                        // Get the album song list
                        mList = MusicUtils.getSongListForAlbum(ShortcutActivity.this, getId());
                    } else if (MediaStore.Audio.Genres.CONTENT_TYPE.equals(requestedMimeType)) {

                        // Shuffle the genre track list
                        mShouldShuffle = true;

                        // Get the genre song list
                        mList = MusicUtils.getSongListForGenre(ShortcutActivity.this, getId());
                    } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(requestedMimeType)) {

                        // Don't shuffle the playlist track list
                        mShouldShuffle = false;

                        // Get the playlist song list
                        mList = MusicUtils.getSongListForPlaylist(ShortcutActivity.this, getId());
                    } else if (PAGE_FAVORIT.equals(requestedMimeType)) {

                        // Don't shuffle the Favorites track list
                        mShouldShuffle = false;

                        // Get the Favorites song list
                        mList = MusicUtils.getSongListForFavorites(ShortcutActivity.this);
                    } else if (getString(R.string.playlist_last_added).equals(requestedMimeType)) {
                        // Don't shuffle the last added track list
                        mShouldShuffle = false;
                        // Get the Last added song list
                        String time = Long.toString(System.currentTimeMillis() / 1000 - 2419200);
                        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                PROJECTION, SELECTION + time, null, ORDER);
                        if (cursor != null) {
                            mList = MusicUtils.getSongListForCursor(cursor);
                            cursor.close();
                        }
                    }
                    // Finish up
                    allDone();
                }
            });
        }
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
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mService != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    /**
     * Used to find the Id supplied
     *
     * @return The Id passed into the activity
     */
    private long getId() {
        if (mIntent.getExtras() != null)
            return mIntent.getExtras().getLong(Config.ID);
        return -1;
    }

    /**
     * @return True if the user searched for the favorites playlist
     */
    private boolean isFavorite() {
        if (PAGE_FAVORIT.equals(mVoiceQuery)) {
            return true;
        }
        // Check to see if the user spoke the word "Favorite"
        String favorite = getString(R.string.playlist_favorite);
        return mVoiceQuery.equals(favorite);
    }

    /**
     * Starts playback, open {@link AudioPlayerActivity} and finishes this one
     */
    private void allDone() {
        boolean shouldOpenAudioPlayer = mIntent.getBooleanExtra(OPEN_AUDIO_PLAYER, true);
        // Play the list
        if (mList != null && mList.length > 0) {
            MusicUtils.playAll(mList, 0, mShouldShuffle);
        }

        // Open the now playing screen
        if (shouldOpenAudioPlayer) {
            Intent intent = new Intent(this, AudioPlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        // All done
        finish();
    }
}