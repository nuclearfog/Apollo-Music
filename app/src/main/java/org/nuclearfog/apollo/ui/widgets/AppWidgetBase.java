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
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * super class for all app widgets
 *
 * @author nuclearfog
 */
public abstract class AppWidgetBase extends AppWidgetProvider {

	/**
	 * create pending intent used for playback control
	 *
	 * @param action type of playback control action used by {@link MusicPlaybackService}
	 * @return PendingIntent instance
	 */
	protected PendingIntent createPlaybackControlIntent(Context context, String action, ComponentName serviceName) {
		Intent intent = new Intent(action);
		intent.setComponent(serviceName);
		intent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, false);
		return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
	}

	/**
	 * Check against {@link AppWidgetManager} if there are any instances of this
	 * widget.
	 */
	protected boolean hasInstances(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] mAppWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
		return mAppWidgetIds.length > 0;
	}

	/**
	 *
	 */
	protected void pushUpdate(Context context, Class<?> widgetClass, @Nullable int[] appWidgetIds, RemoteViews views) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			appWidgetManager.updateAppWidget(appWidgetIds, views);
		} else {
			appWidgetManager.updateAppWidget(new ComponentName(context, widgetClass), views);
		}
	}

	/**
	 * Update all active widget instances by pushing changes
	 */
	public abstract void performUpdate(MusicPlaybackService service, int[] appWidgetIds);

	/**
	 * Handle a change notification coming over from
	 * {@link MusicPlaybackService}
	 */
	public abstract void notifyChange(MusicPlaybackService service, String what);
}