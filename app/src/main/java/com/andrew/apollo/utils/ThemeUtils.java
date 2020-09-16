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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
     * The theme package name.
     */
    private final String mThemePackage;
    /**
     * This is the current theme color as set by the color picker.
     */
    private final int mCurrentThemeColor;

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
        mThemePackage = getThemePackageName();
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
        // Get the current theme color
        mCurrentThemeColor = PreferenceUtils.getInstance(context).getDefaultThemeColor(context);
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
     * Used to return a color from the theme resources.
     *
     * @param resourceName The name of the color to return. i.e.
     *                     "action_bar_color".
     * @return A new color from the theme resources.
     */
    public int getColor(String resourceName) {
        try {
            int resourceId = mResources.getIdentifier(resourceName, "color", mThemePackage);
            return mResources.getColor(resourceId);
        } catch (final Resources.NotFoundException e) {
            // If the theme designer wants to allow the user to theme a
            // particular object via the color picker, they just remove the
            // resource item from the theme config.xml file.
        }
        return mCurrentThemeColor;
    }

    /**
     * Used to return a drawable from the theme resources.
     *
     * @param resourceName The name of the drawable to return. i.e.
     *                     "pager_background".
     * @return A new color from the theme resources.
     */
    public Drawable getDrawable(String resourceName) {
        try {
            final int resourceId = mResources.getIdentifier(resourceName, "drawable", mThemePackage);
            return ResourcesCompat.getDrawable(mResources, resourceId, null);
        } catch (final Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Used to tell if the action bar's backgrond color is dark or light and
     * depending on which the proper overflow icon is set from a style.
     *
     * @return True if the action bar color is dark, false if light.
     */
    public boolean isActionBarDark() {
        return ApolloUtils.isColorDark(getColor("action_bar_color"));
    }

    /**
     * Sets the corret overflow icon in the action bar depending on whether or
     * not the current action bar color is dark or light.
     *
     * @param app The {@link AppCompatActivity} used to set the theme.
     */
    public void setOverflowStyle(AppCompatActivity app) {
        if (isActionBarDark()) {
            app.setTheme(R.style.Apollo_Theme_Dark);
        } else {
            app.setTheme(R.style.Apollo_Theme_Light);
        }
    }

    /**
     * This is used to set the color of a {@link MenuItem}. For instance, when
     * the current song is a favorite, the favorite icon will use the current
     * theme color.
     *
     * @param menuItem             The {@link MenuItem} to set.
     * @param resourceColorName    The color theme resource key.
     * @param resourceDrawableName The drawable theme resource key.
     */
    public void setMenuItemColor(MenuItem menuItem, String resourceColorName, String resourceDrawableName) {

        Drawable maskDrawable = getDrawable(resourceDrawableName);
        if (!(maskDrawable instanceof BitmapDrawable)) {
            return;
        }

        Bitmap maskBitmap = ((BitmapDrawable) maskDrawable).getBitmap();
        int width = maskBitmap.getWidth();
        int height = maskBitmap.getHeight();

        Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outBitmap);
        canvas.drawBitmap(maskBitmap, 0, 0, null);

        Paint maskedPaint = new Paint();
        maskedPaint.setColor(getColor(resourceColorName));
        maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        canvas.drawRect(0, 0, width, height, maskedPaint);

        BitmapDrawable outDrawable = new BitmapDrawable(mResources, outBitmap);
        menuItem.setIcon(outDrawable);
    }

    /**
     * Sets the {@link MenuItem} icon for the favorites action.
     *
     * @param favorite The favorites action.
     */
    public void setFavoriteIcon(Menu favorite) {
        MenuItem favoriteAction = favorite.findItem(R.id.menu_favorite);
        String favoriteIconId = "ic_action_favorite";
        if (MusicUtils.isFavorite()) {
            setMenuItemColor(favoriteAction, "favorite_selected", favoriteIconId);
        } else {
            setMenuItemColor(favoriteAction, "favorite_normal", favoriteIconId);
        }
    }

    /**
     * Sets the {@link MenuItem} icon for the search action.
     *
     * @param search The Menu used to find the "menu_search" action.
     */
    public void setSearchIcon(Menu search) {
        final MenuItem searchAction = search.findItem(R.id.menu_search);
        final String searchIconId = "ic_action_search";
        setMenuItemColor(searchAction, "search_action", searchIconId);
    }

    /**
     * Sets the {@link MenuItem} icon for the shop action.
     *
     * @param search The Menu used to find the "menu_shop" action.
     */
    public void setShopIcon(Menu search) {
        final MenuItem shopAction = search.findItem(R.id.menu_shop);
        final String shopIconId = "ic_action_shop";
        setMenuItemColor(shopAction, "shop_action", shopIconId);
    }

    /**
     * Sets the {@link MenuItem} icon for the add to Home screen action.
     *
     * @param search The Menu used to find the "add_to_homescreen" item.
     */
    public void setAddToHomeScreenIcon(Menu search) {
        MenuItem pinnAction = search.findItem(R.id.menu_add_to_homescreen);
        String pinnIconId = "ic_action_pinn_to_home";
        setMenuItemColor(pinnAction, "pinn_to_action", pinnIconId);
    }

    /**
     * Builds a custom layout and applies it to the action bar, then themes the
     * background, title, and subtitle.
     *
     * @param actionBar The {@link ActionBar} to use.
     * @param title     The title for the action bar
     */
    public void themeActionBar(ActionBar actionBar, String title) {
        // Set the custom layout
        actionBar.setCustomView(mActionBarLayout);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        // Theme the action bar background
        actionBar.setBackgroundDrawable(getDrawable("action_bar"));

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
            actionBarTitle.setTextColor(getColor("action_bar_title"));
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
            actionBarSubtitle.setTextColor(getColor("action_bar_subtitle"));
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