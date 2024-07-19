package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.model.Music;

import java.util.LinkedList;

/**
 * This adapter supports fast scroll with alphabetical thumb
 *
 * @author nuclearfog
 */
public abstract class AlphabeticalAdapter<T> extends ArrayAdapter<T> implements SectionIndexer {

	private LinkedList<String> caps = new LinkedList<>();

	private int columns = 1;


	protected AlphabeticalAdapter(Context context, @LayoutRes int layoutId) {
		super(context, layoutId);
	}

	/**
	 * @param columns number of items to show in one row
	 */
	protected AlphabeticalAdapter(Context context, int columns, @LayoutRes int layoutId) {
		super(context, layoutId);
		this.columns = columns;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(@Nullable T item) {
		super.add(item);
		if (item instanceof Music) {
			Music music = (Music) item;
			if (getCount() % columns == 0) {
				if (music.getName().length() > 1) {
					String cap = music.getName().substring(0, 1).toUpperCase();
					caps.addLast(cap);
				} else {
					caps.addLast("");
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String[] getSections() {
		return caps.toArray(new String[0]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int getPositionForSection(int sectionIndex) {
		return sectionIndex * columns;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int getSectionForPosition(int position) {
		return position / columns;
	}
}