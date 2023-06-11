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

package org.nuclearfog.apollo.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.provider.MediaStore;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.HomeActivity;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.activities.SearchActivity;
import org.nuclearfog.apollo.ui.activities.SettingsActivity;

/**
 * Various navigation helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class NavUtils {

	/**
	 * Opens the profile of an artist.
	 *
	 * @param context    The {@link Context} to use.
	 * @param artistName The name of the artist
	 */
	public static void openArtistProfile(Context context, String artistName) {
		// Create a new bundle to transfer the artist info
		Bundle bundle = new Bundle();
		bundle.putLong(Config.ID, MusicUtils.getIdForArtist(context, artistName));
		bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
		bundle.putString(Config.ARTIST_NAME, artistName);
		// Create the intent to launch the profile activity
		Intent intent = new Intent(context, ProfileActivity.class);
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	/**
	 * Opens the profile of an album.
	 *
	 * @param context    The {@link Context} to use.
	 * @param albumName  The name of the album
	 * @param artistName The name of the album artist
	 * @param albumId    The id of the album
	 */
	public static void openAlbumProfile(Context context, String albumName, String artistName, long albumId) {
		// Create a new bundle to transfer the album info
		Bundle bundle = new Bundle();
		bundle.putString(Config.ALBUM_YEAR, MusicUtils.getReleaseDateForAlbum(context, albumId));
		bundle.putString(Config.ARTIST_NAME, artistName);
		bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
		bundle.putLong(Config.ID, albumId);
		bundle.putString(Config.NAME, albumName);
		// Create the intent to launch the profile activity
		Intent intent = new Intent(context, ProfileActivity.class);
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	/**
	 * Opens the sound effects panel or DSP manager in CM
	 *
	 * @param activity The {@link Activity} to use.
	 */
	public static void openEffectsPanel(Activity activity) {
		try {
			Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
			effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getAudioSessionId());
			activity.startActivity(effects);
		} catch (ActivityNotFoundException notFound) {
			AppMsg.makeText(activity, activity.getString(R.string.no_effects_for_you), AppMsg.STYLE_ALERT);
			if (BuildConfig.DEBUG) {
				notFound.printStackTrace();
			}
		}
	}

	/**
	 * Opens to {@link SettingsActivity}.
	 *
	 * @param context The {@link Context} to use.
	 */
	public static void openSettings(Context context) {
		Intent intent = new Intent(context, SettingsActivity.class);
		context.startActivity(intent);
	}

	/**
	 * Opens to {@link SearchActivity}.
	 *
	 * @param activity The {@link Activity} to start a new Activity.
	 * @param query    The search query.
	 */
	public static void openSearch(Activity activity, String query) {
		Intent intent = new Intent(activity, SearchActivity.class);
		intent.putExtra(SearchManager.QUERY, query);
		activity.startActivity(intent);
	}

	/**
	 * Opens to {@link HomeActivity}.
	 *
	 * @param activity The {@link Activity} to use.
	 */
	public static void goHome(Activity activity) {
		Intent intent = new Intent(activity, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activity.startActivity(intent);
		activity.finish();
	}
}