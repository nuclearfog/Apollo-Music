package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.CursorFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to get all songs of a music folder
 * decompiled from Apollo.APK version 1.6
 */
public class FolderSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * folder to search tracks
     */
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
        Cursor cursor = CursorFactory.makeFolderSongCursor(getContext(), mFolder);
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
                        long duration = cursor.getLong(4);
                        Song song = new Song(id, songTitle, artistName, albumTitle, duration);
                        result.add(song);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }
}