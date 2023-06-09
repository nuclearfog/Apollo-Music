package org.nuclearfog.apollo.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;


/**
 * Populates the {@link GridView} with the available themes
 */
public class ThemesAdapter extends ArrayAdapter<ThemesAdapter.ThemeHolder> {

	/**
	 * Item layout resource
	 */
	private static final int ITEM_LAYOUT = R.layout.fragment_themes_base;

	/**
	 * fragment layout inflater
	 */
	private LayoutInflater inflater;

	/**
	 * Constructor of <code>ThemesAdapter</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public ThemesAdapter(Context context) {
		super(context, ITEM_LAYOUT);
		inflater = LayoutInflater.from(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		/* Recycle ViewHolder's items */
		MusicHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(ITEM_LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the data holder
		ThemeHolder themeHolder = getItem(position);
		// Set the theme preview
		holder.mImage.setImageDrawable(themeHolder.mPreview);
		// Set the theme name
		holder.mLineOne.setText(themeHolder.mName);
		return convertView;
	}

	/**
	 * holder class for items of {@link ThemesAdapter}
	 */
	public final static class ThemeHolder {

		/**
		 * theme name
		 */
		public final String mName;

		/**
		 * package of the theme
		 */
		public final String mPackage;

		/**
		 * preview drawable
		 */
		@Nullable
		public final Drawable mPreview;

		/**
		 * Constructor of <code>ThemeHolder</code>
		 */
		public ThemeHolder(String pack, String name, @Nullable Drawable prev) {
			mPackage = pack;
			mName = name;
			mPreview = prev;
		}
	}
}