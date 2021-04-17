package com.andrew.apollo.loaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.utils.PreferenceUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

/**
 * return all music folders from storage
 */
public class FolderLoader extends WrappedAsyncTaskLoader<List<File>> {

    /**
     * SQL Selection
     */
    private static final String SELECTION = "is_music=1 AND title != ''";

    /**
     * SQL Projection
     */
    private static final String[] PROJECTION = {"_data"};

    /**
     * @param paramContext Activity context
     */
    public FolderLoader(Context paramContext) {
        super(paramContext);
    }


    @Override
    public List<File> loadInBackground() {
        HashSet<File> hashSet = new HashSet<>();
        Cursor cursor = makeSongCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String pathName = cursor.getString(0);
                    File folder = new File(pathName).getAbsoluteFile().getParentFile();
                    hashSet.add(folder);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        List<File> result = new LinkedList<>(hashSet);
        Collections.sort(result, new Comparator<File>() {
            public int compare(File param1File1, File param1File2) {
                return param1File1.getName().compareToIgnoreCase(param1File2.getName());
            }
        });
        return result;
    }

    /**
     * create cursor to get data from
     *
     * @return cursor
     */
    private Cursor makeSongCursor() {
        ContentResolver contentResolver = getContext().getContentResolver();
        String sortOrder = PreferenceUtils.getInstance(getContext()).getSongSortOrder();
        return contentResolver.query(EXTERNAL_CONTENT_URI, PROJECTION, SELECTION, null, sortOrder);
    }
}