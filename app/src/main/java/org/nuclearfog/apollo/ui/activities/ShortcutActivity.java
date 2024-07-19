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

import androidx.appcompat.app.AppCompatActivity;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.FavoritesLoader;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.async.loader.LastAddedLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.async.loader.PopularSongsLoader;
import org.nuclearfog.apollo.async.loader.SearchLoader;
import org.nuclearfog.apollo.model.Song;
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
public class ShortcutActivity extends AppCompatActivity implements ServiceBinderCallback {

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
	 * Used to shuffle the tracks or play them in order
	 */
	private boolean mShouldShuffle;
	private boolean shouldOpenAudioPlayer;
	/**
	 * Search query from a voice action
	 */
	private String mVoiceQuery;

	private AsyncCallback<List<Song>> onSongsLoaded = this::onSongsLoaded;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize the intent
		mIntent = getIntent();
		mLoader = new SearchLoader(this);
		shouldOpenAudioPlayer = mIntent.getBooleanExtra(OPEN_AUDIO_PLAYER, true);
		mVoiceQuery = StringUtils.capitalize(mIntent.getStringExtra(SearchManager.QUERY));
		MusicUtils.bindToService(this, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// bind activity to service
		MusicUtils.notifyForegroundStateChanged(this, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		MusicUtils.notifyForegroundStateChanged(this, false);
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		// Unbind from the service
		MusicUtils.unbindFromService(this);
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		// Check for a voice query
		if (mIntent.getAction() != null && mIntent.getAction().equals(PLAY_FROM_SEARCH)) {
			mLoader.execute(mVoiceQuery, onSongsLoaded);
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
					ArtistSongLoader artistSongLoader = new ArtistSongLoader(this);
					artistSongLoader.execute(id, onSongsLoaded);
					break;

				case MediaStore.Audio.Albums.CONTENT_TYPE:
					// Shuffle the album track list
					mShouldShuffle = true;
					// Get the album song list
					AlbumSongLoader albumSongLoader = new AlbumSongLoader(this);
					albumSongLoader.execute(id, onSongsLoaded);
					break;

				case MediaStore.Audio.Genres.CONTENT_TYPE:
					// Shuffle the genre track list
					mShouldShuffle = true;
					// Get the genre song list
					String ids = mIntent.getStringExtra(Config.IDS);
					GenreSongLoader genreSongLoader = new GenreSongLoader(this);
					genreSongLoader.execute(ids, onSongsLoaded);
					break;

				case MediaStore.Audio.Playlists.CONTENT_TYPE:
					// Don't shuffle the playlist track list
					mShouldShuffle = false;
					// Get the playlist song list
					PlaylistSongLoader playlistLoader = new PlaylistSongLoader(this);
					playlistLoader.execute(id, onSongsLoaded);
					break;

				case ProfileActivity.PAGE_FAVORIT:
					// Don't shuffle the Favorites track list
					mShouldShuffle = false;
					// Get the Favorites song list
					FavoritesLoader favoriteLoader = new FavoritesLoader(this);
					favoriteLoader.execute(null, onSongsLoaded);
					break;

				case ProfileActivity.PAGE_MOST_PLAYED:
					// Don't shuffle the popular track list
					mShouldShuffle = false;
					// Get the popular song list
					PopularSongsLoader popularLoader = new PopularSongsLoader(this);
					popularLoader.execute(null, onSongsLoaded);
					break;

				case ProfileActivity.PAGE_FOLDERS:
					// Don't shuffle the folders track list
					mShouldShuffle = false;
					// get folder path
					String folder = mIntent.getStringExtra(Config.NAME);
					// Get folder song list
					FolderSongLoader folderSongLoader = new FolderSongLoader(this);
					folderSongLoader.execute(folder, onSongsLoaded);
					break;

				case ProfileActivity.PAGE_LAST_ADDED:
					// Don't shuffle the last added track list
					mShouldShuffle = false;
					// Get the Last added song list
					LastAddedLoader lastAddedLoader = new LastAddedLoader(this);
					lastAddedLoader.execute(null, onSongsLoaded);
					break;
			}
		}
	}

	/**
	 * set song ID list after loading asyncronously
	 *
	 * @param items list of songs
	 */
	private void onSongsLoaded(List<Song> items) {
		long[] mList = new long[items.size()];
		for (int i = 0; i < mList.length; i++) {
			mList[i] = items.get(i).getId();
		}
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