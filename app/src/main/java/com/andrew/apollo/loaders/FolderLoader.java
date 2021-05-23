package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.utils.CursorFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * return all music folders from storage
 */
public class FolderLoader extends WrappedAsyncTaskLoader<List<File>> {

    /**
     * custom comparator to sort folders by name and not by path
     */
    private static final Comparator<File> fileComparator = new Comparator<File>() {
        @Override
        public int compare(File file1, File file2) {
            return file1.getName().compareTo(file2.getName());
        }
    };

    /**
     * @param paramContext Activity context
     */
    public FolderLoader(Context paramContext) {
        super(paramContext);
    }


    @Override
    public List<File> loadInBackground() {
        // init tree set to sort folder by name
        TreeSet<File> tree = new TreeSet<>(fileComparator);

        Cursor cursor = CursorFactory.makeFolderCursor(getContext());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String pathName = cursor.getString(0);
                    File folder = new File(pathName).getAbsoluteFile().getParentFile();
                    tree.add(folder);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return new ArrayList<>(tree);
    }
}