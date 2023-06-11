package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.utils.PreferenceUtils;

/**
 * Audio effect class providing methods to manage effects at realtime
 *
 * @author nuclearfog
 */
public final class AudioEffects {

	/**
	 * max limit of the bass boost effect defined in {@link BassBoost}
	 */
	public static final int MAX_BASSBOOST = 1000;

	/**
	 * max reverb steps definded in {@link PresetReverb}
	 */
	public static final int MAX_REVERB = 6;

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
			if (instance == null || instance.sessionId != sessionId) {
				instance = new AudioEffects(context, sessionId);
			}
			return instance;
		} catch (Exception e) {
			// thrown if there is no support for audio effects
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * @param sessionId current audio session ID
	 */
	private AudioEffects(Context context, int sessionId) {
		equalizer = new Equalizer(0, sessionId);
		bassBooster = new BassBoost(0, sessionId);
		reverb = new PresetReverb(0, sessionId);
		prefs = PreferenceUtils.getInstance(context);
		this.sessionId = sessionId;
		boolean active = prefs.isAudioFxEnabled();

		equalizer.setEnabled(active);
		bassBooster.setEnabled(active);
		if (active) {
			setEffectValues();
		}
	}

	/**
	 * enable/disable audio effects
	 *
	 * @param enable true to enable all audio effects
	 */
	public void enableAudioFx(boolean enable) {
		equalizer.setEnabled(enable);
		bassBooster.setEnabled(enable);
		prefs.setAudioFxEnabled(enable);
		if (enable) {
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
	 * get min, max limits of the eq band
	 *
	 * @return array with min and max limits
	 */
	public int[] getBandLevelRange() {
		short[] ranges = equalizer.getBandLevelRange();
		return new int[]{ranges[0], ranges[1]};
	}

	/**
	 * get band frequencies
	 *
	 * @return array of band frequencies, starting with the lowest frequency
	 */
	public int[] getBandFrequencies() {
		short bandCount = equalizer.getNumberOfBands();
		int[] freq = new int[bandCount];
		for (short i = 0; i < bandCount; i++) {
			freq[i] = equalizer.getCenterFreq(i) / 1000;
		}
		return freq;
	}

	/**
	 * get equalizer bands
	 *
	 * @return array of band levels and frequencies starting from the lowest equalizer frequency
	 */
	public int[] getBandLevel() {
		short bandCount = equalizer.getNumberOfBands();
		int[] level = new int[bandCount];
		for (short i = 0; i < bandCount; i++) {
			level[i] = equalizer.getBandLevel(i);
		}
		return level;
	}

	/**
	 * set a new equalizer band value
	 *
	 * @param band  index of the equalizer band
	 * @param level level of the band
	 */
	public void setBandLevel(int band, int level) {
		// set single band level
		equalizer.setBandLevel((short) band, (short) level);
		// save all equalizer band levels
		short bandCount = equalizer.getNumberOfBands();
		int[] bands = new int[bandCount];
		for (short i = 0; i < bandCount; i++) {
			bands[i] = equalizer.getBandLevel(i);
		}
		prefs.setEqualizerBands(bands);
	}

	/**
	 * return bass boost strength
	 *
	 * @return bassbost strength value from 0 to 1000
	 */
	public int getBassLevel() {
		return bassBooster.getRoundedStrength();
	}

	/**
	 * set bass boost level
	 *
	 * @param level bassbost strength value from 0 to 1000
	 */
	public void setBassLevel(int level) {
		bassBooster.setStrength((short) level);
		prefs.setBassLevel(level);
	}

	/**
	 * get reverb level
	 *
	 * @return reverb level
	 */
	public int getReverbLevel() {
		return reverb.getPreset();
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level
	 */
	public void setReverbLevel(int level) {
		reverb.setPreset((short) level);
		prefs.setReverbLevel(level);
	}

	/**
	 * set saved values for audio effects
	 */
	private void setEffectValues() {
		// setup audio effects
		bassBooster.setStrength((short) prefs.getBassLevel());
		reverb.setPreset((short) prefs.getReverbLevel());
		int[] bandLevel = prefs.getEqualizerBands();
		for (short i = 0; i < bandLevel.length; i++) {
			equalizer.setBandLevel(i, (short) bandLevel[i]);
		}
	}
}