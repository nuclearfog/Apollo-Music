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

package org.nuclearfog.apollo.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * Settings.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SettingsActivity extends AppCompatActivity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_layout);
		Toolbar toolbar = findViewById(R.id.settings_toolbar);
		// setup toolbar
		setSupportActionBar(toolbar);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar != null) {
			actionbar.setDisplayHomeAsUpEnabled(true);
			ThemeUtils mResources = new ThemeUtils(this);
			mResources.themeActionBar(actionbar, R.string.menu_settings);
		}
		//attach fragment
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().replace(R.id.settings_frame, AppPreference.class, null).commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Preference fragment class
	 */
	public static class AppPreference extends PreferenceFragmentCompat implements OnPreferenceClickListener {

		private static final String LICENSE = "open_source";

		private static final String DEL_CACHE = "delete_cache";

		private static final String THEME_SEL = "theme_chooser";

		private static final String COLOR_SEL = "color_scheme";

		private static final String VERSION = "version";

		private static final String BAT_OPT = "disable_battery_opt";

		private static final String OLD_NOTIFICATION = "old_notification_layout";

		private static final String DOWNLOAD_IMAGES = "download_missing_artist_images";

		private static final String DOWNLOAD_ARTWORK = "download_missing_artwork";

		private static final String DOWNLOAD_WIFI = "only_on_wifi";

		/**
		 * dialogs to ask the user for actions
		 */
		private AlertDialog licenseDialog, cacheClearDialog, colorPicker;

		@Nullable
		private CheckBoxPreference downloadImages;
		@Nullable
		private CheckBoxPreference downloadArtwork;
		@Nullable
		private CheckBoxPreference downloadWifi;

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.settings);
			Preference mOpenSourceLicenses = findPreference(LICENSE);
			Preference deleteCache = findPreference(DEL_CACHE);
			Preference themeChooser = findPreference(THEME_SEL);
			Preference colorScheme = findPreference(COLOR_SEL);
			Preference batteryOpt = findPreference(BAT_OPT);
			Preference oldNotification = findPreference(OLD_NOTIFICATION);
			downloadImages = findPreference(DOWNLOAD_IMAGES);
			downloadArtwork = findPreference(DOWNLOAD_ARTWORK);
			downloadWifi = findPreference(DOWNLOAD_WIFI);
			Preference version = findPreference(VERSION);

			if (version != null)
				version.setSummary(BuildConfig.VERSION_NAME);
			if (mOpenSourceLicenses != null)
				mOpenSourceLicenses.setOnPreferenceClickListener(this);
			if (deleteCache != null)
				deleteCache.setOnPreferenceClickListener(this);
			if (themeChooser != null)
				themeChooser.setOnPreferenceClickListener(this);
			if (colorScheme != null)
				colorScheme.setOnPreferenceClickListener(this);
			if (downloadImages != null && downloadArtwork != null) {
				downloadImages.setOnPreferenceClickListener(this);
				downloadArtwork.setOnPreferenceClickListener(this);
				if (downloadWifi != null) {
					downloadWifi.setEnabled(downloadImages.isChecked() || downloadArtwork.isChecked());
				}
			}
			if (batteryOpt != null) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
					batteryOpt.setVisible(false);
				} else {
					batteryOpt.setOnPreferenceClickListener(this);
				}
			}
			if (oldNotification != null) {
				oldNotification.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
			}
			licenseDialog = ApolloUtils.createOpenSourceDialog(requireContext());
			cacheClearDialog = ApolloUtils.createCacheClearDialog(requireContext());
			colorPicker = ApolloUtils.showColorPicker(requireActivity());
		}


		@Override
		public boolean onPreferenceClick(@NonNull Preference preference) {
			switch (preference.getKey()) {
				case LICENSE:
					if (licenseDialog != null && !licenseDialog.isShowing())
						licenseDialog.show();
					return true;

				case DEL_CACHE:
					if (cacheClearDialog != null && !cacheClearDialog.isShowing())
						cacheClearDialog.show();
					return true;

				case COLOR_SEL:
					if (colorPicker != null && !colorPicker.isShowing())
						colorPicker.show();
					return true;

				case THEME_SEL:
					Intent themeChooserIntent = new Intent(requireContext(), ThemesActivity.class);
					startActivity(themeChooserIntent);
					return true;

				case DOWNLOAD_IMAGES:
				case DOWNLOAD_ARTWORK:
					if (downloadWifi != null && downloadArtwork != null && downloadImages != null) {
						downloadWifi.setEnabled(downloadArtwork.isChecked() || downloadImages.isChecked());
					}
					break;

				case BAT_OPT:
					ApolloUtils.redirectToBatteryOptimization(requireActivity());
					return true;
			}
			return false;
		}
	}
}