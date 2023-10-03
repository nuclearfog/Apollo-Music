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

package org.nuclearfog.apollo.ui.adapters.listview.holder;

import android.view.View;
import android.widget.AbsListView.RecyclerListener;

import org.nuclearfog.apollo.R;

/**
 * A @ {@link RecyclerListener} for {@link MusicHolder}'s views.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecycleHolder implements RecyclerListener {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMovedToScrapHeap(View view) {
		MusicHolder holder;
		if (view.getTag() instanceof MusicHolder) {
			holder = (MusicHolder) view.getTag();
		} else {
			holder = new MusicHolder(view);
			view.setTag(holder);
		}

		// set default artwork
		if (holder.mImage != null) {
			holder.mImage.setImageResource(R.drawable.default_artwork);
		}

		// Release mLineOne's reference
		if (holder.mLineOne != null) {
			holder.mLineOne.setText("");
		}

		// Release mLineTwo's reference
		if (holder.mLineTwo != null) {
			holder.mLineTwo.setText("");
		}

		// Release mLineThree's reference
		if (holder.mLineThree != null) {
			holder.mLineThree.setText("");
		}
	}
}