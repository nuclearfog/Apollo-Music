package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.loader.PresetLoader.Param;
import org.nuclearfog.apollo.model.AudioPreset;
import org.nuclearfog.apollo.store.PresetStore;

import java.util.List;

/**
 * @author nuclearfog
 */
public class PresetLoader extends AsyncExecutor<Param, List<AudioPreset>> {

	private PresetStore presetStore;

	public PresetLoader(Context context) {
		super(context);
		presetStore = PresetStore.getInstance(context);
	}


	@Override
	protected List<AudioPreset> doInBackground(Param param) {
		if (param.mode == Param.SAVE) {
			presetStore.savePreset(param.preset);
		} else if (param.mode == Param.DEL) {
			presetStore.deletePreset(param.preset);
		}
		return presetStore.loadPresets();
	}


	public static class Param {

		public static final int LOAD = 1;
		public static final int SAVE = 2;
		public static final int DEL = 3;

		final int mode;
		final AudioPreset preset;

		public Param(int mode, AudioPreset preset) {
			this.mode = mode;
			this.preset = preset;
		}
	}
}