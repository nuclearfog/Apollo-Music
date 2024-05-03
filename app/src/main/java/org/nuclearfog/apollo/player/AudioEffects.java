package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.util.Log;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.utils.PreferenceUtils;

/**
 * Audio effect class providing methods to manage effects at realtime
 *
 * @author nuclearfog
 */
public final class AudioEffects {

	private static final String TAG = "AudioEffects";

	/**
	 * max limit of the bass boost effect defined in {@link BassBoost}
	 */
	public static final int MAX_BASSBOOST = 1000;

	/**
	 * max reverb steps definded in {@link PresetReverb}
	 */
	public static final int MAX_REVERB = 6;

	/**
	 * priority used by audiofx (default 0, high > 0, low < 0)
	 */
	private static final int FX_PRIORITY = 1;

	/**
	 * singleton instance
	 * regenerated if session ID changes
	 */
	private static AudioEffects instance;

	private Equalizer equalizer;
	private BassBoost bassBooster;
	private PresetReverb reverb;
	private PreferenceUtils prefs;

	private int sessionId;

	/**
	 * get singleton instance
	 *
	 * @param context   context to get equalizer settings
	 * @param sessionId current audio session ID
	 * @return {@link AudioEffects} instance or null if audio effects isn't supported
	 */
	@Nullable
	public static AudioEffects getInstance(Context context, int sessionId) {
		try {
			if (sessionId != 0) {
				if (instance == null || instance.sessionId != sessionId) {
					instance = new AudioEffects(context, sessionId);
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "audio_session_id=" + sessionId);
					}
				}
			} else {
				Log.e(TAG, "init audio effects failed, audio session id is '0'!");
			}
		} catch (Exception e) {
			// thrown if there is no support for audio effects
			Log.d(TAG, "audio effects not supported!");
		}
		return instance;
	}

	/**
	 * release all audioeffects from usage
	 */
	public static void release() {
		if (instance != null) {
			try {
				instance.equalizer.release();
				instance.bassBooster.release();
				instance.reverb.release();
			} catch (RuntimeException exception) {
				if (BuildConfig.DEBUG) {
					exception.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param sessionId current audio session ID
	 */
	private AudioEffects(Context context, int sessionId) {
		equalizer = new Equalizer(FX_PRIORITY, sessionId);
		bassBooster = new BassBoost(FX_PRIORITY, sessionId);
		reverb = new PresetReverb(FX_PRIORITY, sessionId);
		prefs = PreferenceUtils.getInstance(context);
		this.sessionId = sessionId;
		boolean active = prefs.isAudioFxEnabled();

		equalizer.setEnabled(active);
		bassBooster.setEnabled(active);
		reverb.setEnabled(active);
		if (active) {
			setEffectValues();
		}
	}

	/**
	 * @return true if audio FX is enabled
	 */
	public boolean isAudioFxEnabled() {
		return prefs.isAudioFxEnabled();
	}

	/**
	 * enable/disable audio effects
	 *
	 * @param enable true to enable all audio effects
	 */
	public void enableAudioFx(boolean enable) {
		try {
			equalizer.setEnabled(enable);
			bassBooster.setEnabled(enable);
			reverb.setEnabled(enable);
			prefs.setAudioFxEnabled(enable);
			if (enable) {
				setEffectValues();
			}
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * get min, max limits of the eq band
	 *
	 * @return array with min and max limits
	 */
	public int[] getBandLevelRange() {
		try {
			short[] ranges = equalizer.getBandLevelRange();
			return new int[]{ranges[0], ranges[1]};
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return new int[2];
	}

	/**
	 * get band frequencies
	 *
	 * @return array of band frequencies, starting with the lowest frequency
	 */
	public int[] getBandFrequencies() {
		try {
			short bandCount = equalizer.getNumberOfBands();
			int[] freq = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				freq[i] = equalizer.getCenterFreq(i) / 1000;
			}
			return freq;
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return new int[0];
	}

	/**
	 * get equalizer bands
	 *
	 * @return array of band levels and frequencies starting from the lowest equalizer frequency
	 */
	public int[] getBandLevel() {
		try {
			short bandCount = equalizer.getNumberOfBands();
			int[] level = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				level[i] = equalizer.getBandLevel(i);
			}
			return level;
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return new int[0];
	}

	/**
	 * set a new equalizer band value
	 *
	 * @param band  index of the equalizer band
	 * @param level level of the band
	 */
	public void setBandLevel(int band, int level) {
		try {
			// set single band level
			equalizer.setBandLevel((short) band, (short) level);
			// save all equalizer band levels
			short bandCount = equalizer.getNumberOfBands();
			int[] bands = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				bands[i] = equalizer.getBandLevel(i);
			}
			prefs.setEqualizerBands(bands);
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * return bass boost strength
	 *
	 * @return bassbost strength value from 0 to 1000
	 */
	public int getBassLevel() {
		try {
			return bassBooster.getRoundedStrength();
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * set bass boost level
	 *
	 * @param level bassbost strength value from 0 to 1000
	 */
	public void setBassLevel(int level) {
		try {
			bassBooster.setStrength((short) level);
			prefs.setBassLevel(level);
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * get reverb level
	 *
	 * @return reverb level
	 */
	public int getReverbLevel() {
		try {
			return reverb.getPreset();
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level
	 */
	public void setReverbLevel(int level) {
		try {
			reverb.setPreset((short) level);
			prefs.setReverbLevel(level);
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * set saved values for audio effects
	 */
	private void setEffectValues() {
		try {
			// setup audio effects
			bassBooster.setStrength((short) prefs.getBassLevel());
			reverb.setPreset((short) prefs.getReverbLevel());
			int[] bandLevel = prefs.getEqualizerBands();
			for (short i = 0; i < bandLevel.length; i++) {
				equalizer.setBandLevel(i, (short) bandLevel[i]);
			}
		} catch (RuntimeException exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}
}