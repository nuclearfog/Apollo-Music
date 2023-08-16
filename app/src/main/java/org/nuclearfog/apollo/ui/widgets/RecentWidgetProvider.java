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

package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.service.RecentWidgetService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.activities.ShortcutActivity;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.lang.ref.WeakReference;

/**
 * App-Widget used to display a list of recently listened albums.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentWidgetProvider extends AppWidgetBase {

	private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;

	public static final String SET_ACTION = "set_action";

	public static final String OPEN_PROFILE = "open_profile";

	public static final String PLAY_ALBUM = "play_album";

	public static final String CMDAPPWIDGETUPDATE = "app_widget_recents_update";

	public static final String CLICK_ACTION = PACKAGE_NAME + ".recents.appwidget.action.CLICK";

	private static final int REQUEST_RECENT = 0x5103;

	private static Handler sWorkerQueue;

	private RemoteViews mViews;

	/**
	 * Constructor of <code>RecentWidgetProvider</code>
	 */
	public RecentWidgetProvider() {
		// Start the worker thread
		HandlerThread workerThread = new HandlerThread("RecentWidgetProviderWorker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
		workerThread.start();
		sWorkerQueue = new Handler(workerThread.getLooper());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		int intentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			intentFlag |= PendingIntent.FLAG_IMMUTABLE;
		}
		for (int appWidgetId : appWidgetIds) {
			// Create the remote views
			mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents);
			// Link actions buttons to intents
			linkButtons(context, mViews, false);

			Intent recentIntent = new Intent(context, RecentWidgetService.class);
			recentIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			recentIntent.setData(Uri.parse(recentIntent.toUri(Intent.URI_INTENT_SCHEME)));
			mViews.setRemoteAdapter(R.id.app_widget_recents_list, recentIntent);

			Intent updateIntent = new Intent(MusicPlaybackService.SERVICECMD);
			updateIntent.putExtra(MusicPlaybackService.CMDNAME, RecentWidgetProvider.CMDAPPWIDGETUPDATE);
			updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			updateIntent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			context.sendBroadcast(updateIntent);

			Intent onClickIntent = new Intent(context, RecentWidgetProvider.class);
			onClickIntent.setAction(RecentWidgetProvider.CLICK_ACTION);
			onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
			PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, REQUEST_RECENT, onClickIntent, intentFlag);
			mViews.setPendingIntentTemplate(R.id.app_widget_recents_list, onClickPendingIntent);

			// Update the widget
			appWidgetManager.updateAppWidget(appWidgetId, mViews);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (CLICK_ACTION.equals(intent.getAction())) { // fixme albumId and action invalid on Android 10+ (see RecentWidgetService.WidgetRemoteViewsFactory.getViewAt(int))
			long albumId = intent.getLongExtra(Config.ID, -1);
			String action = intent.getStringExtra(SET_ACTION);
			if (PLAY_ALBUM.equals(action)) {
				// Play the selected album
				Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
				shortcutIntent.setAction(Intent.ACTION_VIEW);
				shortcutIntent.putExtra(Config.ID, albumId);
				shortcutIntent.putExtra(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
				shortcutIntent.putExtra(ShortcutActivity.OPEN_AUDIO_PLAYER, false);
				shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(shortcutIntent);
			} else if (OPEN_PROFILE.equals(action)) {
				// Transfer the album name and MIME type
				// Open the album profile
				Intent profileIntent = new Intent(context, ProfileActivity.class);
				profileIntent.putExtra(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
				profileIntent.putExtra(Config.NAME, intent.getStringExtra(Config.NAME));
				profileIntent.putExtra(Config.ARTIST_NAME, intent.getStringExtra(Config.ARTIST_NAME));
				profileIntent.putExtra(Config.ALBUM_YEAR, MusicUtils.getReleaseDateForAlbum(context, albumId));
				profileIntent.putExtra(Config.ID, albumId);
				profileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				profileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(profileIntent);
			}
		}
		super.onReceive(context, intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyChange(MusicPlaybackService service, String what) {
		if (hasInstances(service)) {
			if (MusicPlaybackService.CHANGED_PLAYSTATE.equals(what)) {
				performUpdate(service, null);
			} else if (MusicPlaybackService.CHANGED_META.equals(what)) {
				sWorkerQueue.post(new Updater(service));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void performUpdate(MusicPlaybackService service, int[] appWidgetIds) {
		mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents);
		/* Set correct drawable for pause state */
		boolean isPlaying = service.isPlaying();
		if (isPlaying) {
			mViews.setImageViewResource(R.id.app_widget_recents_play, R.drawable.btn_playback_pause);
		} else {
			mViews.setImageViewResource(R.id.app_widget_recents_play, R.drawable.btn_playback_play);
		}
		// Link actions buttons to intents
		linkButtons(service, mViews, isPlaying);
		// Update the app-widget
		pushUpdate(service, appWidgetIds, mViews);
	}

	/**
	 * Check against {@link AppWidgetManager} if there are any instances of this
	 * widget.
	 */
	private boolean hasInstances(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
		return appWidgetIds.length > 0;
	}

	/**
	 *
	 */
	private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			appWidgetManager.updateAppWidget(appWidgetIds, views);
		} else {
			appWidgetManager.updateAppWidget(new ComponentName(context, this.getClass()), views);
		}
	}

	/**
	 * Link up various button actions using {@link PendingIntent}.
	 *
	 * @param playerActive True if player is active in background, which means widget click will launch {@link AudioPlayerActivity}
	 */
	private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
		Intent action;
		if (playerActive) {
			// Now playing
			action = new Intent(context, AudioPlayerActivity.class);
		} else {
			// Home
			action = new Intent(context, HomeActivity.class);
		}
		int intentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			intentFlag |= PendingIntent.FLAG_IMMUTABLE;
		}
		ComponentName serviceName = new ComponentName(context, MusicPlaybackService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, action, intentFlag);
		views.setOnClickPendingIntent(R.id.app_widget_recents_action_bar, pendingIntent);
		// Previous track
		pendingIntent = buildPendingIntent(context, MusicPlaybackService.ACTION_PREVIOUS, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_recents_previous, pendingIntent);
		// Play and pause
		pendingIntent = buildPendingIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_recents_play, pendingIntent);
		// Next track
		pendingIntent = buildPendingIntent(context, MusicPlaybackService.ACTION_NEXT, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_recents_next, pendingIntent);
	}

	/**
	 *
	 */
	private static class Updater implements Runnable {

		private WeakReference<Service> service;

		Updater(Service service) {
			this.service = new WeakReference<>(service);
		}

		@Override
		public void run() {
			Service service = this.service.get();
			if (service != null) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(service);
				ComponentName componentName = new ComponentName(service, RecentWidgetProvider.class);
				appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(componentName), R.id.app_widget_recents_list);
			}
		}
	}
}