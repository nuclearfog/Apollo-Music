package org.nuclearfog.apollo.ui.adapters.viewpager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.ArtistFragment;
import org.nuclearfog.apollo.ui.fragments.FolderFragment;
import org.nuclearfog.apollo.ui.fragments.GenreFragment;
import org.nuclearfog.apollo.ui.fragments.PlaylistFragment;
import org.nuclearfog.apollo.ui.fragments.RecentFragment;
import org.nuclearfog.apollo.ui.fragments.SongFragment;

import java.util.Locale;

/**
 * @author nuclearfog
 */
public class MusicBrowserAdapter extends FragmentStatePagerAdapter {

	private String[] titles;

	/**
	 *
	 */
	public MusicBrowserAdapter(Context context, FragmentManager fm) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		titles = context.getResources().getStringArray(R.array.page_titles);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Fragment getItem(int position) {
		switch (position) {
			default:
			case 0:
				return new PlaylistFragment();

			case 1:
				return new RecentFragment();

			case 2:
				return new ArtistFragment();

			case 3:
				return new AlbumFragment();

			case 4:
				return new SongFragment();

			case 5:
				return new GenreFragment();

			case 6:
				return new FolderFragment();

		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		return 7;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position].toUpperCase(Locale.getDefault());
	}
}