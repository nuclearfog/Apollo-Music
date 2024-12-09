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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.preference.PreferenceFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * Apollo settings activity
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
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
		View root = findViewById(R.id.settings_root);
		// setup toolbar
		setSupportActionBar(toolbar);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar != null) {
			actionbar.setDisplayHomeAsUpEnabled(true);
			ThemeUtils mResources = new ThemeUtils(this);
			mResources.themeActionBar(actionbar, R.string.menu_settings);
			mResources.setBackground(root);
		}
		//attach fragment
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().replace(R.id.settings_frame, PreferenceFragment.class, null).commit();
		}
		ApolloUtils.setWakelock(this);
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
}