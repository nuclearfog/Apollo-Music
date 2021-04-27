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
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;

import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.FavoritesStore.FavoriteColumns;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.provider.RecentStore.RecentStoreColumns;

import java.io.File;

import static android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
import static com.andrew.apollo.provider.RecentStore.RecentStoreColumns.NAME;
import static com.andrew.apollo.provider.RecentStore.RecentStoreColumns.TIMEPLAYED;

/**
 * class to create MediaStore cursor to access all music files
 *
 * @author nuclearfog
 */
public class CursorCreator {

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
    public static final String[] ARTIST_COLUMN = {
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
            FavoriteColumns.PLAYCOUNT
    };

    /**
     * Projection of the columns
     */
    public static final String[] PLAYLIST_COLUMNS = {
            Playlists._ID,
            Playlists.NAME
    };

    /**
     * projection for genre columns
     */
    private static final String[] GENRE_COLUMNS = {
            Genres._ID,
            Genres.NAME
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
    private static final String GENRE_SELECT = "name!=''";


    /**
     * Selection to filter songs with empty name
     */
    public static final String TRACK_SELECT = "is_music=1 AND title!=''";

    /**
     *
     */
    public static final String LAST_ADDED_SELECT = "is_music=1 AND title!='' AND date_added>?";

    /**
     * folder track selection
     */
    private static final String FOLDER_TRACK_SELECT = "is_music=1 AND title!='' AND _data LIKE ?";

    /**
     * SQL selection
     */
    private static final String ARTIST_SONG_SELECT = "is_music=1 AND title!='' AND artist_id=?";

    /**
     * selection for albums of an artist
     */
    private static final String ARTIST_ALBUM_SELECT = "artist_id=?";

    /**
     * SQL Query
     */
    private static final String ALBUM_SONG_SELECT = "is_music=1 AND title!='' AND album_id=?";

    /**
     * select specific album matching artist and name
     */
    private static final String ALBUM_SELECT_NAME = ALBUM_COLUMN[1] + "=? AND " + ALBUM_COLUMN[2] + "=?";

    /**
     * select specific album matching artist and name
     */
    private static final String ALBUM_SELECT_ID = ALBUM_COLUMN[0] + "=?";

    /**
     * select specific artist matching name
     */
    private static final String ARTIST_SELECT = ARTIST_COLUMN[1] + "=?";

    /**
     * select specific artist matching name
     */
    private static final String PLAYLIST_SELECT = PLAYLIST_COLUMNS[1] + "=?";

    /**
     * selection to find artists matchin search
     */
    private static final String ARTIST_MATCH = Artists.ARTIST + " LIKE ?";

    /**
     * selection to find albums matching search
     */
    private static final String ALBUM_MATCH = Albums.ALBUM + " LIKE ?";

    /**
     * selection to find title matching search
     */
    private static final String TRACK_MATCH = Media.TITLE + " LIKE ?";

    /**
     *
     */
    private static final String PLAYLIST_TRACK_ORDER = "play_order";

    /**
     *
     */
    private static final String PLAYLIST_ORDER = PLAYLIST_COLUMNS[1];

    /**
     * order by
     */
    private static final String GENRE_TRACK_ORDER = "title_key";

    /**
     * sort genres by name
     */
    private static final String GENRE_ORDER = "name";

    /**
     * sort recent played audio tracks
     */
    private static final String RECENT_ORDER = TIMEPLAYED + " DESC";

    /**
     * sort folder tracks
     */
    private static final String FOLER_TRACKS_ORDER = "title_key";

    /**
     * sort folder
     */
    public static final String ORDER_TIME = "date_added DESC";

    /**
     * SQLite sport order
     */
    public static final String ORDER = FAVORITE_COLUMNS[4] + " DESC";


    private CursorCreator() {
    }


    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makeTrackCursor(Context context) {
        String sort = PreferenceUtils.getInstance(context).getSongSortOrder();
        return context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_SELECT, null, sort);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the album query.
     */
    public static Cursor makeRecentCursor(Context context) {
        SQLiteDatabase database = RecentStore.getInstance(context).getReadableDatabase();
        return database.query(NAME, RECENT_COLUMNS, null, null, null, null, RECENT_ORDER);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param id playlist ID
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makePlaylistSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        Uri media = Playlists.Members.getContentUri("external", id);
        return resolver.query(media, TRACK_COLUMNS, TRACK_SELECT, null, PLAYLIST_TRACK_ORDER);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the playlist query.
     */
    public static Cursor makePlaylistCursor(Context context) {
        return context.getContentResolver().query(Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, null, null, PLAYLIST_ORDER);
    }

    /**
     * create cursor for a playlist item
     *
     * @param name name of the playlist
     * @return cursor
     */
    public static Cursor makePlaylistCursor(Context context, String name) {
        String[] args = {name};
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, PLAYLIST_COLUMNS, PLAYLIST_SELECT, args, null);
    }

    /**
     * create cursor to get artist information
     *
     * @return cursor
     */
    @SuppressLint("Recycle")
    public static Cursor[] SearchCursor(Context context, String search) {
        Cursor[] cursors = new Cursor[3];
        ContentResolver resolver = context.getContentResolver();
        String[] args = {search};
        cursors[0] = resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMN, ARTIST_MATCH, args, null);
        cursors[1] = resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, TRACK_MATCH, args, null);
        cursors[2] = resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_MATCH, args, null);
        return cursors;
    }

    /**
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makeLastAddedCursor(Context context) {
        String[] select = {Long.toString(System.currentTimeMillis() / 1000 - 2419200)};

        ContentResolver resolver = context.getContentResolver();
        return resolver.query(EXTERNAL_CONTENT_URI, TRACK_COLUMNS, LAST_ADDED_SELECT, select, ORDER_TIME);
    }

    /**
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeGenreSongCursor(Context context, long id) {
        // Match the songs up with the genre
        Uri media = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(media, TRACK_COLUMNS, TRACK_SELECT, null, GENRE_TRACK_ORDER);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the genre query.
     */
    public static Cursor makeGenreCursor(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Genres.EXTERNAL_CONTENT_URI, GENRE_COLUMNS, GENRE_SELECT, null, GENRE_ORDER);
    }

    /**
     * create cursor to pare all music files
     *
     * @return cursor
     */
    public static Cursor makeFolderSongCursor(Context context, File folder) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] args = {folder.toString() + "%"};
        return contentResolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, FOLDER_TRACK_SELECT, args, FOLER_TRACKS_ORDER);
    }

    /**
     * create cursor to get data from
     *
     * @return cursor
     */
    public static Cursor makeFolderCursor(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String sortOrder = PreferenceUtils.getInstance(context).getSongSortOrder();
        return contentResolver.query(Media.EXTERNAL_CONTENT_URI, FOLDER_PROJECTION, TRACK_SELECT, null, sortOrder);
    }

    /**
     * @return The {@link Cursor} used to run the favorites query.
     */
    public static Cursor makeFavoritesCursor(Context context) {
        SQLiteDatabase data = FavoritesStore.getInstance(context).getReadableDatabase();
        return data.query(FavoritesStore.FavoriteColumns.NAME, FAVORITE_COLUMNS, null, null, null, null, ORDER);
    }

    /**
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeArtistSongCursor(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        String order = PreferenceUtils.getInstance(context).getArtistSongSortOrder();
        String[] args = {Long.toString(id)};
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ARTIST_SONG_SELECT, args, order);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the artist query.
     */
    public static Cursor makeArtistCursor(Context context) {
        String order = PreferenceUtils.getInstance(context).getArtistSortOrder();
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMN, null, null, order);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the artist query.
     */
    public static Cursor makeArtistCursor(Context context, String name) {
        String[] args = {name};
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_COLUMN, ARTIST_SELECT, args, null);
    }

    /**
     * Create cursor
     *
     * @return sql cursor
     */
    public static Cursor makeArtistAlbumCursor(Context context, long id) {
        String[] args = {Long.toString(id)};
        String order = PreferenceUtils.getInstance(context).getArtistAlbumSortOrder();
        return context.getContentResolver().query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ARTIST_ALBUM_SELECT, args, order);
    }

    /**
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeAlbumSongCursor(Context context, long id) {
        String[] args = {Long.toString(id)};
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSongSortOrder();
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS, ALBUM_SONG_SELECT, args, sortOrder);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the album query.
     */
    public static Cursor makeAlbumCursor(Context context) {
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, null, null, sortOrder);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the album query.
     */
    public static Cursor makeAlbumCursor(Context context, long id) {
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        ContentResolver resolver = context.getContentResolver();
        String[] args = {Long.toString(id)};
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_SELECT_ID, args, sortOrder);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @return The {@link Cursor} used to run the album query.
     */
    public static Cursor makeAlbumCursor(Context context, String album, String artist) {
        String sortOrder = PreferenceUtils.getInstance(context).getAlbumSortOrder();
        ContentResolver resolver = context.getContentResolver();
        String[] args = {album, artist};
        return resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_COLUMN, ALBUM_SELECT_NAME, args, sortOrder);
    }
}