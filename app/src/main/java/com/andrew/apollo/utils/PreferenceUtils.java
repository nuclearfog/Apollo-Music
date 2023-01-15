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

package com.andrew.apollo.utils;

import static com.andrew.apollo.MusicPlaybackService.REPEAT_NONE;
import static com.andrew.apollo.MusicPlaybackService.SHUFFLE_NONE;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.AlbumFragment;
import com.andrew.apollo.ui.fragments.ArtistFragment;
import com.andrew.apollo.ui.fragments.SongFragment;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import com.andrew.apollo.ui.fragments.profile.AlbumSongFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistSongFragment;

import java.util.LinkedList;
import java.util.List;

/**
 * A collection of helpers designed to get and set various preferences across
 * Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class PreferenceUtils {

	/* Default start page (Artist page) */
	public static final int DEFFAULT_PAGE = 3;
	/* Saves the last page the pager was on in {@link MusicBrowserPhoneFragment} */
	public static final String START_PAGE = "start_page";
	// Sort order for the artist list
	public static final String ARTIST_SORT_ORDER = "artist_sort_order";
	// Sort order for the artist song list
	public static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";
	// Sort order for the artist album list
	public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";
	// Sort order for the album list
	public static final String ALBUM_SORT_ORDER = "album_sort_order";
	// Sort order for the album song list
	public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";
	// Sort order for the song list
	public static final String SONG_SORT_ORDER = "song_sort_order";
	// Sets the type of layout to use for the artist list
	public static final String ARTIST_LAYOUT = "artist_layout";
	// Sets the type of layout to use for the album list
	public static final String ALBUM_LAYOUT = "album_layout";
	// Sets the type of layout to use for the recent list
	public static final String RECENT_LAYOUT = "recent_layout";
	// Key used to download images only on Wi-Fi
	public static final String ONLY_ON_WIFI = "only_on_wifi";
	// Key that gives permissions to download missing album covers
	public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";
	// Key that gives permissions to download missing artist images
	public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";
	// Key used to set the overall theme color
	public static final String DEFAULT_THEME_COLOR = "default_theme_color";
	public static final String LAYOUT_SIMPLE = "simple";
	public static final String LAYOUT_DETAILED = "detailed";
	public static final String LAYOUT_GRID = "grid";
	//
	private static final String MODE_SHUFFLE = "shufflemode";
	private static final String MODE_REPEAT = "repeatmode";
	private static final String POS_SEEK = "seekpos";
	private static final String POS_CURSOR = "curpos";
	private static final String HISTORY = "history";
	private static final String QUEUE = "queue";
	private static final String ID_CARD = "cardid";
	// equalizer settings
	private static final String FX_ENABLE = "fx_enable";
	private static final String FX_EQUALIZER_BANDS = "fx_equalizer_bands";
	private static final String FX_BASSBOOST = "fx_bassbost";
	private static final String FX_REVERB = "fx_reverb";
	private static final String FX_PREFER_EXT = "fx_prefer_external";
	private static final String LASTFM_API_KEY = "api_key";

	private static volatile PreferenceUtils sInstance;

	private SharedPreferences mPreferences;
	private int themeColor;
	private int startPage;

	/**
	 * Constructor for <code>PreferenceUtils</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	private PreferenceUtils(Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		themeColor = mPreferences.getInt(DEFAULT_THEME_COLOR, context.getResources().getColor(R.color.holo_green));
		startPage = mPreferences.getInt(START_PAGE, DEFFAULT_PAGE);
	}

	/**
	 * @param context The {@link Context} to use.
	 * @return A singleton of this class
	 */
	public static PreferenceUtils getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new PreferenceUtils(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * Returns the last page the user was on when the app was exited.
	 *
	 * @return The page to start on when the app is opened.
	 */
	public int getStartPage() {
		return startPage;
	}

	/**
	 * Saves the current page the user is on when they close the app.
	 *
	 * @param value The last page the pager was on when the onDestroy is called
	 *              in {@link MusicBrowserPhoneFragment}.
	 */
	public void setStartPage(int value) {
		startPage = value;
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(START_PAGE, value);
		editor.apply();
	}

	/**
	 * Sets the new theme color.
	 *
	 * @param value The new theme color to use.
	 */
	public void setDefaultThemeColor(int value) {
		themeColor = value;
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(DEFAULT_THEME_COLOR, value);
		editor.apply();
	}

	/**
	 * Returns the current theme color.
	 *
	 * @return The default theme color.
	 */
	public int getDefaultThemeColor() {
		return themeColor;
	}

	/**
	 * @return True if the user has checked to only download images on Wi-Fi,
	 * false otherwise
	 */
	public boolean onlyOnWifi() {
		return mPreferences.getBoolean(ONLY_ON_WIFI, true);
	}

	/**
	 * @return True if the user has checked to download missing album covers,
	 * false otherwise.
	 */
	public boolean downloadMissingArtwork() {
		return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTWORK, false);
	}

	/**
	 * @return True if the user has checked to download missing artist images,
	 * false otherwise.
	 */
	public boolean downloadMissingArtistImages() {
		return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTIST_IMAGES, false);
	}

	/**
	 * Saves the sort order for a list.
	 *
	 * @param key   Which sort order to change
	 * @param value The new sort order
	 */
	private void setSortOrder(String key, String value) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(key, value);
		editor.apply();
	}

	/**
	 * @return The sort order used for the artist list in {@link ArtistFragment}
	 */
	public String getArtistSortOrder() {
		// This is only to prevent return an invalid field name caused by bug BUGDUMP-21136
		String defaultSortKey = SortOrder.ArtistSortOrder.ARTIST_A_Z;
		String key = mPreferences.getString(ARTIST_SORT_ORDER, defaultSortKey);
		if (key.equals(SortOrder.ArtistSongSortOrder.SONG_FILENAME)) {
			key = defaultSortKey;
		}
		return key;
	}

	/**
	 * Sets the sort order for the artist list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistSortOrder(String value) {
		setSortOrder(ARTIST_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the artist song list in
	 * {@link ArtistSongFragment}
	 */
	public String getArtistSongSortOrder() {
		return mPreferences.getString(ARTIST_SONG_SORT_ORDER, SortOrder.ArtistSongSortOrder.SONG_A_Z);
	}

	/**
	 * Sets the sort order for the artist song list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistSongSortOrder(String value) {
		setSortOrder(ARTIST_SONG_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the artist album list in
	 * {@link ArtistAlbumFragment}
	 */
	public String getArtistAlbumSortOrder() {
		return mPreferences.getString(ARTIST_ALBUM_SORT_ORDER, SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
	}

	/**
	 * Sets the sort order for the artist album list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistAlbumSortOrder(String value) {
		setSortOrder(ARTIST_ALBUM_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the album list in {@link AlbumFragment}
	 */
	public String getAlbumSortOrder() {
		return mPreferences.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
	}

	/**
	 * Sets the sort order for the album list.
	 *
	 * @param value The new sort order
	 */
	public void setAlbumSortOrder(String value) {
		setSortOrder(ALBUM_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the album song in
	 * {@link AlbumSongFragment}
	 */
	public String getAlbumSongSortOrder() {
		return mPreferences.getString(ALBUM_SONG_SORT_ORDER, SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
	}

	/**
	 * get last playlist
	 *
	 * @return playlist
	 */
	public List<Long> getPlaylist() {
		List<Long> playList = new LinkedList<>();
		String trackQueue = mPreferences.getString(QUEUE, "");
		if (!trackQueue.isEmpty()) {
			int separatorPos = trackQueue.indexOf(";");
			int cut = 0;
			while (separatorPos != -1) {
				String part = trackQueue.substring(cut, separatorPos);
				long trackId = Long.parseLong(part, 16);
				cut = separatorPos + 1;
				playList.add(trackId);
				separatorPos = trackQueue.indexOf(";", cut);
			}
		}
		return playList;
	}

	/**
	 * get track history
	 *
	 * @return list of track numbers
	 */
	public List<Integer> getTrackHistory() {
		List<Integer> history = new LinkedList<>();
		String trackHistory = mPreferences.getString(HISTORY, "");
		if (!trackHistory.isEmpty()) {
			int separatorPos = trackHistory.indexOf(";");
			int cut = 0;
			while (separatorPos != -1) {
				String part = trackHistory.substring(cut, separatorPos);
				int num = Integer.parseInt(part, 16);
				cut = separatorPos + 1;
				history.add(num);
				separatorPos = trackHistory.indexOf(";", cut);
			}
		}
		return history;
	}

	/**
	 * get current card ID
	 *
	 * @return card ID
	 */
	public int getCardId() {
		return mPreferences.getInt(ID_CARD, -1);
	}

	/**
	 * get current cursor position
	 *
	 * @return cursor position
	 */
	public int getCursorPosition() {
		return mPreferences.getInt(POS_CURSOR, 0);
	}

	/**
	 * Sets the sort order for the album song list.
	 *
	 * @param value The new sort order
	 */
	public void setAlbumSongSortOrder(String value) {
		setSortOrder(ALBUM_SONG_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the song list in {@link SongFragment}
	 */
	public String getSongSortOrder() {
		return mPreferences.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
	}

	/**
	 * get last seek position
	 *
	 * @return position of the seekbar
	 */
	public long getSeekPosition() {
		return mPreferences.getLong(POS_SEEK, 0);
	}

	/**
	 * get status of the repeat mode
	 *
	 * @return integer mode {@link com.andrew.apollo.MusicPlaybackService#REPEAT_NONE#REPEAT_CURRENT#REPEAT_ALL}
	 */
	public int getRepeatMode() {
		return mPreferences.getInt(MODE_REPEAT, REPEAT_NONE);
	}

	/**
	 * get status of the shuffle mode
	 *
	 * @return integer mode {@link com.andrew.apollo.MusicPlaybackService#SHUFFLE_NONE#SHUFFLE_NORMAL#SHUFFLE_AUTO}
	 */
	public int getShuffleMode() {
		return mPreferences.getInt(MODE_SHUFFLE, SHUFFLE_NONE);
	}

	/**
	 * Sets the sort order for the song list.
	 *
	 * @param value The new sort order
	 */
	public void setSongSortOrder(String value) {
		setSortOrder(SONG_SORT_ORDER, value);
	}

	/**
	 * Saves the layout type for a list
	 *
	 * @param key   Which layout to change
	 * @param value The new layout type
	 */
	private void setLayoutType(String key, String value) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(key, value);
		editor.apply();
	}

	/**
	 * Sets the layout type for the artist list
	 *
	 * @param value The new layout type
	 */
	public void setArtistLayout(String value) {
		setLayoutType(ARTIST_LAYOUT, value);
	}

	/**
	 * Sets the layout type for the album list
	 *
	 * @param value The new layout type
	 */
	public void setAlbumLayout(String value) {
		setLayoutType(ALBUM_LAYOUT, value);
	}

	/**
	 * Sets the layout type for the recent list
	 *
	 * @param value The new layout type
	 */
	public void setRecentLayout(String value) {
		setLayoutType(RECENT_LAYOUT, value);
	}

	/**
	 * save current playlist
	 *
	 * @param playlist list of tracks
	 * @param cardId   list id
	 */
	public void setPlayList(List<Long> playlist, int cardId) {
		SharedPreferences.Editor editor = mPreferences.edit();
		StringBuilder buffer = new StringBuilder();
		for (long n : playlist) {
			buffer.append(Long.toHexString(n));
			buffer.append(";");
		}
		editor.putString(QUEUE, buffer.toString());
		editor.putInt(ID_CARD, cardId);
		editor.apply();
	}

	/**
	 * set track history
	 *
	 * @param history list of track history
	 */
	public void setHistory(List<Integer> history) {
		SharedPreferences.Editor editor = mPreferences.edit();
		StringBuilder buffer = new StringBuilder();
		for (long n : history) {
			buffer.append(Long.toHexString(n));
			buffer.append(";");
		}
		editor.putString(HISTORY, buffer.toString());
		editor.apply();
	}

	/**
	 * set current ursor position
	 *
	 * @param position cursor position
	 */
	public void setCursorPosition(int position) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(POS_CURSOR, position);
		editor.apply();
	}

	/**
	 * set last seekbar position
	 *
	 * @param seekPos seekbar position
	 */
	public void setSeekPosition(long seekPos) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(POS_SEEK, seekPos);
		editor.apply();
	}

	/**
	 * set repeat and shuffle mode
	 *
	 * @param repeatMode  repeat mode flags
	 * @param shuffleMode shuffle mode flags
	 */
	public void setRepeatAndShuffleMode(int repeatMode, int shuffleMode) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(MODE_REPEAT, repeatMode);
		editor.putInt(MODE_SHUFFLE, shuffleMode);
		editor.apply();
	}

	/**
	 * check if page is configured to show a simple layout
	 *
	 * @param which Which page to check: {@link #ARTIST_LAYOUT}, {@link #ALBUM_LAYOUT} or {@link #RECENT_LAYOUT}
	 * @return True if the layout type is the simple layout, false otherwise.
	 */
	public boolean isSimpleLayout(String which) {
		String result = mPreferences.getString(which, LAYOUT_GRID);
		return LAYOUT_SIMPLE.equals(result);
	}

	/**
	 * check if page is configured to show a detailled layout
	 *
	 * @param which Which page to check: {@link #ARTIST_LAYOUT}, {@link #ALBUM_LAYOUT} or {@link #RECENT_LAYOUT}
	 * @return True if the layout type is the detailled layout, false otherwise.
	 */
	public boolean isDetailedLayout(String which) {
		String result = mPreferences.getString(which, LAYOUT_GRID);
		return LAYOUT_DETAILED.equals(result);
	}

	/**
	 * check if audiofx is enabled
	 *
	 * @return true if audiofx is enabled
	 */
	public boolean isAudioFxEnabled() {
		return mPreferences.getBoolean(FX_ENABLE, false);
	}

	/**
	 * enable/disable audiofx
	 *
	 * @param enable true to enable audiofx
	 */
	public void setAudioFxEnabled(boolean enable) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putBoolean(FX_ENABLE, enable);
		editor.apply();
	}

	/**
	 * save new equalizer band setup
	 *
	 * @param bands array of band levels starting with the lowest frequency
	 */
	public void setEqualizerBands(int[] bands) {
		StringBuilder result = new StringBuilder();
		for (int band : bands)
			result.append(band).append(';');
		if (result.length() > 0)
			result.deleteCharAt(result.length() - 1);

		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(FX_EQUALIZER_BANDS, result.toString());
		editor.apply();
	}

	/**
	 * get equalizer band setup
	 *
	 * @return array of band levels starting with the lowest frequency
	 */
	public int[] getEqualizerBands() {
		String serializedBands = mPreferences.getString(FX_EQUALIZER_BANDS, "");
		if (serializedBands.isEmpty())
			return new int[0];

		String[] bands = serializedBands.split(";");
		int[] result = new int[bands.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Integer.parseInt(bands[i]);
		}
		return result;
	}

	/**
	 * set bass boost level
	 *
	 * @param level bass level from 0 to 1000
	 */
	public void setBassLevel(int level) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(FX_BASSBOOST, level);
		editor.apply();
	}

	/**
	 * get bass boost level
	 *
	 * @return bass level from 0 to 1000
	 */
	public int getBassLevel() {
		return mPreferences.getInt(FX_BASSBOOST, 0);
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level (room size)
	 */
	public void setReverbLevel(int level) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(FX_REVERB, level);
		editor.apply();
	}

	/**
	 * get reverb level
	 *
	 * @return reverb level (room size)
	 */
	public int getReverbLevel() {
		return mPreferences.getInt(FX_REVERB, 0);
	}

	/**
	 * check if external audio effect app is prefered
	 *
	 * @return true if external audio effect app is prefered
	 */
	public boolean isExternalAudioFxPrefered() {
		return mPreferences.getBoolean(FX_PREFER_EXT, false);
	}

	/**
	 * get LastFM API key
	 *
	 * @return API key string
	 */
	public String getApiKey() {
		return mPreferences.getString(LASTFM_API_KEY, "");
	}
}