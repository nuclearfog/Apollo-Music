package com.andrew.apollo.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.widgets.AppWidgetBase;
import com.andrew.apollo.widgets.AppWidgetLarge;
import com.andrew.apollo.widgets.AppWidgetLargeAlternate;
import com.andrew.apollo.widgets.AppWidgetSmall;
import com.andrew.apollo.widgets.RecentWidgetProvider;

import java.lang.ref.WeakReference;

/**
 * widget Broadcast listener
 */
public class WidgetBroadcastReceiver extends BroadcastReceiver {

	private AppWidgetBase smallWidget = new AppWidgetSmall();
	private AppWidgetBase largeWidget = new AppWidgetLarge();
	private AppWidgetBase altWidget = new AppWidgetLargeAlternate();
	private AppWidgetBase recentWidget = new RecentWidgetProvider();

	private WeakReference<MusicPlaybackService> mReference;


	public WidgetBroadcastReceiver(MusicPlaybackService mService) {
		mReference = new WeakReference<>(mService);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		MusicPlaybackService mService = mReference.get();
		if (mService != null) {
			String command = intent.getStringExtra(MusicPlaybackService.CMDNAME);
			int[] small = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

			if (AppWidgetSmall.CMDAPPWIDGETUPDATE.equals(command)) {
				smallWidget.performUpdate(mService, small);
			} else if (AppWidgetLarge.CMDAPPWIDGETUPDATE.equals(command)) {
				largeWidget.performUpdate(mService, small);
			} else if (AppWidgetLargeAlternate.CMDAPPWIDGETUPDATE.equals(command)) {
				altWidget.performUpdate(mService, small);
			} else if (RecentWidgetProvider.CMDAPPWIDGETUPDATE.equals(command)) {
				recentWidget.performUpdate(mService, small);
			} else {
				mService.handleCommandIntent(intent);
			}
		}
	}

	/**
	 * update app widgets
	 */
	public void updateWidgets(MusicPlaybackService service, String what) {
		smallWidget.notifyChange(service, what);
		largeWidget.notifyChange(service, what);
		altWidget.notifyChange(service, what);
		recentWidget.notifyChange(service, what);
	}
}