package org.nuclearfog.apollo;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.model.Album;

interface IApolloService {
    int getAudioSessionId();
    void openFile(in Uri uri);
    void open(in long[] list, int position);
    void pause(boolean force);
    void play();
    void gotoNext();
    void gotoPrev();
    void enqueue(in long[] list, int action);
    void setQueuePosition(int index);
    void moveQueueItem(int from, int to);
    long[] getQueue();
    int getQueuePosition();
    void clearQueue();
    int getShuffleMode();
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    void refresh();
    boolean isPlaying();
    long getPlayerPosition();
    void setPlayerPosition(long pos);
    void removeTrack(int pos);
    int removeTracks(in long[] ids);
    Song getCurrentTrack();
    Album getCurrentAlbum();
}