package org.nuclearfog.apollo.ui.fragments.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;
import org.nuclearfog.apollo.ui.views.dragdrop.DragSortListView;
import org.nuclearfog.apollo.ui.views.dragdrop.DragSortListView.DragScrollProfile;
import org.nuclearfog.apollo.ui.views.dragdrop.DragSortListView.ItemChangeListener;
import org.nuclearfog.apollo.ui.views.dragdrop.VerticalScrollController;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.FragmentViewModel;

/**
 * this fragment hosts a {@link ListView} with a {@link ProfileTabCarousel} header
 *
 * @author nuclearfog
 */
public abstract class ProfileFragment extends Fragment implements OnItemClickListener, Observer<String>, ItemChangeListener, DragScrollProfile {

	private static final String TAG = "ProfileFragment";

	/**
	 * notification used to reload content of the fragment
	 */
	public static final String REFRESH = TAG + ".REFRESH";

	/**
	 * notification used to scroll to the current track
	 */
	public static final String SHOW_CURRENT = TAG + ".CURRENT_TRACK";

	/**
	 * notification used to scroll the list to top
	 */
	public static final String SCROLL_TOP = TAG + ".SCROLL_TOP";

	/**
	 * list view of this fragment
	 */
	private DragSortListView mList;

	/**
	 * textview shown when the list is empty
	 */
	private TextView emptyInfo;

	/**
	 * Profile header
	 */
	private ProfileTabCarousel mProfileTabCarousel;

	/**
	 * viewmodel used to communicate with this fragment
	 */
	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onAttach(@NonNull Context context) {
		super.onAttach(context);
		Activity activity = (Activity) context;
		mProfileTabCarousel = activity.findViewById(R.id.activity_profile_base_tab_carousel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// The View for the fragment's UI
		View rootView = inflater.inflate(R.layout.list_base, container, false);
		// empty info
		emptyInfo = rootView.findViewById(R.id.list_base_empty_info);
		// Initialize the list
		mList = rootView.findViewById(R.id.list_base);
		// Set empty list info
		mList.setEmptyView(emptyInfo);
		// Release any references to the recycled Views
		mList.setRecyclerListener(new RecycleHolder());
		// disable fast scroll
		mList.setFastScrollEnabled(false);
		// Listen for ContextMenus to be created
		mList.setOnCreateContextMenuListener(this);
		// Play the selected song
		mList.setOnItemClickListener(this);
		mList.setItemChangeListener(this);
		mList.setDragScrollProfile(this);
		// To help make scrolling smooth
		mList.setOnScrollListener(new VerticalScrollController(null, mProfileTabCarousel, 0));
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		init(getArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onDestroyView() {
		viewModel.getSelectedItem().removeObserver(this);
		super.onDestroyView();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		onItemClick(view, position, id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onChanged(String action) {
		if (isAdded()) {
			switch (action) {
				case SHOW_CURRENT:
					moveToCurrent();
					break;

				case REFRESH:
					refresh();
					break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getSpeed(float w) {
		return Constants.DRAG_DROP_MAX_SPEED * w;
	}

	/**
	 * sets text when the list is empty
	 *
	 * @param res string resource
	 */
	protected void setEmptyText(@StringRes int res) {
		emptyInfo.setText(res);
	}

	/**
	 *
	 */
	protected void setAdapter(ListAdapter adapter) {
		mList.setAdapter(adapter);
	}

	/**
	 * scroll to list position
	 *
	 * @param position position to scroll
	 */
	public void scrollTo(int position) {
		if (position >= 0 && position < mList.getCount()) {
			mList.smoothScrollToPosition(position);
		}
	}

	/**
	 * initializes the fragment
	 */
	protected abstract void init(@Nullable Bundle args);

	/**
	 * called when an item was clicked
	 *
	 * @param v   clicked View
	 * @param pos position of the item
	 * @param id  ID of the item
	 */
	protected abstract void onItemClick(View v, int pos, long id);

	/**
	 * scroll list to current track
	 */
	protected abstract void moveToCurrent();

	/**
	 * reload list content
	 */
	protected abstract void refresh();
}