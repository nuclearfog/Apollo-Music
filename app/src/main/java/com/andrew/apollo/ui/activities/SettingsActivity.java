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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.ui.fragments.ThemeFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.widgets.ColorSchemeDialog;

/**
 * Settings.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SettingsActivity extends PreferenceActivity {

    /**
     * Image cache
     */
    private ImageCache mImageCache;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Initialze the image cache
        mImageCache = ImageCache.getInstance(this);
        // UP
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        // Add the preferences
        addPreferencesFromResource(R.xml.settings);
        // Interface settings
        initInterface();
        // Removes the cache entries
        deleteCache();
        // About
        showOpenSourceLicenses();
        // Update the version number
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            findPreference("version").setSummary(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            findPreference("version").setSummary("?");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
     * Initializes the preferences under the "Interface" category
     */
    private void initInterface() {
        // Color scheme picker
        updateColorScheme();
        // Open the theme chooser
        openThemeChooser();
    }

    /**
     * Shows the {@link ColorSchemeDialog} and then saves the changes.
     */
    private void updateColorScheme() {
        Preference colorScheme = findPreference("color_scheme");
        colorScheme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ApolloUtils.showColorPicker(SettingsActivity.this);
                return true;
            }
        });
    }

    /**
     * Opens the {@link ThemeFragment}.
     */
    private void openThemeChooser() {
        final Preference themeChooser = findPreference("theme_chooser");
        themeChooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Intent themeChooserIntent = new Intent(SettingsActivity.this, ThemesAppCompat.class);
                startActivity(themeChooserIntent);
                return true;
            }
        });
    }

    /**
     * Removes all of the cache entries.
     */
    private void deleteCache() {
        Preference deleteCache = findPreference("delete_cache");
        deleteCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new AlertDialog.Builder(SettingsActivity.this).setMessage(R.string.delete_warning)
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                mImageCache.clearCaches();
                            }
                        }).setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.dismiss();
                    }
                }).create().show();
                return true;
            }
        });
    }

    /**
     * Show the open source licenses
     */
    private void showOpenSourceLicenses() {
        Preference mOpenSourceLicenses = findPreference("open_source");
        mOpenSourceLicenses.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ApolloUtils.createOpenSourceDialog(SettingsActivity.this).show();
                return true;
            }
        });
    }
}