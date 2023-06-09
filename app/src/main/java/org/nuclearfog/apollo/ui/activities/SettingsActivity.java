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
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
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
			getSupportFragmentManager().beginTransaction().replace(R.id.settings_frame, new AppPreference()).commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		MusicUtils.notifyForegroundStateChanged(this, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		super.onStop();
		MusicUtils.notifyForegroundStateChanged(this, false);
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

		/**
		 * dialogs to ask the user for actions
		 */
		private AlertDialog licenseDialog, cacheClearDialog, colorPicker;


		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.settings);

			Preference mOpenSourceLicenses = findPreference(LICENSE);
			Preference deleteCache = findPreference(DEL_CACHE);
			Preference themeChooser = findPreference(THEME_SEL);
			Preference colorScheme = findPreference(COLOR_SEL);
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
			}
			return false;
		}
	}
}