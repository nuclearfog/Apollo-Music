package com.andrew.apollo.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.format.PrefixHighlighter;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Music;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import java.util.Locale;

/**
 * Used to populate the list view with the search results.
 */
public class SearchAdapter extends ArrayAdapter<Music> {

    /**
     * Number of views (ImageView and TextView)
     */
    private static final int VIEW_TYPE_COUNT = 2;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Highlights the query
     */
    private final PrefixHighlighter mHighlighter;

    /**
     * The prefix that's highlighted
     */
    private char[] mPrefix;

    /**
     * Constructor for <code>SearchAdapter</code>
     *
     * @param activity The {@link FragmentActivity} to use.
     */
    public SearchAdapter(FragmentActivity activity) {
        super(activity, 0);
        // Initialize the cache & image fetcher
        mImageFetcher = ApolloUtils.getImageFetcher(activity);
        // Create the prefix highlighter
        mHighlighter = new PrefixHighlighter(activity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MusicHolder holder;
        if (convertView == null) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            convertView = inf.inflate(R.layout.list_item_detailed, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
        } else {
            /* Recycle ViewHolder's items */
            holder = (MusicHolder) convertView.getTag();
        }

        Music music = getItem(position);
        if (music instanceof Artist) {
            Context context = parent.getContext();
            Artist artist = (Artist) music;
            holder.mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // Get the artist name
            holder.mLineOne.setText(artist.getName());
            // Get the album count
            holder.mLineTwo.setText(MusicUtils.makeLabel(context, R.plurals.Nalbums, artist.getAlbumCount()));
            // Get the song count
            holder.mLineThree.setText(MusicUtils.makeLabel(context, R.plurals.Nsongs, artist.getTrackCount()));
            // Asynchronously load the artist image into the adapter
            mImageFetcher.loadArtistImage(artist.getName(), holder.mImage);
            // Highlight the query
            mHighlighter.setText(holder.mLineOne, artist.getName(), mPrefix);
        } else if (music instanceof Album) {
            Album album = (Album) music;
            holder.mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // Get the album name
            holder.mLineOne.setText(album.getName());
            // Get the artist name
            holder.mLineTwo.setText(album.getArtist());
            // Asynchronously load the album images into the adapter
            mImageFetcher.loadAlbumImage(album.getArtist(), album.getName(), album.getId(), holder.mImage);
            // Asynchronously load the artist image into the adapter
            mImageFetcher.loadArtistImage(album.getArtist(), holder.mBackground);
            // Highlight the query
            mHighlighter.setText(holder.mLineOne, album.getName(), mPrefix);
        } else if (music instanceof Song) {
            Song song = (Song) music;
            holder.mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.mImage.setImageResource(R.drawable.header_temp);
            // Get the track name
            holder.mLineOne.setText(song.getName());
            // Get the album name
            holder.mLineTwo.setText(song.getAlbum());
            // Asynchronously load the artist image into the adapter
            mImageFetcher.loadArtistImage(song.getArtist(), holder.mBackground);
            holder.mLineThree.setText(song.getArtist());
            // Highlight the query
            mHighlighter.setText(holder.mLineOne, song.getName(), mPrefix);
        }
        return convertView;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(boolean pause) {
        if (mImageFetcher != null) {
            mImageFetcher.setPauseDiskCache(pause);
        }
    }

    /**
     * @param prefix The query to filter.
     */
    public void setPrefix(CharSequence prefix) {
        if (!TextUtils.isEmpty(prefix)) {
            mPrefix = prefix.toString().toUpperCase(Locale.getDefault()).toCharArray();
        } else {
            mPrefix = null;
        }
    }
}