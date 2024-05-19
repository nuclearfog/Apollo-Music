package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.AudioPreset;
import org.nuclearfog.apollo.store.PresetStore;

import java.util.List;

/**
 * @author nuclearfog
 */
public class PresetLoader extends AsyncExecutor<AudioPreset, List<AudioPreset>> {

	private PresetStore presetStore;

	public PresetLoader(Context context) {
		super(context);
		presetStore = PresetStore.getInstance(context);
	}


	@Override
	protected List<AudioPreset> doInBackground(AudioPreset param) {
		if (param != null) {
			presetStore.savePreset(param);
		}
		return presetStore.loadPresets();
	}
}