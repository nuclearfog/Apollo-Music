/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
@SuppressLint("NewApi")
public class NotificationHelper {

    /**
     * Notification ID
     */
    private static final int APOLLO_MUSIC_SERVICE = 1;

    /**
     * Context
     */
    private final MusicPlaybackService mService;

    private NotificationManager mNotificationManager;

    private RemoteViews mSmallContent;

    private Notification mNotification;

    /**
     * API 16+ bigContentView
     */
    private RemoteViews mExpandedView;

    /**
     * Constructor of <code>NotificationHelper</code>
     *
     * @param service The {@link Context} to use
     */
    public NotificationHelper(MusicPlaybackService service) {
        mService = service;
        mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification() {
        // Default notfication layout
        mSmallContent = new RemoteViews(mService.getPackageName(), R.layout.notification_template_base);
        mExpandedView = new RemoteViews(mService.getPackageName(), R.layout.notification_template_expanded_base);

        // Set up the content view
        initCollapsedLayout(mService.getTrackName(), mService.getArtistName(), mService.getAlbumArt());
        // Notification Builder
        mNotification = new NotificationCompat.Builder(mService, Long.toString(mService.getAlbumId()))
                .setSmallIcon(R.drawable.stat_notify_music)
                .setContentIntent(getPendingIntent())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCustomBigContentView(mExpandedView)
                .setCustomContentView(mSmallContent)
                .build();
        // Control playback from the notification
        initPlaybackActions(mService.isPlaying());
        // Control playback from the notification
        initExpandedPlaybackActions(mService.isPlaying());
        // Set up the expanded content view
        initExpandedLayout(mService.getTrackName(), mService.getAlbumName(), mService.getArtistName(), mService.getAlbumArt());
        mService.startForeground(APOLLO_MUSIC_SERVICE, mNotification);
    }

    /**
     * Remove notification
     */
    public void killNotification() {
        mService.stopForeground(true);
    }

    /**
     * Changes the playback controls in and out of a paused state
     *
     * @param isPlaying True if music is playing, false otherwise
     */
    public void updatePlayState(boolean isPlaying) {
        if (mNotification == null || mNotificationManager == null) {
            return;
        }
        if (mSmallContent != null) {
            mSmallContent.setImageViewResource(R.id.notification_base_play,
                    isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        }
        if (mExpandedView != null) {
            mExpandedView.setImageViewResource(R.id.notification_expanded_base_play,
                    isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        }
        mNotificationManager.notify(APOLLO_MUSIC_SERVICE, mNotification);
    }

    /**
     * Open to the now playing screen
     */
    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(mService, 0, new Intent("com.andrew.apollo.AUDIO_PLAYER")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions(boolean isPlaying) {
        // Play and pause
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play, retreivePlaybackActions(1));
        // Skip tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next, retreivePlaybackActions(2));
        // Previous tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous, retreivePlaybackActions(3));
        // Stop and collapse the notification
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse, retreivePlaybackActions(4));
        // Update the play button image
        mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions(boolean isPlaying) {
        // Play and pause
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_play, retreivePlaybackActions(1));
        // Skip tracks
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_next, retreivePlaybackActions(2));
        // Previous tracks
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_previous, retreivePlaybackActions(3));
        // Stop and collapse the notification
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_collapse, retreivePlaybackActions(4));
        // Update the play button image
        mSmallContent.setImageViewResource(R.id.notification_base_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
    }

    /**
     * @param which Which {@link PendingIntent} to return
     * @return A {@link PendingIntent} ready to control playback
     */
    private PendingIntent retreivePlaybackActions(int which) {
        Intent action;
        PendingIntent pendingIntent;
        ComponentName serviceName = new ComponentName(mService, MusicPlaybackService.class);
        switch (which) {
            case 1:
                // Play and pause
                action = new Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 1, action, 0);
                return pendingIntent;

            case 2:
                // Skip tracks
                action = new Intent(MusicPlaybackService.NEXT_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 2, action, 0);
                return pendingIntent;

            case 3:
                // Previous tracks
                action = new Intent(MusicPlaybackService.PREVIOUS_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 3, action, 0);
                return pendingIntent;

            default:
            case 4:
                // Stop and collapse the notification
                action = new Intent(MusicPlaybackService.STOP_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mService, 4, action, 0);
                return pendingIntent;
        }
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void initCollapsedLayout(String trackName, String artistName, Bitmap albumArt) {
        // Track name (line one)
        mSmallContent.setTextViewText(R.id.notification_base_line_one, trackName);
        // Artist name (line two)
        mSmallContent.setTextViewText(R.id.notification_base_line_two, artistName);
        // Album art
        mSmallContent.setImageViewBitmap(R.id.notification_base_image, albumArt);
    }

    /**
     * Sets the track name, album name, artist name, and album art in the
     * expanded layout
     */
    private void initExpandedLayout(String trackName, String artistName, String albumName, Bitmap albumArt) {
        // Track name (line one)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_one, trackName);
        // Album name (line two)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_two, albumName);
        // Artist name (line three)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_three, artistName);
        // Album art
        mExpandedView.setImageViewBitmap(R.id.notification_expanded_base_image, albumArt);
    }
}