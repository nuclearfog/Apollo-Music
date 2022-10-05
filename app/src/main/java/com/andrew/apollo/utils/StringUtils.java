package com.andrew.apollo.utils;

import android.content.Context;
import android.text.TextUtils;

import com.andrew.apollo.R;

/**
 * This class contains utils for strings
 */
public class StringUtils {

	/* This class is never initiated */
	private StringUtils() {
	}

	public static String capitalize(String str) {
		return capitalize(str, null);
	}

	/**
	 * Capitalizes the first character in a string
	 *
	 * @param str        The string to capitalize
	 * @param delimiters The delimiters
	 * @return A captitalized string
	 */
	public static String capitalize(String str, char[] delimiters) {
		int delimLen = delimiters == null ? -1 : delimiters.length;
		if (TextUtils.isEmpty(str) || delimLen == 0) {
			return str;
		}
		char[] buffer = str.toCharArray();
		boolean capitalizeNext = true;
		for (int i = 0; i < buffer.length; i++) {
			char ch = buffer[i];
			if (isDelimiter(ch, delimiters)) {
				capitalizeNext = true;
			} else if (capitalizeNext) {
				buffer[i] = Character.toTitleCase(ch);
				capitalizeNext = false;
			}
		}
		return new String(buffer);
	}

	/**
	 * Used to create a formatted time string for the duration of tracks.
	 *
	 * @param context The {@link Context} to use.
	 * @param secs    The track in seconds.
	 * @return Duration of a track that's properly formatted.
	 */
	public static String makeTimeString(Context context, int secs) {
		if (secs < 0) {
			// invalid time
			return "--:--";
		}
		if (secs == 0) {
			// no need to calculate
			return "0:00";
		}
		int min = secs / 60;
		int hour = min / 60;
		String durationFormat = context.getString(hour == 0 ? R.string.durationformatshort : R.string.durationformatlong);
		return String.format(durationFormat, hour, min % 60, secs % 60);
	}

	/**
	 * Is the character a delimiter.
	 *
	 * @param ch         the character to check
	 * @param delimiters the delimiters
	 * @return true if it is a delimiter
	 */
	private static boolean isDelimiter(char ch, char[] delimiters) {
		if (delimiters == null) {
			return Character.isWhitespace(ch);
		}
		for (char delimiter : delimiters) {
			if (ch == delimiter) {
				return true;
			}
		}
		return false;
	}
}