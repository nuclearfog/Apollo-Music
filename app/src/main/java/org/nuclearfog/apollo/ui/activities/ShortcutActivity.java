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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.SearchLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;
import org.nuclearfog.apollo.utils.StringUtils;

import java.util.List;

/**
 * This class is opened when the user touches a Home screen shortcut or album
 * art in an app-wdget, and then carries out the proper action. It is also
 * responsible for processing voice queries and playing the spoken artist,
 * album, song, playlist, or genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ShortcutActivity extends AppCompatActivity implements ServiceBinderCallback, AsyncCallback<List<Song>> {

	/**
	 * Play from search intent
	 */
	private static final String PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
	/**
	 * If true, this class will begin playback and open
	 * {@link AudioPlayerActivity}, false will close the class after playback,
	 * which is what happens when a user starts playing something from an
	 * app-widget
	 */
	public static final String OPEN_AUDIO_PLAYER = null;
	/**
	 * Gather the intent action and extras
	 */
	private Intent mIntent;

	private SearchLoader mLoader;
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
		MusicUtils.bindToService(this, this);
		// Initialize the intent
		mIntent = getIntent();
		mLoader = new SearchLoader(this);
		mVoiceQuery = StringUtils.capitalize(mIntent.getStringExtra(SearchManager.QUERY));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		// Check for a voice query
		if (mIntent.getAction() != null && mIntent.getAction().equals(PLAY_FROM_SEARCH)) {
			mLoader.execute(mVoiceQuery, this);
		} else {
			//sHandler.post(new AsyncHandler(this));
			String requestedMimeType = mIntent.getStringExtra(Config.MIME_TYPE);
			long id = mIntent.getLongExtra(Config.ID, -1L);
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
					long[] ids = ApolloUtils.readSerializedIDs(mIntent.getStringExtra(Config.IDS));
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
					String folder = "%" + mIntent.getStringExtra(Config.NAME);
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
	public void onServiceDisconnected() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
		MusicUtils.unbindFromService(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Song> songs) {
		// If the user searched for a playlist or genre, this list will
		// return empty
		if (songs.isEmpty()) {
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
		String song = songs.get(0).getName();
		String album = songs.get(0).getAlbum();
		String artist = songs.get(0).getArtist();
		// This tripes as the song, album, and artist Id
		long id = songs.get(0).getId();
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
			MusicUtils.playAll(this, mList, 0, mShouldShuffle);
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