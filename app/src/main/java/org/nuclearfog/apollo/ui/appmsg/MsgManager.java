/*
 * Copyright 2012 Evgeny Shishkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nuclearfog.apollo.ui.appmsg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Evgeny Shishkin
 */
class MsgManager extends Handler {

	private static final int MESSAGE_DISPLAY = 0xc2007;
	private static final int MESSAGE_ADD_VIEW = 0xc20074dd;
	private static final int MESSAGE_REMOVE = 0xc2007de1;

	private static final MsgManager INSTANCE = new MsgManager();

	private Queue<AppMsg> msgQueue = new LinkedList<>();
	private Animation inAnimation, outAnimation;

	/**
	 *
	 */
	private MsgManager() {
		super(Looper.getMainLooper());
	}

	/**
	 * @return The currently used instance of the {@link MsgManager}.
	 */
	static synchronized MsgManager getInstance() {
		return INSTANCE;
	}


	@Override
	public void handleMessage(Message msg) {
		AppMsg appMsg;
		switch (msg.what) {
			case MESSAGE_DISPLAY:
				displayMsg();
				break;

			case MESSAGE_ADD_VIEW:
				appMsg = (AppMsg) msg.obj;
				addMsgToView(appMsg);
				break;

			case MESSAGE_REMOVE:
				appMsg = (AppMsg) msg.obj;
				removeMsg(appMsg);
				break;

			default:
				super.handleMessage(msg);
				break;
		}
	}

	/**
	 * Inserts a {@link AppMsg} to be displayed.
	 */
	void add(AppMsg appMsg) {
		msgQueue.add(appMsg);
		if (inAnimation == null) {
			inAnimation = AnimationUtils.loadAnimation(appMsg.getContext(), android.R.anim.fade_in);
		}
		if (outAnimation == null) {
			outAnimation = AnimationUtils.loadAnimation(appMsg.getContext(), android.R.anim.fade_out);
		}
		displayMsg();
	}

	/**
	 * Displays the next {@link AppMsg} within the queue.
	 */
	private void displayMsg() {
		if (msgQueue.isEmpty()) {
			return;
		}
		// First peek whether the AppMsg is being displayed.
		AppMsg appMsg = msgQueue.peek();
		if (appMsg != null) {
			// If the activity is null we throw away the AppMsg.
			if (appMsg.getContext() == null) {
				msgQueue.poll();
			}
			Message msg;
			if (!appMsg.isShowing()) {
				// Display the AppMsg
				msg = obtainMessage(MESSAGE_ADD_VIEW);
				msg.obj = appMsg;
				sendMessage(msg);
			} else {
				msg = obtainMessage(MESSAGE_DISPLAY);
				sendMessageDelayed(msg, appMsg.getDuration() + inAnimation.getDuration() + outAnimation.getDuration());
			}
		}
	}

	/**
	 * Removes the {@link AppMsg}'s view after it's display duration.
	 *
	 * @param appMsg The {@link AppMsg} added to a {@link ViewGroup} and should be removed.s
	 */
	private void removeMsg(AppMsg appMsg) {
		ViewGroup parent = ((ViewGroup) appMsg.getView().getParent());
		if (parent != null) {
			appMsg.getView().startAnimation(outAnimation);
			// Remove the AppMsg from the queue.
			msgQueue.poll();
			// Remove the AppMsg from the view's parent.
			parent.removeView(appMsg.getView());
			Message msg = obtainMessage(MESSAGE_DISPLAY);
			sendMessage(msg);
		}
	}


	private void addMsgToView(AppMsg appMsg) {
		if (appMsg.getView().getParent() == null) {
			appMsg.addContentView(appMsg.getView(), appMsg.getLayoutParams());
		}
		appMsg.getView().startAnimation(inAnimation);
		Message msg = obtainMessage(MESSAGE_REMOVE);
		msg.obj = appMsg;
		sendMessageDelayed(msg, appMsg.getDuration());
	}
}