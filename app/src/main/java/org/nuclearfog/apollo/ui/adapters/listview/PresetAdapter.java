package org.nuclearfog.apollo.ui.adapters.listview;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.AudioPreset;

import java.util.ArrayList;
import java.util.List;

/**
 * Dropdown adapter used to show audio effect presets
 *
 * @author nuclearfog
 */
public class PresetAdapter extends BaseAdapter {

	private List<AudioPreset> items = new ArrayList<>();


	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		TextView tv;
		if (convertView instanceof TextView) {
			tv = (TextView) convertView;
		} else {
			tv = new TextView(parent.getContext());
			float textSize = parent.getResources().getDimension(R.dimen.text_size_large);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}
		tv.setText(items.get(position).getName());
		return tv;
	}


	@Nullable
	@Override
	public AudioPreset getItem(int position) {
		return items.get(position);
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public int getCount() {
		return items.size();
	}

	/**
	 * set adapter items
	 */
	public void setItems(List<AudioPreset> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}
}