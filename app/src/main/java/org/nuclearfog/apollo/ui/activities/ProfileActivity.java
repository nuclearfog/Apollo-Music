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

package org.nuclearfog.apollo.ui.activities;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.FavoriteSongLoader;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.async.loader.LastAddedLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.async.loader.PopularSongLoader;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.ui.adapters.viewpager.ProfileAdapter;
import org.nuclearfog.apollo.ui.dialogs.PhotoSelectionDialog;
import org.nuclearfog.apollo.ui.dialogs.PhotoSelectionDialog.ProfileType;
import org.nuclearfog.apollo.ui.fragments.profile.ProfileFragment;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel.Listener;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.SortOrder;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.List;

/**
 * The {@link AppCompatActivity} is used to display the data for specific
 * artists, albums, playlists, and genres. This class is only used on phones.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileActivity extends ActivityBase implements ActivityResultCallback<ActivityResult>, OnPageChangeListener, Listener, OnClickListener {

	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.FolderSongFragment}
	 */
	public static final String PAGE_FOLDERS = "page_folders";
	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment}
	 */
	public static final String PAGE_FAVORIT = "page_fav";
	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.LastAddedFragment}
	 */
	public static final String PAGE_LAST_ADDED = "last_added";
	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.LastAddedFragment}
	 */
	public static final String PAGE_MOST_PLAYED = "page_most";

	/**
	 *
	 */
	private static final String[] GET_MEDIA = {MediaColumns.DATA};

	/**
	 *
	 */
	private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this);

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;

	/**
	 * View pager
	 */
	private ViewPager mViewPager;

	/**
	 * Profile header carousel
	 */
	private ProfileTabCarousel mTabCarousel;

	/**
	 * content type to show on this activity
	 */
	private Type type;

	/**
	 * ID used for albums, artist genres etc
	 */
	private long[] ids = {0};

	/**
	 * MIME type for album/artist images
	 */
	private static final String MIME_IMAGE = "image/*";

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
	 * folder path of the music files
	 */
	private String folderPath = "";

	/**
	 * Image cache
	 */
	private ImageFetcher mImageFetcher;
	private ArtistSongLoader artistSongLoader;
	private AlbumSongLoader albumSongLoader;
	private GenreSongLoader genreSongLoader;
	private PlaylistSongLoader playlistSongLoader;
	private FavoriteSongLoader favoriteSongLoader;
	private LastAddedLoader lastAddedLoader;
	private PopularSongLoader popularSongLoader;
	private FolderSongLoader folderSongLoader;

	private PreferenceUtils mPreferences;

	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getContentView() {
		return R.layout.activity_profile_base;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("SourceLockedOrientationActivity")
	@Override
	protected void init(Bundle savedInstanceState) {
		Toolbar toolbar = findViewById(R.id.activity_profile_base_toolbar);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		// Set the overflow style
		mResources.setOverflowStyle(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.app_name);
		}
		String year = "";
		// Temporary until I can work out a nice landscape layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// init fragment callback
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		// Get the preferences
		mPreferences = PreferenceUtils.getInstance(this);
		// Initialze the image fetcher
		mImageFetcher = ApolloUtils.getImageFetcher(this);
		artistSongLoader = new ArtistSongLoader(this);
		albumSongLoader = new AlbumSongLoader(this);
		genreSongLoader = new GenreSongLoader(this);
		playlistSongLoader = new PlaylistSongLoader(this);
		favoriteSongLoader = new FavoriteSongLoader(this);
		lastAddedLoader = new LastAddedLoader(this);
		popularSongLoader = new PopularSongLoader(this);
		folderSongLoader = new FolderSongLoader(this);
		// Initialize the Bundle
		Bundle mArguments = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
		// Get the MIME type
		if (mArguments != null) {
			// get mime type
			mType = mArguments.getString(Constants.MIME_TYPE, "");
			// Get the profile title
			mProfileName = mArguments.getString(Constants.NAME, "");
			// Get the artist name
			mArtistName = mArguments.getString(Constants.ARTIST_NAME, "");
			// Get the ID
			if (mArguments.containsKey(Constants.IDS)) {
				ids = ApolloUtils.readSerializedIDs(mArguments.getString(Constants.IDS, ""));
			} else {
				ids = new long[]{mArguments.getLong(Constants.ID)};
			}
			// get album yeas
			year = mArguments.getString(Constants.ALBUM_YEAR, "");
			// get folder name if defined
			folderPath = mArguments.getString(Constants.FOLDER, "");
		}
		type = Type.getEnum(mType);
		// Initialize the pager adapter
		ProfileAdapter mPagerAdapter = new ProfileAdapter(getSupportFragmentManager(), mArguments, type);
		// Initialze the carousel
		mTabCarousel = findViewById(R.id.activity_profile_base_tab_carousel);
		mTabCarousel.reset();
		mTabCarousel.getPhoto().setOnClickListener(this);
		// Set up the action bar
		ActionBar actionBar = getSupportActionBar();

		switch (type) {
			case ALBUM:
				// Add the carousel images
				mTabCarousel.setAlbumProfileHeader(this, mProfileName, mArtistName);
				// Album profile fragments
				if (actionBar != null) {
					// Action bar title = album name
					actionBar.setTitle(mProfileName);
					if (mArguments != null) {
						// Action bar subtitle = year released
						actionBar.setSubtitle(year);
					}
				}
				break;

			case ARTIST:
				// Add the carousel images
				mTabCarousel.setArtistProfileHeader(this, mArtistName);
				// Artist profile fragments
				if (actionBar != null) {
					actionBar.setDisplayHomeAsUpEnabled(true);
					actionBar.setTitle(mArtistName);
				}
				break;

			case FOLDER:
				mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
				if (actionBar != null) {
					actionBar.setTitle(this.mProfileName);
				}
				break;

			case GENRE:
			case FAVORITE:
			case PLAYLIST:
			case LAST_ADDED:
			case POPULAR:
				// Add the carousel images
				mTabCarousel.setPlaylistOrGenreProfileHeader(this, mProfileName);
				// most played fragment
				// Action bar title = Last added
				if (actionBar != null) {
					actionBar.setTitle(mProfileName);
				}
				break;
		}
		// Initialize the ViewPager
		mViewPager = findViewById(R.id.activity_profile_base_pager);
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
	protected void onDestroy() {
		artistSongLoader.cancel();
		albumSongLoader.cancel();
		genreSongLoader.cancel();
		playlistSongLoader.cancel();
		favoriteSongLoader.cancel();
		lastAddedLoader.cancel();
		popularSongLoader.cancel();
		folderSongLoader.cancel();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Theme the add to home screen icon
		MenuItem shuffle = menu.findItem(R.id.menu_shuffle);
		if (type == Type.FAVORITE || type == Type.LAST_ADDED || type == Type.PLAYLIST || type == Type.POPULAR) {
			shuffle.setTitle(R.string.menu_play_all);
		} else {
			shuffle.setTitle(R.string.menu_shuffle);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Pin to Home screen
		getMenuInflater().inflate(R.menu.add_to_homescreen, menu);
		// Shuffle
		getMenuInflater().inflate(R.menu.shuffle, menu);
		// Sort orders
		if (type == Type.ARTIST) {
			if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
				getMenuInflater().inflate(R.menu.artist_song_sort_by, menu);
			} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
				getMenuInflater().inflate(R.menu.artist_album_sort_by, menu);
			}
		} else if (type == Type.ALBUM) {
			getMenuInflater().inflate(R.menu.album_song_sort_by, menu);
		} else if (type == Type.POPULAR) {
			getMenuInflater().inflate(R.menu.popular_songs_clear, menu);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// return to home
		if (item.getItemId() == android.R.id.home) {
			// If an album profile, go up to the artist profile
			if (type == Type.ALBUM) {
				NavUtils.openArtistProfile(this, mArtistName);
				finish();
			} else {
				// Otherwise just go back
				finish();
			}
		}
		// add item to home screen
		else if (item.getItemId() == R.id.menu_add_to_homescreen) {
			// Place the artist, album, genre, or playlist onto the Home
			// screen. Definitely one of my favorite features.
			if (type == Type.ARTIST) {
				ApolloUtils.createShortcutIntent(mArtistName, mArtistName, mType, this, ids);
			} else if (type == Type.FOLDER) {
				ApolloUtils.createShortcutIntent(folderPath, mArtistName, mType, this, ids);
			} else {
				ApolloUtils.createShortcutIntent(mProfileName, mArtistName, mType, this, ids);
			}
		}
		// shuffle tracks
		else if (item.getItemId() == R.id.menu_shuffle) {
			switch (type) {
				case ARTIST:
					artistSongLoader.execute(ids[0], onPlaySongs);
					break;

				case ALBUM:
					albumSongLoader.execute(ids[0], onPlaySongs);
					break;

				case GENRE:
					String genreIds = ApolloUtils.serializeIDs(ids);
					genreSongLoader.execute(genreIds, onPlaySongs);
					break;

				case PLAYLIST:
					playlistSongLoader.execute(ids[0], onPlaySongs);
					break;

				case FAVORITE:
					favoriteSongLoader.execute(null, onPlaySongs);
					break;

				case LAST_ADDED:
					lastAddedLoader.execute(null, onPlaySongs);
					break;

				case POPULAR:
					popularSongLoader.execute(null, onPlaySongs);
					break;

				case FOLDER:
					folderSongLoader.execute(folderPath, onPlaySongs);
					break;
			}
		}
		// sort alphabetical
		else if (item.getItemId() == R.id.menu_sort_by_az) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_A_Z);
				} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
					mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
				}
			} else if (type == Type.ALBUM) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ALBUM_SONG) {
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
				}
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort alphabetical reverse
		else if (item.getItemId() == R.id.menu_sort_by_za) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_Z_A);
				} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
					mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
				}
			} else if (type == Type.ALBUM) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ALBUM_SONG) {
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
				}
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by album name
		else if (item.getItemId() == R.id.menu_sort_by_album) {
			if (type == Type.ARTIST && mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
				mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_ALBUM);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by release date
		else if (item.getItemId() == R.id.menu_sort_by_year) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_YEAR);
				} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
					mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
				}
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by track duration
		else if (item.getItemId() == R.id.menu_sort_by_duration) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DURATION);
				}
			} else if (type == Type.ALBUM) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ALBUM_SONG) {
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
				}
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by date added
		else if (item.getItemId() == R.id.menu_sort_by_date_added) {
			if (type == Type.ARTIST && mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
				mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DATE);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by default order
		else if (item.getItemId() == R.id.menu_sort_by_track_list) {
			mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by file name
		else if (item.getItemId() == R.id.menu_sort_by_filename) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_FILENAME);
				}
			} else if (type == Type.ALBUM) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ALBUM_SONG) {
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_FILENAME);
				}
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// clear popular playlist
		else if (item.getItemId() == R.id.menu_clear_popular) {
			if (type == Type.POPULAR) {
				PopularStore.getInstance(this).removeAll();
				viewModel.notify(ProfileFragment.REFRESH);
			}
		}
		// sort by track count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_songs) {
			if (type == Type.ARTIST) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
					mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_TRACK_COUNT);
				}
				viewModel.notify(ProfileFragment.REFRESH);
			}
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putAll(getIntent().getExtras());
		super.onSaveInstanceState(outState);
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
		if (type == Type.ARTIST) {
			viewModel.notify(ProfileFragment.SCROLL_TOP);
		}
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
		if (requestCode == MusicUtils.REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.refresh(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityResult(ActivityResult result) {
		if (result.getResultCode() == RESULT_OK) {
			Intent intent = result.getData();
			if (intent != null && intent.getData() != null) {
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
			dialog.show(getSupportFragmentManager(), PhotoSelectionDialog.NAME);
		} else {
			super.onClick(v);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		viewModel.notify(ProfileFragment.REFRESH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		viewModel.notify(ProfileFragment.SHOW_CURRENT);
	}

	/**
	 * Starts an activity for result that returns an image from the Gallery.
	 */
	public void selectNewPhoto() {
		// First remove the old image
		removeFromCache();
		// Now open the gallery
		Intent intent = new Intent(Intent.ACTION_PICK, null);
		intent.setType(MIME_IMAGE);
		activityResultLauncher.launch(intent);
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
	public void searchWeb() {
		String query;
		if (type == Type.ARTIST) {
			query = mArtistName;
		} else if (type == Type.ALBUM) {
			query = mProfileName + " " + mArtistName;
		} else {
			query = mProfileName;
		}
		Intent webSearch = new Intent(Intent.ACTION_WEB_SEARCH);
		webSearch.putExtra(SearchManager.QUERY, query);
		try {
			startActivity(webSearch);
		} catch (ActivityNotFoundException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
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
	 * called to play asynchronously loaded songs
	 */
	private void onPlaySongs(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(this, ids, 0, true);
	}

	/**
	 * constants defining fragment type
	 */
	public enum Type {
		ARTIST,
		ALBUM,
		GENRE,
		PLAYLIST,
		FOLDER,
		FAVORITE,
		LAST_ADDED,
		POPULAR;

		public static Type getEnum(String mime) {
			switch (mime) {
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
				case PAGE_MOST_PLAYED:
					return POPULAR;
				default:
				case PAGE_LAST_ADDED:
					return LAST_ADDED;
			}
		}
	}
}