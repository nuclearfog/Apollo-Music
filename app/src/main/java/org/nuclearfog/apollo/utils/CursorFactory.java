package org.nuclearfog.apollo.utils;

import static android.provider.MediaStore.VOLUME_EXTERNAL;
import static org.nuclearfog.apollo.store.RecentStore.RecentStoreColumns.NAME;
import static org.nuclearfog.apollo.store.RecentStore.RecentStoreColumns.TIMEPLAYED;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.MediaColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.FavoritesStore.FavoriteColumns;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.store.PopularStore.PopularColumns;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.store.RecentStore.RecentStoreColumns;

import java.util.List;

/**
 * class to create MediaStore cursor to access all music files
 *
 * @author nuclearfog
 */
public class CursorFactory {

	/**
	 * default sort order
	 */
	private static final String DEF_SORT = " DESC";

	/**
	 * custom android version dependent column name
	 */
	private static final String ALBUM_ID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
			Artists.Albums.ALBUM_ID : BaseColumns._ID;

	/**
	 * SQL Projection of an album row
	 */
	@SuppressLint("InlinedApi")
	public static final String[] ALBUM_COLUMN = {
			Albums._ID,
			Albums.ALBUM,
			Albums.ARTIST,
			Albums.NUMBER_OF_SONGS,
			Albums.FIRST_YEAR,
			Albums.ARTIST_ID,
			Albums.ALBUM_ART
	};

	/**
	 * SQL Projection of an album row
	 */
	@SuppressLint("InlinedApi")
	public static final String[] ARTIST_ALBUM_COLUMN = {
			ALBUM_ID,
			Artists.Albums.ALBUM,
			Artists.Albums.ARTIST,
			Artists.Albums.NUMBER_OF_SONGS,
			Artists.Albums.FIRST_YEAR,
			Artists.Albums.ARTIST_ID
	};

	/**
	 * SQL Projection to get song information in a fixed order
	 */
	@SuppressLint("InlinedApi")
	public static final String[] TRACK_COLUMNS = {
			Media._ID,
			Media.TITLE,
			Media.ARTIST,
			Media.ALBUM,
			Media.DURATION,
			Media.DATA,
			Media.MIME_TYPE
	};

	/**
	 * projection of the column
	 */
	public static final String[] ARTIST_COLUMNS = {
			Artists._ID,
			Artists.ARTIST,
			Artists.NUMBER_OF_ALBUMS,
			Artists.NUMBER_OF_TRACKS
	};

	/**
	 * projection of recent tracks
	 */
	public static final String[] RECENT_COLUMNS = {
			RecentStoreColumns.ID,
			RecentStoreColumns.ALBUMNAME,
			RecentStoreColumns.ARTISTNAME,
			RecentStoreColumns.ALBUMSONGCOUNT,
			RecentStoreColumns.ALBUMYEAR,
			RecentStoreColumns.TIMEPLAYED
	};

	/**
	 * Definition of the Columns to get from database
	 */
	public static final String[] FAVORITE_COLUMNS = {
			FavoriteColumns.ID,
			FavoriteColumns.SONGNAME,
			FavoriteColumns.ALBUMNAME,
			FavoriteColumns.ARTISTNAME,
			FavoriteColumns.PLAYCOUNT,
			FavoriteColumns.DURATION
	};

	/**
	 *
	 */
	public static final String[] MOSTPLAYED_COLUMNS = {
			PopularColumns.ID,
			PopularColumns.SONGNAME,
			PopularColumns.ALBUMNAME,
			PopularColumns.ARTISTNAME,
			PopularColumns.PLAYCOUNT,
			PopularColumns.DURATION
	};

	/**
	 * Projection of the columns
	 */
	public static final String[] PLAYLIST_COLUMNS = {
			Playlists._ID,
			Playlists.NAME
	};


	@SuppressLint("InlinedApi")
	public static final String[] PLAYLIST_TRACK_COLUMNS = {
			Playlists.Members.AUDIO_ID,
			Playlists.Members.TITLE,
			Playlists.Members.ARTIST,
			Playlists.Members.ALBUM,
			Playlists.Members.DURATION,
			Playlists.Members.PLAY_ORDER
	};

	/**
	 * projection for genre columns
	 */
	private static final String[] GENRE_COLUMNS = {
			Genres._ID,
			Genres.NAME
	};

	/**
	 * projection for track search
	 */
	@SuppressLint("InlinedApi")
	private static final String[] SEARCH_COLUMNS = {
			Media._ID,
			Media.ARTIST,
			Media.ALBUM,
			Media.TITLE,
			Media.DURATION
	};

	/**
	 * column selection for audio rows
	 */
	private static final String[] AUDIO_COLUMNS = {
			AudioColumns._ID,
			AudioColumns.DATA,
			AudioColumns.ALBUM_ID
	};

	/**
	 *
	 */
	@SuppressLint("InlinedApi")
	public static final String[] NP_COLUMNS = {
			AudioColumns._ID,
			AudioColumns.TITLE,
			AudioColumns.ARTIST,
			AudioColumns.ALBUM,
			AudioColumns.DURATION
	};

	/**
	 * column selection of get playlist count
	 */
	private static final String[] PLAYLIST_COUNT = {
			"COUNT(" + Playlists.Members.AUDIO_ID + ")"
	};

	/**
	 * projection for music folder
	 */
	private static final String[] FOLDER_COLUMNS = {
			Media.DATA,
			Media._ID
	};

	/**
	 * projection for external sd card IDs
	 */
	private static final String[] CARD_COLUMNS = {
			"fsid"
	};

	/**
	 * condition to filter empty names
	 */
	private static final String GENRE_SELECT = Genres.NAME + "!=''";

	/**
	 * condition to filter only valid recent albums
	 */
	private static final String RECENT_SELECT = RecentStoreColumns.ID + ">=0";

	/**
	 * Selection to filter songs with empty name
	 */
	@SuppressLint("InlinedApi")
	public static final String TRACK_FILTER_SELECT = Media.IS_MUSIC + "=1 AND " + Media.TITLE + "!=''";

	/**
	 *
	 */
	public static final String LAST_ADDED_SELECT = TRACK_FILTER_SELECT + " AND " + Media.DATE_ADDED + ">?";

	/**
	 * folder track selection
	 */
	private static final String FOLDER_TRACK_SELECT = TRACK_FILTER_SELECT + " AND " + Media.DATA + " LIKE ?";

	/**
	 * SQL selection
	 */
	private static final String ARTIST_SONG_SELECT = TRACK_FILTER_SELECT + " AND " + Media.ARTIST_ID + "=?";

	/**
	 * SQL Query
	 */
	private static final String ALBUM_SONG_SELECT = TRACK_FILTER_SELECT + " AND " + Media.ALBUM_ID + "=?";

	/**
	 * select specific album matching artist and name
	 */
	private static final String ALBUM_NAME_SELECT = Albums.ALBUM + "=? AND " + Albums.ARTIST + "=?";

	/**
	 * select track matching an audio ID
	 */
	private static final String TRACK_ID_SELECT = MediaColumns._ID + "=?";

	/**
	 * select track matching an audio ID
	 */
	private static final String TRACK_PATH_SELECT = Media.DATA + " LIKE ?";

	/**
	 * select specific album matching artist and name
	 */
	private static final String ALBUM_ID_SELECT = Albums._ID + "=?";

	/**
	 * select specific artist matching name
	 */
	private static final String ARTIST_SELECT = Artists.ARTIST + "=?";

	/**
	 * select specific artist matching name
	 */
	private static final String PLAYLIST_NAME_SELECT = Playlists.NAME + "=?";

	/**
	 * select playlist by ID
	 */
	private static final String PLAYLIST_ID_SELECT = Playlists._ID + "=?";

	/**
	 * selection to find artist name matching search
	 */
	private static final String ARTIST_MATCH = Artists.ARTIST + " LIKE ?";

	/**
	 * selection to find album name matching search
	 */
	private static final String ALBUM_MATCH = Albums.ALBUM + " LIKE ?";

	/**
	 * selection to find track title matching search
	 */
	private static final String TRACK_MATCH = Media.TITLE + " LIKE ?";

	/**
	 * default order of playlist tracks
	 */
	private static final String PLAYLIST_TRACK_ORDER = Playlists.Members.PLAY_ORDER;

	/**
	 * default order of playlists
	 */
	private static final String PLAYLIST_ORDER = Playlists.NAME;

	/**
	 * default track order of a genre
	 */
	private static final String GENRE_TRACK_ORDER = Media.DEFAULT_SORT_ORDER;

	/**
	 * sort genres by name
	 */
	private static final String GENRE_ORDER = Genres.DEFAULT_SORT_ORDER;

	/**
	 * sort recent played audio tracks
	 */
	private static final String RECENT_ORDER = TIMEPLAYED + DEF_SORT;

	/**
	 * sort folder tracks
	 */
	private static final String FOLDER_TRACKS_ORDER = Media.TRACK + "," + Media.TITLE;

	/**
	 * default order to sort last added tracks
	 */
	public static final String ORDER_TIME = Media.DATE_ADDED + DEF_SORT;

	/**
	 * SQLite sport order
	 */
	public static final String FAV_ORDER = FavoriteColumns.PLAYCOUNT + DEF_SORT;

	/**
	 * SQLite sport order
	 */
	public static final String MP_ORDER = PopularColumns.PLAYCOUNT + DEF_SORT;

	/**
	 *
	 */
	private static final Uri CARD_URI = Uri.parse("content://media/external/fs_id");


	private CursorFactory() {
	}


	/**
	 * create a cursor to get all songs with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @return cursor with song information
	 */
	@Nullable
	public static Cursor makeTrackCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		String sort = PreferenceUtils.getInstance(context).getSongSortOrder();
		return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_FILTER_SELECT, null, sort);
	}

	/**
	 * create a cursor to get album history information with fixed column order
	 * {@link #RECENT_COLUMNS}
	 *
	 * @return cursor with album informatiom
	 */
	@Nullable
	public static Cursor makeRecentCursor(Context context) {
		SQLiteDatabase database = RecentStore.getInstance(context).getReadableDatabase();
		return database.query(NAME, RECENT_COLUMNS, RECENT_SELECT, null, null, null, RECENT_ORDER);
	}

	/**
	 * create a cursor to get all tracks of a playlist with fixed column order
	 * {@link #PLAYLIST_TRACK_COLUMNS}
	 *
	 * @param id playlist ID
	 * @return cursor with tracks of a playlist
	 */
	@Nullable
	@SuppressLint("InlinedApi")
	public static Cursor makePlaylistSongCursor(Context context, long id) {
		ContentResolver resolver = context.getContentResolver();

		Uri content = Playlists.Members.getContentUri(VOLUME_EXTERNAL, id);
		return resolver.query(content, PLAYLIST_TRACK_COLUMNS, null, null, PLAYLIST_TRACK_ORDER);
	}

	/**
	 * create a cursor to get all playlists with fixed column order
	 * {@link #PLAYLIST_COLUMNS}
	 *
	 * @return cursor with playlist information
	 */
	@Nullable
	public static Cursor makePlaylistCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		return resolver.query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, null, null, PLAYLIST_ORDER);
	}

	/**
	 * create cursor for a playlist item with fixed column order
	 * {@link #PLAYLIST_COLUMNS}
	 *
	 * @param name name of the playlist
	 * @return cursor with playlist information
	 */
	@Nullable
	public static Cursor makePlaylistCursor(Context context, @NonNull String name) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {name};
		return resolver.query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, PLAYLIST_NAME_SELECT, args, null);
	}

	/**
	 * create a cursor for a single playlist with fixed column order
	 * {@link #PLAYLIST_COLUMNS}
	 *
	 * @param id ID of the playlist
	 * @return cursor with playlist information
	 */
	@Nullable
	public static Cursor makePlaylistCursor(Context context, long id) {
		String[] param = {String.valueOf(id)};
		ContentResolver resolver = context.getContentResolver();
		return resolver.query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, PLAYLIST_ID_SELECT, param, Playlists.NAME);
	}

	/**
	 * create a cursor to search for tracks with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @param search search string matching a name
	 * @return cursor with track information matching the search string
	 */
	@Nullable
	public static Cursor makeTrackSearchCursor(Context context, @NonNull String search) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {'%' + search + '%'};
		return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_MATCH, args, null);
	}

	/**
	 * creates a cursor to search for albums with fixed column order
	 * {@link #ALBUM_COLUMN}
	 *
	 * @param search search string matching a name
	 * @return cursor with albums matching the search string
	 */
	@Nullable
	public static Cursor makeAlbumSearchCursor(Context context, @NonNull String search) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {'%' + search + '%'};
		return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_MATCH, args, null);
	}

	/**
	 * creates a cursor to search for artists with fixed column order
	 * {@link #ARTIST_COLUMNS}
	 *
	 * @param search search string
	 * @return cursor with artits matching the search string
	 */
	@Nullable
	public static Cursor makeArtistSearchCursor(Context context, @NonNull String search) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {'%' + search + '%'};
		return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, ARTIST_MATCH, args, null);
	}

	/**
	 * create a cursor to get last added songs with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @return Cursor with song information
	 */
	@Nullable
	public static Cursor makeLastAddedCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		String[] select = {Long.toString(System.currentTimeMillis() / 1000 - 2419200)};
		return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, LAST_ADDED_SELECT, select, ORDER_TIME);
	}

	/**
	 * create a cursor to get all songs of a genre with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @param genreId genre ID
	 * @return cursor with song information
	 */
	@Nullable
	@SuppressLint("InlinedApi")
	public static Cursor makeGenreSongCursor(Context context, long genreId) {
		ContentResolver resolver = context.getContentResolver();

		Uri media = Genres.Members.getContentUri(VOLUME_EXTERNAL, genreId);
		return resolver.query(media, TRACK_COLUMNS, TRACK_FILTER_SELECT, null, GENRE_TRACK_ORDER);
	}

	/**
	 * create a cursor to parse all genre types with fixed column order
	 * {@link #GENRE_COLUMNS}
	 *
	 * @return cursor with genre information
	 */
	@Nullable
	public static Cursor makeGenreCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		return resolver.query(Genres.EXTERNAL_CONTENT_URI, GENRE_COLUMNS, GENRE_SELECT, null, GENRE_ORDER);
	}

	/**
	 * create cursor to pare a specific folder with tracks with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @param folderName folder where to search tracks. Tracks in Sub-Folders should be ignored
	 * @return cursor with track information matching the path
	 */
	@Nullable
	public static Cursor makeFolderSongCursor(Context context, String folderName) {
		ContentResolver contentResolver = context.getContentResolver();

		String[] args = {folderName + "%"};// todo filter subfolders from results, return only tracks from current folder
		return contentResolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, FOLDER_TRACK_SELECT, args, FOLDER_TRACKS_ORDER);
	}

	/**
	 * create cursor to get all audio files and their paths with fixed column order
	 * {@link #FOLDER_COLUMNS}
	 *
	 * @return cursor with all songs
	 */
	@Nullable
	public static Cursor makeFolderCursor(Context context) {
		ContentResolver contentResolver = context.getContentResolver();

		String sortOrder = PreferenceUtils.getInstance(context).getSongSortOrder();
		return contentResolver.query(Media.EXTERNAL_CONTENT_URI, FOLDER_COLUMNS, TRACK_FILTER_SELECT, null, sortOrder);
	}

	/**
	 * create a cursor to parse a table with favorite lists with fixed column order
	 * {@link #FAVORITE_COLUMNS}
	 *
	 * @return cursor with favorite list information
	 */
	@Nullable
	public static Cursor makeFavoritesCursor(Context context) {
		SQLiteDatabase data = FavoritesStore.getInstance(context).getReadableDatabase();
		return data.query(FavoriteColumns.NAME, FAVORITE_COLUMNS, null, null, null, null, FAV_ORDER);
	}

	/**
	 * create a cursor to parse a table with the most played tracks
	 *
	 * @return cursor with most played tracks
	 */
	@Nullable
	public static Cursor makePopularCursor(Context context) {
		SQLiteDatabase data = PopularStore.getInstance(context).getReadableDatabase();
		return data.query(PopularColumns.NAME, MOSTPLAYED_COLUMNS, null, null, null, null, MP_ORDER);
	}

	/**
	 * create a cursor to parse artist table with fixed column order
	 * {@link #ARTIST_COLUMNS}
	 *
	 * @return cursor with artist information
	 */
	@Nullable
	public static Cursor makeArtistCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		String order = PreferenceUtils.getInstance(context).getArtistSortOrder();
		return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, null, null, order);
	}

	/**
	 * create a cursor for an artist row with fixed column order
	 * {@link #ARTIST_COLUMNS}
	 *
	 * @param artistName name of the artist
	 * @return cursor with artist information
	 */
	@Nullable
	public static Cursor makeArtistCursor(Context context, @NonNull String artistName) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {artistName};
		return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, ARTIST_SELECT, args, null);
	}

	/**
	 * create a cursor to get a table with all albums from an artist with fixed column order
	 * {@link #ALBUM_COLUMN}
	 *
	 * @param artistId ID of the artist
	 * @return cursor with album information
	 */
	@Nullable
	@SuppressLint("InlinedApi")
	public static Cursor makeArtistAlbumCursor(Context context, long artistId) {
		ContentResolver resolver = context.getContentResolver();
		Uri uri = Artists.Albums.getContentUri(VOLUME_EXTERNAL, artistId);

		String order = PreferenceUtils.getInstance(context).getArtistAlbumSortOrder();
		return resolver.query(uri, ARTIST_ALBUM_COLUMN, null, null, order);
	}

	/**
	 * create a cursor to get all songs from an artist with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @param artistId ID of the artist
	 * @return cursor with song information
	 */
	@Nullable
	public static Cursor makeArtistSongCursor(Context context, long artistId) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {Long.toString(artistId)};
		String order = PreferenceUtils.getInstance(context).getArtistSongSortOrder();
		return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ARTIST_SONG_SELECT, args, order);
	}

	/**
	 * create a cursor to get all albums with fixed column order
	 * {@link #ALBUM_COLUMN}
	 *
	 * @return cursor with album table
	 */
	@Nullable
	public static Cursor makeAlbumCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();

		String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
		return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, null, null, sortOrder);
	}

	/**
	 * create a cursor to get all song information from an album with fixed column order
	 * {@link #TRACK_COLUMNS}
	 *
	 * @param id Album ID
	 * @return cursor with song information
	 */
	@Nullable
	public static Cursor makeAlbumSongCursor(Context context, long id) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {Long.toString(id)};
		String sortOrder = PreferenceUtils.getInstance(context).getAlbumSongSortOrder();
		return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ALBUM_SONG_SELECT, args, sortOrder);
	}

	/**
	 * create a cursor with a single album item with fixed column order
	 * {@link #ALBUM_COLUMN}
	 *
	 * @param id album ID
	 * @return cursor with an item
	 */
	@Nullable
	public static Cursor makeAlbumCursor(Context context, long id) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {Long.toString(id)};
		String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
		return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_ID_SELECT, args, sortOrder);
	}

	/**
	 * Creates cursor to search for albums with fixed column order
	 * {@link #ALBUM_COLUMN}
	 *
	 * @param album  album name
	 * @param artist artist name of the album
	 * @return Cursor with matching albums
	 */
	@Nullable
	public static Cursor makeAlbumCursor(Context context, @NonNull String album, @NonNull String artist) {
		ContentResolver resolver = context.getContentResolver();

		String[] args = {album, artist};
		String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
		return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_NAME_SELECT, args, sortOrder);
	}

	/**
	 * creates cursor to search for tracks with fixed column order
	 * {@link #SEARCH_COLUMNS}
	 *
	 * @param query The user's query.
	 * @return The {@link Cursor} used to perform the search.
	 */
	@Nullable
	public static Cursor makeSearchCursor(Context context, String query) {
		ContentResolver resolver = context.getContentResolver();

		Uri media = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(query));
		return resolver.query(media, SEARCH_COLUMNS, null, null, null);
	}

	/**
	 * creates a playlist cursor with fixed columns
	 * {@link #PLAYLIST_COUNT}
	 *
	 * @return cursor with playlist information
	 */
	@Nullable
	public static Cursor makePlaylistCursor(ContentResolver resolver, Uri uri) {
		return resolver.query(uri, PLAYLIST_COUNT, null, null, null);
	}

	/**
	 * creates cursor to search for a single track information
	 *
	 * @param trackId audio ID
	 * @return cursor with track information
	 */
	@Nullable
	public static Cursor makeTrackCursor(Context context, long trackId) {
		String[] args = {Long.toString(trackId)};
		ContentResolver resolver = context.getContentResolver();

		return resolver.query(Media.EXTERNAL_CONTENT_URI, null, TRACK_ID_SELECT, args, null);
	}

	/**
	 * creates cursor to search for a single track information
	 *
	 * @param path path to the audio file
	 * @return cursor with track information
	 */
	@Nullable
	public static Cursor makeTrackCursor(Context context, String path) {
		String[] args = {'%' + path + '%'};
		ContentResolver resolver = context.getContentResolver();

		return resolver.query(Media.EXTERNAL_CONTENT_URI, null, TRACK_PATH_SELECT, args, null);
	}

	/**
	 * creates cursor to search for a single track information
	 *
	 * @param path path to the audio file
	 * @return cursor with track information
	 */
	@Nullable
	public static Cursor makeTrackCursor(Context context, Uri path) {
		ContentResolver resolver = context.getContentResolver();

		return resolver.query(path, null, null, null, null);
	}

	/**
	 * creates a cursor to seach for track information with fixed columns
	 * {@link #AUDIO_COLUMNS}
	 *
	 * @param trackIds query with track IDs
	 * @return cursor with track information
	 */
	@Nullable
	public static Cursor makeTrackListCursor(Context context, long[] trackIds) {
		StringBuilder selection = new StringBuilder();
		selection.append(Media._ID + " IN (");
		for (int i = 0; i < trackIds.length; i++) {
			selection.append(trackIds[i]);
			if (i < trackIds.length - 1) {
				selection.append(",");
			}
		}
		selection.append(")");
		ContentResolver resolver = context.getContentResolver();
		return resolver.query(Media.EXTERNAL_CONTENT_URI, AUDIO_COLUMNS, selection.toString(), null, null);
	}

	/**
	 * creates a cursor to get current track queue with fixed columns
	 * {@link #NP_COLUMNS}
	 *
	 * @param ids query with track IDs
	 * @return cursor with track information
	 */
	@Nullable
	public static Cursor makeNowPlayingCursor(Context context, List<Long> ids) {
		StringBuilder selection = new StringBuilder();
		selection.append(MediaStore.Audio.Media._ID + " IN (");
		for (int i = 0; i < ids.size(); i++) {
			selection.append(ids.get(i));
			if (i < ids.size() - 1) {
				selection.append(",");
			}
		}
		selection.append(")");
		return context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, NP_COLUMNS, selection.toString(), null, Media._ID);
	}

	/**
	 *
	 */
	@Nullable
	public static Cursor makeCardCursor(Context context) {
		ContentResolver resolver = context.getContentResolver();
		return resolver.query(CARD_URI, CARD_COLUMNS, null, null, null);
	}
}