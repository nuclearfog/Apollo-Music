package org.nuclearfog.apollo.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.widgets.AppWidgetBase;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLarge;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLargeAlternate;
import org.nuclearfog.apollo.ui.widgets.AppWidgetSmall;
import org.nuclearfog.apollo.ui.widgets.RecentWidgetProvider;

/**
 * widget Broadcast listener
 *
 * @author nuclearfog
 */
public class WidgetBroadcastReceiver extends BroadcastReceiver {

	private AppWidgetBase smallWidget = new AppWidgetSmall();
	private AppWidgetBase largeWidget = new AppWidgetLarge();
	private AppWidgetBase altWidget = new AppWidgetLargeAlternate();
	private AppWidgetBase recentWidget = new RecentWidgetProvider();

	private MusicPlaybackService service;


	public WidgetBroadcastReceiver(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String command = intent.getStringExtra(MusicPlaybackService.CMDNAME);
		int[] small = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		if (AppWidgetSmall.CMDAPPWIDGETUPDATE.equals(command)) {
			smallWidget.performUpdate(service, small);
		} else if (AppWidgetLarge.CMDAPPWIDGETUPDATE.equals(command)) {
			largeWidget.performUpdate(service, small);
		} else if (AppWidgetLargeAlternate.CMDAPPWIDGETUPDATE.equals(command)) {
			altWidget.performUpdate(service, small);
		} else if (RecentWidgetProvider.CMDAPPWIDGETUPDATE.equals(command)) {
			recentWidget.performUpdate(service, small);
		} else {
			service.handleCommandIntent(intent);
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