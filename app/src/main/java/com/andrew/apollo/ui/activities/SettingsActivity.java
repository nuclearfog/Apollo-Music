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

package com.andrew.apollo.ui.activities;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

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
        View root = findViewById(R.id.settings_root);

        root.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.pager_background, null));
        setSupportActionBar(toolbar);
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // UP
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            ThemeUtils mResources = new ThemeUtils(this);
            mResources.themeActionBar(bar, R.string.menu_settings);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings_frame, new AppPreference()).commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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


    public static class AppPreference extends PreferenceFragmentCompat implements OnPreferenceClickListener {

        /**
         * Image cache
         */
        private ImageCache mImageCache;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);

            // Initialze the image cache
            mImageCache = ImageCache.getInstance(requireContext());

            Preference mOpenSourceLicenses = findPreference("open_source");
            Preference deleteCache = findPreference("delete_cache");
            Preference themeChooser = findPreference("theme_chooser");
            Preference colorScheme = findPreference("color_scheme");
            Preference version = findPreference("version");

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
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {

                case "open_source":
                    ApolloUtils.createOpenSourceDialog(requireContext()).show();
                    return true;

                case "delete_cache":
                    new AlertDialog.Builder(requireContext()).setMessage(R.string.delete_warning)
                            .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mImageCache.clearCaches();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return true;

                case "theme_chooser":
                    Intent themeChooserIntent = new Intent(requireContext(), ThemesAppCompat.class);
                    startActivity(themeChooserIntent);
                    return true;

                case "color_scheme":
                    ApolloUtils.showColorPicker(requireActivity());
                    return true;
            }
            return false;
        }
    }
}