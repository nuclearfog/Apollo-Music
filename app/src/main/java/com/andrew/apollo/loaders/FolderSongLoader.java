package com.andrew.apollo.loaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.model.Song;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static com.andrew.apollo.loaders.SongLoader.TRACK_COLUMNS;
import static com.andrew.apollo.loaders.SongLoader.TRACK_URI;

/**
 * decompiled from Apollo.APK version 1.6
 */
public class FolderSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private static final String SELECTION = "is_music=1 AND title!='' AND _data LIKE ?";

    private static final String ORDER = "title_key";

    private File mFolder;

    /**
     * @param paramContext Application context
     * @param folder       folder to open
     */
    public FolderSongLoader(Context paramContext, File folder) {
        super(paramContext);
        this.mFolder = folder;
    }


    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        Cursor cursor = makeFileSongCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String filename = cursor.getString(5);
                    File file = new File(filename);
                    if (mFolder.equals(file.getParentFile())) {
                        long id = cursor.getLong(0);
                        String songTitle = cursor.getString(1);
                        String artistName = cursor.getString(2);
                        String albumTitle = cursor.getString(3);
                        int duration = (int) (cursor.getLong(4) / 1000);
                        Song song = new Song(id, songTitle, artistName, albumTitle, duration);
                        result.add(song);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }

    /**
     * create cursor to pare all music files
     *
     * @return cursor
     */
    private Cursor makeFileSongCursor() {
        ContentResolver contentResolver = getContext().getContentResolver();
        String[] args = {mFolder.toString() + "%"};
        return contentResolver.query(TRACK_URI, TRACK_COLUMNS, SELECTION, args, ORDER);
    }
}