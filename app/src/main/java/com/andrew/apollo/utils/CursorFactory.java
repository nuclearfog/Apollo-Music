package com.andrew.apollo.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.MediaColumns;

import androidx.annotation.NonNull;

import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.FavoritesStore.FavoriteColumns;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.provider.RecentStore.RecentStoreColumns;

import java.io.File;
import java.util.List;

import static android.provider.MediaStore.VOLUME_EXTERNAL;
import static com.andrew.apollo.provider.RecentStore.RecentStoreColumns.NAME;
import static com.andrew.apollo.provider.RecentStore.RecentStoreColumns.TIMEPLAYED;

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
     * SQL Projection of an album row
     */
    @SuppressLint("InlinedApi")
    public static final String[] ALBUM_COLUMN = {
            Albums._ID,
            Albums.ALBUM,
            Albums.ARTIST,
            Albums.NUMBER_OF_SONGS,
            Albums.FIRST_YEAR,
            Albums.ARTIST_ID
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
            Playlists.Members.DURATION
    };

    /**
     * projection for genre columns
     */
    private static final String[] GENRE_COLUMNS = {
            Genres._ID,
            Genres.NAME
    };

    @SuppressLint("InlinedApi")
    private static final String[] SEARCH_COLUMNS = {
            Media._ID,
            Media.MIME_TYPE,
            Media.ARTIST,
            Media.ALBUM,
            Media.TITLE,
            "data1",
            "data2"
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
    private static final String[] FOLDER_PROJECTION = {
            Media.DATA
    };

    /**
     * condition to filter empty names
     */
    private static final String GENRE_SELECT = Genres.NAME + "!=''";


    /**
     * Selection to filter songs with empty name
     */
    @SuppressLint("InlinedApi")
    public static final String TRACK_FILTER_SELECT = Media.IS_MUSIC + "=1 AND " + Media.TITLE + "!=''";

    /**
     * selection for albums of an artist
     */
    @SuppressLint("InlinedApi")
    private static final String ARTIST_ALBUM_SELECT = Albums.ARTIST_ID + "=?";

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
    private static final String TRACK_PATH_SELECT = Media.DATA + "=?";

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
    private static final String PLAYLIST_SELECT = Playlists.NAME + "=?";

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
     *
     */
    private static final Uri CARD_URI = Uri.parse("content://media/external/fs_id");


    private CursorFactory() {
    }


    /**
     * create a cursor to get all songs
     *
     * @return cursor with song information
     */
    public static Cursor makeTrackCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        String sort = PreferenceUtils.getInstance(context).getSongSortOrder();
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_FILTER_SELECT, null, sort);
    }

    /**
     * create a cursor to get album history information
     *
     * @return cursor with album informatiom
     */
    public static Cursor makeRecentCursor(Context context) {
        SQLiteDatabase database = RecentStore.getInstance(context).getReadableDatabase();
        return database.query(NAME, RECENT_COLUMNS, null, null, null, null, RECENT_ORDER);
    }

    /**
     * create a cursor to get all tracks of a playlist
     *
     * @param id playlist ID
     * @return cursor with tracks of a playlist
     */
    @SuppressLint("InlinedApi")
    public static Cursor makePlaylistSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        Uri content = Playlists.Members.getContentUri(VOLUME_EXTERNAL, id);
        return resolver.query(content, PLAYLIST_TRACK_COLUMNS, null, null, PLAYLIST_TRACK_ORDER);
    }

    /**
     * create a cursor to get all playlists
     *
     * @return cursor with playlist information
     */
    public static Cursor makePlaylistCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        return resolver.query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, null, null, PLAYLIST_ORDER);
    }

    /**
     * create cursor for a playlist item
     *
     * @param name name of the playlist
     * @return cursor
     */
    public static Cursor makePlaylistCursor(Context context, @NonNull String name) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {name};
        return resolver.query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, PLAYLIST_SELECT, args, null);
    }

    /**
     * create a cursor to search for tracks
     *
     * @param search search string matching a name
     * @return cursor with track information matching the search string
     */
    public static Cursor makeTrackSearchCursor(Context context, @NonNull String search) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {'%' + search + '%'};
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_MATCH, args, null);
    }

    /**
     * creates a cursor to search for albums
     *
     * @param search search string matching a name
     * @return cursor with albums matching the search string
     */
    public static Cursor makeAlbumSearchCursor(Context context, @NonNull String search) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {'%' + search + '%'};
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_MATCH, args, null);
    }

    /**
     * creates a cursor to search for artists
     *
     * @param search search string
     * @return cursor with artits matching the search string
     */
    public static Cursor makeArtistSearchCursor(Context context, @NonNull String search) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {'%' + search + '%'};
        return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, ARTIST_MATCH, args, null);
    }

    /**
     * create a cursor to get last added songs
     *
     * @return Cursor with song information
     */
    public static Cursor makeLastAddedCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        String[] select = {Long.toString(System.currentTimeMillis() / 1000 - 2419200)};
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, LAST_ADDED_SELECT, select, ORDER_TIME);
    }

    /**
     * create a cursor to get all songs of a genre
     *
     * @param id genre ID
     * @return cursor with song information
     */
    public static Cursor makeGenreSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        Uri media = Genres.Members.getContentUri("external", id);
        return resolver.query(media, TRACK_COLUMNS, TRACK_FILTER_SELECT, null, GENRE_TRACK_ORDER);
    }

    /**
     * create a cursor to parse all genre types
     *
     * @return cursor with genre information
     */
    public static Cursor makeGenreCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        return resolver.query(Genres.EXTERNAL_CONTENT_URI, GENRE_COLUMNS, GENRE_SELECT, null, GENRE_ORDER);
    }

    /**
     * create cursor to pare a specific folder with tracks
     *
     * @param folder folder where to search tracks. Tracks in Sub-Folders should be ignored
     * @return cursor with track information matching the path
     */
    public static Cursor makeFolderSongCursor(Context context, File folder) {
        ContentResolver contentResolver = context.getContentResolver();

        String[] args = {folder.toString() + "%"};
        return contentResolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, FOLDER_TRACK_SELECT, args, FOLDER_TRACKS_ORDER);
    }

    /**
     * create cursor to get all audio files and their paths
     *
     * @return cursor with all songs
     */
    public static Cursor makeFolderCursor(Context context) {
        ContentResolver contentResolver = context.getContentResolver();

        String sortOrder = PreferenceUtils.getInstance(context).getSongSortOrder();
        return contentResolver.query(Media.EXTERNAL_CONTENT_URI, FOLDER_PROJECTION, TRACK_FILTER_SELECT, null, sortOrder);
    }

    /**
     * create a cursor to parse a table with favorite lists
     *
     * @return cursor with favorite list information
     */
    public static Cursor makeFavoritesCursor(Context context) {
        SQLiteDatabase data = FavoritesStore.getInstance(context).getReadableDatabase();
        return data.query(FavoriteColumns.NAME, FAVORITE_COLUMNS, null, null, null, null, FAV_ORDER);
    }

    /**
     * create a cursor to parse artist table
     *
     * @return cursor with artist information
     */
    public static Cursor makeArtistCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        String order = PreferenceUtils.getInstance(context).getArtistSortOrder();
        return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, null, null, order);
    }

    /**
     * create a cursor for an artist row
     *
     * @param name name of the artist
     * @return cursor with artist information
     */
    public static Cursor makeArtistCursor(Context context, @NonNull String name) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {name};
        return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMNS, ARTIST_SELECT, args, null);
    }

    /**
     * create a cursor to get a table with all albums from an artist
     *
     * @param id ID of the artist
     * @return cursor with album information
     */
    public static Cursor makeArtistAlbumCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {Long.toString(id)};
        String order = PreferenceUtils.getInstance(context).getArtistAlbumSortOrder();
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ARTIST_ALBUM_SELECT, args, order);
    }

    /**
     * create a cursor to get all songs from an artist
     *
     * @return cursor with song information
     */
    public static Cursor makeArtistSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {Long.toString(id)};
        String order = PreferenceUtils.getInstance(context).getArtistSongSortOrder();
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ARTIST_SONG_SELECT, args, order);
    }

    /**
     * create a cursor to get all albums
     *
     * @return cursor with album table
     */
    public static Cursor makeAlbumCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();

        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, null, null, sortOrder);
    }

    /**
     * create a cursor to get all song information from an album
     *
     * @param id Album ID
     * @return cursor with song information
     */
    public static Cursor makeAlbumSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {Long.toString(id)};
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSongSortOrder();
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ALBUM_SONG_SELECT, args, sortOrder);
    }

    /**
     * create a cursor with a single album item
     *
     * @param id album ID
     * @return cursor with an item
     */
    public static Cursor makeAlbumCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {Long.toString(id)};
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_ID_SELECT, args, sortOrder);
    }

    /**
     * Creates cursor to search for albums
     *
     * @param album  album name
     * @param artist artist name of the album
     * @return Cursor with matching albums
     */
    public static Cursor makeAlbumCursor(Context context, @NonNull String album, @NonNull String artist) {
        ContentResolver resolver = context.getContentResolver();

        String[] args = {album, artist};
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_NAME_SELECT, args, sortOrder);
    }

    /**
     * creates cursor to search for tracks
     *
     * @param query The user's query.
     * @return The {@link Cursor} used to perform the search.
     */
    public static Cursor makeSearchCursor(Context context, String query) {
        ContentResolver resolver = context.getContentResolver();

        Uri media = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(query));
        return resolver.query(media, SEARCH_COLUMNS, null, null, null);
    }

    /**
     * creates a playlist cursor
     *
     * @return cursor with playlist information
     */
    public static Cursor makePlaylistCursor(ContentResolver resolver, Uri uri) {
        return resolver.query(uri, PLAYLIST_COUNT, null, null, null);
    }

    /**
     * creates cursor to search for a single track information
     *
     * @param id audio ID
     * @return cursor with track information
     */
    public static Cursor makeTrackCursor(Context context, long id) {
        String[] args = {Long.toString(id)};
        ContentResolver resolver = context.getContentResolver();

        return resolver.query(Media.EXTERNAL_CONTENT_URI, null, TRACK_ID_SELECT, args, null);
    }

    /**
     * creates cursor to search for a single track information
     *
     * @param path path to the audio file
     * @return cursor with track information
     */
    public static Cursor makeTrackCursor(Context context, String path) {
        String[] args = {path};
        ContentResolver resolver = context.getContentResolver();

        return resolver.query(Media.EXTERNAL_CONTENT_URI, null, TRACK_PATH_SELECT, args, null);
    }

    /**
     * creates cursor to search for a single track information
     *
     * @param path path to the audio file
     * @return cursor with track information
     */
    public static Cursor makeTrackCursor(Context context, Uri path) {
        ContentResolver resolver = context.getContentResolver();

        return resolver.query(path, null, TRACK_PATH_SELECT, null, null);
    }

    /**
     * creates a cursor to seach for track information
     *
     * @param ids query with track IDs
     * @return cursor with track information
     */
    public static Cursor makeTrackListCursor(Context context, long[] ids) {
        StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < ids.length; i++) {
            selection.append(ids[i]);
            if (i < ids.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Media.EXTERNAL_CONTENT_URI, AUDIO_COLUMNS, selection.toString(), null, null);
    }

    /**
     * creates a cursor to get current track queue
     *
     * @param ids query with track IDs
     * @return cursor with track information
     */
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
    public static Cursor makeCardCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(CARD_URI, null, null, null, null);
    }
}