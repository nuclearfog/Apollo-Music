package org.nuclearfog.apollo.ui.adapters.listview;

import android.view.LayoutInflater;
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
		if (convertView == null)  {
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_dropdown, parent, false);
		}
		TextView tv = convertView.findViewById(R.id.list_item_dropdown_text);
		AudioPreset preset = getItem(position);
		if (preset != null)
			tv.setText(preset.getName());
		else
			tv.setText(R.string.preset_custom);
		return convertView;
	}


	@Nullable
	@Override
	public AudioPreset getItem(int position) {
		if (position == 0)
			return null;
		return items.get(position - 1);
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public int getCount() {
		return items.size() + 1;
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