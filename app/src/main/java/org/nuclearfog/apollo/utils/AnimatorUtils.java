package org.nuclearfog.apollo.utils;

import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.AnimationUtils;

/**
 * Animation utility class used to animate views
 *
 * @author nuclearfog
 */
public final class AnimatorUtils {

	private static final PropertyValuesHolder FADE_IN = PropertyValuesHolder.ofFloat("alpha", 1f);
	private static final PropertyValuesHolder FADE_OUT = PropertyValuesHolder.ofFloat("alpha", 0f);

	private static ObjectAnimator pulse;

	/* This class is never initiated */
	private AnimatorUtils() {
	}

	/**
	 * fade view in or out
	 *
	 * @param view    The view to animate
	 * @param visible true to fade in, false to fade out
	 */
	public static void fade(View view, boolean visible) {
		PropertyValuesHolder holder = visible ? FADE_IN : FADE_OUT;
		ObjectAnimator fade = ObjectAnimator.ofPropertyValuesHolder(view, holder);
		fade.setInterpolator(AnimationUtils.loadInterpolator(view.getContext(), android.R.anim.accelerate_decelerate_interpolator));
		fade.setDuration(400);
		fade.start();
	}

	/**
	 * fade in and out periodically a view
	 *
	 * @param view   view to animate
	 * @param enable true to start animation, false to stop running animation
	 */
	public static void pulse(View view, boolean enable) {
		if (enable) {
			if (pulse == null) {
				pulse = ObjectAnimator.ofPropertyValuesHolder(view, FADE_IN, FADE_OUT);
				pulse.setRepeatCount(ObjectAnimator.INFINITE);
				pulse.setRepeatMode(ObjectAnimator.REVERSE);
				pulse.setDuration(500);
				pulse.start();
			}
		} else if (pulse != null) {
			pulse.cancel();
			pulse = null;
			fade(view, true);
		}
	}

	/**
	 *
	 */
	public static void translate(View view, float y, int duration, AnimatorListener listener) {
		ObjectAnimator tabCarouselAnimator = ObjectAnimator.ofFloat(view, "y", y);
		tabCarouselAnimator.addListener(listener);
		tabCarouselAnimator.setInterpolator(AnimationUtils.loadInterpolator(view.getContext(), android.R.anim.accelerate_decelerate_interpolator));
		tabCarouselAnimator.setDuration(duration);
		tabCarouselAnimator.start();
	}
}