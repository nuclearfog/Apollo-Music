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

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.DeleteDialog.DeleteDialogCallback;
import com.andrew.apollo.menu.PhotoSelectionDialog;
import com.andrew.apollo.menu.PhotoSelectionDialog.ProfileType;
import com.andrew.apollo.ui.fragments.profile.AlbumSongFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment;
import com.andrew.apollo.ui.fragments.profile.ArtistSongFragment;
import com.andrew.apollo.ui.fragments.profile.FavoriteFragment;
import com.andrew.apollo.ui.fragments.profile.FolderSongFragment;
import com.andrew.apollo.ui.fragments.profile.LastAddedFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.SortOrder;
import com.andrew.apollo.widgets.ProfileTabCarousel;
import com.andrew.apollo.widgets.ProfileTabCarousel.Listener;

import java.io.File;
import java.util.Random;

import static com.andrew.apollo.Config.FOLDER;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.ALBUMSONG;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.ARTISTALBUM;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.ARTISTSONG;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.FAVORITE;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.FOLDERSONG;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.GENRESONG;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.LASTADDED;
import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.PLAYLISTSONG;
import static com.andrew.apollo.utils.MusicUtils.REQUEST_DELETE_FILES;

/**
 * The {@link AppCompatActivity} is used to display the data for specific
 * artists, albums, playlists, and genres. This class is only used on phones.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileActivity extends AppCompatBase implements OnPageChangeListener, Listener, OnClickListener, DeleteDialogCallback {

    /**
     * request code to load new photo
     */
    private static final int NEW_PHOTO = 0x487B;
    /**
     * page index of the {@link ArtistSongFragment} if {@link Type#ARTIST} is set
     */
    private static final int ARTIST_SONG = 0;
    /**
     * page index of the {@link ArtistAlbumFragment} if {@link Type#ARTIST} is set
     */
    private static final int ARTIST_ALBUM = 1;
    /**
     * page index of the {@link AlbumSongFragment} if {@link Type#ARTIST} is not set
     */
    private static final int ALBUM_SONG = 0;
    /**
     * mime type of the {@link FolderSongFragment}
     */
    public static final String PAGE_FOLDERS = "page_folders";
    /**
     * mime type of the {@link FavoriteFragment}
     */
    public static final String PAGE_FAVORIT = "page_fav";
    /**
     * mime type of the {@link LastAddedFragment}
     */
    public static final String PAGE_LAST_ADDED = "playlist";

    /**
     *
     */
    private static final String[] GET_MEDIA = {MediaColumns.DATA};

    /**
     * constants defining fragment type
     */
    private enum Type {
        ARTIST,
        ALBUM,
        GENRE,
        PLAYLIST,
        FOLDER,
        FAVORITE,
        LAST_ADDED;

        public static Type getEnum(String s) {
            switch (s) {
                case Audio.Artists.CONTENT_TYPE:
                    return ARTIST;
                case Audio.Albums.CONTENT_TYPE:
                    return ALBUM;
                case Audio.Genres.CONTENT_TYPE:
                    return GENRE;
                case Audio.Playlists.CONTENT_TYPE:
                    return PLAYLIST;
                case PAGE_FOLDERS:
                    return FOLDER;
                case PAGE_FAVORIT:
                    return FAVORITE;
                default:
                case PAGE_LAST_ADDED:
                    return LAST_ADDED;
            }
        }
    }

    /**
     * The Bundle to pass into the Fragments
     */
    private Bundle mArguments;

    /**
     * View pager
     */
    private ViewPager mViewPager;

    /**
     * Pager adapter
     */
    private PagerAdapter mPagerAdapter;

    /**
     * Profile header carousel
     */
    private ProfileTabCarousel mTabCarousel;

    /**
     * content type to show on this activity
     */
    private Type type;

    /**
     * MIME type of the profile
     */
    private String mType = "";

    /**
     * Artist name passed into the class
     */
    private String mArtistName = "";

    /**
     * The main profile title
     */
    private String mProfileName = "";

    /**
     * Image cache
     */
    private ImageFetcher mImageFetcher;

    private PreferenceUtils mPreferences;

    /**
     * {@inheritDoc}
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Temporary until I can work out a nice landscape layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(this);
        // Initialze the image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(this);
        // Initialize the Bundle
        mArguments = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        // Get the MIME type
        if (mArguments != null) {
            // get mime type
            mType = mArguments.getString(Config.MIME_TYPE, "");
            // Get the profile title
            mProfileName = mArguments.getString(Config.NAME, "");
            // Get the artist name
            mArtistName = mArguments.getString(Config.ARTIST_NAME, "");
        }
        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(this);
        // Initialze the carousel
        mTabCarousel = findViewById(R.id.acivity_profile_base_tab_carousel);
        mTabCarousel.reset();
        mTabCarousel.getPhoto().setOnClickListener(this);
        // Set up the action bar
        ActionBar actionBar = getSupportActionBar();

        type = Type.getEnum(mType);
        switch (type) {
            case ALBUM:
                // Add the carousel images
                mTabCarousel.setAlbumProfileHeader(this, mProfileName, mArtistName);
                // Album profile fragments
                mPagerAdapter.add(ALBUMSONG, mArguments);
                if (actionBar != null) {
                    // Action bar title = album name
                    actionBar.setTitle(mProfileName);
                    if (mArguments != null) {
                        // Action bar subtitle = year released
                        actionBar.setSubtitle(mArguments.getString(Config.ALBUM_YEAR));
                    }
                }
                break;

            case GENRE:
                // Add the carousel images
                mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
                // Genre profile fragments
                mPagerAdapter.add(GENRESONG, mArguments);
                // Action bar title = playlist name
                if (actionBar != null) {
                    actionBar.setTitle(mProfileName);
                }
                break;

            case ARTIST:
                // Add the carousel images
                mTabCarousel.setArtistProfileHeader(this, mArtistName);
                // Artist profile fragments
                mPagerAdapter.add(ARTISTSONG, mArguments);
                mPagerAdapter.add(ARTISTALBUM, mArguments);
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setTitle(mArtistName);
                }
                break;

            case FOLDER:
                mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
                mPagerAdapter.add(FOLDERSONG, mArguments);
                if (actionBar != null) {
                    actionBar.setTitle(this.mProfileName);
                }
                break;

            case FAVORITE:
                // Add the carousel images
                mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
                // Favorite fragment
                mPagerAdapter.add(FAVORITE, null);
                // Action bar title = Favorites
                if (actionBar != null) {
                    actionBar.setTitle(mProfileName);
                }
                break;

            case PLAYLIST:
                // Add the carousel images
                mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
                // Playlist profile fragments
                mPagerAdapter.add(PLAYLISTSONG, mArguments);
                // Action bar title = playlist name
                if (actionBar != null) {
                    actionBar.setTitle(mProfileName);
                }
                break;

            case LAST_ADDED:
                // Add the carousel images
                mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
                // Last added fragment
                mPagerAdapter.add(LASTADDED, null);
                // Action bar title = Last added
                if (actionBar != null) {
                    actionBar.setTitle(mProfileName);
                }
                break;
        }
        // Initialize the ViewPager
        mViewPager = findViewById(R.id.acivity_profile_base_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        // Attach the page change listener
        mViewPager.addOnPageChangeListener(this);
        // Attach the carousel listener
        mTabCarousel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Theme the add to home screen icon
        MenuItem shuffle = menu.findItem(R.id.menu_shuffle);
        MenuItem pinnAction = menu.findItem(R.id.menu_add_to_homescreen);

        Drawable pinIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_pinn_to_home, null);
        pinnAction.setIcon(pinIcon);
        String title;
        if (type == Type.FAVORITE || type == Type.LAST_ADDED || type == Type.PLAYLIST) {
            title = getString(R.string.menu_play_all);
        } else {
            title = getString(R.string.menu_shuffle);
        }
        shuffle.setTitle(title);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Pin to Home screen
        getMenuInflater().inflate(R.menu.add_to_homescreen, menu);
        // Shuffle
        getMenuInflater().inflate(R.menu.shuffle, menu);
        // Sort orders
        if (type == Type.ARTIST) {
            if (mViewPager.getCurrentItem() == ARTIST_SONG) {
                getMenuInflater().inflate(R.menu.artist_song_sort_by, menu);
            } else if (mViewPager.getCurrentItem() == ARTIST_ALBUM) {
                getMenuInflater().inflate(R.menu.artist_album_sort_by, menu);
            }
        } else if (type == Type.ALBUM) {
            getMenuInflater().inflate(R.menu.album_song_sort_by, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // If an album profile, go up to the artist profile
            if (type == Type.ALBUM) {
                NavUtils.openArtistProfile(this, mArtistName);
                finish();
            } else {
                // Otherwise just go back
                goBack();
            }
        } else if (itemId == R.id.menu_add_to_homescreen) {
            // Place the artist, album, genre, or playlist onto the Home
            // screen. Definitely one of my favorite features.
            long id = mArguments.getLong(Config.ID);
            if (type == Type.ARTIST) {
                ApolloUtils.createShortcutIntent(mArtistName, mArtistName, id, mType, this);
            } else {
                ApolloUtils.createShortcutIntent(mProfileName, mArtistName, id, mType, this);
            }
        } else if (itemId == R.id.menu_shuffle) {
            long id = mArguments.getLong(Config.ID);
            long[] list = {};
            switch (type) {
                case ARTIST:
                    list = MusicUtils.getSongListForArtist(this, id);
                    break;

                case ALBUM:
                    list = MusicUtils.getSongListForAlbum(this, id);
                    break;

                case GENRE:
                    list = MusicUtils.getSongListForGenre(this, id);
                    break;

                case PLAYLIST:
                    MusicUtils.playPlaylist(this, id);
                    break;

                case FAVORITE:
                    MusicUtils.playFavorites(this);
                    break;

                case LAST_ADDED:
                    MusicUtils.playLastAdded(this);
                    break;

                case FOLDER:
                    File folder = new File(mArguments.getString(FOLDER));
                    if (folder.exists())
                        list = MusicUtils.getSongListForFolder(this, folder);
                    if (list.length > 0) {
                        // play list at random position
                        Random r = new Random();
                        MusicUtils.playAll(list, r.nextInt(list.length), true);
                    }
                    break;
            }
            if (list.length > 0) {
                MusicUtils.playAll(list, 0, true);
            }
        } else if (itemId == R.id.menu_sort_by_az) {
            if (type == Type.ARTIST) {
                if (mViewPager.getCurrentItem() == ARTIST_SONG) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_A_Z);
                    refreshFragment(ARTIST_SONG);
                } else if (mViewPager.getCurrentItem() == ARTIST_ALBUM) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
                    refreshFragment(ARTIST_ALBUM);
                } else {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
                    refreshFragment(ALBUM_SONG);
                }
            }
        } else if (itemId == R.id.menu_sort_by_za) {
            if (type == Type.ARTIST) {
                if (mViewPager.getCurrentItem() == ARTIST_SONG) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_Z_A);
                    refreshFragment(ARTIST_SONG);
                } else if (mViewPager.getCurrentItem() == ARTIST_ALBUM) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
                    refreshFragment(ARTIST_ALBUM);
                } else {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
                    refreshFragment(ALBUM_SONG);
                }
            }
        } else if (itemId == R.id.menu_sort_by_album) {
            if (type == Type.ARTIST && mViewPager.getCurrentItem() == ARTIST_SONG) {
                mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_ALBUM);
                refreshFragment(ARTIST_SONG);
            }
        } else if (itemId == R.id.menu_sort_by_year) {
            if (type == Type.ARTIST) {
                if (mViewPager.getCurrentItem() == ARTIST_SONG) {
                    mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_YEAR);
                    refreshFragment(ARTIST_SONG);
                } else if (mViewPager.getCurrentItem() == ARTIST_ALBUM) {
                    mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
                    refreshFragment(ARTIST_ALBUM);
                }
            }
        } else if (itemId == R.id.menu_sort_by_duration) {
            if (type == Type.ARTIST && mViewPager.getCurrentItem() == ARTIST_SONG) {
                mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DURATION);
                refreshFragment(ARTIST_SONG);
            } else {
                mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                refreshFragment(ALBUM_SONG);
            }
        } else if (itemId == R.id.menu_sort_by_date_added) {
            if (type == Type.ARTIST && mViewPager.getCurrentItem() == ARTIST_SONG) {
                mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DATE);
                refreshFragment(ARTIST_SONG);
            }
        } else if (itemId == R.id.menu_sort_by_track_list) {
            mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
            refreshFragment(ALBUM_SONG);
        } else if (itemId == R.id.menu_sort_by_filename) {
            if (type == Type.ARTIST && mViewPager.getCurrentItem() == ARTIST_SONG) {
                mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_FILENAME);
                refreshFragment(ARTIST_SONG);
            } else {
                mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_FILENAME);
                refreshAll();
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public View getContentView() {
        return View.inflate(this, R.layout.activity_profile_base, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mArguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (!mViewPager.isFakeDragging()) {
            int scrollToX = (int) ((position + positionOffset) * mTabCarousel.getAllowedHorizontalScrollLength());
            mTabCarousel.scrollTo(scrollToX, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(int position) {
        mTabCarousel.setCurrentTab(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            mTabCarousel.restoreYCoordinate(75, mViewPager.getCurrentItem());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchDown() {
        mViewPager.beginFakeDrag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchUp() {
        if (mViewPager.isFakeDragging()) {
            mViewPager.endFakeDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollChanged(int l, int oldl) {
        if (mViewPager.isFakeDragging()) {
            mViewPager.fakeDragBy(oldl - l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTabSelected(int position) {
        mViewPager.setCurrentItem(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == NEW_PHOTO && intent != null && intent.getData() != null) {
                Cursor cursor = getContentResolver().query(intent.getData(), GET_MEDIA, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(GET_MEDIA[0]);
                        String picturePath = cursor.getString(columnIndex);
                        Bitmap bitmap = ImageFetcher.decodeSampledBitmapFromFile(picturePath);
                        if (type == Type.ARTIST) {
                            mImageFetcher.addBitmapToCache(mArtistName, bitmap);
                            mTabCarousel.getPhoto().setImageBitmap(bitmap);
                        } else if (type == Type.ALBUM) {
                            String key = ImageFetcher.generateAlbumCacheKey(mProfileName, mArtistName);
                            mImageFetcher.addBitmapToCache(key, bitmap);
                            mTabCarousel.getAlbumArt().setImageBitmap(bitmap);
                        } else {
                            mImageFetcher.addBitmapToCache(mProfileName, bitmap);
                            mTabCarousel.getPhoto().setImageBitmap(bitmap);
                        }
                    }
                    cursor.close();
                } else {
                    selectOldPhoto();
                }
            } else if (requestCode == REQUEST_DELETE_FILES) {
                MusicUtils.onPostDelete(this);
                refreshAll();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.profile_tab_photo) {
            ProfileType profileType;
            String name;
            if (type == Type.ARTIST) {
                profileType = ProfileType.ARTIST;
                name = mArtistName;
            } else if (type == Type.ALBUM) {
                profileType = ProfileType.ALBUM;
                name = mProfileName;
            } else {
                profileType = ProfileType.OTHER;
                name = mProfileName;
            }
            DialogFragment dialog = PhotoSelectionDialog.newInstance(name, profileType);
            dialog.show(getSupportFragmentManager(), "PhotoSelectionDialog");
        } else {
            super.onClick(v);
        }
    }


    @Override
    public void onDelete() {
        refreshAll();
    }

    /**
     * Starts an activity for result that returns an image from the Gallery.
     */
    public void selectNewPhoto() {
        // First remove the old image
        removeFromCache();
        // Now open the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setType("image/*");
        startActivityForResult(intent, NEW_PHOTO);
    }

    /**
     * Fetchs for the artist or album art, other wise sets the default header
     * image.
     */
    public void selectOldPhoto() {
        // First remove the old image
        removeFromCache();
        // Apply the old photo
        if (type == Type.ARTIST) {
            mTabCarousel.setArtistProfileHeader(this, mArtistName);
        } else if (type == Type.ALBUM) {
            mTabCarousel.setAlbumProfileHeader(this, mProfileName, mArtistName);
        } else {
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
        }
    }

    /**
     * When the user chooses {@code #selectOldPhoto()} while viewing an album
     * profile, the image is, most likely, reverted back to the locally found
     * artwork. This is specifically for fetching the image from Last.fm.
     */
    public void fetchAlbumArt() {
        // First remove the old image
        removeFromCache();
        // Fetch for the artwork
        mTabCarousel.fetchAlbumPhoto(this, mProfileName, mArtistName);
    }

    /**
     * Searches Google for the artist or album
     */
    public void googleSearch() {
        String query;
        if (type == Type.ARTIST) {
            query = mArtistName;
        } else if (type == Type.ALBUM) {
            query = mProfileName + " " + mArtistName;
        } else {
            query = mProfileName;
        }
        Intent googleSearch = new Intent(Intent.ACTION_WEB_SEARCH);
        googleSearch.putExtra(SearchManager.QUERY, query);
        try {
            startActivity(googleSearch);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the header image from the cache.
     */
    private void removeFromCache() {
        String key = mProfileName;
        if (type == Type.ARTIST) {
            key = mArtistName;
        } else if (type == Type.ALBUM) {
            key = ImageFetcher.generateAlbumCacheKey(mProfileName, mArtistName);
        }
        mImageFetcher.removeFromCache(key);
        // Give the disk cache a little time before requesting a new image.
        SystemClock.sleep(80);
    }

    /**
     * Finishes the activity and overrides the default animation.
     */
    private void goBack() {
        finish();
    }

    /**
     * refresh single fragment
     *
     * @param index index of the fragment
     */
    private void refreshFragment(int index) {
        Fragment fragment = mPagerAdapter.getFragment(index);
        if (fragment instanceof FragmentCallback) {
            ((FragmentCallback) fragment).refresh();
        }
    }

    /**
     * refresh all fragments
     */
    private void refreshAll() {
        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            refreshFragment(i);
        }
    }

    /**
     * callback for sub fragments to refreshAll
     */
    public interface FragmentCallback {

        void refresh();
    }
}