package com.andrew.apollo.ui.activities;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.andrew.apollo.R;

public class AudioFxActivity extends AppCompatActivity {

	private ViewGroup eq_bands;

	@Override
	protected void onCreate(Bundle inst) {
		super.onCreate(inst);
		setContentView(R.layout.activity_audiofx);

		eq_bands = findViewById(R.id.eq_bands);

	}
}