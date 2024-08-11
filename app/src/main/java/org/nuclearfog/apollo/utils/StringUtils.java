package org.nuclearfog.apollo.utils;

import android.content.Context;
import android.text.TextUtils;

import org.nuclearfog.apollo.R;

/**
 * This class contains utils for strings
 *
 * @author nuclearfog
 */
public final class StringUtils {

	/* This class is never initiated */
	private StringUtils() {
	}

	/**
	 * Capitalizes the first character in a string
	 *
	 * @param str The string to capitalize
	 * @return A captitalized string
	 */
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
	 * @param context  The {@link Context} to use.
	 * @param duration The track in milliseconds.
	 * @return Duration of a track that's properly formatted.
	 */
	public static String makeTimeString(Context context, long duration) {
		if (duration < 0) {
			// invalid time
			return "--:--";
		}
		long sec = duration / 1000;
		long min = sec / 60;
		long hour = min / 60;
		if (hour > 0)
			return String.format(context.getString(R.string.durationformatlong), hour, min % 60, sec % 60);
		return String.format(context.getString(R.string.durationformatshort), min % 60, sec % 60);
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

	/**
	 * Used to make number of labels for the number of artists, albums, songs,
	 * genres, and playlists.
	 *
	 * @param context   The {@link Context} to use.
	 * @param pluralInt The ID of the plural string to use.
	 * @param number    The number of artists, albums, songs, genres, or playlists.
	 * @return A {@link String} used as a label for the number of artists,
	 * albums, songs, genres, and playlists.
	 */
	public static String makeLabel(Context context, int pluralInt, int number) {
		return context.getResources().getQuantityString(pluralInt, number, number);
	}
}