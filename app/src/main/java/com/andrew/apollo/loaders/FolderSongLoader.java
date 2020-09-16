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

public class FolderSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
    private final File mFolder;

    private final ArrayList<Song> mSongList = Lists.newArrayList();

    public FolderSongLoader(Context paramContext, File paramFile) {
        super(paramContext);
        this.mFolder = paramFile;
    }

    private static Cursor makeFileSongCursor(Context paramContext, File paramFile) {
        ContentResolver contentResolver = paramContext.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String str = paramFile.toString() + '%';
        return contentResolver.query(uri, new String[]{"_id", "title", "album", "artist", "duration"}, "is_music=1 AND title!='' AND _data LIKE ?", new String[]{str}, "title_key");
    }

    public List<Song> loadInBackground() {
        Cursor cursor = makeFileSongCursor(getContext(), this.mFolder);
        if (cursor != null && cursor.moveToFirst())
            do {
                long id = cursor.getLong(0);
                String str1 = cursor.getString(1);
                String str2 = cursor.getString(2);
                Song song = new Song(id, str1, cursor.getString(3), str2, (int) cursor.getLong(4) / 1000);
                this.mSongList.add(song);
            } while (cursor.moveToNext());
        if (cursor != null)
            cursor.close();
        return this.mSongList;
    }
}