package com.andrew.apollo.loaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class FolderLoader extends WrappedAsyncTaskLoader<List<File>> {
    private final ArrayList<File> mFolders = Lists.newArrayList();

    public FolderLoader(Context paramContext) {
        super(paramContext);
    }

    private static Cursor makeSongCursor(Context paramContext) {
        ContentResolver contentResolver = paramContext.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String str = PreferenceUtils.getInstance(paramContext).getSongSortOrder();
        return contentResolver.query(uri, new String[]{"_data"}, "is_music=1 AND title != ''", null, str);
    }

    public List<File> loadInBackground() {
        HashSet<File> hashSet = new HashSet<>();
        Cursor cursor = makeSongCursor(getContext());
        if (cursor != null && cursor.moveToFirst())
            do {
                hashSet.add((new File(cursor.getString(0))).getAbsoluteFile().getParentFile());
            } while (cursor.moveToNext());
        if (cursor != null)
            cursor.close();
        mFolders.clear();
        mFolders.addAll(hashSet);
        Collections.sort(mFolders, new Comparator<File>() {
            public int compare(File param1File1, File param1File2) {
                return param1File1.getName().compareToIgnoreCase(param1File2.getName());
            }
        });
        return this.mFolders;
    }
}