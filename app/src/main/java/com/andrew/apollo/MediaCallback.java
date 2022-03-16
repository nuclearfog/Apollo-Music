package com.andrew.apollo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.andrew.apollo.utils.MusicUtils;

/**
 * callback class used by media buttons to control playback
 * todo add more button methods
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

    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
        MusicUtils.playFile(uri);
    }

    @Override
    public void onAddQueueItem(MediaDescriptionCompat description) {
        if (description.getMediaId() != null) {
            try {
                long id = Long.parseLong(description.getMediaId());
                MusicUtils.addToQueue(id);
            } catch (NumberFormatException e) {
                //
            }
        }
    }
}