package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.utils.CursorCreator;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * return all music folders from storage
 */
public class FolderLoader extends WrappedAsyncTaskLoader<List<File>> {


    /**
     * @param paramContext Activity context
     */
    public FolderLoader(Context paramContext) {
        super(paramContext);
    }


    @Override
    public List<File> loadInBackground() {
        HashSet<File> hashSet = new HashSet<>();
        Cursor cursor = CursorCreator.makeFolderCursor(getContext());
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
}