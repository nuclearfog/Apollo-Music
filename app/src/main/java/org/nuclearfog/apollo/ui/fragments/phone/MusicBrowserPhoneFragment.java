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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.adapters.viewpager.MusicBrowserAdapter;
import org.nuclearfog.apollo.ui.fragments.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.ArtistFragment;
import org.nuclearfog.apollo.ui.fragments.FolderFragment;
import org.nuclearfog.apollo.ui.fragments.GenreFragment;
import org.nuclearfog.apollo.ui.fragments.PlaylistFragment;
import org.nuclearfog.apollo.ui.fragments.RecentFragment;
import org.nuclearfog.apollo.ui.fragments.SongFragment;
import org.nuclearfog.apollo.ui.views.TitlePageIndicator;
import org.nuclearfog.apollo.ui.views.TitlePageIndicator.OnCenterItemClickListener;
import org.nuclearfog.apollo.utils.FragmentViewModel;
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
public class MusicBrowserPhoneFragment extends Fragment implements OnCenterItemClickListener, Observer<String> {

	/**
	 *
	 */
	private static final String TAG = "MusicBrowserPhoneFragment";

	/**
	 *
	 */
	public static final String REFRESH = TAG + ".refresh";

	/**
	 *
	 */
	public static final String META_CHANGED = TAG + ".meta_changed";

	/**
	 * Pager
	 */
	private ViewPager mViewPager;

	/**
	 * Theme resources
	 */
	private ThemeUtils mResources;

	/**
	 *
	 */
	private PreferenceUtils mPreferences;

	/**
	 * viewmodel used to communicate with sub fragments
	 */
	private FragmentViewModel viewModel;

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
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// The View for the fragment's UI
		View rootView = inflater.inflate(R.layout.fragment_music_browser_phone, container, false);
		// Initialize the adapter
		MusicBrowserAdapter adapter = new MusicBrowserAdapter(requireContext(), getChildFragmentManager());
		// Initialize the ViewPager
		mViewPager = rootView.findViewById(R.id.fragment_home_phone_pager);
		// Attach the adapter
		mViewPager.setAdapter(adapter);
		// Offscreen pager loading limit
		mViewPager.setOffscreenPageLimit(adapter.getCount());
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
		//
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		// Enable the options menu
		setHasOptionsMenu(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		viewModel.getSelectedItem().removeObserver(this);
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
			case MusicBrowserAdapter.IDX_RECENT:
				inflater.inflate(R.menu.view_as, menu);
				break;

			case MusicBrowserAdapter.IDX_ARTIST:
				inflater.inflate(R.menu.artist_sort_by, menu);
				inflater.inflate(R.menu.view_as, menu);
				break;

			case MusicBrowserAdapter.IDX_TRACKS:
				inflater.inflate(R.menu.song_sort_by, menu);
				break;

			case MusicBrowserAdapter.IDX_ALBUM:
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
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort track/album/artist list alphabetical reverse
		else if (item.getItemId() == R.id.menu_sort_by_za) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort albums/tracks by artist name
		else if (item.getItemId() == R.id.menu_sort_by_artist) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort tracks by album name
		else if (item.getItemId() == R.id.menu_sort_by_album) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort albums/tracks by release date
		else if (item.getItemId() == R.id.menu_sort_by_year) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort tracks by duration
		else if (item.getItemId() == R.id.menu_sort_by_duration) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort artists/albums by song count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_songs) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort artists by album count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_albums) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// sort tracks by file name
		else if (item.getItemId() == R.id.menu_sort_by_filename) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
			}
			refresh(mViewPager.getCurrentItem());
		}
		// set simple item view
		else if (item.getItemId() == R.id.menu_view_as_simple) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout("simple");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout("simple");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout("simple");
			}
			refresh(mViewPager.getCurrentItem());
		}
		// set detailed item view
		else if (item.getItemId() == R.id.menu_view_as_detailed) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout("detailed");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout("detailed");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout("detailed");
			}
			refresh(mViewPager.getCurrentItem());
		}
		// set grid item view
		else if (item.getItemId() == R.id.menu_view_as_grid) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout("grid");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout("grid");
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout("grid");
			}
			refresh(mViewPager.getCurrentItem());
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		switch (action) {
			case REFRESH:
				break;

			case META_CHANGED:
				// todo update all fragments
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCenterItemClick(int position) {
		switch (position) {
			case MusicBrowserAdapter.IDX_ALBUM:
				viewModel.notify(AlbumFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_ARTIST:
				viewModel.notify(ArtistFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_TRACKS:
				viewModel.notify(SongFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_RECENT:
				viewModel.notify(RecentFragment.SCROLL_TOP);
				break;
		}
	}

	/**
	 * refresh fragment
	 *
	 * @param position page of the fragment to refresh
	 */
	private void refresh(int position) {
		switch (position) {
			case MusicBrowserAdapter.IDX_PLAYLIST:
				viewModel.notify(PlaylistFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_ALBUM:
				viewModel.notify(AlbumFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_ARTIST:
				viewModel.notify(ArtistFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_FOLDER:
				viewModel.notify(FolderFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_GENRE:
				viewModel.notify(GenreFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_TRACKS:
				viewModel.notify(SongFragment.REFRESH);
				break;

			case MusicBrowserAdapter.IDX_RECENT:
				viewModel.notify(RecentFragment.REFRESH);
				break;
		}
	}
}