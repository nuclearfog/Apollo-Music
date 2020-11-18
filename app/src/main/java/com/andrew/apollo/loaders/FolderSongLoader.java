package com.andrew.apollo.loaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * decompiled from Apollo.APK version 1.6
 */
public class FolderSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
    private File mFolder;

    private ArrayList<Song> mSongList = Lists.newArrayList();


    public FolderSongLoader(Context paramContext, File paramFile) {
        super(paramContext);
        this.mFolder = paramFile;
    }


    private static Cursor makeFileSongCursor(Context paramContext, File paramFile) {
        ContentResolver contentResolver = paramContext.getContentResolver();
        String[] projection = {"_id", "title", "album", "artist", "duration"};
        String[] order = {paramFile.toString() + '%'};
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        return contentResolver.query(uri, projection, "is_music=1 AND title!='' AND _data LIKE ?", order, "title_key");
    }


    public List<Song> loadInBackground() {
        Cursor cursor = makeFileSongCursor(getContext(), this.mFolder);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String songTitle = cursor.getString(1);
                    String albumTitle = cursor.getString(2);
                    String artistName = cursor.getString(3);
                    int duration = (int) cursor.getLong(4) / 1000;
                    Song song = new Song(id, songTitle, artistName, albumTitle, duration);
                    this.mSongList.add(song);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return this.mSongList;
    }
}