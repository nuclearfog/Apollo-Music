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

package org.nuclearfog.apollo.ui.fragments.phone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import org.nuclearfog.apollo.ui.views.TitlePageIndicator;
import org.nuclearfog.apollo.ui.views.TitlePageIndicator.OnCenterItemClickListener;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.adapters.PagerAdapter;
import org.nuclearfog.apollo.ui.fragments.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.ArtistFragment;
import org.nuclearfog.apollo.ui.fragments.FolderFragment;
import org.nuclearfog.apollo.ui.fragments.FragmentCallback;
import org.nuclearfog.apollo.ui.fragments.GenreFragment;
import org.nuclearfog.apollo.ui.fragments.PlaylistFragment;
import org.nuclearfog.apollo.ui.fragments.RecentFragment;
import org.nuclearfog.apollo.ui.fragments.SongFragment;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.SortOrder;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 * s for phones.
 * <p>
 * #note  The reason the sort orders are taken care of in this fragment rather
 * than the individual fragments is to keep from showing all of the menu
 * items on tablet interfaces. That being said, I have a tablet interface
 * worked out, but I'm going to keep it in the Play Store version of
 * Apollo for a couple of weeks or so before merging it with CM.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class MusicBrowserPhoneFragment extends Fragment implements OnCenterItemClickListener {

	/**
	 * index of {@link RecentFragment}
	 */
	private static final int RECENT_INDEX = 1;

	/**
	 * index of {@link ArtistFragment}
	 */
	private static final int ARTIST_INDEX = 2;

	/**
	 * index of {@link AlbumFragment}
	 */
	private static final int ALBUMS_INDEX = 3;

	/**
	 * index of {@link SongFragment}
	 */
	private static final int TRACKS_INDEX = 4;

	/**
	 * Pager
	 */
	private ViewPager mViewPager;

	/**
	 * VP's adapter
	 */
	private PagerAdapter mPagerAdapter;

	/**
	 * Theme resources
	 */
	private ThemeUtils mResources;

	private PreferenceUtils mPreferences;

	/**
	 * fragments used for pager
	 */
	private final Fragment[] PAGES = {
			new PlaylistFragment(),
			new RecentFragment(),
			new ArtistFragment(),
			new AlbumFragment(),
			new SongFragment(),
			new GenreFragment(),
			new FolderFragment()
	};

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public MusicBrowserPhoneFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Get the preferences
		mPreferences = PreferenceUtils.getInstance(requireContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// The View for the fragment's UI
		View rootView = inflater.inflate(R.layout.fragment_music_browser_phone, container, false);
		// Initialize the adapter
		mPagerAdapter = new PagerAdapter(requireActivity(), getChildFragmentManager());
		// Initialize pages
		for (Fragment page : PAGES) {
			mPagerAdapter.add(page, null);
		}
		// Initialize the ViewPager
		mViewPager = rootView.findViewById(R.id.fragment_home_phone_pager);
		// Attach the adapter
		mViewPager.setAdapter(mPagerAdapter);
		// Offscreen pager loading limit
		mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
		// Start on the last page the user was on
		mViewPager.setCurrentItem(mPreferences.getStartPage());
		// Initialize the TPI
		TitlePageIndicator pageIndicator = rootView.findViewById(R.id.fragment_home_phone_pager_titles);
		// Theme the selected text color
		pageIndicator.setSelectedColor(ResourcesCompat.getColor(getResources(), R.color.tpi_selected_text_color, null));
		// Theme the unselected text color
		pageIndicator.setTextColor(ResourcesCompat.getColor(getResources(), R.color.tpi_unselected_text_color, null));
		// Attach the ViewPager
		pageIndicator.setViewPager(mViewPager);
		// Scroll to the current artist, album, or song
		pageIndicator.setOnCenterItemClickListener(this);
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// Initialze the theme resources
		mResources = new ThemeUtils(requireActivity());
		// Enable the options menu
		setHasOptionsMenu(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		// Save the last page the use was on
		mPreferences.setStartPage(mViewPager.getCurrentItem());
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPrepareOptionsMenu(@NonNull Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem favorite = menu.findItem(R.id.menu_favorite);
		mResources.setFavoriteIcon(favorite);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// Favorite action
		inflater.inflate(R.menu.favorite, menu);
		// Shuffle all
		inflater.inflate(R.menu.shuffle, menu);
		// Sort orders
		switch (mViewPager.getCurrentItem()) {
			case RECENT_INDEX:
				inflater.inflate(R.menu.view_as, menu);
				break;

			case ARTIST_INDEX:
				inflater.inflate(R.menu.artist_sort_by, menu);
				inflater.inflate(R.menu.view_as, menu);
				break;

			case TRACKS_INDEX:
				inflater.inflate(R.menu.song_sort_by, menu);
				break;

			case ALBUMS_INDEX:
				inflater.inflate(R.menu.album_sort_by, menu);
				inflater.inflate(R.menu.view_as, menu);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		// Shuffle all the songs
		if (item.getItemId() == R.id.menu_shuffle) {
			MusicUtils.shuffleAll(requireContext());
		}
		// Toggle the current track as a favorite and update the menu item
		else if (item.getItemId() == R.id.menu_favorite) {
			MusicUtils.toggleFavorite();
			requireActivity().invalidateOptionsMenu();
		}
		// sort track/album/artist list alphabetical
		else if (item.getItemId() == R.id.menu_sort_by_az) {
			if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
				refresh(ALBUMS_INDEX);
			} else if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
				refresh(TRACKS_INDEX);
			}
		}
		// sort track/album/artist list alphabetical reverse
		else if (item.getItemId() == R.id.menu_sort_by_za) {
			if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
				refresh(ALBUMS_INDEX);
			} else if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
				refresh(TRACKS_INDEX);
			}
		}
		// sort albums/tracks by artist name
		else if (item.getItemId() == R.id.menu_sort_by_artist) {
			if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
				refresh(ALBUMS_INDEX);
			} else if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
				refresh(TRACKS_INDEX);
			}
		}
		// sort tracks by album name
		else if (item.getItemId() == R.id.menu_sort_by_album) {
			if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
				refresh(TRACKS_INDEX);
			}
		}
		// sort albums/tracks by release date
		else if (item.getItemId() == R.id.menu_sort_by_year) {
			if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
				refresh(ALBUMS_INDEX);
			} else if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
				refresh(TRACKS_INDEX);
			}
		}
		// sort tracks by duration
		else if (item.getItemId() == R.id.menu_sort_by_duration) {
			if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
				refresh(TRACKS_INDEX);
			}
		}
		// sort artists/albums by song count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_songs) {
			if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
				refresh(ALBUMS_INDEX);
			}
		}
		// sort artists by album count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_albums) {
			if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
				refresh(ARTIST_INDEX);
			}
		}
		// sort tracks by file name
		else if (item.getItemId() == R.id.menu_sort_by_filename) {
			if (mViewPager.getCurrentItem() == TRACKS_INDEX) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
				refresh(TRACKS_INDEX);
			}
		}
		// set simple item view
		else if (item.getItemId() == R.id.menu_view_as_simple) {
			if (mViewPager.getCurrentItem() == RECENT_INDEX) {
				mPreferences.setRecentLayout("simple");
				refresh(RECENT_INDEX);
			} else if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistLayout("simple");
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumLayout("simple");
				refresh(ALBUMS_INDEX);
			}
		}
		// set detailed item view
		else if (item.getItemId() == R.id.menu_view_as_detailed) {
			if (mViewPager.getCurrentItem() == RECENT_INDEX) {
				mPreferences.setRecentLayout("detailed");
				refresh(RECENT_INDEX);
			} else if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistLayout("detailed");
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumLayout("detailed");
				refresh(ALBUMS_INDEX);
			}
		}
		// set grid item view
		else if (item.getItemId() == R.id.menu_view_as_grid) {
			if (mViewPager.getCurrentItem() == RECENT_INDEX) {
				mPreferences.setRecentLayout("grid");
				refresh(RECENT_INDEX);
			} else if (mViewPager.getCurrentItem() == ARTIST_INDEX) {
				mPreferences.setArtistLayout("grid");
				refresh(ARTIST_INDEX);
			} else if (mViewPager.getCurrentItem() == ALBUMS_INDEX) {
				mPreferences.setAlbumLayout("grid");
				refresh(ALBUMS_INDEX);
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
	public void onCenterItemClick(int index) {
		Fragment fragment = mPagerAdapter.getItem(index);
		if (fragment instanceof FragmentCallback && !fragment.isDetached()) {
			((FragmentCallback) fragment).setCurrentTrack();
		}
	}

	/**
	 * refresh current page
	 */
	public void refreshCurrent() {
		refresh(mViewPager.getCurrentItem());
	}

	/**
	 * refresh page
	 *
	 * @param index index of the page
	 */
	private void refresh(int index) {
		Fragment fragment = mPagerAdapter.getItem(index);
		if (fragment instanceof FragmentCallback && !fragment.isDetached()) {
			((FragmentCallback) fragment).refresh();
		}
	}
}