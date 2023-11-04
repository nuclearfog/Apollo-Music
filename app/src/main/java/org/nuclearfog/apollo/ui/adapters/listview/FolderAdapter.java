package org.nuclearfog.apollo.ui.adapters.listview;

import static android.view.View.GONE;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Folder;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;

/**
 * decompiled from Apollo.APK version 1.6
 * <p>
 * this adapter creates views with music folder information
 */
public class FolderAdapter extends ArrayAdapter<Folder> {

	/**
	 * item layout reource
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * fragment layout inflater
	 */
	private LayoutInflater inflater;

	/**
	 * @param context application context
	 */
	public FolderAdapter(Context context) {
		super(context, LAYOUT);
		// layout inflater from context
		inflater = LayoutInflater.from(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup container) {
		MusicHolder holder;
		if (convertView == null) {
			// inflate view
			convertView = inflater.inflate(LAYOUT, container, false);
			holder = new MusicHolder(convertView);
			// disable unnecessary views
			holder.mLineTwo.setVisibility(GONE);
			holder.mLineThree.setVisibility(GONE);
			// set text size
			float textSize = getContext().getResources().getDimension(R.dimen.text_size_large);
			holder.mLineOne.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			// set folder ICON
			holder.mLineOne.setCompoundDrawablesWithIntrinsicBounds(R.drawable.folder, 0, 0, 0);
			// attach holder to view
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		Folder folder = getItem(position);
		if (folder != null) {
			String name = folder.getName();
			holder.mLineOne.setText(name);
			if (folder.isVisible()) {
				convertView.setAlpha(1.0f);
			} else {
				convertView.setAlpha(Config.OPACITY_HIDDEN);
			}
		}
		return convertView;
	}
}