package com.andrew.apollo.ui.activities;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

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
public class AudioFxActivity extends AppCompatActivity implements EqualizerListener, OnCheckedChangeListener {

	private AudioEffects audioEffects;

	@Override
	protected void onCreate(Bundle inst) {
		super.onCreate(inst);
		setContentView(R.layout.activity_audiofx);

		CompoundButton enableFx = findViewById(R.id.audiofx_enable);
		RecyclerView eq_bands = findViewById(R.id.audiofx_eq_scroll);
		Toolbar toolbar = findViewById(R.id.audiofx_toolbar);

		eq_bands.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
		toolbar.setTitle(R.string.title_audio_effects);

		audioEffects = new AudioEffects(this, MusicUtils.getAudioSessionId());

		eq_bands.setAdapter(new EqualizerAdapter(this, audioEffects.getBands()));
		enableFx.setChecked(audioEffects.isAudioFxEnabled());

		enableFx.setOnCheckedChangeListener(this);
	}

	@Override
	public void onLevelChange(int pos, int level) {
		audioEffects.setBand(pos, level);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() == R.id.audiofx_enable) {
			audioEffects.enableAudioFx(isChecked);
		}
	}
}