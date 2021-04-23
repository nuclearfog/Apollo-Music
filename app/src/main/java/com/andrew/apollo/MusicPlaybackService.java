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

package com.andrew.apollo;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RemoteControlClient;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import com.andrew.apollo.appwidgets.AppWidgetBase;
import com.andrew.apollo.appwidgets.AppWidgetLarge;
import com.andrew.apollo.appwidgets.AppWidgetLargeAlternate;
import com.andrew.apollo.appwidgets.AppWidgetSmall;
import com.andrew.apollo.appwidgets.RecentWidgetProvider;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * A background {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
public class MusicPlaybackService extends Service implements OnAudioFocusChangeListener {

    /**
     * For backwards compatibility reasons, also provide sticky
     * broadcasts under the music package
     */
    public static final String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    /**
     * Indicates that the music has paused or resumed
     */
    public static final String PLAYSTATE_CHANGED = APOLLO_PACKAGE_NAME + ".playstatechanged";
    /**
     * Indicates that music playback position within
     * a title was changed
     */
    public static final String POSITION_CHANGED = APOLLO_PACKAGE_NAME + ".positionchanged";
    /**
     * Indicates the meta data has changed in some way, like a track change
     */
    public static final String META_CHANGED = APOLLO_PACKAGE_NAME + ".metachanged";
    /**
     * Indicates the queue has been updated
     */
    public static final String QUEUE_CHANGED = APOLLO_PACKAGE_NAME + ".queuechanged";
    /**
     * Indicates the repeat mode chaned
     */
    public static final String REPEATMODE_CHANGED = APOLLO_PACKAGE_NAME + ".repeatmodechanged";
    /**
     * Indicates the shuffle mode chaned
     */
    public static final String SHUFFLEMODE_CHANGED = APOLLO_PACKAGE_NAME + ".shufflemodechanged";

    public static final String MUSIC_PACKAGE_NAME = "com.android.music";
    /**
     * Called to indicate a general service commmand. Used in
     * {@link MediaButtonIntentReceiver}
     */
    public static final String SERVICECMD = APOLLO_PACKAGE_NAME + ".musicservicecommand";
    /**
     * Called to go toggle between pausing and playing the music
     */
    public static final String TOGGLEPAUSE_ACTION = APOLLO_PACKAGE_NAME + ".togglepause";
    /**
     * Called to go to pause the playback
     */
    public static final String PAUSE_ACTION = APOLLO_PACKAGE_NAME + ".pause";
    /**
     * Called to go to stop the playback
     */
    public static final String STOP_ACTION = APOLLO_PACKAGE_NAME + ".stop";
    /**
     * Called to go to the previous track
     */
    public static final String PREVIOUS_ACTION = APOLLO_PACKAGE_NAME + ".previous";
    /**
     * Called to go to the next track
     */
    public static final String NEXT_ACTION = APOLLO_PACKAGE_NAME + ".next";
    /**
     * Called to change the repeat mode
     */
    public static final String REPEAT_ACTION = APOLLO_PACKAGE_NAME + ".repeat";
    /**
     * Called to change the shuffle mode
     */
    public static final String SHUFFLE_ACTION = APOLLO_PACKAGE_NAME + ".shuffle";
    /**
     * Called to update the service about the foreground state of Apollo's activities
     */
    public static final String FOREGROUND_STATE_CHANGED = APOLLO_PACKAGE_NAME + ".fgstatechanged";
    public static final String NOW_IN_FOREGROUND = "nowinforeground";
    public static final String FROM_MEDIA_BUTTON = "frommediabutton";
    /**
     * Used to easily notify a list that it should refresh. i.e. A playlist
     * changes
     */
    public static final String REFRESH = APOLLO_PACKAGE_NAME + ".refresh";

    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPLAY = "play";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
    /**
     * Moves a list to the front of the queue
     */
    public static final int NOW = 1;
    /**
     * Moves a list to the next position in the queue
     */
    public static final int NEXT = 2;
    /**
     * Moves a list to the last position in the queue
     */
    public static final int LAST = 3;

    /**
     * Shuffles no songs, turns shuffling off
     */
    public static final int SHUFFLE_NONE = 0;
    /**
     * Shuffles all songs
     */
    public static final int SHUFFLE_NORMAL = 1;
    /**
     * Party shuffle
     */
    public static final int SHUFFLE_AUTO = 2;
    /**
     * Turns repeat off
     */
    public static final int REPEAT_NONE = 0;
    /**
     * Repeats the current track in a list
     */
    public static final int REPEAT_CURRENT = 1;
    /**
     * Repeats all the tracks in a list
     */
    public static final int REPEAT_ALL = 2;


    private static final String TAG = "MusicPlaybackService";
    /**
     * Used by the alarm intent to shutdown the service after being idle
     */
    private static final String SHUTDOWN = APOLLO_PACKAGE_NAME + ".shutdown";
    private static final int IDCOLIDX = 0;
    /**
     * Indicates when the track ends
     */
    private static final int TRACK_ENDED = 1;

    /**
     * Indicates that the current track was changed the next track
     */
    private static final int TRACK_WENT_TO_NEXT = 2;

    /**
     * Indicates the player died
     */
    private static final int SERVER_DIED = 4;

    /**
     * Indicates some sort of focus change, maybe a phone call
     */
    private static final int FOCUSCHANGE = 5;

    /**
     * Indicates to fade the volume down
     */
    private static final int FADEDOWN = 6;

    /**
     * Indicates to fade the volume back up
     */
    private static final int FADEUP = 7;

    /**
     * Idle time before stopping the foreground notfication (1 minute)
     */
    private static final int IDLE_DELAY = 60000;

    /**
     * Song play time used as threshold for rewinding to the beginning of the
     * track instead of skipping to the previous track when getting the PREVIOUS
     * command
     */
    private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;

    /**
     * The max size allowed for the track history
     */
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * The columns used to retrieve any info from the current track
     */
    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };

    /**
     * The columns used to retrieve any info from the current album
     */
    private static final String[] ALBUM_PROJECTION = new String[]{
            MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR
    };

    /**
     * Used to shuffle the tracks
     */
    private static Shuffler mShuffler = new Shuffler();

    /**
     * Keeps a mapping of the track history
     */
    private static List<Integer> mHistory = new LinkedList<>();

    /**
     * current playlist containing track ID's
     */
    private List<Long> mPlayList = new LinkedList<>();

    /**
     * current shuffle list contaning track ID's
     */
    private List<Long> mAutoShuffleList = new LinkedList<>();

    /**
     * Service stub
     */
    private IBinder mBinder = new ServiceStub(this);


    private AppWidgetBase[] widgets = {
            AppWidgetSmall.getInstance(),
            AppWidgetLarge.getInstance(),
            AppWidgetLargeAlternate.getInstance(),
            RecentWidgetProvider.getInstance()
    };

    /**
     * Broadcast receiver for widget actions
     */
    private BroadcastReceiver mIntentReceiver = new WidgetBroadcastReceiver();

    /**
     * broadcast listener for unmounting external storage
     */
    private BroadcastReceiver mUnmountReceiver = null;

    /**
     * The media player
     */
    private MultiPlayer mPlayer;

    /**
     * The path of the current file to play
     */
    private String mFileToPlay;

    /**
     * Alarm intent for removing the notification when nothing is playing
     * for some time
     */
    private AlarmManager mAlarmManager;
    private PendingIntent mShutdownIntent;
    private boolean mShutdownScheduled;

    /**
     * The cursor used to retrieve info on the current track and run the
     * necessary queries to play audio files
     */
    private Cursor mCursor;

    /**
     * The cursor used to retrieve info on the album the current track is
     * part of, if any.
     */
    private Cursor mAlbumCursor;

    /**
     * Monitors the audio state
     */
    private AudioManager mAudioManager;

    /**
     * Used to know when the service is active
     */
    private boolean mServiceInUse = false;

    /**
     * Used to know if something should be playing or not
     */
    private boolean mIsSupposedToBePlaying = false;

    /**
     * Used to indicate if the queue can be saved
     */
    private boolean mQueueIsSaveable = true;

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private boolean mPausedByTransientLossOfFocus = false;

    /**
     * Lock screen controls
     */
    private RemoteControlClient mRemoteControlClient;

    private ComponentName mMediaButtonReceiverComponent;

    private PreferenceUtils settings;

    // We use this to distinguish between different cards when saving/restoring
    // playlists
    private int mCardId;

    private int mPlayPos = -1;

    private int mNextPlayPos = -1;

    private int mMediaMountedCount = 0;

    private int mShuffleMode = SHUFFLE_NONE;

    private int mRepeatMode = REPEAT_ALL;

    private int mServiceStartId = -1;

    private MusicPlayerHandler mPlayerHandler;

    /**
     * Image cache
     */
    private ImageFetcher mImageFetcher;
    /**
     * Used to build the notification
     */
    private NotificationHelper mNotificationHelper;
    /**
     * Recently listened database
     */
    private RecentStore mRecentsCache;
    /**
     * Favorites database
     */
    private FavoritesStore mFavoritesCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
        return mBinder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;
        saveQueue(true);

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        } else if (!mPlayList.isEmpty() || mPlayerHandler.hasMessages(TRACK_ENDED)) {
            scheduleDelayedShutdown();
            return true;
        }
        stopSelf(mServiceStartId);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRebind(Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the favorites and recents databases
        mRecentsCache = RecentStore.getInstance(this);
        mFavoritesCache = FavoritesStore.getInstance(this);

        // Initialize the notification helper
        mNotificationHelper = new NotificationHelper(this);

        // Initialize the image fetcher
        mImageFetcher = ImageFetcher.getInstance(this);
        // Initialize the image cache
        mImageFetcher.setImageCache(ImageCache.getInstance(this));

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt the UI.
        HandlerThread thread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Initialize the handler
        mPlayerHandler = new MusicPlayerHandler(this, thread.getLooper());

        // Initialize the audio manager and register any headset controls for playback
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(this, MediaButtonIntentReceiver.class);
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        // Use the remote control APIs to set the playback state
        setUpRemoteControlClient();

        // Initialize the preferences
        settings = PreferenceUtils.getInstance(this);
        getCardId();

        registerExternalStorageListener();

        // Initialize the media player
        mPlayer = new MultiPlayer(this);
        mPlayer.setHandler(mPlayerHandler);

        // Initialize the intent filter and each action
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        filter.addAction(TOGGLEPAUSE_ACTION);
        filter.addAction(PAUSE_ACTION);
        filter.addAction(STOP_ACTION);
        filter.addAction(NEXT_ACTION);
        filter.addAction(PREVIOUS_ACTION);
        filter.addAction(REPEAT_ACTION);
        filter.addAction(SHUFFLE_ACTION);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);

        // Initialize the delayed shutdown intent
        Intent shutdownIntent = new Intent(this, MusicPlaybackService.class);
        shutdownIntent.setAction(SHUTDOWN);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        // Listen for the idle state
        scheduleDelayedShutdown();

        // Bring the queue back
        reloadQueue();
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        mPlayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
    }

    /**
     * Initializes the remote control client
     */
    private void setUpRemoteControlClient() {
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        mAudioManager.registerRemoteControlClient(mRemoteControlClient);

        // Flags for the media transport control that this client supports.
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        if (ApolloUtils.hasJellyBeanMR2()) {
            flags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;

            mRemoteControlClient.setOnGetPlaybackPositionListener(
                    new RemoteControlClient.OnGetPlaybackPositionListener() {
                        @Override
                        public long onGetPlaybackPosition() {
                            return position();
                        }
                    });
            mRemoteControlClient.setPlaybackPositionUpdateListener(
                    new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                        @Override
                        public void onPlaybackPositionUpdate(long newPositionMs) {
                            seek(newPositionMs);
                        }
                    });
        }
        mRemoteControlClient.setTransportControlFlags(flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove any sound effects
        Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
        // remove any pending alarms
        mAlarmManager.cancel(mShutdownIntent);
        // Remove all pending messages before kill the player
        mPlayerHandler.removeCallbacksAndMessages(null);
        // Release the player
        mPlayer.release();
        // Remove the audio focus listener and lock screen controls
        mAudioManager.abandonAudioFocus(this);
        mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
        // Remove any callbacks from the handler
        mPlayerHandler.removeCallbacksAndMessages(null);
        // Close the cursor
        closeCursor();
        // Unregister the mount listener
        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        if (intent != null) {
            String action = intent.getAction();
            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                boolean isForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false);
                if (isForeground) {
                    stopForeground(true);
                } else if (isPlaying()) {
                    mNotificationHelper.buildNotification();
                }
            }
            if (SHUTDOWN.equals(action)) {
                mShutdownScheduled = false;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }
            handleCommandIntent(intent);
        }
        // Make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        scheduleDelayedShutdown();
        if (intent != null && intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }
        return START_STICKY;
    }

    /**
     *
     */
    private void releaseServiceUiAndStop() {
        if (isPlaying() || mPausedByTransientLossOfFocus || mPlayerHandler.hasMessages(TRACK_ENDED)) {
            return;
        }
        stopForeground(true);
        mAudioManager.abandonAudioFocus(this);
        if (!mServiceInUse) {
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    }

    /**
     *
     */
    private void handleCommandIntent(Intent intent) {
        String action = intent.getAction();
        String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;

        if (CMDNEXT.equals(command) || NEXT_ACTION.equals(action)) {
            gotoNext(true);
        } else if (CMDPREVIOUS.equals(command) || PREVIOUS_ACTION.equals(action)) {
            if (position() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                prev();
            } else {
                seek(0);
                play();
            }
        } else if (CMDTOGGLEPAUSE.equals(command) || TOGGLEPAUSE_ACTION.equals(action)) {
            if (isPlaying()) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else {
                play();
            }
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            pause();
            mPausedByTransientLossOfFocus = false;
        } else if (CMDPLAY.equals(command)) {
            play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            pause();
            mPausedByTransientLossOfFocus = false;
            seek(0);
            releaseServiceUiAndStop();
        } else if (REPEAT_ACTION.equals(action)) {
            cycleRepeat();
        } else if (SHUFFLE_ACTION.equals(action)) {
            cycleShuffle();
        }
    }

    /**
     *
     */
    private void getCardId() {
        try {
            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mCardId = cursor.getInt(0);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     */
    public void closeExternalStorageFiles() {
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
     * intent will call closeExternalStorageFiles() if the external media is
     * going to be ejected, so applications can clean up any files they have
     * open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new UnmountBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, filter);
        }
    }

    /**
     *
     */
    private void scheduleDelayedShutdown() {
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    /**
     *
     */
    private void cancelShutdown() {
        if (mShutdownScheduled) {
            mAlarmManager.cancel(mShutdownIntent);
            mShutdownScheduled = false;
        }
    }

    /**
     * Stops playback
     *
     * @param goToIdle True to go to the idle state, false otherwise
     */
    private void stop(boolean goToIdle) {
        if (mPlayer.isInitialized()) {
            mPlayer.stop();
        }
        mFileToPlay = null;
        closeCursor();
        if (goToIdle) {
            scheduleDelayedShutdown();
            mIsSupposedToBePlaying = false;
        } else {
            stopForeground(false);
        }
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last  The last file to be removed
     * @return the number of tracks deleted
     */
    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) {
                return 0;
            } else if (first < 0) {
                first = 0;
            } else if (last >= mPlayList.size()) {
                last = mPlayList.size() - 1;
            }
            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= last - first + 1;
            }
            // remove a range of tracks from playlist
            mPlayList.subList(first, last + 1).clear();
            if (gotonext) {
                if (mPlayList.isEmpty()) {
                    stop(true);
                    mPlayPos = -1;
                    closeCursor();
                } else {
                    if (mShuffleMode != SHUFFLE_NONE) {
                        mPlayPos = getNextPosition(true);
                    } else if (mPlayPos >= mPlayList.size()) {
                        mPlayPos = 0;
                    }
                    boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                notifyChange(META_CHANGED);
            }
            return last - first + 1;
        }
    }

    /**
     * Adds a music ID list to the current playlist
     *
     * @param list     The list to add
     * @param position The position to place the tracks
     */
    private void addToPlayList(long[] list, int position) {
        if (position < 0) {
            position = 0;
        }
        if (position > mPlayList.size()) {
            position = mPlayList.size();
        }
        for (long l : list) {
            mPlayList.add(position++, l);
        }
        if (mPlayList.isEmpty()) {
            closeCursor();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * @param trackId The track ID
     */
    private void updateCursor(long trackId) {
        updateCursor("_id=" + trackId, null);
    }

    /**
     *
     */
    private void updateCursor(String selection, String[] selectionArgs) {
        synchronized (this) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION, selection, selectionArgs);
        }
        updateAlbumCursor();
    }

    /**
     *
     */
    private void updateCursor(Uri uri) {
        synchronized (this) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(uri, PROJECTION, null, null);
        }
        updateAlbumCursor();
    }

    /**
     *
     */
    private void updateAlbumCursor() {
        long albumId = getAlbumId();
        if (albumId >= 0) {
            mAlbumCursor = openCursorAndGoToFirst(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION, "_id=" + albumId, null);
        } else {
            mAlbumCursor = null;
        }
    }

    /**
     *
     */
    private Cursor openCursorAndGoToFirst(Uri uri, String[] projection, String selection, String[] selectionArgs) {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, null, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    /**
     *
     */
    private void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (mAlbumCursor != null) {
            mAlbumCursor.close();
            mAlbumCursor = null;
        }
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    private void openCurrentAndNext() {
        openCurrentTrack();
        setNextTrack();
    }

    /**
     * Called to open a new file as the current track and prepare the next for playback
     */
    private void openCurrentTrack() {
        synchronized (this) {
            closeCursor();
            if (mPlayList.isEmpty()) {
                return;
            }
            stop(false);
            updateCursor(mPlayList.get(mPlayPos));
            boolean fileOpenFailed;
            if (mCursor != null) {
                String path = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + mCursor.getLong(IDCOLIDX);
                fileOpenFailed = !openFile(path);
            } else {
                fileOpenFailed = false;
            }
            if (fileOpenFailed || mCursor.isClosed()) {
                // if we get here then opening the file failed. We can close the
                // cursor now, because
                // we're either going to create a new one next, or stop trying
                if (mPlayList.size() > 1) {
                    for (int i = 0; i < 10; i++) { // retrying 10 times until failure
                        int pos = getNextPosition(false);
                        if (pos < 0) {
                            scheduleDelayedShutdown();
                            if (mIsSupposedToBePlaying) {
                                mIsSupposedToBePlaying = false;
                                notifyChange(PLAYSTATE_CHANGED);
                            }
                            return;
                        }
                        mPlayPos = pos;
                        stop(false);
                        mPlayPos = pos;
                        updateCursor(mPlayList.get(mPlayPos));
                    }
                }
                Log.w(TAG, "Failed to open file for playback");
                scheduleDelayedShutdown();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
            }
        }
    }

    /**
     * @param force True to force the player onto the track next, false otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(boolean force) {
        if (!force && mRepeatMode == REPEAT_CURRENT) {
            return Math.max(mPlayPos, 0);
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            if (mPlayPos >= 0) {
                mHistory.add(mPlayPos);
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            int numTracks = mPlayList.size();
            int[] tracks = new int[numTracks];
            for (int i = 0; i < numTracks; i++) {
                tracks[i] = i;
            }
            int numHistory = mHistory.size();
            int numUnplayed = numTracks;
            for (int i = 0; i < numHistory; i++) {
                int idx = mHistory.get(i);
                if (idx < numTracks && tracks[idx] >= 0) {
                    numUnplayed--;
                    tracks[idx] = -1;
                }
            }
            if (numUnplayed <= 0) {
                if (mRepeatMode == REPEAT_ALL || force) {
                    numUnplayed = numTracks;
                    for (int i = 0; i < numTracks; i++) {
                        tracks[i] = i;
                    }
                } else {
                    return -1;
                }
            }
            int skip = 0;
            if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
                skip = mShuffler.nextInt(numUnplayed);
            }

            int count = 0;
            for (int i : tracks) {
                count++;
                if (i < 0) {
                    skip--;
                }
                if (skip < 0) {
                    return count;
                }
            }
            return count;
        } else if (mShuffleMode == SHUFFLE_AUTO) {
            doAutoShuffleUpdate();
            return mPlayPos + 1;
        } else {
            if (mPlayPos >= mPlayList.size() - 1) {
                if (mRepeatMode == REPEAT_NONE && !force) {
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    /**
     * Sets the track track to be played
     */
    private void setNextTrack() {
        mNextPlayPos = getNextPosition(false);
        if (mNextPlayPos >= 0) {
            long id = mPlayList.get(mNextPlayPos);
            mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
        } else {
            mPlayer.setNextDataSource(null);
        }
    }

    /**
     * Creates a shuffled playlist used for party mode
     */
    private boolean makeAutoShuffleList() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID}, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
            int len = cursor.getCount();
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            mAutoShuffleList.clear();
            for (long l : list) {
                mAutoShuffleList.add(l);
            }
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    /**
     * Creates the party shuffle playlist
     */
    private void doAutoShuffleUpdate() {
        boolean notify = false;
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        int toAdd = 7 - (mPlayList.size() - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < toAdd; i++) {
            int lookback = mHistory.size();
            int idx;
            do {
                idx = mShuffler.nextInt(mAutoShuffleList.size());
                lookback /= 2;
            } while (wasRecentlyUsed(idx, lookback));

            mHistory.add(idx);
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            mPlayList.add(mAutoShuffleList.get(idx));
            notify = true;
        }
        if (notify) {
            notifyChange(QUEUE_CHANGED);
        }
    }

    /**
     *
     */
    private boolean wasRecentlyUsed(int idx, int lookbacksize) {
        if (lookbacksize == 0) {
            return false;
        }
        int histsize = mHistory.size();
        if (histsize < lookbacksize) {
            lookbacksize = histsize;
        }
        int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            long entry = mHistory.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    private void notifyChange(String what) {
        // Update the lockscreen controls
        updateRemoteControlClient(what);

        if (what.equals(POSITION_CHANGED)) {
            return;
        }
        Intent intent = new Intent(what);
        intent.putExtra("id", getAudioId());
        intent.putExtra("artist", getArtistName());
        intent.putExtra("album", getAlbumName());
        intent.putExtra("track", getTrackName());
        intent.putExtra("playing", isPlaying());
        intent.putExtra("isfavorite", isFavorite());
        sendBroadcast(intent);

        Intent musicIntent = new Intent(intent);
        musicIntent.setAction(what.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
        sendBroadcast(musicIntent);

        if (what.equals(META_CHANGED)) {
            // Increase the play count for favorite songs.
            if (mFavoritesCache.getSongId(getAudioId()) != null) {
                mFavoritesCache.addSongId(getAudioId(), getTrackName(), getAlbumName(), getArtistName());
            }
            // Add the track to the recently played list.
            mRecentsCache.addAlbumId(getAlbumId(), getAlbumName(), getArtistName(),
                    MusicUtils.getSongCountForAlbum(this, getAlbumId()),
                    MusicUtils.getReleaseDateForAlbum(this, getAlbumId()));
        } else if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
            if (isPlaying()) {
                setNextTrack();
            }
        } else {
            saveQueue(false);
        }
        if (what.equals(PLAYSTATE_CHANGED)) {
            mNotificationHelper.updateNotification();
        }
        // Update the app-widgets
        for (AppWidgetBase widget : widgets) {
            widget.notifyChange(this, what);
        }
    }

    /**
     *
     */
    public void updateNotification() {
        mNotificationHelper.updateNotification();
    }

    /**
     * Updates the lockscreen controls.
     *
     * @param what The broadcast
     */
    private void updateRemoteControlClient(String what) {
        int playState = mIsSupposedToBePlaying ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED;
        if (ApolloUtils.hasJellyBeanMR2() && (what.equals(PLAYSTATE_CHANGED) || what.equals(POSITION_CHANGED))) {
            mRemoteControlClient.setPlaybackState(playState, position(), 1.0f);
        } else if (what.equals(PLAYSTATE_CHANGED)) {
            mRemoteControlClient.setPlaybackState(playState);
        } else if (what.equals(META_CHANGED) || what.equals(QUEUE_CHANGED)) {
            Bitmap albumArt = getAlbumArt();
            if (albumArt != null) {
                // RemoteControlClient wants to recycle the bitmaps thrown at it, so we need
                // to make sure not to hand out our cache copy
                Bitmap.Config config = albumArt.getConfig();
                if (config == null) {
                    config = Bitmap.Config.ARGB_8888;
                }
                albumArt = albumArt.copy(config, false);
            }
            mRemoteControlClient
                    .editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getArtistName())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, getAlbumArtistName())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getAlbumName())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getTrackName())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration())
                    .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumArt)
                    .apply();
            if (ApolloUtils.hasJellyBeanMR2()) {
                mRemoteControlClient.setPlaybackState(playState, position(), 1.0f);
            }
        }
    }

    /**
     * Saves the queue
     *
     * @param full True if the queue is full
     */
    private void saveQueue(boolean full) {
        if (!mQueueIsSaveable) {
            return;
        }
        if (full) {
            settings.setPlayList(mPlayList, mCardId);
            if (mShuffleMode != SHUFFLE_NONE) {
                settings.setHistory(mHistory);
            }
        }
        settings.setCursorPosition(mPlayPos);
        if (mPlayer.isInitialized()) {
            settings.setSeekPosition(mPlayer.position());
        }
        settings.setRepeatAndShuffleMode(mRepeatMode, mShuffleMode);
    }

    /**
     * Reloads the queue as the user left it the last time they stopped using
     * Apollo
     */
    private void reloadQueue() {
        int id = settings.getCardId();
        if (id == mCardId) {
            mPlayList.clear();
            mPlayList.addAll(settings.getPlaylist());
        }
        if (!mPlayList.isEmpty()) {
            int pos = settings.getCursorPosition();
            if (pos < 0 || pos >= mPlayList.size()) {
                return;
            }
            mPlayPos = pos;
            updateCursor(mPlayList.get(mPlayPos));
            if (mCursor == null) {
                updateCursor(mPlayList.get(mPlayPos));
            }
            synchronized (this) {
                closeCursor();
                openCurrentAndNext();
            }
            if (!mPlayer.isInitialized()) {
                return;
            }
            long seekpos = settings.getSeekPosition();
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);

            int repmode = settings.getRepeatMode();
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = settings.getShuffleMode();
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
            if (shufmode != SHUFFLE_NONE) {
                mHistory.clear();
                mHistory.addAll(settings.getTrackHistory());
            }
            if (shufmode == SHUFFLE_AUTO) {
                if (!makeAutoShuffleList()) {
                    shufmode = SHUFFLE_NONE;
                }
            }
            mShuffleMode = shufmode;
        }
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public boolean openFile(String path) {
        synchronized (this) {
            if (path == null) {
                return false;
            }
            // If mCursor is null, try to associate path with a database cursor
            if (mCursor == null) {
                Uri uri = Uri.parse(path);
                long id = -1;
                try {
                    if (uri != null && uri.getLastPathSegment() != null)
                        id = Long.parseLong(uri.getLastPathSegment());
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
                if (id != -1 && path.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    updateCursor(uri);

                } else if (id != -1 && path.startsWith(MediaStore.Files.getContentUri("external").toString())) {
                    updateCursor(id);
                } else {
                    String where = MediaStore.Audio.Media.DATA + "=?";
                    String[] selectionArgs = new String[]{path};
                    updateCursor(where, selectionArgs);
                }
                if (mCursor != null && mCursor.moveToFirst()) {
                    mPlayList.add(0, mCursor.getLong(IDCOLIDX));
                    mPlayPos = 0;
                }
            }
            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            if (mPlayer.isInitialized()) {
                return true;
            }
            stop(true);
            return false;
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * Indicates if the media storeage device has been mounted or not
     *
     * @return 1 if Intent.ACTION_MEDIA_MOUNTED is called, 0 otherwise
     */
    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the shuffle mode
     *
     * @return The current shuffle mode (all, party, none)
     */
    public int getShuffleMode() {
        return mShuffleMode;
    }

    /**
     * Sets the shuffle mode
     *
     * @param shufflemode The shuffle mode to use
     */
    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode && !mPlayList.isEmpty()) {
                return;
            }
            mShuffleMode = shufflemode;
            if (mShuffleMode == SHUFFLE_AUTO) {
                if (makeAutoShuffleList()) {
                    doAutoShuffleUpdate();
                    mPlayPos = 0;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                } else {
                    mShuffleMode = SHUFFLE_NONE;
                }
            }
            saveQueue(false);
            notifyChange(SHUFFLEMODE_CHANGED);
        }
    }

    /**
     * Returns the repeat mode
     *
     * @return The current repeat mode (all, one, none)
     */
    public int getRepeatMode() {
        return mRepeatMode;
    }

    /**
     * Sets the repeat mode
     *
     * @param repeatmode The repeat mode to use
     */
    public void setRepeatMode(int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            setNextTrack();
            saveQueue(false);
            notifyChange(REPEATMODE_CHANGED);
        }
    }

    /**
     * Removes all instances of the track with the given ID from the playlist.
     *
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int pos = 0; pos < mPlayList.size(); pos++) {
                if (mPlayList.get(pos) == id) {
                    numremoved += removeTracksInternal(pos, pos);
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last  The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(int first, int last) {
        int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Returns the position in the queue
     *
     * @return the current position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }

    /**
     * Sets the position of a track in the queue
     *
     * @param index The position to place the track
     */
    public void setQueuePosition(int index) {
        synchronized (this) {
            stop(false);
            mPlayPos = index;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }

    /**
     * Returns the path to current song
     *
     * @return The path to the current song
     */
    public String getPath() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.DATA));
        }
    }

    /**
     * Returns the album name
     *
     * @return The current song album Name
     */
    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow("album"));
        }
    }

    /**
     * Returns the song name
     *
     * @return The current song name
     */
    public String getTrackName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.TITLE));
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    public String getArtistName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow("artist"));
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    public String getAlbumArtistName() {
        synchronized (this) {
            if (mAlbumCursor == null) {
                return null;
            }
            return mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));
        }
    }

    /**
     * Returns the album ID
     *
     * @return The current song album ID
     */
    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null || !mCursor.moveToFirst()) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
        }
    }

    /**
     * Returns the artist ID
     *
     * @return The current song artist ID
     */
    public long getArtistId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST_ID));
        }
    }

    /**
     * Returns the current audio ID
     *
     * @return The current track ID
     */
    public long getAudioId() {
        synchronized (this) {
            if (mPlayPos >= 0 && mPlayer.isInitialized()) {
                return mPlayList.get(mPlayPos);
            }
        }
        return -1;
    }

    /**
     * Seeks the current track to a specific time
     *
     * @param position The time to seek to
     * @return The time to play the track at
     */
    public long seek(long position) {
        if (mPlayer.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else if (position > mPlayer.duration()) {
                position = mPlayer.duration();
            }
            long result = mPlayer.seek(position);
            notifyChange(POSITION_CHANGED);
            return result;
        }
        return -1;
    }

    /**
     * Returns the current position in time of the currenttrack
     *
     * @return The current playback position in miliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Returns the full duration of the current track
     *
     * @return The duration of the current track in miliseconds
     */
    public long duration() {
        if (mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the queue
     *
     * @return The queue as a long[]
     */
    public long[] getQueue() {
        synchronized (this) {
            int len = mPlayList.size();
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mPlayList.get(i);
            }
            return list;
        }
    }

    /**
     * @return True if music is playing, false otherwise
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /**
     * True if the current track is a "favorite", false otherwise
     */
    public boolean isFavorite() {
        if (mFavoritesCache != null) {
            synchronized (this) {
                Long id = mFavoritesCache.getSongId(getAudioId());
                return id != null;
            }
        }
        return false;
    }

    /**
     * Opens a list for playback
     *
     * @param list     The list of tracks to open
     * @param position The position to start playback at
     */
    public void open(long[] list, int position) {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_AUTO) {
                mShuffleMode = SHUFFLE_NORMAL;
            }
            long oldId = getAudioId();
            mPlayPos = position >= 0 ? position : mShuffler.nextInt(mPlayList.size());

            boolean newlist = true;
            if (mPlayList.size() == list.length) {
                newlist = false;
                for (int i = 0; i < list.length; i++) {
                    if (list[i] != mPlayList.get(i)) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                mPlayList.clear();
                for (long track : list)
                    mPlayList.add(track);
                notifyChange(QUEUE_CHANGED);
            }
            mHistory.clear();
            openCurrentAndNext();
            if (oldId != getAudioId()) {
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Resumes or starts playback.
     */
    public void play() {
        int status = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this, MediaButtonIntentReceiver.class));
            if (mPlayer.isInitialized()) {
                setNextTrack();
                long duration = mPlayer.duration();
                if (mRepeatMode != REPEAT_CURRENT && duration > 2000 && mPlayer.position() >= duration - 2000) {
                    gotoNext(true);
                }
                mPlayer.start();
                mPlayerHandler.removeMessages(FADEDOWN);
                mPlayerHandler.sendEmptyMessage(FADEUP);
                if (!mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = true;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                cancelShutdown();
                updateNotification();
            } else if (mPlayList.isEmpty()) {
                setShuffleMode(SHUFFLE_AUTO);
            }
        }
    }

    /**
     * Temporarily pauses playback.
     */
    public void pause() {
        synchronized (this) {
            mPlayerHandler.removeMessages(FADEUP);
            if (mIsSupposedToBePlaying) {
                mPlayer.pause();
                scheduleDelayedShutdown();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
            }
        }
    }

    /**
     * Changes from the current track to the next track
     */
    public void gotoNext(boolean force) {
        synchronized (this) {
            if (mPlayList.isEmpty()) {
                scheduleDelayedShutdown();
                return;
            }
            int pos = getNextPosition(force);
            if (pos < 0) {
                scheduleDelayedShutdown();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                return;
            }
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Changes from the current track to the previous played track
     */
    public void prev() {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_NORMAL) {
                // Go to previously-played track and remove it from the history
                int histsize = mHistory.size();
                if (histsize == 0) {
                    return;
                }
                mPlayPos = mHistory.remove(histsize - 1);
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayList.size() - 1;
                }
            }
            stop(false);
            openCurrent();
            play();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * We don't want to open the current and next track when the user is using
     * the {@code #prev()} method because they won't be able to travel back to
     * the previously listened track if they're shuffling.
     */
    private void openCurrent() {
        openCurrentTrack();
    }

    /**
     * Toggles the current song as a favorite.
     */
    public void toggleFavorite() {
        if (mFavoritesCache != null) {
            synchronized (this) {
                mFavoritesCache.toggleSong(getAudioId(), getTrackName(), getAlbumName(), getArtistName());
            }
        }
    }

    /**
     * Moves an item in the queue from one position to another
     *
     * @param from The position the item is currently at
     * @param to   The position the item is being moved to
     */
    public void moveQueueItem(int from, int to) {
        synchronized (this) {
            if (from >= mPlayList.size()) {
                from = mPlayList.size() - 1;
            }
            if (to >= mPlayList.size()) {
                to = mPlayList.size() - 1;
            }
            // move track
            long trackId = mPlayList.remove(from);
            mPlayList.add(to, trackId);
            // set current play pos
            if (mPlayPos == from) {
                mPlayPos = to;
            } else if (mPlayPos >= from && mPlayPos <= to) {
                mPlayPos--;
            } else if (mPlayPos <= from && mPlayPos >= to) {
                mPlayPos++;
            }
            mNextPlayPos = getNextPosition(false);
            notifyChange(QUEUE_CHANGED);
        }
    }

    /**
     * Queues a new list for playback
     *
     * @param list   The list to queue
     * @param action The action to take
     */
    public void enqueue(long[] list, int action) {
        synchronized (this) {
            if (action == NEXT && mPlayPos + 1 < mPlayList.size()) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
            } else {
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayList.size() - list.length;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Cycles through the different repeat modes
     */
    private void cycleRepeat() {
        if (mRepeatMode == REPEAT_NONE) {
            setRepeatMode(REPEAT_ALL);
        } else if (mRepeatMode == REPEAT_ALL) {
            setRepeatMode(REPEAT_CURRENT);
            if (mShuffleMode != SHUFFLE_NONE) {
                setShuffleMode(SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(REPEAT_NONE);
        }
    }

    /**
     * Cycles through the different shuffle modes
     */
    private void cycleShuffle() {
        if (mShuffleMode == SHUFFLE_NONE) {
            setShuffleMode(SHUFFLE_NORMAL);
            if (mRepeatMode == REPEAT_CURRENT) {
                setRepeatMode(REPEAT_ALL);
            }
        } else if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
            setShuffleMode(SHUFFLE_NONE);
        }
    }

    /**
     * @return The album art for the current album.
     */
    public Bitmap getAlbumArt() {
        // Return the cached artwork
        return mImageFetcher.getArtwork(getAlbumName(), getAlbumId(), getArtistName());
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public void refresh() {
        notifyChange(REFRESH);
    }

    private static final class MusicPlayerHandler extends Handler {
        private WeakReference<MusicPlaybackService> mService;
        private float mCurrentVolume = 1.0f;

        /**
         * Constructor of <code>MusicPlayerHandler</code>
         *
         * @param service The service to use.
         * @param looper  The thread to run on.
         */
        public MusicPlayerHandler(MusicPlaybackService service, Looper looper) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            MusicPlaybackService service = mService.get();
            if (service == null) {
                return;
            }

            switch (msg.what) {
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;

                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;

                case SERVER_DIED:
                    if (service.isPlaying()) {
                        service.gotoNext(true);
                    } else {
                        service.openCurrentAndNext();
                    }
                    break;

                case TRACK_WENT_TO_NEXT:
                    service.mPlayPos = service.mNextPlayPos;
                    if (service.mCursor != null) {
                        service.mCursor.close();
                    }
                    service.updateCursor(service.mPlayList.get(service.mPlayPos));
                    service.notifyChange(META_CHANGED);
                    service.updateNotification();
                    service.setNextTrack();
                    break;

                case TRACK_ENDED:
                    if (service.mRepeatMode == REPEAT_CURRENT) {
                        service.seek(0);
                        service.play();
                    } else {
                        service.gotoNext(false);
                    }
                    break;

                case FOCUSCHANGE:
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (service.isPlaying()) {
                                service.mPausedByTransientLossOfFocus =
                                        msg.arg1 == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                            }
                            service.pause();
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            removeMessages(FADEUP);
                            sendEmptyMessage(FADEDOWN);
                            break;

                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (!service.isPlaying()
                                    && service.mPausedByTransientLossOfFocus) {
                                service.mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                service.mPlayer.setVolume(mCurrentVolume);
                                service.play();
                            } else {
                                removeMessages(FADEDOWN);
                                sendEmptyMessage(FADEUP);
                            }
                            break;
                    }
                    break;
            }
        }
    }

    /**
     * widget Broadcast listener
     */
    private final class WidgetBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra(CMDNAME);
            int[] small = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

            if (AppWidgetSmall.CMDAPPWIDGETUPDATE.equals(command)) {
                widgets[0].performUpdate(MusicPlaybackService.this, small);
            } else if (AppWidgetLarge.CMDAPPWIDGETUPDATE.equals(command)) {
                widgets[1].performUpdate(MusicPlaybackService.this, small);
            } else if (AppWidgetLargeAlternate.CMDAPPWIDGETUPDATE.equals(command)) {
                widgets[2].performUpdate(MusicPlaybackService.this, small);
            } else if (RecentWidgetProvider.CMDAPPWIDGETUPDATE.equals(command)) {
                widgets[3].performUpdate(MusicPlaybackService.this, small);
            } else {
                handleCommandIntent(intent);
            }
        }
    }

    /**
     * Custom broadcast listener for unmounting external storage
     */
    private final class UnmountBroadcastReceiver extends BroadcastReceiver {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                saveQueue(true);
                mQueueIsSaveable = false;
                closeExternalStorageFiles();
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                mMediaMountedCount++;
                getCardId();
                reloadQueue();
                mQueueIsSaveable = true;
                notifyChange(QUEUE_CHANGED);
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     *
     */
    private static final class Shuffler {

        private LinkedList<Integer> mHistoryOfNumbers = new LinkedList<>();

        private TreeSet<Integer> mPreviousNumbers = new TreeSet<>();

        private Random mRandom = new Random();

        private int mPrevious;

        /**
         * Constructor of <code>Shuffler</code>
         */
        public Shuffler() {
            super();
        }

        /**
         * @param interval The length the queue
         * @return The position of the next track to play
         */
        public int nextInt(int interval) {
            int next;
            do {
                next = mRandom.nextInt(interval);
            } while (next == mPrevious && interval > 1 && !mPreviousNumbers.contains(next));
            mPrevious = next;
            mHistoryOfNumbers.add(mPrevious);
            mPreviousNumbers.add(mPrevious);
            cleanUpHistory();
            return next;
        }

        /**
         * Removes old tracks and cleans up the history preparing for new tracks
         * to be added to the mapping
         */
        private void cleanUpHistory() {
            if (!mHistoryOfNumbers.isEmpty() && mHistoryOfNumbers.size() >= MAX_HISTORY_SIZE) {
                for (int i = 0; i < Math.max(1, MAX_HISTORY_SIZE / 2); i++) {
                    mPreviousNumbers.remove(mHistoryOfNumbers.removeFirst());
                }
            }
        }
    }

    /**
     *
     */
    private static final class MultiPlayer implements OnErrorListener, OnCompletionListener {

        private final WeakReference<MusicPlaybackService> mService;

        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();

        private MediaPlayer mNextMediaPlayer;

        private Handler mHandler;

        private boolean mIsInitialized = false;

        /**
         * Constructor of <code>MultiPlayer</code>
         */
        public MultiPlayer(MusicPlaybackService service) {
            mService = new WeakReference<>(service);
            mCurrentMediaPlayer.setScreenOnWhilePlaying(true);
        }

        /**
         * @param path The path of the file, or the http/rtsp URL of the stream
         *             you want to play
         */
        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
            if (mIsInitialized) {
                setNextDataSource(null);
            }
        }

        /**
         * @param player The {@link MediaPlayer} to use
         * @param path   The path of the file, or the http/rtsp URL of the stream
         *               you want to play
         * @return True if the <code>player</code> has been prepared and is
         * ready to play, false otherwise
         */
        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            MusicPlaybackService musicService = mService.get();
            if (musicService != null) {
                try {
                    player.reset();
                    player.setOnPreparedListener(null);
                    if (path.startsWith("content://")) {
                        ContentResolver resolver = musicService.getApplicationContext().getContentResolver();
                        ParcelFileDescriptor pfd = resolver.openFileDescriptor(Uri.parse(path), "r");
                        player.setDataSource(pfd.getFileDescriptor(), 0, pfd.getStatSize());
                        pfd.close();
                    } else {
                        player.setDataSource(path);
                    }
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.prepare();
                } catch (Exception err) {
                    err.printStackTrace();
                    return false;
                }
                player.setOnCompletionListener(this);
                player.setOnErrorListener(this);
                Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, musicService.getPackageName());
                musicService.sendBroadcast(intent);
                return true;
            }
            return false;
        }

        /**
         * Set the MediaPlayer to start when this MediaPlayer finishes playback.
         *
         * @param path The path of the file, or the http/rtsp URL of the stream
         *             you want to play
         */
        public void setNextDataSource(String path) {
            try {
                mCurrentMediaPlayer.setNextMediaPlayer(null);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (path == null) {
                return;
            }
            MusicPlaybackService musicService = mService.get();
            if (musicService != null) {
                mNextMediaPlayer = new MediaPlayer();
                mNextMediaPlayer.setScreenOnWhilePlaying(true);
                mNextMediaPlayer.setWakeMode(musicService, PowerManager.PARTIAL_WAKE_LOCK);
                mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
                if (setDataSourceImpl(mNextMediaPlayer, path)) {
                    mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
                } else {
                    if (mNextMediaPlayer != null) {
                        mNextMediaPlayer.release();
                        mNextMediaPlayer = null;
                    }
                }
            }
        }

        /**
         * Sets the handler
         *
         * @param handler The handler to use
         */
        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        /**
         * @return True if the player is ready to go, false otherwise
         */
        public boolean isInitialized() {
            return mIsInitialized;
        }

        /**
         * Starts or resumes playback.
         */
        public void start() {
            mCurrentMediaPlayer.start();
        }

        /**
         * Resets the MediaPlayer to its uninitialized state.
         */
        public void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * Releases resources associated with this MediaPlayer object.
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        /**
         * Pauses playback. Call start() to resume.
         */
        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        /**
         * Gets the duration of the file.
         *
         * @return The duration in milliseconds
         */
        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        /**
         * Gets the current playback position.
         *
         * @return The current position in milliseconds
         */
        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        /**
         * Gets the current playback position.
         *
         * @param whereto The offset in milliseconds from the start to seek to
         * @return The offset in milliseconds from the start to seek to
         */
        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        /**
         * Sets the volume on this player.
         *
         * @param vol Left and right volume scalar
         */
        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        /**
         * Returns the audio session ID.
         *
         * @return The current audio session ID.
         */
        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            MusicPlaybackService musicService = mService.get();
            if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED && musicService != null) {
                mIsInitialized = false;
                mCurrentMediaPlayer.reset();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = mNextMediaPlayer;
                mNextMediaPlayer = null;
                mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
            } else {
                mHandler.sendEmptyMessage(TRACK_ENDED);
            }
        }
    }

    /**
     *
     */
    private static final class ServiceStub extends IApolloService.Stub {

        private final WeakReference<MusicPlaybackService> mService;

        private ServiceStub(MusicPlaybackService service) {
            mService = new WeakReference<>(service);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void openFile(String path) {
            MusicPlaybackService service = mService.get();
            if (service != null && path != null)
                service.openFile(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void open(long[] list, int position) {
            MusicPlaybackService service = mService.get();
            if (mService.get() != null && list != null)
                service.open(list, position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stop() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.stop();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void pause() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.pause();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void play() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.play();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void prev() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.prev();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void next() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.gotoNext(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void enqueue(long[] list, int action) {
            MusicPlaybackService service = mService.get();
            if (service != null && list != null)
                service.enqueue(list, action);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void moveQueueItem(int from, int to) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.moveQueueItem(from, to);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void toggleFavorite() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.toggleFavorite();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void refresh() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.refresh();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFavorite() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.isFavorite();
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isPlaying() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.isPlaying();
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long[] getQueue() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getQueue();
            return new long[]{};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long duration() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.duration();
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long position() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.position();
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long seek(long position) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.seek(position);
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getAudioId() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getAudioId();
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getArtistId() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getArtistId();
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getAlbumId() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getAlbumId();
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getArtistName() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getArtistName();
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTrackName() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getTrackName();
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getAlbumName() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getAlbumName();
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPath() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getPath();
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getQueuePosition() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getQueuePosition();
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setQueuePosition(int index) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.setQueuePosition(index);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getShuffleMode() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getShuffleMode();
            return SHUFFLE_NONE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setShuffleMode(int shufflemode) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.setShuffleMode(shufflemode);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRepeatMode() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getRepeatMode();
            return REPEAT_NONE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setRepeatMode(int repeatmode) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                service.setRepeatMode(repeatmode);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int removeTracks(int first, int last) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.removeTracks(first, last);
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int removeTrack(long id) {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.removeTrack(id);
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getMediaMountedCount() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getMediaMountedCount();
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAudioSessionId() {
            MusicPlaybackService service = mService.get();
            if (service != null)
                return service.getAudioSessionId();
            return 0;
        }
    }
}