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

/**
 * Several of the context menu items used in Apollo are reused. This class helps
 * keep things tidy.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class ContextMenuItems {

	/**
	 * Removes a single album from the recents pages
	 */
	public static final int REMOVE_FROM_RECENT = 0xDCFE73F7;

	/**
	 * Used to play the selected artist, album, song, playlist, or genre
	 */
	public static final int PLAY_SELECTION = 0x2DC71E3B;

	/**
	 * Used to add to the qeueue
	 */
	public static final int ADD_TO_QUEUE = 0xF8572568;

	/**
	 * Used to add to a playlist
	 */
	public static final int ADD_TO_PLAYLIST = 0x894845F9;

	/**
	 * Used to add to the favorites cache
	 */
	public static final int ADD_TO_FAVORITES = 0x8C114555;

	/**
	 * Used to create a new playlist
	 */
	public static final int NEW_PLAYLIST = 0x4D688CF9;

	/**
	 * Used to rename a playlist
	 */
	public static final int RENAME_PLAYLIST = 0xAFACA06E;

	/**
	 * Used to copy a playlist
	 */
	public static final int COPY_PLAYLIST = 0x146490AA;

	/**
	 * Used to add to a current playlist
	 */
	public static final int PLAYLIST_SELECTED = 0xC5A52C94;

	/**
	 * Used to show more content by an artist
	 */
	public static final int MORE_BY_ARTIST = 0xB1F37A7;

	/**
	 * Used to delete track(s)
	 */
	public static final int DELETE = 0xD6BD96E9;

	/**
	 * Used to set a track as a ringtone
	 */
	public static final int USE_AS_RINGTONE = 0x6733A94D;

	/**
	 * Used to remove a track from the favorites cache
	 */
	public static final int REMOVE_FROM_FAVORITES = 0xDD9FABC9;

	/**
	 * Used to remove a track from a playlist
	 */
	public static final int REMOVE_FROM_PLAYLIST = 0xC777843A;

	/**
	 * Used to remove a track from the queue
	 */
	public static final int REMOVE_FROM_QUEUE = 0x2A538C9D;

	/**
	 * Used to queue a track to be played next
	 */
	public static final int PLAY_NEXT = 0x4ACDEC66;

	/**
	 * Used to remove track from the popular
	 */
	public static final int REMOVE_FROM_POPULAR = 0xB06947EE;
}