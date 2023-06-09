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

package org.nuclearfog.apollo.ui.activities;

import static org.nuclearfog.apollo.Config.ID;
import static org.nuclearfog.apollo.Config.IDS;
import static org.nuclearfog.apollo.Config.MIME_TYPE;
import static org.nuclearfog.apollo.Config.NAME;
import static org.nuclearfog.apollo.Config.PLAY_FROM_SEARCH;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.loaders.SearchLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.MusicUtils.ServiceToken;
import org.nuclearfog.apollo.utils.StringUtils;

import java.util.List;

/**
 * This class is opened when the user touches a Home screen shortcut or album
 * art in an app-wdget, and then carries out the proper action. It is also
 * responsible for processing voice queries and playing the spoken artist,
 * album, song, playlist, or genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ShortcutActivity extends AppCompatActivity implements ServiceConnection, LoaderCallbacks<List<Song>> {

	/**
	 * ID of the loader
	 */
	private static final int LOADER_ID = 0x32942390;
	/**
	 * If true, this class will begin playback and open
	 * {@link AudioPlayerActivity}, false will close the class after playback,
	 * which is what happens when a user starts playing something from an
	 * app-widget
	 */
	public static final String OPEN_AUDIO_PLAYER = null;
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
	private long[] mList = {};
	/**
	 * Used to shuffle the tracks or play them in order
	 */
	private boolean mShouldShuffle;
	/**
	 * Search query from a voice action
	 */
	private String mVoiceQuery;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Bind Apollo's service
		mToken = MusicUtils.bindToService(this, this);
		// Initialize the intent
		mIntent = getIntent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// Check for a voice query
		if (mIntent.getAction() != null && mIntent.getAction().equals(PLAY_FROM_SEARCH)) {
			LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
		} else if (MusicUtils.isConnected()) {
			//sHandler.post(new AsyncHandler(this));
			String requestedMimeType = mIntent.getStringExtra(MIME_TYPE);
			long id = mIntent.getLongExtra(ID, -1);
			if (requestedMimeType == null) {
				return;
			}
			switch (requestedMimeType) {
				case MediaStore.Audio.Artists.CONTENT_TYPE:
					// Shuffle the artist track list
					mShouldShuffle = true;
					// Get the artist song list
					mList = MusicUtils.getSongListForArtist(getApplicationContext(), id);
					break;

				case MediaStore.Audio.Albums.CONTENT_TYPE:
					// Shuffle the album track list
					mShouldShuffle = true;
					// Get the album song list
					mList = MusicUtils.getSongListForAlbum(getApplicationContext(), id);
					break;

				case MediaStore.Audio.Genres.CONTENT_TYPE:
					// Shuffle the genre track list
					mShouldShuffle = true;
					// Get the genre song list
					long[] ids = ApolloUtils.readSerializedIDs(mIntent.getStringExtra(IDS));
					mList = MusicUtils.getSongListForGenres(getApplicationContext(), ids);
					break;

				case MediaStore.Audio.Playlists.CONTENT_TYPE:
					// Don't shuffle the playlist track list
					mShouldShuffle = false;
					// Get the playlist song list
					mList = MusicUtils.getSongListForPlaylist(getApplicationContext(), id);
					break;

				case ProfileActivity.PAGE_FAVORIT:
					// Don't shuffle the Favorites track list
					mShouldShuffle = false;
					// Get the Favorites song list
					mList = MusicUtils.getSongListForFavorites(getApplicationContext());
					break;

				case ProfileActivity.PAGE_MOST_PLAYED:
					// Don't shuffle the popular track list
					mShouldShuffle = false;
					// Get the popular song list
					mList = MusicUtils.getPopularSongList(getApplicationContext());
					break;

				case ProfileActivity.PAGE_FOLDERS:
					// Don't shuffle the folders track list
					mShouldShuffle = false;
					// get folder path
					String folder = "%" + mIntent.getStringExtra(NAME);
					// Get folder song list
					mList = MusicUtils.getSongListForFolder(getApplicationContext(), folder);
					break;

				case ProfileActivity.PAGE_LAST_ADDED:
					// Don't shuffle the last added track list
					mShouldShuffle = false;
					// Get the Last added song list
					mList = MusicUtils.getSongListForLastAdded(getApplicationContext());
					break;
			}
			// Finish up
			allDone();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceDisconnected(ComponentName name) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
		if (MusicUtils.isConnected()) {
			MusicUtils.unbindFromService(mToken);
			mToken = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
		// Get the voice search query
		mVoiceQuery = StringUtils.capitalize(mIntent.getStringExtra(SearchManager.QUERY));
		return new SearchLoader(ShortcutActivity.this, mVoiceQuery);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
		// disable loader
		LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
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
		// What's about to happen is similar to the above process. Apollo
		// runs a
		// series of checks to see if anything comes up. When it does, it
		// assumes (pretty accurately) that it should begin to play that
		// thing.
		// The fancy search query used in {@link SearchLoader} is the key to
		// this. It allows the user to perform very specific queries. i.e.
		// "Listen to Ethio

		String song = data.get(0).getName();
		String album = data.get(0).getAlbum();
		String artist = data.get(0).getArtist();
		// This tripes as the song, album, and artist Id
		long id = data.get(0).getId();
		// First, try to play a song
		if (song != null) {
			mList = new long[]{id};
		} else {
			if (album != null) {
				// Second, try to play an album
				mList = MusicUtils.getSongListForAlbum(ShortcutActivity.this, id);
			} else if (artist != null) {
				// Third, try to play an artist
				mList = MusicUtils.getSongListForArtist(ShortcutActivity.this, id);
			}
		}
		// Finish up
		allDone();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
	}

	/**
	 * @return True if the user searched for the favorites playlist
	 */
	private boolean isFavorite() {
		if (ProfileActivity.PAGE_FAVORIT.equals(mVoiceQuery)) {
			return true;
		}
		// Check to see if the user spoke the word "Favorite"
		String favorite = getString(R.string.playlist_favorite);
		return favorite.equals(mVoiceQuery);
	}

	/**
	 * Starts playback, open {@link AudioPlayerActivity} and finishes this one
	 */
	private void allDone() {
		boolean shouldOpenAudioPlayer = mIntent.getBooleanExtra(OPEN_AUDIO_PLAYER, true);
		// Play the list
		if (mList.length > 0) {
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