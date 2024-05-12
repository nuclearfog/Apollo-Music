package org.nuclearfog.apollo.ui.activities;

import android.annotation.SuppressLint;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.loaders.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.loaders.PresetLoader;
import org.nuclearfog.apollo.model.AudioPreset;
import org.nuclearfog.apollo.player.AudioEffects;
import org.nuclearfog.apollo.ui.adapters.listview.PresetAdapter;
import org.nuclearfog.apollo.ui.adapters.recyclerview.EqualizerAdapter;
import org.nuclearfog.apollo.ui.adapters.recyclerview.EqualizerAdapter.BandLevelChangeListener;
import org.nuclearfog.apollo.ui.dialogs.PresetDialog;
import org.nuclearfog.apollo.ui.dialogs.PresetDialog.OnPresetSaveCallback;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.List;

/**
 * Audio effects activity
 *
 * @author nuclerfog
 */
public class AudioFxActivity extends AppCompatActivity implements BandLevelChangeListener, OnCheckedChangeListener,
		OnSeekBarChangeListener, AsyncCallback<List<AudioPreset>>, OnItemSelectedListener, OnPresetSaveCallback {

	/**
	 * maximum steps of the bassboost seekbar
	 */
	private static final int BASS_STEPS = 20;

	@SuppressLint("UseSwitchCompatOrMaterialCode")
	private Switch enableFx;
	private SeekBar bassBoost, reverb;
	private Spinner presetSelector;
	private EqualizerAdapter eqAdapter;
	private PresetAdapter presetAdapter;
	private AudioEffects audioEffects;
	private PresetLoader presetLoader;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audiofx);
		Toolbar toolbar = findViewById(R.id.audiofx_toolbar);
		RecyclerView eq_bands = findViewById(R.id.audiofx_eq_scroll);
		presetSelector = findViewById(R.id.audiofx_preset);
		enableFx = findViewById(R.id.audiofx_enable);
		bassBoost = findViewById(R.id.audiofx_bass_boost);
		reverb = findViewById(R.id.audiofx_reverb);
		presetAdapter = new PresetAdapter();
		presetLoader = new PresetLoader(this);
		audioEffects = AudioEffects.getInstance(this, MusicUtils.getAudioSessionId());

		// set theme colors
		PreferenceUtils mPrefs = PreferenceUtils.getInstance(this);
		ColorFilter colorFilter = new PorterDuffColorFilter(mPrefs.getDefaultThemeColor(), PorterDuff.Mode.SRC_IN);
		bassBoost.getProgressDrawable().setColorFilter(colorFilter);
		bassBoost.getThumb().setColorFilter(colorFilter);
		reverb.getProgressDrawable().setColorFilter(colorFilter);
		reverb.getThumb().setColorFilter(colorFilter);
		enableFx.getThumbDrawable().setColorFilter(colorFilter);

		bassBoost.setMax(BASS_STEPS);
		reverb.setMax(AudioEffects.MAX_REVERB);
		eq_bands.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

		presetSelector.setAdapter(presetAdapter);

		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			ThemeUtils mResources = new ThemeUtils(this);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			mResources.themeActionBar(getSupportActionBar(), R.string.title_audio_effects);
		}
		if (audioEffects != null) {
			eqAdapter = new EqualizerAdapter(this, audioEffects.getBandFrequencies(), audioEffects.getBandLevelRange());
			eq_bands.setAdapter(eqAdapter);
			setVisibility(audioEffects.isAudioFxEnabled());
			setViews();
			setPreset();
		} else {
			Toast.makeText(this, R.string.error_audioeffects_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		presetLoader.execute(null, this);

		enableFx.setOnCheckedChangeListener(this);
		bassBoost.setOnSeekBarChangeListener(this);
		reverb.setOnSeekBarChangeListener(this);
		presetSelector.setOnItemSelectedListener(this);
	}


	@Override
	protected void onDestroy() {
		presetLoader.cancel();
		super.onDestroy();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.audiofx, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_save_preset) {
			AudioPreset preset = audioEffects.getPreset();
			PresetDialog presetDialog = PresetDialog.newInstance(preset);
			presetDialog.show(getSupportFragmentManager(), PresetDialog.TAG + ":" + preset.getName());
		} else if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return true;
	}


	@Override
	public void onBandLevelChange(int pos, int level) {
		audioEffects.setBandLevel(pos, level);
	}


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() == R.id.audiofx_enable) {
			audioEffects.enableAudioFx(isChecked);
			setVisibility(isChecked);
		}
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			// set bass boost
			if (seekBar.getId() == R.id.audiofx_bass_boost) {
				audioEffects.setBassLevel(progress * AudioEffects.MAX_BASSBOOST / BASS_STEPS);
			}
			// set reverb
			else if (seekBar.getId() == R.id.audiofx_reverb) {
				audioEffects.setReverbLevel(progress);
			}
		}
	}


	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}


	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}


	@Override
	public void onResult(@NonNull List<AudioPreset> audioPresets) {
		presetAdapter.setItems(audioPresets);
		setPreset();
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent.getId() == R.id.audiofx_preset) {
			AudioPreset preset = presetAdapter.getItem(position);
			if (preset != null) {
				audioEffects.setPreset(preset);
				setViews();
			}
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}


	@Override
	public void onPresetSave(AudioPreset preset) {
		audioEffects.setPreset(preset);
		presetLoader.execute(preset, this);
	}

	/**
	 * set view values
	 */
	private void setViews() {
		enableFx.setChecked(audioEffects.isAudioFxEnabled());
		bassBoost.setProgress(audioEffects.getBassLevel() * BASS_STEPS / AudioEffects.MAX_BASSBOOST);
		reverb.setProgress(audioEffects.getReverbLevel());
		eqAdapter.setBands(audioEffects.getBandLevel());
	}


	private void setVisibility(boolean enable) {
		// enable views only if effect is enabled
		reverb.setEnabled(enable);
		bassBoost.setEnabled(enable);
		eqAdapter.setEnabled(enable);
	}

	/**
	 * select current selected preset
	 */
	private void setPreset() {
		String presetName = audioEffects.getPreset().getName();
		for (int i = 0; i < presetAdapter.getCount(); i++) {
			AudioPreset preset = presetAdapter.getItem(i);
			if (preset != null && preset.getName().equals(presetName)) {
				presetSelector.setSelection(i);
				break;
			}
		}
	}
}