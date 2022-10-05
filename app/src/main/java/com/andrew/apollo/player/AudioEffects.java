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
        enableAudioFx(prefs.isAudioFxEnabled());
        setBands(prefs.getEqualizerBands());
        setBassLevel(prefs.getBassLevel());
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
     * get equalizer bands
     *
     * @return array of band levels starting from the lowest equalizer frequency
     */
    public int[] getBands() {
        short bandCount = equalizer.getNumberOfBands();
        int[] bandLevel = new int[bandCount];

        for (short i = 0 ; i < bandCount ; i++) {
            bandLevel[i] = equalizer.getBandLevel(i);
        }
        return bandLevel;
    }

    /**
     * set new equalizer band
     *
     * @param bandLevel array of band levels starting from the lowest equalizer frequency
     */
    public void setBands(int[] bandLevel) {
        for (int i = 0 ; i < bandLevel.length ; i++) {
            setBand(i, bandLevel[i]);
        }
        prefs.setEqualizerBands(bandLevel);
    }

    /**
     * set a new equalizer band value
     * @param pos index of the equalizer band
     * @param band level of the band
     */
    public void setBand(int pos, int band) {
        equalizer.setBandLevel((short) pos, (short) band);
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
     * set bass boost strength
     * @param strength bassbost strength value from 0 to 1000
     */
    public void setBassLevel(int strength) {
        bassBooster.setStrength((short) strength);
        prefs.setBassLevel(strength);
    }
}