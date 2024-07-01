/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.IApolloService;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Folder;
import org.nuclearfog.apollo.model.Genre;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.player.AudioEffects;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.store.ExcludeStore;
import org.nuclearfog.apollo.store.ExcludeStore.Type;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.DeleteDialog;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public final class MusicUtils {

	/**
	 * repeat mode disabled
	 */
	public static final int REPEAT_NONE = 0;

	/**
	 * repeat playlist
	 */
	public static final int REPEAT_ALL = 1;

	/**
	 * repeat current track
	 */
	public static final int REPEAT_CURRENT = 2;

	/**
	 * shuffle mode disabled
	 */
	public static final int SHUFFLE_NONE = 10;

	/**
	 * shuffle playlist
	 */
	public static final int SHUFFLE_NORMAL = 11;

	/**
	 * shuffle all songs
	 */
	public static final int SHUFFLE_AUTO = 12;

	/**
	 * selection to remove track from playlist
	 */
	private static final String PLAYLIST_REMOVE_TRACK = Playlists.Members.AUDIO_ID + "=?";

	/**
	 * selection to remove track from database
	 */
	private static final String DATABASE_REMOVE_TRACK = AudioColumns._ID + "=?";

	/**
	 * code to request file deleting
	 * only for scoped storage
	 */
	public static final int REQUEST_DELETE_FILES = 0x8DA3;

	/**
	 * emty ID list
	 */
	private static final long[] EMPTY_LIST = {};

	/**
	 * information about activities accessing this service interface
	 */
	private static WeakHashMap<Activity, ServiceBinder> mConnectionMap = new WeakHashMap<>(32);

	/**
	 *
	 */
	private static ContentValues[] mContentValuesCache;

	private static int foregroundActivities = 0;
	private static int markedTracks = 0;


	/* This class is never initiated */
	private MusicUtils() {
	}

	/**
	 * @param activity The {@link Activity} to use
	 * @param callback The {@link ServiceBinderCallback} to use
	 */
	public static void bindToService(Activity activity, @Nullable ServiceBinderCallback callback) {
		if (activity.getParent() != null)
			activity = activity.getParent();
		ContextWrapper contextWrapper = new ContextWrapper(activity.getBaseContext());
		Intent intent = new Intent(activity, MusicPlaybackService.class);
		intent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, true);
		ContextCompat.startForegroundService(activity, intent);
		ServiceBinder binder = new ServiceBinder(callback);
		if (contextWrapper.bindService(intent, binder, 0)) {
			mConnectionMap.put(activity, binder);
		}
	}

	/**
	 * unregister connection to service
	 *
	 * @param activity activity to unregister
	 */
	public static void unbindFromService(Activity activity) {
		ServiceBinder mBinder = mConnectionMap.remove(activity);
		if (mBinder != null) {
			activity.unbindService(mBinder);
		}
	}

	/**
	 * Used to build and show a notification when Apollo is sent into the
	 * background
	 */
	public static void notifyForegroundStateChanged(Activity activity, boolean inForeground) {
		if (inForeground) {
			foregroundActivities++;
		} else {
			foregroundActivities--;
		}
		// start foreground service if application is in background
		if (foregroundActivities == 0) {
			Intent intent = new Intent(activity, MusicPlaybackService.class);
			intent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, true);
			ContextCompat.startForegroundService(activity, intent);
		}
		// stop foreground activity of the playback service
		else {
			ServiceBinder binder = mConnectionMap.get(activity);
			if (binder != null) {
				binder.stopForeground();
			}
		}
	}

	/**
	 * check if current activity is connected with the playback service
	 *
	 * @param activity Activity to check
	 * @return true if service is connected to activity
	 */
	public static boolean isConnected(Activity activity) {
		return getService(activity) != null;
	}


	/**
	 * switch to next track
	 */
	public static void next(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.gotoNext();
				int sessionId = service.getAudioSessionId();
				AudioEffects.getInstance(activity, sessionId);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * switch to previous track or repeat current track
	 */
	public static void previous(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.gotoPrev();
				int sessionId = service.getAudioSessionId();
				AudioEffects.getInstance(activity, sessionId);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * toggle playstate
	 */
	public static void togglePlayPause(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				if (service.isPlaying()) {
					service.pause(false);
				} else if (service.getQueue().length > 0) {
					service.play();
					int sessionId = service.getAudioSessionId();
					AudioEffects.getInstance(activity, sessionId);
				} else {
					shuffleAll(activity);
				}
			} catch (Exception err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * Cycles through the repeat options.
	 *
	 * @return repeat mode {@link #REPEAT_ALL,#REPEAT_CURRENT,#REPEAT_NONE}
	 */
	public static int cycleRepeat(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getRepeatMode()) {
					case MusicPlaybackService.REPEAT_NONE:
						service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						return REPEAT_ALL;

					case MusicPlaybackService.REPEAT_ALL:
						service.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
						if (service.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE)
							service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						return REPEAT_CURRENT;

					case MusicPlaybackService.REPEAT_CURRENT:
						service.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
						return REPEAT_NONE;
				}
				int sessionId = service.getAudioSessionId();
				AudioEffects.getInstance(activity, sessionId);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return REPEAT_NONE;
	}

	/**
	 * Cycles through the shuffle options.
	 */
	public static int cycleShuffle(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getShuffleMode()) {
					case MusicPlaybackService.SHUFFLE_NONE:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
						if (service.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
							service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						}
						int sessionId = service.getAudioSessionId();
						AudioEffects.getInstance(activity, sessionId);
						return SHUFFLE_NORMAL;

					case MusicPlaybackService.SHUFFLE_NORMAL:
					case MusicPlaybackService.SHUFFLE_AUTO:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						return SHUFFLE_NONE;
				}
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return SHUFFLE_NONE;
	}

	/**
	 * @return True if we're playing music, false otherwise.
	 */
	public static boolean isPlaying(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.isPlaying();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 * get current shuffle mode
	 *
	 * @return The current shuffle mode {@link #SHUFFLE_NONE,#SHUFFLE_NORMAL,#SHUFFLE_AUTO}
	 */
	public static int getShuffleMode(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getShuffleMode()) {
					case MusicPlaybackService.SHUFFLE_AUTO:
						return SHUFFLE_AUTO;

					case MusicPlaybackService.SHUFFLE_NORMAL:
						return SHUFFLE_NORMAL;

					case MusicPlaybackService.SHUFFLE_NONE:
						return SHUFFLE_NONE;
				}
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return SHUFFLE_NONE;
	}

	/**
	 * @return The current repeat mode.
	 */
	public static int getRepeatMode(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getRepeatMode()) {
					case MusicPlaybackService.REPEAT_ALL:
						return REPEAT_ALL;

					case MusicPlaybackService.REPEAT_NONE:
						return REPEAT_NONE;

					case MusicPlaybackService.REPEAT_CURRENT:
						return REPEAT_CURRENT;
				}
				return service.getRepeatMode();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return REPEAT_NONE;
	}

	/**
	 * @return The current track name.
	 */
	@Nullable
	public static Song getCurrentTrack(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getCurrentTrack();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * @return The current album name.
	 */
	@Nullable
	public static Album getCurrentAlbum(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getCurrentAlbum();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * @return The audio session ID or 0 if not initialized or if an error occured
	 */
	public static int getAudioSessionId(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getAudioSessionId();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return 0;
	}

	/**
	 * @return The queue.
	 */
	@NonNull
	public static long[] getQueue(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getQueue();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return EMPTY_LIST;
	}

	/**
	 * remove track from the current playlist
	 *
	 * @param pos index of the track
	 */
	public static void removeQueueItem(Activity activity, int pos) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.removeTrack(pos);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @return The position of the current track in the queue.
	 */
	public static int getQueuePosition(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getQueuePosition();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return 0;
	}

	/**
	 * @param position The position to move the queue to
	 */
	public static void setQueuePosition(Activity activity, int position) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setQueuePosition(position);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param context The {@link Context} to use.
	 * @param id      The ID of the artist.
	 * @return The song list for an artist.
	 */
	@NonNull
	public static long[] getSongListForArtist(Context context, long id) {
		Cursor cursor = CursorFactory.makeArtistSongCursor(context, id);
		long[] mList = EMPTY_LIST;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				mList = new long[cursor.getCount()];
				for (int i = 0; i < mList.length; i++) {
					mList[i] = cursor.getLong(0);
					cursor.moveToNext();
				}
			}
			cursor.close();
		}
		return mList;
	}

	/**
	 * @param context The {@link Context} to use.
	 * @param id      The ID of the album.
	 * @return The song list for an album.
	 */
	@NonNull
	public static long[] getSongListForAlbum(Context context, long id) {
		Cursor cursor = CursorFactory.makeAlbumSongCursor(context, id);
		long[] result = EMPTY_LIST;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				int index = 0;
				result = new long[cursor.getCount()];
				do {
					result[index++] = cursor.getLong(0);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * @param context The {@link Context} to use.
	 * @param id      The ID of the genre.
	 * @return The song list for an genre.
	 */
	@NonNull
	public static long[] getSongListForGenre(Context context, long id) {
		Cursor cursor = CursorFactory.makeGenreSongCursor(context, id);
		long[] ids = EMPTY_LIST;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				ids = new long[cursor.getCount()];
				for (int i = 0; i < ids.length; i++) {
					ids[i] = cursor.getLong(0);
					cursor.moveToNext();
				}
			}
			cursor.close();
		}
		return ids;
	}

	/**
	 * get list of songs from multiple genre IDs
	 *
	 * @param context The {@link Context} to use.
	 * @param ids     list of genre IDs
	 * @return song IDs from genres
	 */
	@NonNull
	public static long[] getSongListForGenres(Context context, long[] ids) {
		int size = 0;
		long[][] data = new long[ids.length][];
		for (int i = 0; i < ids.length; i++) {
			data[i] = getSongListForGenre(context, ids[i]);
			size += data[i].length;
		}
		int index = 0;
		long[] result = new long[size];
		for (long[] array : data) {
			for (long element : array) {
				result[index++] = element;
			}
		}
		return result;
	}

	/**
	 * @param uri The source of the file
	 */
	public static void playFile(Activity activity, Uri uri) {
		IApolloService service = getService(activity);
		if (uri != null && service != null) {
			try {
				int sessionId = service.getAudioSessionId();
				AudioEffects.getInstance(activity, sessionId);
				service.openFile(uri);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param list         The list of songs to play.
	 * @param position     Specify where to start.
	 * @param forceShuffle True to force a shuffle, false otherwise.
	 */
	public static void playAll(Activity activity, long[] list, int position, boolean forceShuffle) {
		IApolloService service = getService(activity);
		if (list.length > 0 && service != null) {
			try {
				if (forceShuffle) {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_AUTO);
					service.open(list, -1);
				} else {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
					service.open(list, position);
				}
				int sessionId = service.getAudioSessionId();
				AudioEffects.getInstance(activity, sessionId);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 *
	 */
	public static void playAllFromUserItemClick(Activity activity, ArrayAdapter<Song> adapter, int position) {
		if (position >= adapter.getViewTypeCount() - 1) {
			// if view type count is greater than 1, a header exists at first position
			// calculate position offset
			int off = (adapter.getViewTypeCount() - 1);
			// length of the arrayadapter
			int len = adapter.getCount();
			// calculate real position
			position -= off;
			// copy all IDs to an array
			long[] list = new long[len - off];
			for (int i = 0; i < list.length; i++) {
				list[i] = adapter.getItemId(i + off);
			}
			// play whole ID list
			playAll(activity, list, position, false);
		}
	}

	/**
	 * shuffle all available songs
	 */
	public static void shuffleAll(Activity activity) {
		Cursor cursor = CursorFactory.makeTrackCursor(activity.getApplicationContext());
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				long[] mTrackList = new long[cursor.getCount()];
				for (int i = 0; i < mTrackList.length; i++) {
					mTrackList[i] = cursor.getLong(0);
					cursor.moveToNext();
				}
				if (mTrackList.length > 0) {
					playAll(activity, mTrackList, -1, true);
				}
			}
			cursor.close();
		}
	}

	/**
	 * Returns The ID for a playlist.
	 *
	 * @param context The {@link Context} to use.
	 * @param name    The name of the playlist.
	 * @return The ID for a playlist.
	 */
	public static long getIdForPlaylist(Context context, String name) {
		Cursor cursor = CursorFactory.makePlaylistCursor(context, name);
		long playlistId = -1L;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				playlistId = cursor.getLong(0);
			}
			cursor.close();
		}
		return playlistId;
	}

	/**
	 * Returns the Id for an artist.
	 *
	 * @param context The {@link Context} to use.
	 * @param name    The name of the artist.
	 * @return The ID for an artist.
	 */
	public static long getIdForArtist(Context context, String name) {
		Cursor cursor = CursorFactory.makeArtistCursor(context, name);
		long id = -1L;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				id = cursor.getLong(0);
			}
			cursor.close();
		}
		return id;
	}

	/**
	 * get all songs in a folder
	 *
	 * @param context The {@link Context} to use.
	 * @param folder  folder containing songs
	 * @return array of track IDs
	 */
	@NonNull
	public static long[] getSongListForFolder(Context context, String folder) {
		Cursor cursor = CursorFactory.makeFolderSongCursor(context, folder);
		long[] result = EMPTY_LIST;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				// use dynamic array because the result size differs from cursor size
				List<Long> ids = new LinkedList<>();
				int idxName = folder.length() + 1;
				do {
					String filename = cursor.getString(5);
					// filter sub folders from results
					if (filename.indexOf('/', idxName) < 0) {
						ids.add(cursor.getLong(0));
					}
				} while (cursor.moveToNext());
				// convert to array
				result = new long[ids.size()];
				for (int pos = 0; pos < ids.size(); pos++) {
					result[pos] = ids.get(pos);
				}
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * Returns the ID for an album.
	 *
	 * @param context    The {@link Context} to use.
	 * @param albumName  The name of the album.
	 * @param artistName The name of the artist
	 * @return The ID for an album.
	 */
	public static long getIdForAlbum(Context context, String albumName, String artistName) {
		Cursor cursor = CursorFactory.makeAlbumCursor(context, albumName, artistName);
		long id = -1L;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				id = cursor.getLong(0);
			}
			cursor.close();
		}
		return id;
	}

	/**
	 * @param name The name of the new playlist.
	 * @return A new playlist ID.
	 */
	public static long createPlaylist(Activity activity, String name) {
		try {
			if (name != null && !name.trim().isEmpty()) {
				// check if playlist already exists
				if (getIdForPlaylist(activity, name) != -1)
					return -1;
				Cursor cursor = CursorFactory.makePlaylistCursor(activity, name);
				// check if playlist exists
				if (cursor != null) {
					// create only playlist if there isn't any conflict
					if (!cursor.moveToFirst()) {
						ContentResolver resolver = activity.getContentResolver();
						ContentValues values = new ContentValues(1);
						values.put(Playlists.NAME, name);
						Uri uri = resolver.insert(Playlists.EXTERNAL_CONTENT_URI, values);
						if (uri != null && uri.getLastPathSegment() != null) {
							return Long.parseLong(uri.getLastPathSegment());
						}
					}
					cursor.close();
				}
			}
		} catch (Exception exception) {
			// thrown when the app does not own the playlist
			String message = activity.getString(R.string.error_create_playlist);
			AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return -1L;
	}

	/**
	 * @param activity   The {@link Context} to use.
	 * @param ids        The id of the song(s) to add.
	 * @param playlistid The id of the playlist being added to.
	 */
	@SuppressLint("InlinedApi")
	public static void addToPlaylist(Activity activity, long[] ids, long playlistid) {
		int size = ids.length;
		ContentResolver resolver = activity.getContentResolver();
		Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistid);
		Cursor cursor = CursorFactory.makePlaylistCursor(resolver, uri);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					int base = cursor.getInt(0);
					int numinserted = 0;
					for (int offSet = 0; offSet < size; offSet += 1000) {
						makeInsertItems(ids, offSet, 1000, base);
						numinserted += resolver.bulkInsert(uri, mContentValuesCache);
					}
					String message = activity.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
					AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				}
			} catch (Exception exception) {
				// thrown when the app does not own the playlist
				String message = activity.getString(R.string.error_add_playlist);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				if (BuildConfig.DEBUG) {
					exception.printStackTrace();
				}
			}
			cursor.close();
		}
	}

	/**
	 * rename existing playlist
	 *
	 * @param id   ID of the playlist to rename
	 * @param name new playlist name
	 */
	public static void renamePlaylist(Activity activity, long id, String name) {
		try {
			// seting new name
			ContentValues values = new ContentValues(1);
			values.put(Playlists.NAME, StringUtils.capitalize(name));
			// update old playlist
			Uri uri = ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, id);
			ContentResolver resolver = activity.getContentResolver();
			resolver.update(uri, values, null, null);
		} catch (Exception exception) {
			// thrown when the app does not own the playlist
			String message = activity.getString(R.string.error_rename_playlist);
			AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * move a track of a playlist to a new position
	 *
	 * @param playlistId ID of the playlist
	 * @param from       location of the track
	 * @param to         new location of the track
	 * @param off        the offset of the positions, or '0'
	 * @return true if success
	 */
	public static boolean movePlaylistTrack(Context context, long playlistId, int from, int to, int off) {
		ContentResolver resolver = context.getContentResolver();
		return Playlists.Members.moveItem(resolver, playlistId, from - off, to - off);
	}

	/**
	 * Removes a single track from a given playlist
	 *
	 * @param trackId    The id of the song to remove.
	 * @param playlistId The id of the playlist being removed from.
	 */
	@SuppressLint("InlinedApi")
	public static boolean removeFromPlaylist(Activity activity, long trackId, long playlistId) {
		String[] args = {Long.toString(trackId)};
		Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId);
		ContentResolver resolver = activity.getContentResolver();
		int count = resolver.delete(uri, PLAYLIST_REMOVE_TRACK, args);
		if (count > 0) {
			String message = activity.getResources().getQuantityString(R.plurals.NNNtracksfromplaylist, count, count);
			AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			return true;
		}
		return false;
	}

	/**
	 * @param list The list to enqueue.
	 */
	public static void addToQueue(Activity activity, long[] list) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.enqueue(list, MusicPlaybackService.MOVE_LAST);
				String message = StringUtils.makeLabel(activity, R.plurals.NNNtrackstoqueue, list.length);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param id The song ID.
	 */
	public static void setRingtone(Activity activity, long id) {
		ContentResolver resolver = activity.getContentResolver();
		Uri uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id);
		// Set ringtone
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				// check if app can set ringtone
				if (Settings.System.canWrite(activity)) {
					// set ringtone
					RingtoneManager.setActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE, uri);
				} else {
					// explain why we need permission to write settings
					Toast.makeText(activity, R.string.explain_permission_write_settings, Toast.LENGTH_LONG).show();
					// open settings so user can set write permissions
					Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
					intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
					activity.startActivity(intent);
					return;
				}
			} else {
				// set ringtone directly
				ContentValues values = new ContentValues(1);
				values.put(AudioColumns.IS_RINGTONE, true);
				resolver.update(uri, values, null, null);
				Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
			}
		} catch (Exception err) {
			if (BuildConfig.DEBUG) {
				err.printStackTrace();
			}
			return;
		}
		// print message if succeded
		Cursor cursor = CursorFactory.makeTrackCursor(activity, id);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				// get title of the current track
				String title = cursor.getString(1);
				// truncate title
				if (title.length() > 20)
					title = title.substring(0, 20) + "...";
				String message = activity.getString(R.string.set_as_ringtone, title);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			}
			cursor.close();
		}
	}

	/**
	 * @param context The {@link Context} to use.
	 * @param id      The id of the album.
	 * @return The release date for an album.
	 */
	public static String getReleaseDateForAlbum(Context context, long id) {
		String releaseDate = "";
		if (id >= 0) {
			Cursor cursor = CursorFactory.makeAlbumCursor(context, id);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					releaseDate = cursor.getString(4);
				}
				cursor.close();
			}
		}
		return releaseDate;
	}

	/**
	 * @param from The index the item is currently at.
	 * @param to   The index the item is moving to.
	 */
	public static void moveQueueItem(Activity activity, int from, int to) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.moveQueueItem(from, to);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @return True if the current song is a favorite, false otherwise.
	 */
	public static boolean isFavorite(Song song, Context context) {
		FavoritesStore mFavoritesCache = FavoritesStore.getInstance(context.getApplicationContext());
		return mFavoritesCache.exists(song.getId());
	}

	/**
	 * @param context    The {@link Context} to sue
	 * @param playlistId The playlist Id
	 * @return The track list for a playlist
	 */
	@NonNull
	public static long[] getSongListForPlaylist(Context context, long playlistId) {
		Cursor cursor = CursorFactory.makePlaylistSongCursor(context, playlistId);
		long[] ids = EMPTY_LIST;
		if (cursor != null) {
			cursor.moveToFirst();
			ids = new long[cursor.getCount()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = cursor.getLong(0);
				cursor.moveToNext();
			}
			cursor.close();
		}
		return ids;
	}

	/**
	 * @param context The {@link Context} to use
	 * @return The song list from our favorites database
	 */
	@NonNull
	public static long[] getSongListForFavorites(Context context) {
		List<Song> favorits = FavoritesStore.getInstance(context).getFavorites();
		long[] ids = new long[favorits.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = favorits.get(i).getId();
		}
		return ids;
	}

	/**
	 * @param context The {@link Context} to use
	 * @return The song list for the last added playlist
	 */
	@NonNull
	public static long[] getSongListForLastAdded(Context context) {
		Cursor cursor = CursorFactory.makeLastAddedCursor(context);
		long[] list = EMPTY_LIST;
		if (cursor != null) {
			list = new long[cursor.getCount()];
			for (int i = 0; i < list.length; i++) {
				cursor.moveToNext();
				list[i] = cursor.getLong(0);
			}
			cursor.close();
		}
		return list;
	}

	/**
	 * create an ID list of popular tracks
	 *
	 * @param context The {@link Context} to use
	 * @return The song list for the last added playlist
	 */
	@NonNull
	public static long[] getPopularSongList(Context context) {
		List<Song> songs = PopularStore.getInstance(context).getSongs();
		long[] ids = new long[songs.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = songs.get(i).getId();
		}
		return ids;
	}

	/**
	 * @param list The list to enqueue.
	 */
	public static void playNext(Activity activity, long[] list) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.enqueue(list, MusicPlaybackService.MOVE_NEXT);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * Plays songs by an artist.
	 *
	 * @param artistId The artist Id.
	 * @param position Specify where to start.
	 */
	public static void playArtist(Activity activity, long artistId, int position) {
		long[] artistList = getSongListForArtist(activity, artistId);
		if (artistList.length > position) {
			playAll(activity, artistList, position, false);
		}
	}

	/**
	 * Plays songs from an album.
	 *
	 * @param albumId  The album Id.
	 * @param position Specify where to start.
	 */
	public static void playAlbum(Activity activity, long albumId, int position) {
		long[] albumList = getSongListForAlbum(activity, albumId);
		if (albumList.length > 0) {
			playAll(activity, albumList, position, false);
		}
	}

	/**
	 * Plays a user created playlist.
	 *
	 * @param playlistId The playlist Id.
	 */
	public static void playPlaylist(Activity activity, long playlistId) {
		playAll(activity, getSongListForPlaylist(activity, playlistId), 0, false);
	}

	/**
	 * Plays the last added songs from the past two weeks.
	 */
	public static void playLastAdded(Activity activity) {
		playAll(activity, getSongListForLastAdded(activity), 0, false);
	}

	/**
	 * Plays popular tracks starting with the most listened tracks
	 */
	public static void playPopular(Activity activity) {
		playAll(activity, getPopularSongList(activity), 0, false);
	}

	/**
	 * Play the songs that have been marked as favorites.
	 */
	public static void playFavorites(Activity activity) {
		playAll(activity, getSongListForFavorites(activity), 0, false);
	}

	/**
	 * Creates a sub menu used to add items to a new playlist or an existsing
	 * one.
	 *
	 * @param context       The {@link Context} to use.
	 * @param groupId       The group Id of the menu.
	 * @param subMenu       The {@link SubMenu} to add to.
	 * @param showFavorites True if we should show the option to add to the
	 *                      Favorites cache.
	 */
	public static void makePlaylistMenu(Context context, int groupId, SubMenu subMenu, boolean showFavorites) {
		subMenu.clear();
		if (showFavorites) {
			subMenu.add(groupId, ContextMenuItems.ADD_TO_FAVORITES, Menu.NONE, R.string.add_to_favorites);
		}
		subMenu.add(groupId, ContextMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
		Cursor cursor = CursorFactory.makePlaylistCursor(context);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					String name = cursor.getString(1);
					if (name != null) {
						Intent intent = new Intent();
						intent.putExtra("playlist", getIdForPlaylist(context, name));
						subMenu.add(groupId, ContextMenuItems.PLAYLIST_SELECTED, Menu.NONE, name).setIntent(intent);
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
	}

	/**
	 * Called when one of the lists should refresh or requery.
	 */
	public static void refresh(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.refresh();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * Seeks the current track to a desired position
	 *
	 * @param position The position to seek to
	 */
	public static void seek(Activity activity, long position) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setPlayerPosition(position);
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * @return The current position time of the track
	 */
	public static long getPositionMillis(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getPlayerPosition();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return 0;
	}

	/**
	 * @return The total duration of the current track
	 */
	public static long getDurationMillis(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				Song song = service.getCurrentTrack();
				if (song != null)
					return song.getDuration();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return 0;
	}

	/**
	 * create dialog to save current queue to playlist
	 *
	 * @param activity activity of the fragment
	 */
	public static void saveQueue(FragmentActivity activity) {
		long[] ids = MusicUtils.getQueue(activity);
		PlaylistDialog.show(activity.getSupportFragmentManager(), PlaylistDialog.CREATE, 0, ids, "");
	}

	/**
	 * Clears the qeueue
	 */
	public static void clearQueue(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.clearQueue();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * get the file path of the audio playback
	 *
	 * @return path to the music file
	 */
	public static String getPlaybackFilePath(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				Song song = service.getCurrentTrack();
				if (song != null)
					return song.getPath();
			} catch (RemoteException err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Perminately deletes item(s) from the user's device
	 *
	 * @param activity Activity used to access scoped storage. on old android version
	 *                 otherwise its a context
	 * @param list     The item(s) to delete.
	 */
	public static void deleteTracks(Activity activity, long[] list) {
		markedTracks = list.length;
		ContentResolver resolver = activity.getContentResolver();
		String[] paths = removeTracksFromDatabase(activity, list);

		// Use Scoped storage and build in dialog
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			try {
				List<Uri> uris = new LinkedList<>();
				for (long id : list) {
					uris.add(Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id));
				}
				PendingIntent requestRemove = MediaStore.createDeleteRequest(resolver, uris);
				activity.startIntentSenderForResult(requestRemove.getIntentSender(), REQUEST_DELETE_FILES, null, 0, 0, 0);
			} catch (Exception err) {
				// thrown when no audio file were found
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		// remove tracks directly from storage
		else {
			for (String filename : paths) {
				try {
					File file = new File(filename);
					// File.delete can throw a security exception
					if (!file.delete()) {
						if (BuildConfig.DEBUG) {
							Log.e("MusicUtils", "Failed to delete file " + filename);
						}
					}
				} catch (Exception ex) {
					// catch exception if file was not found
					if (BuildConfig.DEBUG) {
						ex.printStackTrace();
					}
				}
			}
			onPostDelete(activity);
		}
	}

	/**
	 * open delete dialog for tracks
	 *
	 * @param activity activity
	 * @param title    title of the dialog
	 * @param ids      list of IDs to remove
	 */
	public static void openDeleteDialog(FragmentActivity activity, String title, long[] ids) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Use system Dialog to delete media files
			deleteTracks(activity, ids);
		} else {
			DeleteDialog dialog = DeleteDialog.newInstance(title, ids, null);
			dialog.show(activity.getSupportFragmentManager(), DeleteDialog.NAME);
		}
	}

	/**
	 * Action to take after tracks are removed
	 *
	 * @param activity activity context
	 */
	public static void onPostDelete(Activity activity) {
		String message = StringUtils.makeLabel(activity, R.plurals.NNNtracksdeleted, markedTracks);
		AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
		// We deleted a number of tracks, which could affect any number of
		// things in the media content domain, so update everything.
		activity.getContentResolver().notifyChange(Uri.parse("content://media"), null);
		// Notify the lists to update
		refresh(activity);
	}

	/**
	 * remove tracks from media database
	 *
	 * @param ids list of track IDs
	 * @return path to removed entries
	 */
	private static String[] removeTracksFromDatabase(Activity activity, long[] ids) {
		String[] result = {};
		// get cursor to fetch track information
		Cursor cursor = CursorFactory.makeTrackListCursor(activity, ids);
		IApolloService service = getService(activity);
		// Step 1: Remove selected tracks from the current playlist, as well
		// as from the album art cache
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result = new String[cursor.getCount()];
				FavoritesStore favStore = FavoritesStore.getInstance(activity);
				RecentStore recents = RecentStore.getInstance(activity);
				PopularStore popular = PopularStore.getInstance(activity);
				ContentResolver resolver = activity.getContentResolver();
				for (int i = 0; i < ids.length; i++) {
					// Remove from current playlist
					long trackId = cursor.getLong(0);
					result[i] = cursor.getString(1);
					long albumId = cursor.getLong(2);
					String[] idStr = {Long.toString(trackId)};
					// Remove from the favorites playlist
					favStore.removeFavorite(trackId);
					// Remove any items in the recents database
					recents.removeAlbum(albumId);
					// remove track from most played list
					popular.removeItem(trackId);
					// remove track from database
					resolver.delete(Media.EXTERNAL_CONTENT_URI, DATABASE_REMOVE_TRACK, idStr);
					// move to next track
					cursor.moveToNext();
				}
			}
			cursor.close();
		}
		// remove tracks from queue
		if (service != null) {
			try {
				service.removeTracks(ids);
			} catch (RemoteException exception) {
				if (BuildConfig.DEBUG) {
					exception.printStackTrace();
				}
			}
		}
		// return path to the files
		return result;
	}

	/**
	 *
	 */
	public static void excludeAlbum(Context context, Album album) {
		ExcludeStore exclude = ExcludeStore.getInstance(context);
		if (album.isVisible()) {
			exclude.addIds(Type.ALBUM, album.getId());
		} else {
			exclude.removeIds(Type.ALBUM, album.getId());
		}
	}

	/**
	 *
	 */
	public static void excludeSong(Context context, Song song) {
		ExcludeStore exclude = ExcludeStore.getInstance(context);
		if (song.isVisible()) {
			exclude.addIds(Type.SONG, song.getId());
		} else {
			exclude.removeIds(Type.SONG, song.getId());
		}
	}


	/**
	 *
	 */
	public static void excludeArtist(Context context, Artist artist) {
		ExcludeStore exclude = ExcludeStore.getInstance(context);
		if (artist.isVisible()) {
			exclude.addIds(Type.ARTIST, artist.getId());
		} else {
			exclude.removeIds(Type.ARTIST, artist.getId());
		}
	}

	/**
	 *
	 */
	public static void excludeGenre(Context context, Genre genre) {
		ExcludeStore exclude = ExcludeStore.getInstance(context);
		if (genre.isVisible()) {
			exclude.addIds(Type.GENRE, genre.getGenreIds());
		} else {
			exclude.removeIds(Type.GENRE, genre.getGenreIds());
		}
	}

	/**
	 *
	 */
	public static void excludeFolder(Context context, Folder folder) {
		ExcludeStore exclude = ExcludeStore.getInstance(context);
		long[] songs = MusicUtils.getSongListForFolder(context, folder.getPath());
		if (folder.isVisible()) {
			exclude.addIds(Type.SONG, songs);
		} else {
			exclude.removeIds(Type.SONG, songs);
		}
	}

	/**
	 * get service connected with a specific activity
	 */
	@Nullable
	private static IApolloService getService(@Nullable Activity activity) {
		if (activity != null) {
			ServiceBinder binder = mConnectionMap.get(activity);
			if (binder != null) {
				return binder.getService();
			}
		}
		return null;
	}

	/**
	 *
	 */
	private static void makeInsertItems(long[] ids, int offset, int len, int base) {
		if (offset + len > ids.length) {
			len = ids.length - offset;
		}
		if (mContentValuesCache == null || mContentValuesCache.length != len) {
			mContentValuesCache = new ContentValues[len];
		}
		for (int i = 0; i < len; i++) {
			if (mContentValuesCache[i] == null) {
				mContentValuesCache[i] = new ContentValues();
			}
			mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
			mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
		}
	}
}