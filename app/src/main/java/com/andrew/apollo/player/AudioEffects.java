package com.andrew.apollo.player;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;

import com.andrew.apollo.utils.PreferenceUtils;

/**
 * Audio effect class providing methods to manage effects at realtime
 *
 * @author nuclearfog
 */
public class AudioEffects {

    private Equalizer equalizer;
    private BassBoost bassBooster;

    private PreferenceUtils prefs;

    /**
     * @param context   context to get equalizer settings
     * @param sessionId current audio session ID
     */
    public AudioEffects(Context context, int sessionId) {
        equalizer = new Equalizer(0, sessionId);
        bassBooster = new BassBoost(0, sessionId);

        prefs = PreferenceUtils.getInstance(context);

        equalizer.setEnabled(prefs.isAudioFxEnabled());
        bassBooster.setEnabled(prefs.isAudioFxEnabled());
        bassBooster.setStrength((short) prefs.getBassLevel());
        int[] bandLevel = prefs.getEqualizerBands();
        for (short i = 0 ; i < bandLevel.length ; i++) {
            equalizer.setBandLevel( i, (short) bandLevel[i]);
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
    }

    /**
     * @return true if audio FX is enabled
     */
    public boolean isAudioFxEnabled() {
        return prefs.isAudioFxEnabled();
    }

    /**
     * get equalizer bands
     *
     * @return array of band levels and frequencies starting from the lowest equalizer frequency
     */
    public int[][] getBandLevel() {
        short bandCount = equalizer.getNumberOfBands();
        int[][] level = new int[2][bandCount];
        for (short i = 0 ; i < bandCount ; i++) {
            level[0][i] = equalizer.getBandLevel(i);
        }
        for (short i = 0 ; i < bandCount ; i++) {
            level[1][i] = equalizer.getCenterFreq(i) / 1000;
        }
        return level;
    }

    /**
     * set a new equalizer band value
     * @param band  index of the equalizer band
     * @param level level of the band
     */
    public void setBandLevel(int band, int level) {
        // set single band level
        equalizer.setBandLevel((short) band, (short) level);
        // save all equalizer band levels
        short bandCount = equalizer.getNumberOfBands();
        int[] bands = new int[bandCount];
        for (short i = 0 ; i < bandCount ; i++) {
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
}