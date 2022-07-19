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

package com.andrew.apollo.adapters;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.andrew.apollo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link FragmentStatePagerAdapter} class for swiping between playlists, recent,
 * artists, albums, songs, and genre {@link Fragment}s on phones.<br/>
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

	private List<Fragment> fragments = new ArrayList<>(4);

	private String[] titles;

	/**
	 * Constructor of <code>PagerAdapter<code>
	 */
	public PagerAdapter(Context context, FragmentManager fm) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		titles = context.getResources().getStringArray(R.array.page_titles);
	}

	/**
	 * Method that adds a new fragment class to the viewer (the fragment is
	 * internally instantiate)
	 *
	 * @param fragment The full qualified name of fragment class.
	 * @param params   The instantiate params.
	 */
	public void add(Fragment fragment, @Nullable Bundle params) {
		if (params != null)
			fragment.setArguments(params);
		fragments.add(fragment);
		notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@NonNull
	public Fragment getItem(int position) {
		return fragments.get(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		return fragments.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position].toUpperCase(Locale.getDefault());
	}

	/**
	 * clear all fragments from adapter
	 */
	public void clear() {
		fragments.clear();
		notifyDataSetChanged();
	}
}