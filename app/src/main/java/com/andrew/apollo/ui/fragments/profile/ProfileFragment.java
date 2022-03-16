package com.andrew.apollo.ui.fragments.profile;

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

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.dragdrop.DragSortListView;
import com.andrew.apollo.dragdrop.DragSortListView.DragScrollProfile;
import com.andrew.apollo.dragdrop.DragSortListView.DropListener;
import com.andrew.apollo.dragdrop.DragSortListView.RemoveListener;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.activities.ProfileActivity.FragmentCallback;
import com.andrew.apollo.views.ProfileTabCarousel;
import com.andrew.apollo.views.VerticalScrollListener;

/**
 * this fragment hosts a {@link ListView} with a {@link ProfileTabCarousel} header
 *
 * @author nuclearfog
 */
public abstract class ProfileFragment extends Fragment implements OnItemClickListener,
        DropListener, RemoveListener, FragmentCallback, DragScrollProfile {

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
    public abstract void refresh();

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