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

package org.nuclearfog.apollo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class NotificationHelper {

	/**
	 * Notification ID
	 * use different notification IDs for each build to avoid conflics
	 */
	private static final int APOLLO_MUSIC_SERVICE = BuildConfig.DEBUG ? 0x5D74E856 : 0x28E61796;

	/**
	 *
	 */
	private static final String INTENT_AUDIO_PLAYER = BuildConfig.APPLICATION_ID + ".AUDIO_PLAYER";

	/**
	 * Notification name
	 */
	private static final String NOTFICIATION_NAME = "Apollo Controlpanel";

	/**
	 * Service context
	 */
	private MusicPlaybackService mService;

	/**
	 * manage and update notification
	 */
	private NotificationManager notificationManager;

	private NotificationCompat.Builder notificationBuilder;

	/**
	 * notification views
	 */
	private RemoteViews mSmallContent, mExpandedView;

	/**
	 * callbacks to the service
	 */
	private PendingIntent[] callbacks;

	/**
	 * Constructor of <code>NotificationHelper</code>
	 *
	 * @param service callback to the service
	 */
	public NotificationHelper(MusicPlaybackService service) {
		mService = service;
		// get notificationmanager and create notificationchannel if required
		notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(MusicPlaybackService.NOTIFICAITON_ID, NOTFICIATION_NAME, NotificationManager.IMPORTANCE_LOW);
			notificationManager.createNotificationChannel(channel);
		}
		// initialize callbacks
		ComponentName serviceName = new ComponentName(mService, MusicPlaybackService.class);
		callbacks = new PendingIntent[]{
				PendingIntent.getService(mService, 1, new Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION).setComponent(serviceName), PendingIntent.FLAG_IMMUTABLE),
				PendingIntent.getService(mService, 2, new Intent(MusicPlaybackService.NEXT_ACTION).setComponent(serviceName), PendingIntent.FLAG_IMMUTABLE),
				PendingIntent.getService(mService, 3, new Intent(MusicPlaybackService.PREVIOUS_ACTION).setComponent(serviceName), PendingIntent.FLAG_IMMUTABLE),
				PendingIntent.getService(mService, 4, new Intent(MusicPlaybackService.STOP_ACTION).setComponent(serviceName), PendingIntent.FLAG_IMMUTABLE)
		};
		// initialize small notification view
		mSmallContent = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification_template_base);
		// initialize expanded notification view
		mExpandedView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification_template_expanded_base);
		// create notification builder
		notificationBuilder = new NotificationCompat.Builder(mService, MusicPlaybackService.NOTIFICAITON_ID)
				.setSmallIcon(R.drawable.stat_notify_music)
				.setContentIntent(getPendingIntent())
				.setPriority(NotificationCompat.PRIORITY_LOW)
				.setCustomBigContentView(mExpandedView)
				.setCustomContentView(mSmallContent)
				.setDefaults(Notification.DEFAULT_ALL)
				.setOnlyAlertOnce(true)
				.setAutoCancel(false)
				.setSilent(true)
				.setOngoing(true);
	}

	/**
	 * Call this to build the {@link Notification}.
	 */
	public void buildNotification() {
		initPlaybackActions();
		initExpandedPlaybackActions();
		updateNotification();
	}

	/**
	 * Changes the playback controls in and out of a paused state
	 */
	public void updateNotification() {
		int iconRes = mService.isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play;
		mSmallContent.setImageViewResource(R.id.notification_base_play, iconRes);
		mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, iconRes);
		updateExpandedLayout();
		updateCollapsedLayout();
		// Update notification
		notificationBuilder.setCustomBigContentView(mExpandedView).setCustomContentView(mSmallContent);
		notificationManager.notify(APOLLO_MUSIC_SERVICE, notificationBuilder.build());
	}

	/**
	 * cancel notification when app is in foreground
	 */
	public void cancelNotification() {
		notificationManager.cancel(APOLLO_MUSIC_SERVICE);
	}

	/**
	 * Open to the now playing screen
	 */
	private PendingIntent getPendingIntent() {
		Intent intent = new Intent(INTENT_AUDIO_PLAYER);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return PendingIntent.getActivity(mService, 0, intent, PendingIntent.FLAG_IMMUTABLE);
	}

	/**
	 * Lets the buttons in the remote view control playback in the expanded
	 * layout
	 */
	private void initExpandedPlaybackActions() {
		// Play and pause
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play, callbacks[0]);
		// Skip tracks
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next, callbacks[1]);
		// Previous tracks
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous, callbacks[2]);
		// Stop and collapse the notification
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse, callbacks[3]);
	}

	/**
	 * Lets the buttons in the remote view control playback in the normal layout
	 */
	private void initPlaybackActions() {
		// Play and pause
		mSmallContent.setOnClickPendingIntent(R.id.notification_base_play, callbacks[0]);
		// Skip tracks
		mSmallContent.setOnClickPendingIntent(R.id.notification_base_next, callbacks[1]);
		// Previous tracks
		mSmallContent.setOnClickPendingIntent(R.id.notification_base_previous, callbacks[2]);
		// Stop and collapse the notification
		mSmallContent.setOnClickPendingIntent(R.id.notification_base_collapse, callbacks[3]);
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