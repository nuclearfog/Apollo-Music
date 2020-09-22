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

package com.andrew.apollo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;

import com.andrew.apollo.R;

/**
 * In order to implement the theme chooser for Apollo, this class returns a
 * {@link Resources} object that can be used like normal. In other words, when
 * {@code getDrawable()} or {@code getColor()} is called, the object returned is
 * from the current theme package name and because all of the theme resource
 * identifiers are the same as all of Apollo's resources a little less code is
 * used to implement the theme chooser.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeUtils {

    /**
     * Default package name.
     */
    public static final String APOLLO_PACKAGE = "com.andrew.apollo";
    /**
     * Current theme package name.
     */
    public static final String PACKAGE_NAME = "theme_package_name";
    /**
     * Used to searc the "Apps" section of the Play Store for "Apollo Themes".
     */
    private static final String SEARCH_URI = "https://market.android.com/search?q=%s&c=apps&featured=APP_STORE_SEARCH";
    /**
     * Used to search the Play Store for a specific theme.
     */
    private static final String APP_URI = "market://details?id=";
    /**
     * The keyword to use when search for different themes.
     */
    private static String sApolloSearch;
    /**
     * Used to get and set the theme package name.
     */
    private final SharedPreferences mPreferences;

    /**
     * Custom action bar layout
     */
    private final View mActionBarLayout;

    /**
     * The theme resources.
     */
    private Resources mResources;

    /**
     * Constructor for <code>ThemeUtils</code>
     *
     * @param context The {@link Context} to use.
     */
    public ThemeUtils(Context context) {
        // Get the search query
        sApolloSearch = context.getString(R.string.apollo_themes_shop_key);
        // Get the preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Get the theme package name
        String mThemePackage = getThemePackageName();
        // Initialze the package manager
        PackageManager mPackageManager = context.getPackageManager();
        try {
            // Find the theme resources
            mResources = mPackageManager.getResourcesForApplication(mThemePackage);
        } catch (Exception e) {
            // If the user isn't using a theme, then the resources should be
            // Apollo's.
            setThemePackageName(APOLLO_PACKAGE);
            mResources = context.getResources();
            e.printStackTrace();
        }
        // Inflate the custom layout
        mActionBarLayout = View.inflate(context, R.layout.action_bar, null);
    }

    /**
     * Used to search the Play Store for a specific app.
     *
     * @param context   The {@link Context} to use.
     * @param themeName The theme name to search for.
     */
    public static void openAppPage(Context context, String themeName) {
        Intent shopIntent = new Intent(Intent.ACTION_VIEW);
        shopIntent.setData(Uri.parse(APP_URI + themeName));
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (shopIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(shopIntent);
        }
    }

    /**
     * Return the current theme package name.
     *
     * @return The default theme package name.
     */
    public final String getThemePackageName() {
        return mPreferences.getString(PACKAGE_NAME, APOLLO_PACKAGE);
    }

    /**
     * Set the new theme package name.
     *
     * @param packageName The package name of the theme to be set.
     */
    public void setThemePackageName(String packageName) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PACKAGE_NAME, packageName);
        editor.apply();
    }

    /**
     * Sets the corret overflow icon in the action bar depending on whether or
     * not the current action bar color is dark or light.
     *
     * @param app The {@link Context} used to set the theme.
     */
    public void setOverflowStyle(Context app) {
        app.setTheme(R.style.Apollo_Theme_Dark);
    }

    /**
     * This is used to set the color of a {@link MenuItem}. For instance, when
     * the current song is a favorite, the favorite icon will use the current
     * theme color.
     *
     * @param menuItem The {@link MenuItem} to set.
     * @param maskDrawable The drawable theme resource key.
     */
    public void setMenuItemColor(MenuItem menuItem, Drawable maskDrawable) {
        if (maskDrawable instanceof BitmapDrawable) {
            Bitmap maskBitmap = ((BitmapDrawable) maskDrawable).getBitmap();
            int width = maskBitmap.getWidth();
            int height = maskBitmap.getHeight();

            Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(outBitmap);
            canvas.drawBitmap(maskBitmap, 0, 0, null);

            Paint maskedPaint = new Paint();
            maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

            canvas.drawRect(0, 0, width, height, maskedPaint);

            BitmapDrawable outDrawable = new BitmapDrawable(mResources, outBitmap);
            menuItem.setIcon(outDrawable);
        }
    }

    /**
     * Sets the {@link MenuItem} icon for the favorites action.
     *
     * @param favorite The favorites action.
     */
    public void setFavoriteIcon(MenuItem favorite) {
        Drawable favIcon;
        if (MusicUtils.isFavorite()) {
            favIcon = ResourcesCompat.getDrawable(mResources, R.color.favorite_selected, null);
        } else {
            favIcon = ResourcesCompat.getDrawable(mResources, R.color.favorite_normal, null);
        }
        favorite.setIcon(favIcon);
    }

    /**
     * Sets the {@link MenuItem} icon for the search action.
     *
     * @param search The Menu used to find the "menu_search" action.
     */
    public void setSearchIcon(MenuItem search) {
        Drawable searchIcon = ResourcesCompat.getDrawable(mResources, R.drawable.ic_action_search, null);
        setMenuItemColor(search, searchIcon);
    }

    /**
     * Sets the {@link MenuItem} icon for the shop action.
     *
     * @param search The Menu used to find the "menu_shop" action.
     */
    public void setShopIcon(Menu search) {
        MenuItem shopAction = search.findItem(R.id.menu_shop);
        Drawable shopIcon = ResourcesCompat.getDrawable(mResources, R.drawable.ic_action_shop, null);
        setMenuItemColor(shopAction, shopIcon);
    }

    /**
     * Sets the {@link MenuItem} icon for the add to Home screen action.
     *
     * @param search The Menu used to find the "add_to_homescreen" item.
     */
    public void setAddToHomeScreenIcon(Menu search) {
        MenuItem pinnAction = search.findItem(R.id.menu_add_to_homescreen);
        Drawable pinIcon = ResourcesCompat.getDrawable(mResources, R.drawable.ic_action_pinn_to_home, null);
        setMenuItemColor(pinnAction, pinIcon);
    }

    /**
     * Builds a custom layout and applies it to the action bar, then themes the
     * background, title, and subtitle.
     *
     * @param actionBar The {@link ActionBar} to use.
     * @param titleID     The title for the action bar
     */
    public void themeActionBar(ActionBar actionBar, @StringRes int titleID) {
        String title = mResources.getString(titleID);
        // Set the custom layout
        actionBar.setCustomView(mActionBarLayout);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        // Theme the action bar background
        Drawable background = ResourcesCompat.getDrawable(mResources, R.drawable.action_bar, null);
        actionBar.setBackgroundDrawable(background);
        // Theme the title
        setTitle(title);
    }

    /**
     * Themes the action bar subtitle
     */
    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            // Get the title text view
            TextView actionBarTitle = mActionBarLayout.findViewById(R.id.action_bar_title);
            // Theme the title
            int textColor = ResourcesCompat.getColor(mResources, R.color.action_bar_title, null);
            actionBarTitle.setTextColor(textColor);
            // Set the title
            actionBarTitle.setText(title);
        }
    }

    /**
     * Themes the action bar subtitle
     *
     * @param subtitle The subtitle to use
     */
    public void setSubtitle(String subtitle) {
        if (!TextUtils.isEmpty(subtitle)) {
            TextView actionBarSubtitle = mActionBarLayout.findViewById(R.id.action_bar_subtitle);
            actionBarSubtitle.setVisibility(View.VISIBLE);
            // Theme the subtitle
            int color = ResourcesCompat.getColor(mResources, R.color.action_bar_subtitle, null);
            actionBarSubtitle.setTextColor(color);
            // Set the subtitle
            actionBarSubtitle.setText(subtitle);
        }
    }

    /**
     * Used to search the Play Store for "Apollo Themes".
     *
     * @param context The {@link Context} to use.
     */
    public void shopFor(Context context) {
        Intent shopIntent = new Intent(Intent.ACTION_VIEW);
        shopIntent.setData(Uri.parse(String.format(SEARCH_URI, Uri.encode(sApolloSearch))));
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (shopIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(shopIntent);
        }
    }
}