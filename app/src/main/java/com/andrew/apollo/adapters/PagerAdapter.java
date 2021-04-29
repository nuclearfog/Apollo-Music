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

package com.andrew.apollo.adapters;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentPagerAdapter;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.AlbumFragment;
import com.andrew.apollo.ui.fragments.ArtistFragment;
import com.andrew.apollo.ui.fragments.FolderFragment;
import com.andrew.apollo.ui.fragments.GenreFragment;
import com.andrew.apollo.ui.fragments.PlaylistFragment;
import com.andrew.apollo.ui.fragments.RecentFragment;
import com.andrew.apollo.ui.fragments.SongFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link FragmentPagerAdapter} class for swiping between playlists, recent,
 * artists, albums, songs, and genre {@link Fragment}s on phones.<br/>
 */
public class PagerAdapter extends FragmentPagerAdapter {

    private SparseArray<WeakReference<Fragment>> mFragmentArray = new SparseArray<>();

    private List<Holder> mHolderList = new ArrayList<>(4);

    private FragmentActivity mFragmentActivity;

    /**
     * Constructor of <code>PagerAdapter<code>
     *
     * @param fragmentActivity The {@link FragmentActivity} of the {@link androidx.fragment.app.Fragment}.
     */
    public PagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mFragmentActivity = fragmentActivity;
    }

    /**
     * Method that adds a new fragment class to the viewer (the fragment is
     * internally instantiate)
     *
     * @param className The full qualified name of fragment class.
     * @param params    The instantiate params.
     */
    @SuppressWarnings("synthetic-access")
    public void add(Class<? extends Fragment> className, Bundle params) {
        Holder mHolder = new Holder();
        mHolder.mClassName = className.getName();
        mHolder.mParams = params;
        mHolderList.add(mHolder);
        notifyDataSetChanged();
    }

    /**
     * Method that returns the {@link Fragment} in the argument
     * position.
     *
     * @param position The position of the fragment to return.
     * @return Fragment The {@link Fragment} in the argument position.
     */
    public Fragment getFragment(int position) {
        WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null && mWeakFragment.get() != null) {
            return mWeakFragment.get();
        }
        return getItem(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment mFragment = (Fragment) super.instantiateItem(container, position);
        WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
        mFragmentArray.put(position, new WeakReference<>(mFragment));
        return mFragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Fragment getItem(int position) {
        Holder mCurrentHolder = mHolderList.get(position);
        FragmentFactory fragmentFactory = mFragmentActivity.getSupportFragmentManager().getFragmentFactory();
        Fragment result = fragmentFactory.instantiate(mFragmentActivity.getClassLoader(), mCurrentHolder.mClassName);
        result.setArguments(mCurrentHolder.mParams);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mHolderList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentActivity.getResources().getStringArray(R.array.page_titles)[position].toUpperCase(Locale.getDefault());
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public enum MusicFragments {

        /**
         * The playlist fragment
         */
        PLAYLIST(PlaylistFragment.class),
        /**
         * The recent fragment
         */
        RECENT(RecentFragment.class),
        /**
         * The artist fragment
         */
        ARTIST(ArtistFragment.class),
        /**
         * The album fragment
         */
        ALBUM(AlbumFragment.class),
        /**
         * The song fragment
         */
        SONG(SongFragment.class),
        /**
         * The genre fragment
         */
        GENRE(GenreFragment.class),

        /**
         * The folder fragment
         */
        FOLDER(FolderFragment.class);

        private Class<? extends Fragment> mFragmentClass;

        /**
         * Constructor of <code>MusicFragments</code>
         *
         * @param fragmentClass The fragment class
         */
        MusicFragments(Class<? extends Fragment> fragmentClass) {
            mFragmentClass = fragmentClass;
        }

        /**
         * Method that returns the fragment class.
         *
         * @return Class<? extends Fragment> The fragment class.
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

    }

    /**
     * A private class with information about fragment initialization
     */
    private static final class Holder {
        String mClassName;

        Bundle mParams;
    }
}