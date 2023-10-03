package org.nuclearfog.apollo.ui.adapters.recyclerview;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.PreferenceUtils;

import java.text.NumberFormat;

/**
 * Recyclerview Adapter to show equalizer band items
 *
 * @author nuclearfog
 */
public class EqualizerAdapter extends RecyclerView.Adapter<EqualizerAdapter.EqualizerHolder> {

	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

	private BandLevelChangeListener listener;
	private int[] level, frequency, range;

	private boolean enabled = true;

	/**
	 * @param listener  listener to call if equalizer level changes
	 * @param level     array of band levels (mB)
	 * @param frequency array of band frequencies (Hz)
	 * @param range     min/max limits of the band
	 */
	public EqualizerAdapter(BandLevelChangeListener listener, int[] level, int[] frequency, int[] range) {
		this.listener = listener;
		this.level = level;
		this.frequency = frequency;
		this.range = range;
	}

	@NonNull
	@Override
	public EqualizerAdapter.EqualizerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		final EqualizerHolder holder = new EqualizerHolder(parent);
		holder.slider.setMax((range[1] - range[0]) / 100);
		holder.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					int position = holder.getLayoutPosition();
					if (position != NO_POSITION) {
						// calculate new band level
						level[position] = progress * 100 + range[0];
						holder.level.setText(NUMBER_FORMAT.format(level[position] / 100.0));
						//
						listener.onBandLevelChange(position, level[position]);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull EqualizerAdapter.EqualizerHolder holder, int position) {
		// set enabled
		holder.slider.setEnabled(enabled);
		// calculate seekbar position
		holder.slider.setProgress((level[position] - range[0]) / 100);
		// band level
		holder.level.setText(NUMBER_FORMAT.format(level[position] / 100.0));
		// band frequency
		if (frequency[position] >= 1000) {
			holder.frequency.setText(NUMBER_FORMAT.format(frequency[position] / 1000.0));
			holder.frequency.append("K");
		} else {
			holder.frequency.setText(NUMBER_FORMAT.format(frequency[position]));
		}
	}

	@Override
	public int getItemCount() {
		return Math.min(level.length, frequency.length);
	}

	/**
	 * enable/disable slider
	 *
	 * @param enable true to enable slider
	 */
	public void setEnabled(boolean enable) {
		this.enabled = enable;
		notifyDataSetChanged();
	}

	/**
	 *
	 */
	static class EqualizerHolder extends RecyclerView.ViewHolder {

		final SeekBar slider;
		final TextView level, frequency;

		public EqualizerHolder(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_equalizer_band, parent, false));
			slider = itemView.findViewById(R.id.eq_seekbar);
			level = itemView.findViewById(R.id.eq_level);
			frequency = itemView.findViewById(R.id.eq_freq);

			PreferenceUtils mPrefs = PreferenceUtils.getInstance(parent.getContext());
			slider.getProgressDrawable().setColorFilter(mPrefs.getDefaultThemeColor(), PorterDuff.Mode.SRC_IN);
			slider.getThumb().setColorFilter(mPrefs.getDefaultThemeColor(), PorterDuff.Mode.SRC_IN);
		}
	}

	/**
	 * Adapter item listener
	 */
	public interface BandLevelChangeListener {

		/**
		 * called when a band seekbar changes
		 *
		 * @param pos   adapter position of the band
		 * @param level level value
		 */
		void onBandLevelChange(int pos, int level);
	}
}