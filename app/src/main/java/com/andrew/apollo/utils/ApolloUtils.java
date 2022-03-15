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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.ui.activities.HomeActivity;
import com.andrew.apollo.ui.activities.ShortcutActivity;
import com.andrew.apollo.widgets.ColorPickerView;
import com.andrew.apollo.widgets.ColorSchemeDialog;
import com.devspark.appmsg.AppMsg;

import java.io.File;

/**
 * Mostly general and UI helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class ApolloUtils {

    /* This class is never initiated */
    private ApolloUtils() {
    }

    /**
     * Used to determine if the device is currently in landscape mode
     *
     * @param context The {@link Context} to use.
     * @return True if the device is in landscape mode, false otherwise.
     */
    public static boolean isLandscape(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Used to determine if there is an active data connection and what type of
     * connection it is if there is one
     *
     * @param context The {@link Context} to use
     * @return True if there is an active data connection, false otherwise.
     * Also, if the user has checked to only download via Wi-Fi in the
     * settings, the mobile data and other network connections aren't
     * returned at all
     */
    public static boolean isOnline(Context context) {
        /*
         * This sort of handles a sudden configuration change, but I think it
         * should be dealt with in a more professional way.
         */
        if (context == null) {
            return false;
        }

        boolean state = false;
        boolean onlyOnWifi = PreferenceUtils.getInstance(context).onlyOnWifi();
        /* Monitor network connections */
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        /* Wi-Fi connection */
        NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null) {
            state = wifiNetwork.isConnectedOrConnecting();
        }
        /* Mobile data connection */
        NetworkInfo mbobileNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mbobileNetwork != null) {
            if (!onlyOnWifi) {
                state = mbobileNetwork.isConnectedOrConnecting();
            }
        }
        /* Other networks */
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (!onlyOnWifi) {
                state = activeNetwork.isConnectedOrConnecting();
            }
        }
        return state;
    }

    /**
     * Display a {@link Toast} letting the user know what an item does when long
     * pressed.
     *
     * @param view The {@link View} to copy the content description from.
     */
    public static void showCheatSheet(View view) {
        int[] screenPos = new int[2]; // origin is device display
        Rect displayFrame = new Rect(); // includes decorations (e.g.
        // status bar)
        view.getLocationOnScreen(screenPos);
        view.getWindowVisibleDisplayFrame(displayFrame);

        Context context = view.getContext();
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        int viewCenterX = screenPos[0] + viewWidth / 2;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int estimatedToastHeight = (int) (48 * context.getResources().getDisplayMetrics().density);
        Toast cheatSheet = Toast.makeText(context, view.getContentDescription(), Toast.LENGTH_SHORT);
        boolean showBelow = screenPos[1] < estimatedToastHeight;
        if (showBelow) {
            // Show below
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, viewCenterX
                    - screenWidth / 2, screenPos[1] - displayFrame.top + viewHeight);
        } else {
            // Show above
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, viewCenterX
                    - screenWidth / 2, displayFrame.bottom - screenPos[1]);
        }
        cheatSheet.show();
    }

    /**
     * creates a license dialog
     *
     * @param context The {@link Context} to use.
     * @return An {@link AlertDialog} used to show the open source licenses used
     * in Apollo.
     */
    public static AlertDialog createOpenSourceDialog(Context context) {
        WebView webView = new WebView(context);
        webView.loadUrl("file:///android_asset/licenses.html");
        return new AlertDialog.Builder(context)
                .setTitle(R.string.settings_open_source_licenses)
                .setView(webView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    /**
     * creates a dialog to ask if cache should be cleared
     *
     * @param context The {@link Context} to use.
     * @return Dialog instance
     */
    public static AlertDialog createCacheClearDialog(final Context context) {
        return new AlertDialog.Builder(context).setMessage(R.string.delete_warning)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ImageCache mImageCache = ImageCache.getInstance(context);
                        mImageCache.clearCaches();
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
    }

    /**
     * Creates a new instance of the {@link ImageCache} and {@link ImageFetcher}
     *
     * @param context The {@link Context} to use.
     * @return A new {@link ImageFetcher} used to fetch images asynchronously.
     */
    public static ImageFetcher getImageFetcher(Context context) {
        ImageFetcher imageFetcher = ImageFetcher.getInstance(context);
        imageFetcher.setImageCache(ImageCache.getInstance(context));
        return imageFetcher;
    }

    /**
     * Used to create shortcuts for an artist, album, or playlist that is then
     * placed on the default launcher homescreen
     *
     * @param displayName The shortcut name
     * @param ids         The ID of the artist, album, playlist, or genre
     * @param mimeType    The MIME type of the shortcut
     * @param activity    The {@link FragmentActivity} to use to
     */
    public static void createShortcutIntent(String displayName, String artistName, String mimeType, FragmentActivity activity, long[] ids) {
        try {
            Bitmap bitmap;
            ImageFetcher fetcher = getImageFetcher(activity);
            if (mimeType.equals(MediaStore.Audio.Albums.CONTENT_TYPE)) {
                bitmap = fetcher.getCachedBitmap(ImageFetcher.generateAlbumCacheKey(displayName, artistName));
            } else {
                bitmap = fetcher.getCachedBitmap(displayName);
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.default_artwork);
            }
            // Intent used when the icon is touched
            Intent shortcutIntent = new Intent(activity, ShortcutActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            shortcutIntent.putExtra(Config.ID, ids[0]);
            shortcutIntent.putExtra(Config.IDS, ApolloUtils.serializeIDs(ids));
            shortcutIntent.putExtra(Config.NAME, displayName);
            shortcutIntent.putExtra(Config.MIME_TYPE, mimeType);
            // check if displayname is a path
            if (displayName.startsWith("/")) {
                File file = new File(displayName);
                if (file.exists()) {
                    // use file name as label
                    displayName = file.getName();
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Intent that actually sets the shortcut
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapUtils.resizeAndCropCenter(bitmap, 96));
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName);
                intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                activity.sendBroadcast(intent);
                String resultMsg = activity.getString(R.string.pinned_to_home_screen, displayName);
                AppMsg.makeText(activity, resultMsg, AppMsg.STYLE_ALERT).show();
            } else {
                // use shortcut manager to install shortcut
                ShortcutManager sManager = activity.getSystemService(ShortcutManager.class);
                if (sManager.isRequestPinShortcutSupported()) {
                    Icon icon = Icon.createWithBitmap(bitmap);
                    String shortcutId = displayName + "|" + artistName + "|" + ids[0];
                    ShortcutInfo sInfo = new ShortcutInfo.Builder(activity, shortcutId).setIcon(icon)
                            .setIntent(shortcutIntent).setShortLabel(displayName).build();
                    sManager.requestPinShortcut(sInfo, null);
                }
            }
        } catch (Exception e) {
            Log.e("ApolloUtils", "createShortcutIntent", e);
            String resultMsg = activity.getString(R.string.could_not_be_pinned_to_home_screen, displayName);
            AppMsg.makeText(activity, resultMsg, AppMsg.STYLE_ALERT).show();
        }
    }

    /**
     * generate the {@link ColorPickerView}
     *
     * @param activity The {@link Context} to use.
     */
    public static AlertDialog showColorPicker(final Activity activity) {
        final ColorSchemeDialog colorPickerView = new ColorSchemeDialog(activity);
        colorPickerView.setButton(AlertDialog.BUTTON_POSITIVE,
                activity.getString(android.R.string.ok), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // RESTART main activity
                        PreferenceUtils.getInstance(activity).setDefaultThemeColor(colorPickerView.getColor());
                        Intent newActivity = new Intent(activity, HomeActivity.class);
                        newActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(newActivity);
                        activity.finish();
                    }
                });
        colorPickerView.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getString(R.string.cancel), (OnClickListener) null);
        return colorPickerView;
    }

    /**
     * serialize ID array into a string
     *
     * @param ids IDs to serialize
     * @return serialized ID array
     */
    public static String serializeIDs(long[] ids) {
        StringBuilder result = new StringBuilder();
        for (long id : ids) {
            result.append(id);
            result.append(';');
        }
        // remove last separator
        if (result.length() > 0)
            result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    /**
     * read serialized ID array
     *
     * @param idsStr serialized string to read
     * @return ID array
     */
    public static long[] readSerializedIDs(String idsStr) {
        String[] ids = idsStr.split(";");
        long[] result = new long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            if (!id.isEmpty()) {
                result[i] = Long.parseLong(id);
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    /**
     * Method that removes the support for HardwareAcceleration from a {@link View}.<br/>
     * <br/>
     * Check AOSP notice:<br/>
     * <pre>
     * 'ComposeShader can only contain shaders of different types (a BitmapShader and a
     * LinearGradient for instance, but not two instances of BitmapShader)'. But, 'If your
     * application is affected by any of these missing features or limitations, you can turn
     * off hardware acceleration for just the affected portion of your application by calling
     * setLayerType(View.LAYER_TYPE_SOFTWARE, null).'</pre>
     *
     * @param v The view
     */
    public static void removeHardwareAccelerationSupport(View v) {
        if (v.getLayerType() != View.LAYER_TYPE_SOFTWARE) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /**
     * register an ListView click listener for a sub view
     *
     * @param view      sub view of the view item
     * @param container parent view of the view item
     * @param pos       position of the view item
     * @param id        Item ID
     */
    public static void registerItemViewListener(@NonNull View view, final ViewGroup container, final int pos, final long id) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if container is a list
                if (container instanceof AbsListView) {
                    AbsListView list = ((AbsListView) container);
                    list.performItemClick(v, pos, id);
                }
                // check if parent is a list
                else if (container.getParent() instanceof AbsListView) {
                    AbsListView list = ((AbsListView) container.getParent());
                    list.performItemClick(v, pos, id);
                }
            }
        });
    }
}