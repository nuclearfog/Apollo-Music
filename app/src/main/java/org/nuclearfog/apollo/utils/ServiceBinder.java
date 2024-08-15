package org.nuclearfog.apollo.utils;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import org.nuclearfog.apollo.IApolloService;

import java.lang.ref.WeakReference;

/**
 * Connector class used by Activities to communicate to Service
 *
 * @author nuclearfog
 */
public class ServiceBinder implements ServiceConnection {

	private WeakReference<ServiceBinderCallback> callback;
	private IApolloService service;

	private boolean stopForeground = false;

	/**
	 * @param callback callbeck used to informa activity that the service is connected
	 */
	public ServiceBinder(ServiceBinderCallback callback) {
		this.callback = new WeakReference<>(callback);
	}


	@Override
	public void onServiceConnected(ComponentName name, IBinder iBinder) {
		service = IApolloService.Stub.asInterface(iBinder);
		try {
			if (stopForeground) {
				stopForeground = false;
				service.stopForeground();
			}
		} catch (RemoteException e) {
			// ignore
		}
		if (callback.get() != null) {
			callback.get().onServiceConnected();
		}
	}


	@Override
	public void onServiceDisconnected(ComponentName name) {
		service = null;
		stopForeground = false;
	}

	/**
	 * get attached service interface
	 */
	public IApolloService getService() {
		return service;
	}

	/**
	 * stop foreground activity of the service after connecting
	 */
	public void stopForeground() {
		if (service != null) {
			try {
				service.stopForeground();
			} catch (RemoteException e) {
				stopForeground = true;
			}
		} else {
			stopForeground = true;
		}
	}

	/**
	 * callback interface used to inform the activity when a service is connected/disconnected
	 */
	public interface ServiceBinderCallback {

		/**
		 * called when the service was connected successfully
		 */
		void onServiceConnected();
	}
}