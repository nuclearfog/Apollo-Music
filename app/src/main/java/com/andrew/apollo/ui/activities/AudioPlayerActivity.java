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

package com.andrew.apollo.ui.activities;

import android.animation.ObjectAnimator;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Playlists;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.DeleteDialog.DeleteDialogCallback;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment.BrowserCallback;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PlaybackStatus;
import com.andrew.apollo.utils.PlaybackStatus.PlayStatusListener;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.PlayPauseButton;
import com.andrew.apollo.widgets.RepeatButton;
import com.andrew.apollo.widgets.RepeatingImageButton;
import com.andrew.apollo.widgets.RepeatingImageButton.RepeatListener;
import com.andrew.apollo.widgets.ShuffleButton;

import java.io.File;
import java.lang.ref.WeakReference;

import static com.andrew.apollo.adapters.PagerAdapter.MusicFragments.QUEUE;
import static com.andrew.apollo.utils.MusicUtils.REQUEST_DELETE_FILES;

/**
 * Apollo's "now playing" interface.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AudioPlayerActivity extends AppCompatActivity implements ServiceConnection, OnSeekBarChangeListener,
        OnQueryTextListener, DeleteDialogCallback, OnClickListener, RepeatListener, PlayStatusListener {

    /**
     * Message to refresh the time
     */
    private static final int MSG_ID = 0x65059CC4;
    /**
     * The service token
     */
    private ServiceToken mToken;
    /**
     * Play and pause button
     */
    private PlayPauseButton mPlayPauseButton;
    /**
     * Repeat button
     */
    private RepeatButton mRepeatButton;
    /**
     * Shuffle button
     */
    private ShuffleButton mShuffleButton;
    /**
     * Track name
     */
    private TextView mTrackName;
    /**
     * Artist name
     */
    private TextView mArtistName;
    /**
     * Album art
     */
    private ImageView mAlbumArt;
    /**
     * Tiny artwork
     */
    private ImageView mAlbumArtSmall;
    /**
     * Current time
     */
    private TextView mCurrentTime;
    /**
     * Total time
     */
    private TextView mTotalTime;
    /**
     * Queue switch
     */
    private ImageView mQueueSwitch;
    /**
     * Progess
     */
    private SeekBar mProgress;
    /**
     * Broadcast receiver
     */
    private PlaybackStatus mPlaybackStatus;
    /**
     * Handler used to update the current time
     */
    private TimeHandler mTimeHandler;
    /**
     * Pager adpater
     */
    private PagerAdapter mPagerAdapter;
    /**
     * ViewPager
     */
    private ViewPager mViewPager;
    /**
     * Header
     */
    private LinearLayout mAudioPlayerHeader;
    /**
     * Image cache
     */
    private ImageFetcher mImageFetcher;
    /**
     * Theme resources
     */
    private ThemeUtils mResources;

    private int themeColor;
    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mLastShortSeekEventTime;
    private boolean mIsPaused = false;
    private boolean mFromTouch = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout
        setContentView(R.layout.activity_player_base);
        // set toolbar
        Toolbar toolbar = findViewById(R.id.player_toolbar);
        setSupportActionBar(toolbar);
        // Initialze the theme resources
        mResources = new ThemeUtils(this);
        // Set the overflow style
        mResources.setOverflowStyle(this);
        themeColor = PreferenceUtils.getInstance(this).getDefaultThemeColor();
        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);
        // Initialize the image fetcher/cache
        mImageFetcher = ApolloUtils.getImageFetcher(this);
        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);
        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);
        // Theme the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            mResources.themeActionBar(actionBar, R.string.app_name);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Cache all the items
        initPlaybackControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        startPlayback();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicUtils.connectService(service);
        // Check whether we were asked to start any playback
        startPlayback();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        invalidateOptionsMenu();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.disconnectService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
        if (!fromuser || !MusicUtils.isConnected()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            MusicUtils.seek(mPosOverride);
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        } else if (now - mLastShortSeekEventTime > 5) {
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            refreshCurrentTimeText(mPosOverride);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
        mCurrentTime.setVisibility(View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(SeekBar bar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
        }
        mPosOverride = -1;
        mFromTouch = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favorite = menu.findItem(R.id.menu_favorite);
        MenuItem effects = menu.findItem(R.id.menu_audio_player_equalizer);
        // Add fav icon
        mResources.setFavoriteIcon(favorite);
        // Hide the EQ option if it can't be opened
        Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if (getPackageManager().resolveActivity(intent, 0) == null) {
            effects.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Search view
        getMenuInflater().inflate(R.menu.search, menu);
        // Theme the search icon
        MenuItem searchAction = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchAction.getActionView();
        // Add voice search
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(searchableInfo);
        // Perform the search
        searchView.setOnQueryTextListener(this);
        // Favorite action
        getMenuInflater().inflate(R.menu.favorite, menu);
        // Shuffle all
        getMenuInflater().inflate(R.menu.shuffle, menu);
        // Share, ringtone, and equalizer
        getMenuInflater().inflate(R.menu.audio_player, menu);
        // Settings
        getMenuInflater().inflate(R.menu.activity_base, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int vId = item.getItemId();
        if (vId == android.R.id.home) {
            // Go back to the home activity
            NavUtils.goHome(this);
        } else if (vId == R.id.menu_shuffle) {
            // Shuffle all the songs
            MusicUtils.shuffleAll(this);
            // Refresh the queue
            getFragment().refresh();
        } else if (vId == R.id.menu_favorite) {
            // Toggle the current track as a favorite and update the menu item
            MusicUtils.toggleFavorite();
            invalidateOptionsMenu();
        } else if (vId == R.id.menu_audio_player_ringtone) {
            // Set the current track as a ringtone
            MusicUtils.setRingtone(this, MusicUtils.getCurrentAudioId());
        } else if (vId == R.id.menu_audio_player_share) {
            // Share the current meta data
            shareCurrentTrack();
        } else if (vId == R.id.menu_audio_player_equalizer) {
            // Sound effects
            NavUtils.openEffectsPanel(this);
        } else if (vId == R.id.menu_settings) {
            // Settings
            NavUtils.openSettings(this);
        } else if (vId == R.id.menu_audio_player_delete) {
            // Delete current song
            long[] ids = {MusicUtils.getCurrentAudioId()};
            MusicUtils.openDeleteDialog(this, MusicUtils.getTrackName(), ids);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
            MusicUtils.onPostDelete(this);

        }
    }


    @Override
    public void onDelete() {
        getFragment().refresh();
        if (MusicUtils.getQueue().length == 0) {
            NavUtils.goHome(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavUtils.goHome(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Refresh the queue
        getFragment().refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        long next = refreshCurrentTime();
        queueNextRefresh(next);
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(this, false);
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(MSG_ID);
        // Unbind from the service
        if (MusicUtils.isConnected()) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        // Open the search activity
        NavUtils.openSearch(AudioPlayerActivity.this, query);
        return true;
    }


    @Override
    public boolean onQueryTextChange(String newText) {
        // Nothing to do
        return false;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.audio_player_header) {
            String albumname = MusicUtils.getAlbumName();
            String artistname = MusicUtils.getArtistName();
            long albumId = MusicUtils.getCurrentAlbumId();
            NavUtils.openAlbumProfile(this, albumname, artistname, albumId);
        } else if (v.getId() == R.id.audio_player_switch) {
            if (mViewPager.getVisibility() == View.VISIBLE) {
                // Show the artwork, hide the queue
                showAlbumArt();
            } else {
                // Scroll to the current track
                mAudioPlayerHeader.setOnClickListener(this);
                getFragment().setCurrentTrack();
                // Show the queue, hide the artwork
                hideAlbumArt();
            }
        }
    }

    @Override
    public void onRepeat(View v, long howlong, int repcnt) {
        if (v.getId() == R.id.action_button_previous) {
            scanBackward(repcnt, howlong);
        } else if (v.getId() == R.id.action_button_next) {
            scanForward(repcnt, howlong);
        }
    }


    @Override
    public void onMetaChange() {
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        invalidateOptionsMenu();
        // jumpt to current track
        getFragment().setCurrentTrack();
    }


    @Override
    public void onStateChange() {
        // Set the play and pause image
        mPlayPauseButton.updateState();
    }


    @Override
    public void onModeChange() {
        // Set the repeat image
        mRepeatButton.updateRepeatState();
        // Set the shuffle image
        mShuffleButton.updateShuffleState();
    }


    @Override
    public void refresh() {
    }

    /**
     * Initializes the items in the now playing screen
     */
    private void initPlaybackControls() {
        // Now playing header
        mAudioPlayerHeader = findViewById(R.id.audio_player_header);
        // Opens the currently playing album profile
        mAudioPlayerHeader.setOnClickListener(this);
        // Used to hide the artwork and show the queue
        FrameLayout mSwitch = findViewById(R.id.audio_player_switch);
        mSwitch.setOnClickListener(this);
        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(this);
        // Queue
        mPagerAdapter.add(QUEUE, null);
        // Initialize the ViewPager
        // View pager
        mViewPager = findViewById(R.id.audio_player_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(1);
        // Play and pause button
        mPlayPauseButton = findViewById(R.id.action_button_play);
        // Shuffle button
        mShuffleButton = findViewById(R.id.action_button_shuffle);
        // Repeat button
        mRepeatButton = findViewById(R.id.action_button_repeat);
        // Previous button
        // Previous button
        RepeatingImageButton mPreviousButton = findViewById(R.id.action_button_previous);
        // Next button
        // Next button
        RepeatingImageButton mNextButton = findViewById(R.id.action_button_next);
        // Track name
        mTrackName = findViewById(R.id.audio_player_track_name);
        // Artist name
        mArtistName = findViewById(R.id.audio_player_artist_name);
        // Album art
        mAlbumArt = findViewById(R.id.audio_player_album_art);
        // Small album art
        mAlbumArtSmall = findViewById(R.id.audio_player_switch_album_art);
        // Current time
        mCurrentTime = findViewById(R.id.audio_player_current_time);
        // Total time
        mTotalTime = findViewById(R.id.audio_player_total_time);
        // Used to show and hide the queue fragment
        mQueueSwitch = findViewById(R.id.audio_player_switch_queue);
        // Theme the queue switch icon
        mQueueSwitch.setImageResource(R.drawable.btn_switch_queue);//mResources.getDrawable("btn_switch_queue"));
        // Progress
        mProgress = findViewById(android.R.id.progress);
        // Set the repeat listner for the previous button
        mPreviousButton.setRepeatListener(this);
        // Set the repeat listner for the next button
        mNextButton.setRepeatListener(this);
        // Update the progress
        mProgress.setOnSeekBarChangeListener(this);
        // set colors
        mShuffleButton.setColor(themeColor);
        mRepeatButton.setColor(themeColor);
        // set seek bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgress.setProgressTintList(ColorStateList.valueOf(themeColor));
        }
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        // Set the track name
        mTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mArtistName.setText(MusicUtils.getArtistName());
        // Set the total time
        mTotalTime.setText(MusicUtils.makeTimeString(this, (int) MusicUtils.duration() / 1000));
        // Set the album art
        mImageFetcher.loadCurrentArtwork(mAlbumArt);
        // Set the small artwork
        mImageFetcher.loadCurrentArtwork(mAlbumArtSmall);
        // Update the current time
        queueNextRefresh(1);
    }


    private long parseIdFromIntent(Intent intent, String longKey, String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return id;
    }

    /**
     * Checks whether the passed intent contains a playback request,
     * and starts playback if that's the case
     */
    private void startPlayback() {
        Intent intent = getIntent();
        if (intent != null && MusicUtils.isConnected()) {
            Uri uri = intent.getData();
            String mimeType = intent.getType();
            boolean handled = false;
            // open file
            if (uri != null && uri.toString().length() > 0) {
                MusicUtils.playFile(uri);
                handled = true;
            }
            // open playlist
            else if (Playlists.CONTENT_TYPE.equals(mimeType)) {
                long id = parseIdFromIntent(intent, "playlistId", "playlist");
                if (id >= 0) {
                    MusicUtils.playPlaylist(this, id);
                    handled = true;
                }
            }
            // open album
            else if (Albums.CONTENT_TYPE.equals(mimeType)) {
                long id = parseIdFromIntent(intent, "albumId", "album");
                if (id >= 0) {
                    int position = intent.getIntExtra("position", 0);
                    MusicUtils.playAlbum(this, id, position);
                    handled = true;
                }
            }
            // open artist
            else if (Artists.CONTENT_TYPE.equals(mimeType)) {
                long id = parseIdFromIntent(intent, "artistId", "artist");
                if (id >= 0) {
                    int position = intent.getIntExtra("position", 0);
                    MusicUtils.playArtist(this, id, position);
                    handled = true;
                }
            }
            // clear intent
            if (handled) {
                // Make sure to process intent only once
                setIntent(new Intent());
                // Refresh the queue
                getFragment().refresh();
            }
        }
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mPlayPauseButton.updateState();
        // Set the shuffle image
        mShuffleButton.updateShuffleState();
        // Set the repeat image
        mRepeatButton.updateRepeatState();
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(long delay) {
        if (!mIsPaused) {
            Message message = mTimeHandler.obtainMessage(MSG_ID);
            mTimeHandler.removeMessages(MSG_ID);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanBackward(int repcnt, long delta) {
        if (MusicUtils.isConnected()) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(this);
                long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanForward(int repcnt, long delta) {
        if (!MusicUtils.isConnected()) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    private void refreshCurrentTimeText(long pos) {
        mCurrentTime.setText(MusicUtils.makeTimeString(this, (int) pos / 1000));
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (!MusicUtils.isConnected()) {
            return 500;
        }
        try {
            long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            if (pos >= 0 && MusicUtils.duration() > 0) {
                refreshCurrentTimeText(pos);
                int progress = (int) (1000 * pos / MusicUtils.duration());
                mProgress.setProgress(progress);

                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            long smoothrefreshtime = MusicUtils.duration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }

    /**
     * @param v     The view to animate
     * @param alpha The alpha to apply
     */
    private void fade(View v, float alpha) {
        ObjectAnimator fade = ObjectAnimator.ofFloat(v, "alpha", alpha);
        fade.setInterpolator(AnimationUtils.loadInterpolator(this, android.R.anim.accelerate_decelerate_interpolator));
        fade.setDuration(400);
        fade.start();
    }

    /**
     * Called to show the album art and hide the queue
     */
    private void showAlbumArt() {
        mViewPager.setVisibility(View.INVISIBLE);
        mAlbumArtSmall.setVisibility(View.GONE);
        mQueueSwitch.setVisibility(View.VISIBLE);
        // Fade out the pager container
        fade(mViewPager, 0f);
        // Fade in the album art
        fade(mAlbumArt, 1f);
    }

    /**
     * Called to hide the album art and show the queue
     */
    public void hideAlbumArt() {
        mViewPager.setVisibility(View.VISIBLE);
        mQueueSwitch.setVisibility(View.GONE);
        mAlbumArtSmall.setVisibility(View.VISIBLE);
        // Fade out the artwork
        fade(mAlbumArt, 0f);
        // Fade in the pager container
        fade(mViewPager, 1f);
    }

    /**
     * Used to shared what the user is currently listening to
     */
    private void shareCurrentTrack() {
        String path = MusicUtils.getPlaybackFilePath();
        if (path != null) {
            try {
                File file = new File(path);
                Uri fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("audio/*");
                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * @return Queue Fragment
     */
    private BrowserCallback getFragment() {
        return (BrowserCallback) mPagerAdapter.getFragment(0);
    }

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private WeakReference<AudioPlayerActivity> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        public TimeHandler(AudioPlayerActivity player) {
            super();
            mAudioPlayer = new WeakReference<>(player);
        }


        @Override
        public void handleMessage(Message msg) {
            AudioPlayerActivity activity = mAudioPlayer.get();
            if (msg.what == MSG_ID && activity != null) {
                long next = activity.refreshCurrentTime();
                activity.queueNextRefresh(next);
            }
        }
    }
}