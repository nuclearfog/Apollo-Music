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

package com.andrew.apollo.widgets;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.andrew.apollo.MusicPlaybackService;

public abstract class AppWidgetBase extends AppWidgetProvider {

	@SuppressLint("UnspecifiedImmutableFlag")
	protected PendingIntent buildPendingIntent(Context context, String action, ComponentName serviceName) {
		Intent intent = new Intent(action);
		intent.setComponent(serviceName);
		intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		return PendingIntent.getService(context, 0, intent, 0);
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