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

import static org.nuclearfog.apollo.utils.MusicUtils.REQUEST_DELETE_FILES;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends ActivityBase {

	/**
	 *
	 */
	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getContentView() {
		return R.layout.activity_base;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void init(Bundle savedInstanceState) {
		Toolbar toolbar = findViewById(R.id.activity_base_toolbar);
		// init fragment callback
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		// Set the overflow style
		mResources.setOverflowStyle(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.app_name);
		}
		getSupportFragmentManager().beginTransaction().replace(R.id.activity_base_content, MusicBrowserPhoneFragment.class, null).commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		viewModel.notify(MusicBrowserPhoneFragment.REFRESH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		viewModel.notify(MusicBrowserPhoneFragment.META_CHANGED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.onPostDelete(this);
		}
	}
}