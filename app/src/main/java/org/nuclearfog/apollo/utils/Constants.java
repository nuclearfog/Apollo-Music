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

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;

import android.os.Build;

/**
 * App-wide constants.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public final class Constants {

	/**
	 * link to the source code repository
	 */
	public static final String SOURCE_URL = "https://github.com/nuclearfog/Apollo-Music";

	/**
	 * The ID of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String ID = "id";
	/**
	 * a group of IDs
	 */
	public static final String IDS = "ids";
	/**
	 * The name of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String NAME = "name";
	/**
	 * The name of an artist passed to the profile activity
	 */
	public static final String ARTIST_NAME = "artist_name";
	/**
	 * The year an album was released passed to the profile activity
	 */
	public static final String ALBUM_YEAR = "album_year";
	/**
	 * The MIME type passed to a the profile activity
	 */
	public static final String MIME_TYPE = "mime_type";
	/**
	 * path to a music folder
	 */
	public static final String FOLDER = "folder_path";
	/**
	 * key used for contextmenu to add IDs to playlist entries
	 */
	public static final String PLAYLIST_ID = "context_playlist_id";
	/**
	 * maximal scroll speed when dragging a list element
	 */
	public static final float DRAG_DROP_MAX_SPEED = 3.0f;

	public static final float OPACITY_HIDDEN = 0.4f;

	/**
	 * permissions used for Android 6+
	 */
	public static final String[] PERMISSIONS;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			PERMISSIONS = new String[]{READ_MEDIA_AUDIO, READ_MEDIA_IMAGES, POST_NOTIFICATIONS};
		} else {
			PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE};
		}
	}

	/* This class is never initiated. */
	private Constants() {
	}
}