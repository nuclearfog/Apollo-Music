package com.andrew.apollo.utils;

import com.andrew.apollo.MusicPlaybackService;

import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

/**
 *
 */
public class Shuffler {

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
	 * @param interval The duration the queue
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
		if (!mHistoryOfNumbers.isEmpty() && mHistoryOfNumbers.size() >= MusicPlaybackService.MAX_HISTORY_SIZE) {
			for (int i = 0; i < Math.max(1, MusicPlaybackService.MAX_HISTORY_SIZE / 2); i++) {
				mPreviousNumbers.remove(mHistoryOfNumbers.removeFirst());
			}
		}
	}
}
