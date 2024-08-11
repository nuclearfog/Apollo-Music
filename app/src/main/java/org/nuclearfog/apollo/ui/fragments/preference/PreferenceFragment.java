package org.nuclearfog.apollo.ui.fragments.preference;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.ThemesActivity;
import org.nuclearfog.apollo.ui.dialogs.CacheClearDialog;
import org.nuclearfog.apollo.ui.dialogs.ColorSchemeDialog;
import org.nuclearfog.apollo.ui.dialogs.LicenseDialog;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;

/**
 * Preference fragment class
 *
 * @see org.nuclearfog.apollo.ui.activities.SettingsActivity
 * @author nuclearfog
 */
public class PreferenceFragment extends PreferenceFragmentCompat implements OnPreferenceClickListener {

	private static final String DEL_CACHE = "delete_cache";
	private static final String THEME_SEL = "theme_chooser";
	private static final String COLOR_SEL = "color_scheme";
	private static final String VERSION = "version";
	private static final String BAT_OPT = "disable_battery_opt";
	private static final String DOWNLOAD_IMAGES = "download_missing_artist_images";
	private static final String DOWNLOAD_ARTWORK = "download_missing_artwork";
	private static final String DOWNLOAD_WIFI = "only_on_wifi";
	private static final String SOURCECODE = "source_code";
	private static final String LICENSE = "licenses";


	@Nullable
	private CheckBoxPreference downloadImages;
	@Nullable
	private CheckBoxPreference downloadArtwork;
	@Nullable
	private CheckBoxPreference downloadWifi;


	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		addPreferencesFromResource(R.xml.settings);
		Preference mOpenSourceLicenses = findPreference(LICENSE);
		Preference deleteCache = findPreference(DEL_CACHE);
		Preference themeChooser = findPreference(THEME_SEL);
		Preference colorScheme = findPreference(COLOR_SEL);
		Preference batteryOpt = findPreference(BAT_OPT);
		Preference sourceCode = findPreference(SOURCECODE);
		downloadImages = findPreference(DOWNLOAD_IMAGES);
		downloadArtwork = findPreference(DOWNLOAD_ARTWORK);
		downloadWifi = findPreference(DOWNLOAD_WIFI);
		Preference version = findPreference(VERSION);

		if (version != null)
			version.setSummary(BuildConfig.VERSION_NAME);
		if (mOpenSourceLicenses != null)
			mOpenSourceLicenses.setOnPreferenceClickListener(this);
		if (deleteCache != null)
			deleteCache.setOnPreferenceClickListener(this);
		if (themeChooser != null)
			themeChooser.setOnPreferenceClickListener(this);
		if (colorScheme != null)
			colorScheme.setOnPreferenceClickListener(this);
		if (sourceCode != null)
			sourceCode.setOnPreferenceClickListener(this);
		if (downloadImages != null && downloadArtwork != null) {
			downloadImages.setOnPreferenceClickListener(this);
			downloadArtwork.setOnPreferenceClickListener(this);
			if (downloadWifi != null) {
				downloadWifi.setEnabled(downloadImages.isChecked() || downloadArtwork.isChecked());
			}
		}
		if (batteryOpt != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				batteryOpt.setVisible(false);
			} else {
				batteryOpt.setOnPreferenceClickListener(this);
			}
		}
	}


	@Override
	public boolean onPreferenceClick(@NonNull Preference preference) {
		switch (preference.getKey()) {
			case LICENSE:
				LicenseDialog.show(requireActivity());
				return true;

			case DEL_CACHE:
				CacheClearDialog.show(requireActivity());
				return true;

			case COLOR_SEL:
				ColorSchemeDialog.show(requireActivity());
				return true;

			case THEME_SEL:
				startActivity(new Intent(requireContext(), ThemesActivity.class));
				return true;

			case BAT_OPT:
				ApolloUtils.redirectToBatteryOptimization(requireActivity());
				return true;

			case DOWNLOAD_IMAGES:
			case DOWNLOAD_ARTWORK:
				if (downloadWifi != null && downloadArtwork != null && downloadImages != null) {
					downloadWifi.setEnabled(downloadArtwork.isChecked() || downloadImages.isChecked());
				}
				break;

			case SOURCECODE:
				String url = Constants.SOURCE_URL;
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					// ignore
				}
				break;
		}
		return false;
	}
}