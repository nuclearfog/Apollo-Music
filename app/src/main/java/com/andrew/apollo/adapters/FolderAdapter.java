package com.andrew.apollo.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.MusicHolder.DataHolder;

import java.io.File;
import java.util.ArrayList;

import static android.view.View.GONE;

/**
 * decompiled from Apollo.APK version 1.6
 * <p>
 * this adapter creates views with music folder information
 */
public class FolderAdapter extends ArrayAdapter<File> {

    private final int mLayoutId;

    private ArrayList<DataHolder> mData = new ArrayList<>();

    /**
     * @param context application context
     * @param redId   ID of the view item
     */
    public FolderAdapter(Context context, @LayoutRes int redId) {
        super(context, redId);
        mLayoutId = redId;
    }


    @NonNull
    @Override
    public View getView(int paramInt, @Nullable View paramView, @NonNull ViewGroup paramViewGroup) {
        MusicHolder holder;
        if (paramView == null) {
            paramView = LayoutInflater.from(getContext()).inflate(mLayoutId, paramViewGroup, false);
            float textSize = getContext().getResources().getDimension(R.dimen.text_size_large);
            holder = new MusicHolder(paramView);
            holder.mImage.setVisibility(GONE);
            holder.mLineTwo.setVisibility(GONE);
            holder.mLineThree.setVisibility(GONE);
            holder.mLineOne.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            if (holder.mBackground != null)
                holder.mBackground.setVisibility(GONE);
            paramView.setTag(holder);
        } else {
            holder = (MusicHolder) paramView.getTag();
        }
        DataHolder dataHolder1 = mData.get(paramInt);
        holder.mLineOne.setText(dataHolder1.mLineOne);
        return paramView;
    }


    @Override
    public void clear() {
        super.clear();
        mData.clear();
    }

    /**
     * build data cache
     */
    public void buildCache() {
        mData.clear();
        mData.ensureCapacity(getCount());
        for (int i = 0; i < getCount(); i++) {
            File file = getItem(i);
            if (file != null) {
                DataHolder holder = new DataHolder();
                holder.mLineOne = file.getName();
                mData.add(holder);
            }
        }
    }
}