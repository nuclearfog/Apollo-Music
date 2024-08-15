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

package org.nuclearfog.apollo.ui.fragments;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.adapters.listview.ThemesAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.ThemesAdapter.ThemeHolder;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.utils.PreferenceUtils;

/**
 * Used to show all of the available themes on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ThemeFragment extends Fragment implements OnItemClickListener {

	/**
	 * grid list adapter to show themes
	 */
	private ThemesAdapter mAdapter;
	private PreferenceUtils mPreferences;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public ThemeFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View rootView = inflater.inflate(R.layout.grid_base, container, false);
		TextView emptyInfo = rootView.findViewById(R.id.grid_base_empty_info);
		GridView mGridView = rootView.findViewById(R.id.grid_base);
		// disable empty view holder
		emptyInfo.setVisibility(View.GONE);
		// init adapter
		mAdapter = new ThemesAdapter(requireContext());
		mPreferences = PreferenceUtils.getInstance(requireContext());
		// Release any reference to the recycled Views
		mGridView.setRecyclerListener(new RecycleHolder());
		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		mGridView.setAdapter(mAdapter);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			// Limit the columns to one in portrait mode
			mGridView.setNumColumns(1);
		} else {
			// And two for landscape
			mGridView.setNumColumns(2);
		}
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// Default theme
		String defName = getString(R.string.app_name);
		Drawable defPrev = ResourcesCompat.getDrawable(getResources(), R.drawable.theme_preview, null);
		ThemeHolder defTheme = new ThemeHolder(defName, defPrev);
		mAdapter.add(defTheme);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ThemeHolder selection = mAdapter.getItem(position);
		if (selection != null) {
			String name = getString(R.string.theme_set, selection.mName);
			mPreferences.setThemeSelectionIndex(position);
			AppMsg.makeText(requireActivity(), name, AppMsg.STYLE_CONFIRM).show();
		}
	}
}