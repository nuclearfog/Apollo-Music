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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.PresetLoader;
import org.nuclearfog.apollo.model.AudioPreset;
import org.nuclearfog.apollo.player.AudioEffects;
import org.nuclearfog.apollo.ui.adapters.listview.PresetAdapter;
import org.nuclearfog.apollo.ui.adapters.recyclerview.EqualizerAdapter;
import org.nuclearfog.apollo.ui.adapters.recyclerview.EqualizerAdapter.BandLevelChangeListener;
import org.nuclearfog.apollo.ui.dialogs.PresetDialog;
import org.nuclearfog.apollo.ui.dialogs.PresetDialog.OnPresetSaveCallback;
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
	 * Bundle key used to set current audio session id
	 * value type is Integer
	 */
	public static final String KEY_SESSION_ID = "session_id";

	/**
	 * maximum steps of the bassboost seekbar
	 */
	private static final int BASS_STEPS = 20;

	@SuppressLint("UseSwitchCompatOrMaterialCode")
	private Switch enableFx;
	private SeekBar bassBoost, reverb;
	private Spinner presetSelector;
	private View presetLabel;
	private EqualizerAdapter eqAdapter;
	private PresetAdapter presetAdapter;
	private AudioEffects audioEffects;
	private PresetLoader presetLoader;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audiofx);
		Toolbar toolbar = findViewById(R.id.audiofx_toolbar);
		RecyclerView eq_bands = findViewById(R.id.audiofx_eq_scroll);
		View root = findViewById(R.id.audiofx_root);
		presetLabel = findViewById(R.id.audiofx_preset_label);
		presetSelector = findViewById(R.id.audiofx_preset);
		enableFx = findViewById(R.id.audiofx_enable);
		bassBoost = findViewById(R.id.audiofx_bass_boost);
		reverb = findViewById(R.id.audiofx_reverb);

		int sessionId = getIntent().getIntExtra(KEY_SESSION_ID, 0);
		presetAdapter = new PresetAdapter();
		presetLoader = new PresetLoader(this);
		audioEffects = AudioEffects.getInstance(this, sessionId);
		ThemeUtils mResources = new ThemeUtils(this);
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
		mResources.setBackground(root);

		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
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
			if (sessionId != 0)
				Toast.makeText(this, R.string.error_audioeffects_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		PresetLoader.Param param = new PresetLoader.Param(PresetLoader.Param.LOAD, null);
		presetLoader.execute(param, this);

		enableFx.setOnCheckedChangeListener(this);
		bassBoost.setOnSeekBarChangeListener(this);
		reverb.setOnSeekBarChangeListener(this);
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
		} else if (item.getItemId() == R.id.menu_delete_preset) {
			Object selectedItem = presetSelector.getSelectedItem();
			if (selectedItem instanceof AudioPreset) {
				PresetLoader.Param param = new PresetLoader.Param(PresetLoader.Param.DEL, (AudioPreset) selectedItem);
				presetLoader.execute(param, this);
			}
		} else if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return true;
	}


	@Override
	public void onBandLevelChange(int pos, int level) {
		audioEffects.setBandLevel(pos, level);
		setCustomPreset();
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
		if (seekBar.getId() == R.id.audiofx_bass_boost || seekBar.getId() == R.id.audiofx_reverb) {
			setCustomPreset();
		}
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
			audioEffects.setPreset(preset);
			if (preset != null) {
				setViews();
			}
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}


	@Override
	public void onPresetSave(AudioPreset preset) {
		PresetLoader.Param param = new PresetLoader.Param(PresetLoader.Param.SAVE, preset);
		presetLoader.execute(param, this);
		audioEffects.setPreset(preset);
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

	/**
	 * set visibity of the views
	 *
	 * @param enable true to make views visible
	 */
	private void setVisibility(boolean enable) {
		// enable views only if effect is enabled
		reverb.setEnabled(enable);
		bassBoost.setEnabled(enable);
		eqAdapter.setEnabled(enable);
		presetLabel.setEnabled(enable);
		presetSelector.setEnabled(enable);
	}

	/**
	 * set selector to custom after any change
	 */
	private void setCustomPreset() {
		if (presetSelector.getCount() > 0 && presetSelector.getSelectedItemPosition() > 0) {
			presetSelector.setSelection(0, false);
		}
	}

	/**
	 * select current selected preset
	 */
	private void setPreset() {
		String presetName = audioEffects.getPreset().getName();
		int index = 0;
		for (int i = 0; i < presetAdapter.getCount(); i++) {
			AudioPreset preset = presetAdapter.getItem(i);
			if (preset != null && preset.getName().equals(presetName)) {
				index = i;
				break;
			}
		}
		presetSelector.setSelection(index, false);
		presetSelector.setOnItemSelectedListener(this);
	}
}