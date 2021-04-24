/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;

import androidx.legacy.content.WakefulBroadcastReceiver;

import com.andrew.apollo.ui.activities.HomeActivity;

import static androidx.legacy.content.WakefulBroadcastReceiver.startWakefulService;

/**
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: next track
 * Triple press: previous track
 * Long press: voice search
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;

    private static final int LONG_PRESS_DELAY = 1000;
    private static final int DOUBLE_CLICK = 800;

    private static WakeLock mWakeLock = null;
    private static int mClickCounter = 0;
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;


    private static void startService(Context context, String command) {
        Intent i = new Intent(context, MusicPlaybackService.class);
        i.setAction(MusicPlaybackService.SERVICECMD);
        i.putExtra(MusicPlaybackService.CMDNAME, command);
        i.putExtra(MusicPlaybackService.FROM_MEDIA_BUTTON, true);
        startWakefulService(context, i);
    }


    @SuppressLint("InvalidWakeLockTag")
    private static void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
        if (mWakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Apollo headset button");
            mWakeLock.setReferenceCounted(false);
        }
        // Make sure we don't indefinitely hold the wake lock under any circumstances
        mWakeLock.acquire(10000);
        MessageHandler.mHandler.sendMessageDelayed(msg, delay);
    }


    private static void releaseWakeLockIfHandlerIdle() {
        if (MessageHandler.mHandler.hasMessages(MSG_LONGPRESS_TIMEOUT)
                || MessageHandler.mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            return;
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @SuppressWarnings("deprecation")
    public static void completeWakefulIntent(Intent i) { // TODO replace it
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            WakefulBroadcastReceiver.completeWakefulIntent(i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            startService(context, MusicPlaybackService.CMDPAUSE);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MusicPlaybackService.CMDSTOP;
                    break;

                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MusicPlaybackService.CMDTOGGLEPAUSE;
                    break;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MusicPlaybackService.CMDNEXT;
                    break;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MusicPlaybackService.CMDPREVIOUS;
                    break;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MusicPlaybackService.CMDPAUSE;
                    break;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MusicPlaybackService.CMDPLAY;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if (MusicPlaybackService.CMDTOGGLEPAUSE.equals(command)
                                || MusicPlaybackService.CMDPLAY.equals(command)) {
                            if (mLastClickTime != 0 && eventtime - mLastClickTime > LONG_PRESS_DELAY) {
                                acquireWakeLockAndSendMessage(context, MessageHandler.mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context), 0);
                            }
                        }
                    } else if (event.getRepeatCount() == 0) {
                        // Only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it
                        // a command.
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (eventtime - mLastClickTime >= DOUBLE_CLICK) {
                                mClickCounter = 0;
                            }
                            mClickCounter++;
                            MessageHandler.mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);
                            Message msg = MessageHandler.mHandler.obtainMessage(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context);

                            long delay = mClickCounter < 3 ? DOUBLE_CLICK : 0;
                            if (mClickCounter >= 3) {
                                mClickCounter = 0;
                            }
                            mLastClickTime = eventtime;
                            acquireWakeLockAndSendMessage(context, msg, delay);
                        } else {
                            startService(context, command);
                        }
                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    MessageHandler.mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
                releaseWakeLockIfHandlerIdle();
            }
        }
    }

    /**
     *
     */
    static class MessageHandler extends Handler {

        static final MessageHandler mHandler = new MessageHandler();

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        Context context = (Context) msg.obj;
                        Intent i = new Intent();
                        i.setClass(context, HomeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;

                case MSG_HEADSET_DOUBLE_CLICK_TIMEOUT:
                    int clickCount = msg.arg1;
                    String command;
                    switch (clickCount) {
                        case 1:
                            command = MusicPlaybackService.CMDTOGGLEPAUSE;
                            break;
                        case 2:
                            command = MusicPlaybackService.CMDNEXT;
                            break;
                        case 3:
                            command = MusicPlaybackService.CMDPREVIOUS;
                            break;
                        default:
                            command = null;
                            break;
                    }
                    if (command != null) {
                        Context context = (Context) msg.obj;
                        startService(context, command);
                    }
                    break;
            }
            releaseWakeLockIfHandlerIdle();
        }
    }
}