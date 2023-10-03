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

import org.nuclearfog.apollo.Config;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;
import org.nuclearfog.apollo.ui.views.dragdrop.DragSortListView;
import org.nuclearfog.apollo.ui.views.dragdrop.VerticalScrollListener;
import org.nuclearfog.apollo.utils.FragmentViewModel;

/**
 * this fragment hosts a {@link ListView} with a {@link ProfileTabCarousel} header
 *
 * @author nuclearfog
 */
public abstract class ProfileFragment extends Fragment implements OnItemClickListener, Observer<String>,
		DragSortListView.DropListener, DragSortListView.RemoveListener, DragSortListView.DragScrollProfile {

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
	public void onCreate(@Nullable Bundle savedInstanceState) {
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
		mList.setDropListener(this);
		mList.setRemoveListener(this);
		mList.setDragScrollProfile(this);
		// To help make scrolling smooth
		mList.setOnScrollListener(new VerticalScrollListener(null, mProfileTabCarousel, 0));
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel.getSelectedItem().observe(getViewLifecycleOwner(), this);
		init();
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
	public void onDestroyView() {
		super.onDestroyView();
		viewModel.getSelectedItem().removeObserver(this);
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
	public float getSpeed(float w) {
		return Config.DRAG_DROP_MAX_SPEED * w;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChanged(String action) {
		if (action.equals(FragmentViewModel.REFRESH)) {
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void scrollToTop() {
		mList.setSelection(0);
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
	 *
	 */
	protected abstract void refresh();

	/**
	 * initializes the fragment
	 */
	protected abstract void init();

	/**
	 * called when an item was clicked
	 *
	 * @param v   clicked View
	 * @param pos position of the item
	 * @param id  ID of the item
	 */
	protected abstract void onItemClick(View v, int pos, long id);
}