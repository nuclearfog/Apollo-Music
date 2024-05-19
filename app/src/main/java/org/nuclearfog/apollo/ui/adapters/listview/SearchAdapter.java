package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PrefixHighlighter;

import java.util.Locale;

/**
 * Used to populate the list view with the search results.
 */
public class SearchAdapter extends ArrayAdapter<Music> {

	/**
	 * layout resource file
	 */
	private static final int LAYOUT = R.layout.list_item_detailed;

	/**
	 * fragment layout inflater
	 */
	private LayoutInflater inflater;

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
		super(activity, LAYOUT);
		// Initialize the cache & image fetcher
		mImageFetcher = ApolloUtils.getImageFetcher(activity);
		// Create the prefix highlighter
		mHighlighter = new PrefixHighlighter(activity);
		// get inflater from fragment
		inflater = activity.getLayoutInflater();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		MusicHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			convertView.setTag(holder);
		} else {
			/* Recycle ViewHolder's items */
			holder = (MusicHolder) convertView.getTag();
		}
		Music music = getItem(position);
		if (music instanceof Artist) {
			// set artist information
			Context context = parent.getContext();
			Artist artist = (Artist) music;
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
			// set album information
			Album album = (Album) music;
			// Get the album name
			holder.mLineOne.setText(album.getName());
			// Get the artist name
			holder.mLineTwo.setText(album.getArtist());
			// Asynchronously load the album images into the adapter
			mImageFetcher.loadAlbumImage(album, holder.mImage);
			// Highlight the query
			mHighlighter.setText(holder.mLineOne, album.getName(), mPrefix);
		} else if (music instanceof Song) {
			// set track information
			Song song = (Song) music;
			// set image
			holder.mImage.setImageResource(R.drawable.header_temp);
			// Get the track name
			holder.mLineOne.setText(song.getName());
			// Get the album name
			holder.mLineTwo.setText(song.getAlbum());
			// Get the artist name
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
	public long getItemId(int position) {
		Music music = getItem(position);
		if (music != null)
			return music.getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
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