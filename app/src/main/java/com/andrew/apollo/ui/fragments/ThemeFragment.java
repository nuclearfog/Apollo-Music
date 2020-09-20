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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.widget.ArrayAdapter;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.MusicHolder;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.ThemeUtils;
import com.devspark.appmsg.AppMsg;

import java.util.List;

/**
 * Used to show all of the available themes on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeFragment extends Fragment implements OnItemClickListener {

    private static final int OPEN_IN_PLAY_STORE = 0;

    private GridView mGridView;

    private String[] mEntries;

    private String[] mValues;

    private Drawable[] mThemePreview;

    private Resources mThemeResources;

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
        // The View for the fragment's UI
        View rootView = inflater.inflate(R.layout.grid_base, container, false);
        // Initialize the grid
        mGridView = rootView.findViewById(R.id.grid_base);
        // Release any reference to the recycled Views
        mGridView.setRecyclerListener(new RecycleHolder());
        // Set the new theme
        mGridView.setOnItemClickListener(this);
        // Listen for ContextMenus to be created
        mGridView.setOnCreateContextMenuListener(this);
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
        Intent apolloThemeIntent = new Intent("com.andrew.apollo.THEMES");
        apolloThemeIntent.addCategory("android.intent.category.DEFAULT");

        PackageManager mPackageManager = requireActivity().getPackageManager();
        List<ResolveInfo> mThemes = mPackageManager.queryIntentActivities(apolloThemeIntent, 0);
        mEntries = new String[mThemes.size() + 1];
        mValues = new String[mThemes.size() + 1];
        mThemePreview = new Drawable[mThemes.size() + 1];

        // Default items
        mEntries[0] = getString(R.string.app_name);
        // mValues[0] = ThemeUtils.APOLLO_PACKAGE;
        mThemePreview[0] = ResourcesCompat.getDrawable(getResources(), R.drawable.theme_preview, null);

        for (int i = 0; i < mThemes.size(); i++) {
            String mThemePackageName = mThemes.get(i).activityInfo.packageName;
            String mThemeName = mThemes.get(i).loadLabel(mPackageManager).toString();
            mEntries[i + 1] = mThemeName;
            mValues[i + 1] = mThemePackageName;
            // Theme resources
            try {
                mThemeResources = mPackageManager.getResourcesForApplication(mThemePackageName);
            } catch (NameNotFoundException ignored) {
            }
            // Theme preview
            final int previewId = mThemeResources.getIdentifier("theme_preview", "drawable", mThemePackageName);
            if (previewId != 0) {
                mThemePreview[i + 1] = ResourcesCompat.getDrawable(getResources(), previewId, null);
            }
        }
        // Initialize the Adapter
        ThemesAdapter mAdapter = new ThemesAdapter(requireContext(), R.layout.fragment_themes_base);
        // Bind the data
        mGridView.setAdapter(mAdapter);
        // Get the theme utils
        mTheme = new ThemeUtils(requireContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (info.position > 0) {
            // Open to the theme's Play Store page
            menu.add(Menu.NONE, OPEN_IN_PLAY_STORE, Menu.NONE, R.string.context_menu_open_in_play_store);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == OPEN_IN_PLAY_STORE) {
            ThemeUtils.openAppPage(requireContext(), mValues[info.position]);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mTheme.setThemePackageName(mValues[position]);
        AppMsg.makeText(getActivity(), getString(R.string.theme_set, mEntries[position]), AppMsg.STYLE_CONFIRM).show();
    }

    public final static class DataHolder {

        public String mName;

        public Drawable mPreview;

        /**
         * Constructor of <code>DataHolder</code>
         */
        public DataHolder() {
            super();
        }
    }

    /**
     * Populates the {@link GridView} with the available themes
     */
    private class ThemesAdapter extends ArrayAdapter<ResolveInfo> {

        /**
         * Number of views (ImageView and TextView)
         */
        private static final int VIEW_TYPE_COUNT = 2;

        /**
         * The resource ID of the layout to inflate
         */
        private final int mLayoutID;

        /**
         * Used to cache the theme info
         */
        private DataHolder[] mData;

        /**
         * Constructor of <code>ThemesAdapter</code>
         *
         * @param context  The {@link Context} to use.
         * @param layoutID The resource ID of the view to inflate.
         */
        public ThemesAdapter(Context context, int layoutID) {
            super(context, 0);
            // Get the layout ID
            mLayoutID = layoutID;
            // Build the cache
            buildCache();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return mEntries.length;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            /* Recycle ViewHolder's items */
            MusicHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mLayoutID, parent, false);
                holder = new MusicHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (MusicHolder) convertView.getTag();
            }
            // Retrieve the data holder
            DataHolder dataHolder = mData[position];
            // Set the theme preview
            holder.mImage.setImageDrawable(dataHolder.mPreview);
            // Set the theme name
            holder.mLineOne.setText(dataHolder.mName);
            return convertView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasStableIds() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        /**
         * Method used to cache the data used to populate the list or grid. The
         * idea is to cache everything before
         * {@code #getView(int, View, ViewGroup)} is called.
         */
        private void buildCache() {
            mData = new DataHolder[getCount()];
            for (int i = 0; i < getCount(); i++) {
                // Build the data holder
                mData[i] = new DataHolder();
                // Theme names (line one)
                mData[i].mName = mEntries[i];
                // Theme preview
                mData[i].mPreview = mThemePreview[i];
            }
        }

    }
}