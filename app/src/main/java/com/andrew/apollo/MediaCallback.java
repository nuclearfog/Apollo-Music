package com.andrew.apollo;

import android.support.v4.media.session.MediaSessionCompat;

import com.andrew.apollo.utils.MusicUtils;

/**
 * callback class used by media buttons to control playback
 *
 * @author nuclearfog
 */
public class MediaCallback extends MediaSessionCompat.Callback {

    @Override
    public void onPlay() {
        MusicUtils.play();
    }

    @Override
    public void onPause() {
        MusicUtils.pause();
    }

    @Override
    public void onStop() {
        MusicUtils.stop();
    }

    @Override
    public void onSkipToNext() {
        MusicUtils.next();
    }

    @Override
    public void onSkipToPrevious() {
        MusicUtils.previous();
    }

    @Override
    public void onSeekTo(long pos) {
        MusicUtils.seek(pos);
    }
}