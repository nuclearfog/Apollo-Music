/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers All rights
 * reserved. Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met: - Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following disclaimer. -
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. THIS SOFTWARE IS
 * PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.andrew.apollo.lastfm;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for Album related API calls and Album Bean.
 *
 * @author Janni Kovacs
 */
public class Album extends MusicEntry {

	protected final static ItemFactory<Album> FACTORY = new AlbumFactory();

	private String artist;

	private Album() {
		super("", "");
		this.artist = "";
	}

	/**
	 * Get the metadata for an album on Last.fm using the album name or a
	 * musicbrainz id. See playlist.fetch on how to get the album playlist.
	 *
	 * @param artist      Artist's name
	 * @param albumOrMbid Album name or MBID
	 * @return Album metadata
	 */
	public static Album getInfo(String artist, String albumOrMbid) {
		return getInfo(artist, albumOrMbid, null);
	}

	/**
	 * Get the metadata for an album on Last.fm using the album name or a
	 * musicbrainz id. See playlist.fetch on how to get the album playlist.
	 *
	 * @param artist      Artist's name
	 * @param albumOrMbid Album name or MBID
	 * @param username    The username for the context of the request. If supplied,
	 *                    the user's playcount for this album is included in the
	 *                    response.
	 * @return Album metadata
	 */
	public static Album getInfo(String artist, String albumOrMbid, String username) {
		Map<String, String> params = new HashMap<>();
		params.put("artist", artist);
		params.put("album", albumOrMbid);
		MapUtilities.nullSafePut(params, "username", username);
		Result result = Caller.getInstance().call("album.getInfo", params);
		return ResponseBuilder.buildItem(result, Album.class);
	}

	private final static class AlbumFactory implements ItemFactory<Album> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Album createItemFromElement(DomElement element) {
			if (element == null) {
				return null;
			}
			Album album = new Album();
			MusicEntry.loadStandardInfo(album, element);
			if (element.hasChild("artist")) {
				album.artist = element.getChild("artist").getChildText("name");
				if (album.artist == null) {
					album.artist = element.getChildText("artist");
				}
			}
			return album;
		}
	}
}