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
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.player.AudioEffects;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.DeleteTracksDialog;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;

import java.util.ArrayList;
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

	private static int foregroundActivities = 0;


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
			if (isPlaying(activity)) {
				Intent intent = new Intent(activity, MusicPlaybackService.class);
				intent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, true);
				ContextCompat.startForegroundService(activity, intent);
			}
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
	 *
	 * @return true if succeed, false if queue is empty or if an error occured
	 */
	public static boolean togglePlayPause(Activity activity) {
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
					return false;
				}
				return true;
			} catch (Exception err) {
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		}
		return false;
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
			AppMsg.makeText(activity, R.string.error_create_playlist, AppMsg.STYLE_CONFIRM).show();
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
		try {
			Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistid);
			Cursor cursor = CursorFactory.makePlaylistCursor(activity.getContentResolver(), uri);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int base = cursor.getInt(0);
					int numinserted = 0;
					for (int offset = 0; offset < ids.length; offset += 1000) {
						int len = ids.length;
						if (offset + len > ids.length) {
							len = ids.length - offset;
						}
						ContentValues[] mContentValuesCache = new ContentValues[len];
						for (int i = 0; i < len; i++) {
							mContentValuesCache[i] = new ContentValues();
							mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
							mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
						}
						numinserted += activity.getContentResolver().bulkInsert(uri, mContentValuesCache);
					}
					String message = activity.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
					AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				}
				cursor.close();
			}
		} catch (Exception exception) {
			// thrown when the app does not own the playlist
			AppMsg.makeText(activity, R.string.error_add_playlist, AppMsg.STYLE_CONFIRM).show();
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
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
			AppMsg.makeText(activity, R.string.error_rename_playlist, AppMsg.STYLE_CONFIRM).show();
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
				AppMsg.makeText(activity, R.plurals.NNNtrackstoqueue, list.length, AppMsg.STYLE_CONFIRM).show();
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
		if (showFavorites)
			subMenu.add(groupId, ContextMenuItems.ADD_TO_FAVORITES, Menu.NONE, R.string.add_to_favorites);
		subMenu.add(groupId, ContextMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
		try {
			Cursor cursor = CursorFactory.makePlaylistCursor(context);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						long id = cursor.getLong(0);
						String name = cursor.getString(1);
						if (name != null) {
							Intent intent = new Intent();
							intent.putExtra(Constants.PLAYLIST_ID, id);
							subMenu.add(groupId, ContextMenuItems.PLAYLIST_SELECTED, Menu.NONE, name).setIntent(intent);
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		} catch (Exception exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
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
	 * open delete dialog for tracks
	 *
	 * @param activity activity
	 * @param title    title of the dialog
	 * @param ids      list of IDs to remove
	 */
	public static void openDeleteDialog(FragmentActivity activity, String title, long[] ids) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Use system Dialog to delete media files
			try {
				List<Uri> uris = new ArrayList<>(ids.length);
				for (long id : ids)
					uris.add(Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id));
				PendingIntent requestRemove = MediaStore.createDeleteRequest(activity.getContentResolver(), uris);
				activity.startIntentSenderForResult(requestRemove.getIntentSender(), REQUEST_DELETE_FILES, null, 0, 0, 0);
			} catch (Exception err) {
				// thrown when no audio file were found
				if (BuildConfig.DEBUG) {
					err.printStackTrace();
				}
			}
		} else {
			DeleteTracksDialog dialog = DeleteTracksDialog.newInstance(title, ids);
			dialog.show(activity.getSupportFragmentManager(), DeleteTracksDialog.NAME);
		}
	}

	/**
	 * create an array of track ids from a song list
	 */
	public static long[] getIDsFromSongList(List<Song> songs) {
		long[] ids = new long[songs.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = songs.get(i).getId();
		}
		return ids;
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
}