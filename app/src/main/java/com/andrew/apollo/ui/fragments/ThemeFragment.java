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

package com.andrew.apollo.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ThemesAdapter;
import com.andrew.apollo.adapters.ThemesAdapter.ThemeHolder;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.ThemeUtils;
import com.devspark.appmsg.AppMsg;

import java.util.List;

import static android.content.Intent.CATEGORY_DEFAULT;

/**
 * Used to show all of the available themes on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeFragment extends Fragment implements OnItemClickListener {

    /**
     * context menu item ID
     */
    private static final int OPEN_IN_PLAY_STORE = 0x5E31DA11;

    /**
     * context menu ID
     */
    private static final int GROUP_ID = 0x500FC67C;

    /**
     *
     */
    private static final String THEME_PREVIEW = "theme_preview";

    /**
     * grid list adapter to show themes
     */
    private ThemesAdapter mAdapter;

    /**
     * utils to setup theme
     */
    private ThemeUtils mTheme;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public ThemeFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // init views
        View rootView = inflater.inflate(R.layout.grid_base, container, false);
        GridView mGridView = rootView.findViewById(R.id.grid_base);
        // init adapter
        mAdapter = new ThemesAdapter(requireContext());
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent apolloThemeIntent = new Intent(BuildConfig.APPLICATION_ID + ".THEMES");
        apolloThemeIntent.addCategory(CATEGORY_DEFAULT);
        mTheme = new ThemeUtils(requireContext());

        // get all compatible themes
        PackageManager mPackageManager = requireActivity().getPackageManager();
        List<ResolveInfo> mThemes = mPackageManager.queryIntentActivities(apolloThemeIntent, 0);

        // Default theme
        String defName = getString(R.string.app_name);
        String defPack = BuildConfig.APPLICATION_ID;
        Drawable defPrev = ResourcesCompat.getDrawable(getResources(), R.drawable.theme_preview, null);
        ThemeHolder defTheme = new ThemeHolder(defPack, defName, defPrev);
        mAdapter.add(defTheme);

        for (int i = 0; i < mThemes.size(); i++) {
            try {
                Drawable prev = null;
                String tPackage = mThemes.get(i).activityInfo.packageName;
                Resources mThemeResources = mPackageManager.getResourcesForApplication(tPackage);
                String name = mThemes.get(i).loadLabel(mPackageManager).toString();

                // get preview
                int previewId = mThemeResources.getIdentifier(THEME_PREVIEW, "drawable", tPackage);
                if (previewId > 0) {
                    prev = ResourcesCompat.getDrawable(mThemeResources, previewId, null);
                }

                // add to adapter
                ThemeHolder holder = new ThemeHolder(tPackage, name, prev);
                mAdapter.add(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (info.position > 0) {
            // Open to the theme's Play Store page
            menu.add(GROUP_ID, OPEN_IN_PLAY_STORE, Menu.NONE, R.string.context_menu_open_in_play_store);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getGroupId() == GROUP_ID && item.getItemId() == OPEN_IN_PLAY_STORE) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            ThemeHolder holder = mAdapter.getItem(info.position);
            if (holder != null) {
                ThemeUtils.openAppPage(requireContext(), holder.mName);
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ThemeHolder holder = mAdapter.getItem(position);
        if (holder != null) {
            String name = getString(R.string.theme_set, holder.mName);
            mTheme.setThemePackageName(holder.mPackage);
            AppMsg.makeText(requireActivity(), name, AppMsg.STYLE_CONFIRM).show();
        }
    }
}