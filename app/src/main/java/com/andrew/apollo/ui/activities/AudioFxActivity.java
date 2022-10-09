package com.andrew.apollo.ui.activities;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.EqualizerAdapter;
import com.andrew.apollo.adapters.EqualizerAdapter.EqualizerListener;
import com.andrew.apollo.player.AudioEffects;
import com.andrew.apollo.utils.MusicUtils;

/**
 * Audio effects activity
 *
 * @author nuclerfog
 */
public class AudioFxActivity extends AppCompatActivity implements EqualizerListener, OnCheckedChangeListener, OnSeekBarChangeListener {

	private AudioEffects audioEffects;

	@Override
	protected void onCreate(Bundle inst) {
		super.onCreate(inst);
		setContentView(R.layout.activity_audiofx);

		CompoundButton enableFx = findViewById(R.id.audiofx_enable);
		RecyclerView eq_bands = findViewById(R.id.audiofx_eq_scroll);
		Toolbar toolbar = findViewById(R.id.audiofx_toolbar);
		SeekBar bassBoost = findViewById(R.id.audiofx_bass_boost);

		eq_bands.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
		toolbar.setTitle(R.string.title_audio_effects);

		audioEffects = new AudioEffects(this, MusicUtils.getAudioSessionId());

		eq_bands.setAdapter(new EqualizerAdapter(this, audioEffects.getBandLevel()));
		enableFx.setChecked(audioEffects.isAudioFxEnabled());
		bassBoost.setProgress(audioEffects.getBassLevel());

		enableFx.setOnCheckedChangeListener(this);
		bassBoost.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onLevelChange(int pos, int level) {
		audioEffects.setBandLevel(pos, level);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() == R.id.audiofx_enable) {
			audioEffects.enableAudioFx(isChecked);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (seekBar.getId() == R.id.audiofx_bass_boost) {
			audioEffects.setBassLevel(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}