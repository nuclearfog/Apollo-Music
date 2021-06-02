package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Loader to get the most played tracks, sorted by popularity
 *
 * @author nuclearfog
 */
public class MostPlayedLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     *
     */
    public MostPlayedLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        List<Song> result = new LinkedList<>();
        // Create the Cursor
        Cursor mCursor = CursorFactory.makeMostPlayedCursor(getContext());
        // Gather the data
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    long id = mCursor.getLong(0);
                    // Copy the song name
                    String songName = mCursor.getString(1);
                    // Copy the artist name
                    String artist = mCursor.getString(3);
                    // Copy the album name
                    String album = mCursor.getString(2);
                    // Copy the duration value in milliseconds
                    long duration = mCursor.getLong(5);
                    // Create a new song
                    Song song = new Song(id, songName, artist, album, duration);
                    // Add everything up
                    result.add(song);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
        return result;
    }
}