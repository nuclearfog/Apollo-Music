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

import android.app.Application;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.cache.ImageCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to turn off logging for jaudiotagger and free up memory when
 * {@code #onLowMemory()} is called on pre-ICS devices. On post-ICS memory is
 * released within {@link ImageCache}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ApolloApplication extends Application implements UncaughtExceptionHandler {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Turn off logging for jaudiotagger.
		Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
		// add error handler to write stacktrace to file
		Thread.setDefaultUncaughtExceptionHandler(this);
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
	 * {@inheritDoc}
	 */
	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
		// write stacktrace file to cache folder
		try {
			File outputFile = new File(getExternalCacheDir(), "stacktrace.txt");
			FileOutputStream fos = new FileOutputStream(outputFile);
			PrintStream ps = new PrintStream(fos);
			e.printStackTrace(ps);
			ps.close();
		} catch (FileNotFoundException ex) {
			// ignore
		}
		// delegate error handling to Android system
		UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (oldHandler != null) {
			oldHandler.uncaughtException(t, e);
		} else {
			System.exit(2);
		}
	}
}