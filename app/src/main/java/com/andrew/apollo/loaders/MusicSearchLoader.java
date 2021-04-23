package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Media;

import androidx.annotation.Nullable;

import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Music;
import com.andrew.apollo.model.Song;

import java.util.LinkedList;
import java.util.List;

import static com.andrew.apollo.loaders.AlbumLoader.ALBUM_COLUMN;
import static com.andrew.apollo.loaders.AlbumLoader.ALBUM_URI;
import static com.andrew.apollo.loaders.ArtistLoader.ARTIST_COLUMN;
import static com.andrew.apollo.loaders.ArtistLoader.ARTIST_URI;
import static com.andrew.apollo.loaders.SongLoader.TRACK_COLUMNS;
import static com.andrew.apollo.loaders.SongLoader.TRACK_URI;

/**
 * Backend loader to search for music information
 *
 * @author nuclearfog
 */
public class MusicSearchLoader extends WrappedAsyncTaskLoader<List<Music>> {

    /**
     *
     */
    private static final String ARTIST_SEARCH = Artists.ARTIST + " LIKE ?";

    /**
     *
     */
    private static final String ALBUM_SEARCH = Albums.ALBUM + " LIKE ?";

    /**
     *
     */
    private static final String TRACK_SEARCH = Media.TITLE + " LIKE ?";

    /**
     * search string as argument
     */
    private String[] search;


    public MusicSearchLoader(Context context, String search) {
        super(context);
        this.search = new String[]{'%' + search + '%'};
    }


    @Nullable
    @Override
    public List<Music> loadInBackground() {
        List<Music> result = new LinkedList<>();

        // Search for Tracks
        Cursor cursor = makeArtistCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String artistName = cursor.getString(1);
                    int albumCount = cursor.getInt(2);
                    int songCount = cursor.getInt(3);
                    Artist artist = new Artist(id, artistName, songCount, albumCount);
                    result.add(artist);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // search for Albums
        cursor = makeAlbumCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String albumName = cursor.getString(1);
                    String artist = cursor.getString(2);
                    int songCount = cursor.getInt(3);
                    String year = cursor.getString(4);
                    Album album = new Album(id, albumName, artist, songCount, year);
                    result.add(album);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // Search for tracks
        cursor = makeTrackCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String mime = cursor.getString(6);
                    if (mime.startsWith("audio/") || mime.equals("application/ogg")
                            || mime.equals("application/x-ogg")) {
                        long id = cursor.getLong(0);
                        String songName = cursor.getString(1);
                        String artist = cursor.getString(2);
                        String album = cursor.getString(3);
                        long duration = cursor.getLong(4);
                        Song song = new Song(id, songName, artist, album, duration);
                        result.add(song);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }

    /**
     * create cursor to get artist information
     *
     * @return cursor
     */
    private Cursor makeArtistCursor() {
        return getContext().getContentResolver().query(ARTIST_URI, ARTIST_COLUMN, ARTIST_SEARCH, search, null);
    }

    /**
     * create cursor to get track information
     *
     * @return cursor
     */
    private Cursor makeTrackCursor() {
        return getContext().getContentResolver().query(TRACK_URI, TRACK_COLUMNS, TRACK_SEARCH, search, null);
    }

    /**
     * create album cursor to get album information
     *
     * @return cursor
     */
    private Cursor makeAlbumCursor() {
        return getContext().getContentResolver().query(ALBUM_URI, ALBUM_COLUMN, ALBUM_SEARCH, search, null);
    }
}