package org.nuclearfog.apollo;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.model.Album;

interface IApolloService {

    void openFile(in Uri uri);
    void open(in long[] list, int position);
    void pause(boolean force);
    void play();
    void gotoNext();
    void gotoPrev();
    void enqueue(in long[] list, int action);
    void setQueuePosition(int index);
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void refresh();
    boolean isPlaying();
    long[] getQueue();
    long duration();
    long position();
    void seek(long pos);
    int getQueuePosition();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id); 
    int getRepeatMode();
    int getAudioSessionId();
    Song getCurrentTrack();
    Album getCurrentAlbum();
    String getPath();
}