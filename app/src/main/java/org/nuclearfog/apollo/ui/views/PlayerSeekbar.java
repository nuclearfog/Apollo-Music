package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.StringUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom view providing a seekbar for the player and progress/duration viewer
 *
 * @author nuclearfog
 */
public class PlayerSeekbar extends LinearLayout implements OnSeekBarChangeListener {

	private TextView[] times = new TextView[2];
	private SeekBar seekbar;

	@Nullable
	private OnPlayerSeekListener listener;

	/**
	 * current position of the seekbar in milliseconds
	 */
	private long position;

	/**
	 * duration of the seekbar in milliseconds
	 */
	private long duration;

	/**
	 * set to true to enable automatic seekbar updates
	 */
	private boolean updateSeekbar;

	private Future<?> updateTask;

	/**
	 * thread pool used to run a task to periodically update seekbar and time
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	private Runnable timeHandler = new TimeHandler(this);

	/**
	 * {@inheritDoc}
	 */
	public PlayerSeekbar(Context context) {
		this(context, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public PlayerSeekbar(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		PreferenceUtils mPrefs = PreferenceUtils.getInstance(context);
		LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
		int themeColor = mPrefs.getDefaultThemeColor();
		float textsize = getResources().getDimension(R.dimen.text_size_micro);
		float width = getResources().getDimension(R.dimen.audio_player_time_width);
		int color = getResources().getColor(R.color.audio_player_current_time);
		// theme views
		seekbar = new SeekBar(context);
		for (int i = 0; i < times.length; i++) {
			times[i] = new TextView(context);
			times[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize);
			times[i].setWidth((int) width);
			times[i].setGravity(Gravity.CENTER);
			times[i].setTextColor(color);
			times[i].setSingleLine();
		}
		seekbar.getProgressDrawable().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		seekbar.getThumb().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		//
		setCurrentTimeText(0);
		// configure seekbar
		seekbar.setMax(1000);
		seekbar.setLayoutParams(param);
		seekbar.setOnSeekBarChangeListener(this);
		// configure root view
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER);
		addView(times[0]);
		addView(seekbar);
		addView(times[1]);
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (listener != null && fromUser) {
			position = (duration * progress) / 1000L;
			setCurrentTimeText(position);
			listener.onSeek(position);
		}
	}


	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		updateSeekbar = false;
	}


	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		updateSeekbar = true;
	}

	/**
	 * clean up resources associated with this view
	 */
	public void release() {
		threadPool.shutdown();
	}

	/**
	 * set time for seekbar
	 *
	 * @param time time in milliseconds
	 */
	public void setCurrentTime(long time) {
		if (time >= 0 && time <= duration && duration > 0) {
			position = time;
			setCurrentTimeText(time);
			seekbar.setProgress((int) (1000 * time / duration));
		} else {
			position = 0;
			setCurrentTimeText(0);
			seekbar.setProgress(0);
		}
	}

	/**
	 * set duration of the seekbar
	 *
	 * @param time time in milliseconds
	 */
	public void setTotalTime(long time) {
		times[1].setText(StringUtils.makeTimeString(getContext(), time));
		duration = time;
		seek(0);
	}

	/**
	 * seek to a new position
	 *
	 * @param to time in milliseconds
	 */
	public void seek(long to) {
		if (duration > 0) {
			position = to;
			int pos = (int) (to * 1000L / duration);
			seekbar.setProgress(pos);
		} else {
			position = 0;
			seekbar.setProgress(0);
		}
		setCurrentTimeText(position);
	}

	/**
	 * enable automatic seekbar moving
	 *
	 * @param isPlaying true to move the seekbar automatically
	 */
	public void setPlayStatus(boolean isPlaying) {
		updateSeekbar = isPlaying;
		AnimatorUtils.pulse(times[0], !isPlaying);
		if (isPlaying) {
			if (updateTask == null)
				updateTask = threadPool.scheduleWithFixedDelay(timeHandler, TimeHandler.CYCLE_MS, TimeHandler.CYCLE_MS, TimeUnit.MILLISECONDS);
		} else if (updateTask != null) {
			updateTask.cancel(true);
			updateTask = null;
		}
	}

	/**
	 * set listener to call when the user interact with the seekbar
	 */
	public void setOnPlayerSeekListener(OnPlayerSeekListener listener) {
		this.listener = listener;
	}

	/**
	 * print current time value of the position
	 */
	private void setCurrentTimeText(long time) {
		times[0].setText(StringUtils.makeTimeString(getContext(), time));
	}

	/**
	 * called periodically by {@link TimeHandler} to update the seekbar positioin
	 */
	private void update() {
		if (updateSeekbar) {
			position += TimeHandler.CYCLE_MS;
			setCurrentTimeText(position);
			seek(position);
		}
	}

	/**
	 * Listener interface used to update player position after user interaction
	 */
	public interface OnPlayerSeekListener {

		/**
		 * called to seek to a specific position
		 *
		 * @param position position in milliseconds
		 */
		void onSeek(long position);
	}

	/**
	 * Runnable used to update the seekbar position peridocally
	 */
	private static final class TimeHandler implements Runnable {

		/**
		 * time to refresh seekbar/position
		 */
		public static final int CYCLE_MS = 250;

		private WeakReference<PlayerSeekbar> callback;

		/**
		 * @param player callback to this view
		 */
		public TimeHandler(PlayerSeekbar player) {
			callback = new WeakReference<>(player);
		}


		@Override
		public void run() {
			PlayerSeekbar seekbar = callback.get();
			if (seekbar != null) {
				try {
					seekbar.update();
				} catch (Exception exception) {
					// ignore
				}
			}
		}
	}
}