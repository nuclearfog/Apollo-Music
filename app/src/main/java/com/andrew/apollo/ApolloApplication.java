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

import android.app.Application;
import android.os.StrictMode;

import com.andrew.apollo.cache.ImageCache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to turn off logging for jaudiotagger and free up memory when
 * {@code #onLowMemory()} is called on pre-ICS devices. On post-ICS memory is
 * released within {@link ImageCache}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ApolloApplication extends Application {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Enable strict mode logging
		if (BuildConfig.DEBUG) {
			//enableStrictMode();
		}
		// Turn off logging for jaudiotagger.
		Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLowMemory() {
		ImageCache.getInstance(this).evictAll();
		super.onLowMemory();
	}

	/**
	 *
	 */
	private void enableStrictMode() {
		StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog();
		StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder().detectAll().penaltyLog();
		threadPolicyBuilder.penaltyFlashScreen();
		StrictMode.setThreadPolicy(threadPolicyBuilder.build());
		StrictMode.setVmPolicy(vmPolicyBuilder.build());
	}
}