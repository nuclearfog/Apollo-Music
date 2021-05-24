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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.andrew.apollo.MusicPlaybackService.NOTIFICAITON_ID;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class NotificationHelper {

    /**
     * Notification ID
     */
    private static final int APOLLO_MUSIC_SERVICE = BuildConfig.DEBUG ? 0x5D74E856 : 0x28E61796;

    /**
     * Service context
     */
    private MusicPlaybackService mService;

    /**
     * manage and update notification
     */
    @Nullable
    private NotificationManager mNotificationManager;

    /**
     * current notification
     */
    private Notification mNotification;

    /**
     * notification views
     */
    @NonNull
    private RemoteViews mSmallContent, mExpandedView;

    /**
     * callbacks to the service
     */
    private PendingIntent[] callbacks;

    private boolean updateEnable = false;

    /**
     * Constructor of <code>NotificationHelper</code>
     *
     * @param service callback to the service
     */
    public NotificationHelper(MusicPlaybackService service) {
        mService = service;
        mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        // Default notfication layout
        mSmallContent = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification_template_base);
        // Expanded notification layout
        mExpandedView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification_template_expanded_base);
        initCallbacks();
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification() {
        // Notification Builder
        mNotification = new NotificationCompat.Builder(mService, Long.toString(mService.getAlbumId()))
                .setSmallIcon(R.drawable.stat_notify_music)
                .setContentIntent(getPendingIntent())
                .setPriority(Notification.PRIORITY_HIGH)
                .setCustomBigContentView(mExpandedView)
                .setChannelId(NOTIFICAITON_ID)
                .setCustomContentView(mSmallContent)
                .build();

        // Control playback from the notification
        initPlaybackActions(mService.isPlaying());
        // Set up the content view
        updateCollapsedLayout();
        // Control playback from the notification
        initExpandedPlaybackActions(mService.isPlaying());
        // Set up the expanded content view
        updateExpandedLayout();
        // start notification
        mService.startForeground(APOLLO_MUSIC_SERVICE, mNotification);
        // Enable notification update
        updateEnable = true;
    }

    /**
     * Changes the playback controls in and out of a paused state
     */
    public void updateNotification() {
        if (mNotification != null && mNotificationManager != null && updateEnable) {
            int iconRes = mService.isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play;
            mSmallContent.setImageViewResource(R.id.notification_base_play, iconRes);
            mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, iconRes);
            updateExpandedLayout();
            updateCollapsedLayout();
            // Update notification
            mNotificationManager.notify(APOLLO_MUSIC_SERVICE, mNotification);
        }
    }

    /**
     * ignore {@link #updateNotification()} when called, to prevent pop up while app is in foreground.
     * calling {@link #buildNotification()} will enable updates
     */
    public void ignoreUpdate() {
        updateEnable = false;
    }

    /**
     * cancel notification on service end
     */
    public void cancelNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(APOLLO_MUSIC_SERVICE);
        }
    }

    /**
     * Open to the now playing screen
     */
    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".AUDIO_PLAYER");
        return PendingIntent.getActivity(mService, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions(boolean isPlaying) {
        // Play and pause
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play, callbacks[0]);
        // Skip tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next, callbacks[1]);
        // Previous tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous, callbacks[2]);
        // Stop and collapse the notification
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse, callbacks[3]);
        // Update the play button image
        mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions(boolean isPlaying) {
        // Play and pause
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_play, callbacks[0]);
        // Skip tracks
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_next, callbacks[1]);
        // Previous tracks
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_previous, callbacks[2]);
        // Stop and collapse the notification
        mSmallContent.setOnClickPendingIntent(R.id.notification_base_collapse, callbacks[3]);
        // Update the play button image
        mSmallContent.setImageViewResource(R.id.notification_base_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
    }

    /**
     * Initialize notification callbacks
     */
    private void initCallbacks() {
        callbacks = new PendingIntent[4];
        ComponentName serviceName = new ComponentName(mService, MusicPlaybackService.class);

        Intent action = new Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION).setComponent(serviceName);
        callbacks[0] = PendingIntent.getService(mService, 1, action, 0);

        action = new Intent(MusicPlaybackService.NEXT_ACTION).setComponent(serviceName);
        callbacks[1] = PendingIntent.getService(mService, 2, action, 0);

        action = new Intent(MusicPlaybackService.PREVIOUS_ACTION).setComponent(serviceName);
        callbacks[2] = PendingIntent.getService(mService, 3, action, 0);

        action = new Intent(MusicPlaybackService.STOP_ACTION).setComponent(serviceName);
        callbacks[3] = PendingIntent.getService(mService, 4, action, 0);
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void updateCollapsedLayout() {
        // Track name (line one)
        mSmallContent.setTextViewText(R.id.notification_base_line_one, mService.getTrackName());
        // Artist name (line two)
        mSmallContent.setTextViewText(R.id.notification_base_line_two, mService.getArtistName());
        // Album art
        mSmallContent.setImageViewBitmap(R.id.notification_base_image, mService.getAlbumArt());
    }

    /**
     * Sets the track name, album name, artist name, and album art in the
     * expanded layout
     */
    private void updateExpandedLayout() {
        // Track name (line one)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_one, mService.getTrackName());
        // Album name (line two)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_two, mService.getAlbumName());
        // Artist name (line three)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_three, mService.getArtistName());
        // Album art
        mExpandedView.setImageViewBitmap(R.id.notification_expanded_base_image, mService.getAlbumArt());
    }
}