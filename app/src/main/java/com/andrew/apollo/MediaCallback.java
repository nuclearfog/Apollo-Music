package com.andrew.apollo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

/**
 * callback class used by media buttons to control playback
 * todo add more button methods
 *
 * @author nuclearfog
 */
public class MediaCallback extends MediaSessionCompat.Callback {

    private MusicPlaybackService service;

    public MediaCallback(MusicPlaybackService service) {
        this.service = service;
    }

    @Override
    public void onPlay() {
        service.play();
    }

    @Override
    public void onPause() {
        service.pause();
    }

    @Override
    public void onStop() {
        service.stop();
    }

    @Override
    public void onSkipToNext() {
        service.gotoNext(true);
    }

    @Override
    public void onSkipToPrevious() {
        service.goToPrev();
    }

    @Override
    public void onSeekTo(long pos) {
        service.seek(pos);
    }

    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
        service.openFile(uri);
    }
}