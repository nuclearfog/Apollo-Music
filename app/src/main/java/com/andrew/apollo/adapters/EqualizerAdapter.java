package com.andrew.apollo.adapters;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.andrew.apollo.R;

import java.text.NumberFormat;

/**
 * Recyclerview Adapter to show equalizer band items
 *
 * @author nuclearfog
 */
public class EqualizerAdapter extends RecyclerView.Adapter<EqualizerAdapter.EqualizerHolder> {

	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

	private EqualizerListener listener;
	private int[][] bands;

	/**
	 * @param listener  listener to call if equalizer level changes
	 * @param bands     amplitude/frequency matrix
	 */
	public EqualizerAdapter(EqualizerListener listener, int[][] bands) {
		this.listener = listener;
		this.bands = bands;
	}


	@NonNull
	@Override
	public EqualizerAdapter.EqualizerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		final EqualizerHolder holder = new EqualizerHolder(parent);
		holder.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					int position = holder.getLayoutPosition();
					if (position != NO_POSITION) {
						listener.onLevelChange(position, progress);
						holder.level.setText(NUMBER_FORMAT.format(progress));
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
		holder.slider.setProgress(bands[0][position]);
		holder.level.setText(NUMBER_FORMAT.format(bands[0][position]));
		holder.frequency.setText(NUMBER_FORMAT.format(bands[1][position]));
	}

	@Override
	public int getItemCount() {
		return Math.min(bands[0].length, bands[1].length);
	}

	/**
	 */
	public static class EqualizerHolder extends RecyclerView.ViewHolder {

		public static final int SLIDER_MAX = 2000;

		public final SeekBar slider;
		public final TextView level, frequency;

		public EqualizerHolder(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_equalizer_band, parent, false));
			slider = itemView.findViewById(R.id.eq_seekbar);
			level = itemView.findViewById(R.id.eq_level);
			frequency = itemView.findViewById(R.id.eq_freq);

			slider.setMax(SLIDER_MAX);
		}
	}

	public interface EqualizerListener {

		void onLevelChange(int pos, int level);
	}
}