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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;
import org.nuclearfog.apollo.utils.BitmapUtils;

/**
 * 4x1 App-Widget
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AppWidgetSmall extends AppWidgetBase {


	public static final String CMDAPPWIDGETUPDATE = "app_widget_small_update";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		defaultAppWidget(context, appWidgetIds);
		Intent updateIntent = new Intent(MusicPlaybackService.SERVICECMD);
		updateIntent.putExtra(MusicPlaybackService.CMDNAME, AppWidgetSmall.CMDAPPWIDGETUPDATE);
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		context.sendBroadcast(updateIntent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyChange(MusicPlaybackService service, String what) {
		if (hasInstances(service)) {
			if (MusicPlaybackService.CHANGED_META.equals(what) || MusicPlaybackService.CHANGED_PLAYSTATE.equals(what)) {
				performUpdate(service, null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void performUpdate(MusicPlaybackService service, int[] appWidgetIds) {
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_small);
		Song song = service.getCurrentSong();
		Album album = service.getCurrentAlbum();
		if (song != null && album != null) {
			Bitmap bitmap = BitmapUtils.getAlbumArt(service, album);
			// Set the titles and artwork
			appWidgetView.setViewVisibility(R.id.app_widget_small_info_container, View.VISIBLE);
			appWidgetView.setTextViewText(R.id.app_widget_small_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_small_line_two, song.getArtist());
			appWidgetView.setImageViewBitmap(R.id.app_widget_small_image, bitmap);
		}
		// Set correct drawable for pause state
		boolean isPlaying = service.isPlaying();
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_small_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_small_play, service.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_small_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_small_play, service.getString(R.string.accessibility_play));
		}
		// Link actions buttons to intents
		linkButtons(service, appWidgetView, isPlaying);
		// Update the app-widget
		pushUpdate(service, getClass(), appWidgetIds, appWidgetView);
	}

	/**
	 * Initialize given widgets to default state, where we launch Music on
	 * default click and hide actions if service not running.
	 */
	private void defaultAppWidget(Context context, int[] appWidgetIds) {
		RemoteViews appWidgetViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_small);
		appWidgetViews.setViewVisibility(R.id.app_widget_small_info_container, View.INVISIBLE);
		linkButtons(context, appWidgetViews, false);
		pushUpdate(context, getClass(), appWidgetIds, appWidgetViews);
	}

	/**
	 * Link up various button actions using {@link PendingIntent}.
	 *
	 * @param playerActive True if player is active in background, which means widget click will launch {@link AudioPlayerActivity}
	 */
	private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
		ComponentName serviceName = new ComponentName(context, MusicPlaybackService.class);
		Intent action = new Intent(context, playerActive ? AudioPlayerActivity.class : HomeActivity.class);
		//
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE);
		views.setOnClickPendingIntent(R.id.app_widget_small_info_container, pendingIntent);
		views.setOnClickPendingIntent(R.id.app_widget_small_image, pendingIntent);
		// Previous track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_PREVIOUS, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_small_previous, pendingIntent);
		// Play and pause
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_small_play, pendingIntent);
		// Next track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_NEXT, serviceName);
		views.setOnClickPendingIntent(R.id.app_widget_small_next, pendingIntent);
	}
}