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

package com.andrew.apollo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
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
import androidx.fragment.app.FragmentActivity;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.player.AudioEffects;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.PopularStore;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.ui.dialogs.DeleteDialog;
import com.andrew.apollo.ui.dialogs.PlaylistCreateDialog;
import com.devspark.appmsg.AppMsg;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {

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
	private static WeakHashMap<Context, ServiceBinder> mConnectionMap = new WeakHashMap<>(32);

	/**
	 * weak reference to the service to avoid memory leaks
	 */
	@Nullable
	private static volatile IApolloService mService;

	/**
	 *
	 */
	private static ContentValues[] mContentValuesCache;

	/**
	 * random initialization for shuffle
	 */
	private static Random random = new Random();

	private static int sForegroundActivities = 0;

	private static int markedTracks = 0;


	/* This class is never initiated */
	private MusicUtils() {
	}

	/**
	 * check if service is connected
	 */
	public static boolean isConnected() {
		return mService != null;
	}

	/**
	 * @param activity The {@link Activity} to use
	 * @param callback The {@link ServiceConnection} to use
	 * @return The new instance of {@link ServiceToken}
	 */
	public static ServiceToken bindToService(Activity activity, @Nullable ServiceConnection callback) {
		if (activity.getParent() != null)
			activity = activity.getParent();
		ContextWrapper contextWrapper = new ContextWrapper(activity);
		contextWrapper.startService(new Intent(contextWrapper, MusicPlaybackService.class));
		ServiceBinder binder = new ServiceBinder(callback);
		if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicPlaybackService.class), binder, 0)) {
			mConnectionMap.put(contextWrapper, binder);
			return new ServiceToken(contextWrapper);
		}
		return null;
	}

	/**
	 * @param token The {@link ServiceToken} to unbind from
	 */
	public static void unbindFromService(ServiceToken token) {
		if (token == null) {
			return;
		}
		ContextWrapper mContextWrapper = token.mWrappedContext;
		ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
		if (mBinder == null) {
			return;
		}
		mContextWrapper.unbindService(mBinder);
		if (mConnectionMap.isEmpty()) {
			Log.v("Utils", "All connections closed, cleaning Service");
			// destroying instance
			mService = null;
		}
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

	/**
	 * switch to next track
	 */
	public static void next(Context context) {
		IApolloService service = mService;
		if (service != null) {
			AudioEffects.getInstance(context, getAudioSessionId());
			try {
				service.goToNext();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * switch to previous track or repeat current track
	 */
	public static void previous(Context context) {
		IApolloService service = mService;
		if (service != null) {
			AudioEffects.getInstance(context, getAudioSessionId());
			try {
				service.goToPrev();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * plays the music.
	 */
	public static void play(Context context) {
		IApolloService service = mService;
		if (service != null) {
			AudioEffects.getInstance(context, getAudioSessionId());
			try {
				service.play();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * pauses the music.
	 */
	public static void pause() {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.pause();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Cycles through the repeat options.
	 */
	public static void cycleRepeat(Context context) {
		IApolloService service = mService;
		if (service != null) {
			AudioEffects.getInstance(context, getAudioSessionId());
			try {
				switch (service.getRepeatMode()) {
					case MusicPlaybackService.REPEAT_NONE:
						service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						break;

					case MusicPlaybackService.REPEAT_ALL:
						service.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
						if (service.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
							service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						}
						break;

					default:
						service.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
						break;
				}
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Cycles through the shuffle options.
	 */
	public static void cycleShuffle(Context context) {
		IApolloService service = mService;
		if (service != null) {
			AudioEffects.getInstance(context, getAudioSessionId());
			try {
				switch (service.getShuffleMode()) {
					case MusicPlaybackService.SHUFFLE_NONE:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
						if (service.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
							service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						}
						break;
					case MusicPlaybackService.SHUFFLE_NORMAL:
					case MusicPlaybackService.SHUFFLE_AUTO:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						break;
					default:
						break;
				}
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @return True if we're playing music, false otherwise.
	 */
	public static boolean isPlaying() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.isPlaying();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * @return The current shuffle mode.
	 */
	public static int getShuffleMode() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getShuffleMode();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * @return The current repeat mode.
	 */
	public static int getRepeatMode() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getRepeatMode();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * @return The current track name.
	 */
	public static String getTrackName() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getTrackName();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @return The current artist name.
	 */
	public static String getArtistName() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getArtistName();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @return The current album name.
	 */
	public static String getAlbumName() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getAlbumName();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @return The current album Id.
	 */
	public static long getCurrentAlbumId() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getAlbumId();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * @return The current song Id.
	 */
	public static long getCurrentAudioId() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getAudioId();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * @return The current artist Id.
	 */
	public static long getCurrentArtistId() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getArtistId();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * @return The audio session Id.
	 */
	public static int getAudioSessionId() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getAudioSessionId();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * @return The queue.
	 */
	@NonNull
	public static long[] getQueue() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getQueue();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return EMPTY_LIST;
	}

	/**
	 * @param id The ID of the track to remove.
	 * @return removes track from a playlist or the queue.
	 */
	public static int removeTrack(long id) {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.removeTrack(id);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * remove track from the current playlist
	 *
	 * @param pos index of the track
	 */
	public static void removeQueueItem(int pos) {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.removeTracks(pos, pos);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @return The position of the current track in the queue.
	 */
	public static int getQueuePosition() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getQueuePosition();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * @param position The position to move the queue to
	 */
	public static void setQueuePosition(int position) {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.setQueuePosition(position);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * remove current tracks from service
	 *
	 * @param which track ID
	 * @return true if track was removed
	 */
	public static boolean removeTracks(int which) {
		IApolloService service = mService;
		try {
			if (service != null && service.removeTracks(which, which) > 0) {
				return true;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
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
				do
				{
					result[index++] = cursor.getLong(0);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * Plays songs by an artist.
	 *
	 * @param context  The {@link Context} to use.
	 * @param artistId The artist Id.
	 * @param position Specify where to start.
	 */
	public static void playArtist(Context context, long artistId, int position) {
		long[] artistList = getSongListForArtist(context, artistId);
		if (artistList.length > position) {
			playAll(artistList, position, false);
		}
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
	public static void playFile(Uri uri) {
		IApolloService service = mService;
		if (uri != null && service != null) {
			try {
				service.openFile(uri);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @param list         The list of songs to play.
	 * @param position     Specify where to start.
	 * @param forceShuffle True to force a shuffle, false otherwise.
	 */
	public static void playAll(long[] list, int position, boolean forceShuffle) {
		IApolloService service = mService;
		if (list.length > 0 && service != null) {
			try {
				if (forceShuffle) {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
				} else {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
				}
				long currentId = service.getAudioId();
				int currentQueuePosition = getQueuePosition();
				if (position != -1 && currentQueuePosition == position && currentId == list[position]) {
					long[] playlist = getQueue();
					if (Arrays.equals(list, playlist)) {
						service.play();
						return;
					}
				}
				if (position < 0) {
					position = 0;
				}
				service.open(list, forceShuffle ? 0 : position);
				service.play();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @param list The list to enqueue.
	 */
	public static void playNext(long[] list) {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.enqueue(list, MusicPlaybackService.NEXT);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @param context The {@link Context} to use.
	 */
	public static void shuffleAll(Context context) {
		Cursor cursor = CursorFactory.makeTrackCursor(context);
		IApolloService service = mService;
		if (service != null && cursor != null) {
			cursor.moveToFirst();
			long[] mTrackList = new long[cursor.getCount()];
			for (int i = 0; i < mTrackList.length; i++) {
				mTrackList[i] = cursor.getLong(0);
				cursor.moveToNext();
			}
			cursor.close();
			if (mTrackList.length == 0) {
				return;
			}
			try {
				service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
				long mCurrentId = service.getAudioId();
				int mCurrentQueuePosition = getQueuePosition();
				if (mCurrentQueuePosition == 0 && mCurrentId == mTrackList[0]) {
					long[] mPlaylist = getQueue();
					if (Arrays.equals(mTrackList, mPlaylist)) {
						service.play();
						return;
					}
				}
				int pos = random.nextInt(mTrackList.length - 1);
				service.open(mTrackList, pos);
				service.play();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
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
		Cursor cursor = CursorFactory.makePlaylistCursor(context);
		long playlistId = -1;
		if (cursor != null) {
			if (cursor.moveToFirst() && name != null) {
				do
				{
					String playlist = cursor.getString(1);
					if (name.equals(playlist)) {
						playlistId = cursor.getLong(0);
						break;
					}
				} while (cursor.moveToNext());
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
		long id = -1;
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
				do
				{
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
		int id = -1;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				id = cursor.getInt(0);
			}
			cursor.close();
		}
		return id;
	}

	/**
	 * Plays songs from an album.
	 *
	 * @param context  The {@link Context} to use.
	 * @param albumId  The album Id.
	 * @param position Specify where to start.
	 */
	public static void playAlbum(Context context, long albumId, int position) {
		long[] albumList = getSongListForAlbum(context, albumId);
		if (albumList.length > 0) {
			playAll(albumList, position, false);
		}
	}

	/**
	 *
	 */
	public static void makeInsertItems(long[] ids, int offset, int len, int base) {
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

	/**
	 * @param context The {@link Context} to use.
	 * @param name    The name of the new playlist.
	 * @return A new playlist ID.
	 */
	public static long createPlaylist(Context context, String name) {
		if (name != null && name.length() > 0) {
			Cursor cursor = CursorFactory.makePlaylistCursor(context, name);
			// check if playlist exists
			if (cursor != null) {
				// create only playlist if there isn't any conflict
				if (!cursor.moveToFirst()) {
					ContentResolver resolver = context.getContentResolver();
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
		return -1;
	}

	/**
	 * @param context    The {@link Context} to use.
	 * @param playlistId The playlist ID.
	 */
	@SuppressLint("InlinedApi")
	public static void clearPlaylist(Context context, long playlistId) {
		Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId);
		context.getContentResolver().delete(uri, null, null);
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
			} catch (SecurityException err) {
				// thrown when the app does not own the playlist
				String message = activity.getString(R.string.error_add_playlist);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				err.printStackTrace();
			}
			cursor.close();
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
		IApolloService service = mService;
		if (service != null) {
			try {
				service.enqueue(list, MusicPlaybackService.LAST);
				String message = makeLabel(activity, R.plurals.NNNtrackstoqueue, list.length);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			} catch (RemoteException err) {
				err.printStackTrace();
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
			err.printStackTrace();
			return;
		}
		// print message if succeded
		Cursor cursor = CursorFactory.makeTrackCursor(activity, id);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				// get title of the current track
				String title = cursor.getString(cursor.getColumnIndexOrThrow(AudioColumns.TITLE));
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
	 * @return The song count for an album.
	 */
	public static String getSongCountForAlbum(Context context, long id) {
		String count = "";
		if (id >= 0) {
			Cursor cursor = CursorFactory.makeAlbumCursor(context, id);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					count = cursor.getString(3);
				}
				cursor.close();
			}
		}
		return count;
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
	public static void moveQueueItem(int from, int to) {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.moveQueueItem(from, to);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Toggles the current song as a favorite.
	 */
	public static void toggleFavorite() {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.toggleFavorite();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @return True if the current song is a favorite, false otherwise.
	 */
	public static boolean isFavorite() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.isFavorite();

			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return false;
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
	 * Plays a user created playlist.
	 *
	 * @param context    The {@link Context} to use.
	 * @param playlistId The playlist Id.
	 */
	public static void playPlaylist(Context context, long playlistId) {
		long[] playlistList = getSongListForPlaylist(context, playlistId);
		playAll(playlistList, -1, false);
	}

	/**
	 * @param context The {@link Context} to use
	 * @return The song list from our favorites database
	 */
	@NonNull
	public static long[] getSongListForFavorites(Context context) {
		Cursor cursor = CursorFactory.makeFavoritesCursor(context);
		long[] ids = EMPTY_LIST;
		if (cursor != null) {
			ids = new long[cursor.getCount()];
			if (cursor.moveToFirst()) {
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
	 * Play the songs that have been marked as favorites.
	 *
	 * @param context The {@link Context} to use
	 */
	public static void playFavorites(Context context) {
		playAll(getSongListForFavorites(context), 0, false);
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
		Cursor cursor = CursorFactory.makePopularCursor(context);
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
	 * Plays the last added songs from the past two weeks.
	 *
	 * @param context The {@link Context} to use
	 */
	public static void playLastAdded(Context context) {
		playAll(getSongListForLastAdded(context), 0, false);
	}

	/**
	 * Plays popular tracks starting with the most listened tracks
	 */
	public static void playPopular(Context context) {
		playAll(getPopularSongList(context), 0, false);
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
				do
				{
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
	public static void refresh() {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.refresh();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Queries {@link RecentStore} for the last album played by an artist
	 *
	 * @param context    The {@link Context} to use
	 * @param artistName The artist name
	 * @return The last album name played by an artist
	 */
	public static String getLastAlbumForArtist(Context context, String artistName) {
		return RecentStore.getInstance(context).getAlbumName(artistName);
	}

	/**
	 * Seeks the current track to a desired position
	 *
	 * @param position The position to seek to
	 */
	public static void seek(long position) {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.seek(position);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * @return The current position time of the track
	 */
	public static long position() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.position();
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * @return The total duration of the current track
	 */
	public static long duration() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.duration();
			} catch (RemoteException err) {
				err.printStackTrace();
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
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(new QueueWorker(activity));
	}

	/**
	 * Clears the qeueue
	 */
	public static void clearQueue() {
		IApolloService service = mService;
		if (service != null) {
			try {
				service.removeTracks(0, Integer.MAX_VALUE);
			} catch (RemoteException err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Used to build and show a notification when Apollo is sent into the
	 * background
	 *
	 * @param context The {@link Context} to use.
	 */
	public static void notifyForegroundStateChanged(Context context, boolean inForeground) {
		int old = sForegroundActivities;
		if (inForeground) {
			sForegroundActivities++;
		} else {
			sForegroundActivities--;
		}

		if (old == 0 || sForegroundActivities == 0) {
			Intent intent = new Intent(context, MusicPlaybackService.class);
			intent.setAction(MusicPlaybackService.FOREGROUND_STATE_CHANGED);
			intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, sForegroundActivities != 0);
			context.startService(intent);
		}
	}

	/**
	 * get the file path of the audio playback
	 *
	 * @return path to the music file
	 */
	public static String getPlaybackFilePath() {
		IApolloService service = mService;
		if (service != null) {
			try {
				return service.getPath();
			} catch (RemoteException err) {
				err.printStackTrace();
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
		String[] paths = removeTracksFromDatabase(activity.getApplicationContext(), list);

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
				err.printStackTrace();
			}
		}
		// remove tracks directly from storage
		else {
			for (String filename : paths) {
				try {
					File file = new File(filename);
					// File.delete can throw a security exception
					if (!file.delete()) {
						Log.e("MusicUtils", "Failed to delete file " + filename);
					}
				} catch (Exception ex) {
					// catch exception if file was not found
					ex.printStackTrace();
				}
			}
			onPostDelete(activity);
		}
	}

	/**
	 *
	 */
	public static void playAllFromUserItemClick(ArrayAdapter<Song> adapter, int position) {
		if (position < adapter.getViewTypeCount() - 1) {
			// invalid position
			return;
		}
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
		playAll(list, position, false);
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
		String message = makeLabel(activity, R.plurals.NNNtracksdeleted, markedTracks);
		AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
		// We deleted a number of tracks, which could affect any number of
		// things in the media content domain, so update everything.
		activity.getContentResolver().notifyChange(Uri.parse("content://media"), null);
		// Notify the lists to update
		refresh();
	}

	/**
	 * remove tracks from media database
	 *
	 * @param context application context
	 * @param ids     list of track IDs
	 * @return path to removed entries
	 */
	private static String[] removeTracksFromDatabase(Context context, long[] ids) {
		String[] result = {};
		// get cursor to fetch track information
		Cursor cursor = CursorFactory.makeTrackListCursor(context, ids);

		// Step 1: Remove selected tracks from the current playlist, as well
		// as from the album art cache
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result = new String[cursor.getCount()];
				FavoritesStore favStore = FavoritesStore.getInstance(context);
				RecentStore recents = RecentStore.getInstance(context);
				PopularStore popular = PopularStore.getInstance(context);
				ContentResolver resolver = context.getContentResolver();
				for (int i = 0; i < ids.length; i++) {
					// Remove from current playlist
					long trackId = cursor.getLong(0);
					result[i] = cursor.getString(1);
					long albumId = cursor.getLong(2);
					String[] idStr = {Long.toString(trackId)};
					//
					removeTrack(trackId);
					// Remove from the favorites playlist
					favStore.removeItem(trackId);
					// Remove any items in the recents database
					recents.removeItem(albumId);
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
		// return path to the files
		return result;
	}

	/**
	 *
	 */
	public static final class ServiceToken {
		public ContextWrapper mWrappedContext;

		/**
		 * Constructor of <code>ServiceToken</code>
		 *
		 * @param context The {@link ContextWrapper} to use
		 */
		public ServiceToken(ContextWrapper context) {
			mWrappedContext = context;
		}
	}

	/**
	 *
	 */
	private static final class ServiceBinder implements ServiceConnection {

		/**
		 * callback called when the service is connected/disconnected
		 */
		private final ServiceConnection mCallback;

		/**
		 * Constructor of <code>ServiceBinder</code>
		 *
		 * @param callback The {@link ServiceConnection} to use
		 */
		public ServiceBinder(@Nullable ServiceConnection callback) {
			mCallback = callback;
		}

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = IApolloService.Stub.asInterface(service);
			if (mCallback != null) {
				mCallback.onServiceConnected(className, service);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			if (mCallback != null) {
				mCallback.onServiceDisconnected(className);
			}
			mService = null;
		}
	}

	/**
	 * background worker to create a dialog for saving the current queue to a playlist
	 */
	private static class QueueWorker implements Runnable {

		private WeakReference<FragmentActivity> activity;


		QueueWorker(FragmentActivity activity) {
			this.activity = new WeakReference<>(activity);
		}


		@Override
		public void run() {
			Activity activity = this.activity.get();
			if (activity != null) {
				// fetch all track IDs of the qurrent queue
				NowPlayingCursor queue = new NowPlayingCursor(activity.getApplicationContext());
				queue.moveToFirst();
				final long[] ids = new long[queue.getCount()];
				for (int i = 0; i < ids.length && !queue.isAfterLast(); i++) {
					ids[i] = queue.getLong(0);
					queue.moveToNext();
				}
				queue.close();

				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						FragmentActivity activity = QueueWorker.this.activity.get();
						if (activity != null) {
							PlaylistCreateDialog.getInstance(ids).show(activity.getSupportFragmentManager(), PlaylistCreateDialog.NAME);
						}
					}
				});
			}
		}
	}
}