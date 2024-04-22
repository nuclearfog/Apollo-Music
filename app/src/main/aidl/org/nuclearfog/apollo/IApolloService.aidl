package org.nuclearfog.apollo;

import android.graphics.Bitmap;
import android.net.Uri;

interface IApolloService
{
    void openFile(in Uri uri);
    void open(in long [] list, int position);
    void stop();
    void pause(boolean force);
    void play();
    void prev();
    void goToNext();
    void goToPrev();
    void enqueue(in long [] list, int action);
    void setQueuePosition(int index);
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void toggleFavorite();
    void refresh();
    boolean isFavorite();
    boolean isPlaying();
    long [] getQueue();
    long duration();
    long position();
    long seek(long pos);
    long getAudioId();
    long getArtistId();
    long getAlbumId();
    String getArtistName();
    String getTrackName();
    String getAlbumName();
    String getPath();
    int getQueuePosition();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id); 
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
}
