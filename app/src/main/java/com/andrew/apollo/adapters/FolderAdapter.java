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
import com.andrew.apollo.ui.MusicHolder;

import java.io.File;

import static android.view.View.GONE;

public class FolderAdapter extends ArrayAdapter<File> {

    private final int mLayoutId;
    private MusicHolder.DataHolder[] mData;


    public FolderAdapter(Context paramContext, @LayoutRes int paramInt) {
        super(paramContext, paramInt);
        mLayoutId = paramInt;
    }


    public void buildCache() {
        mData = new MusicHolder.DataHolder[getCount()];
        for (int i = 0; i < getCount(); i++) {
            File file = getItem(i);
            if (file != null) {
                this.mData[i] = new MusicHolder.DataHolder();
                this.mData[i].mLineOne = file.getName();
            }
        }
    }


    @NonNull
    public View getView(int paramInt, @Nullable View paramView, @NonNull ViewGroup paramViewGroup) {
        MusicHolder holder;
        if (paramView == null) {
            paramView = LayoutInflater.from(getContext()).inflate(mLayoutId, paramViewGroup, false);
            holder = new MusicHolder(paramView);
            holder.mImage.get().setVisibility(GONE);
            holder.mLineTwo.get().setVisibility(GONE);
            holder.mLineThree.get().setVisibility(GONE);
            holder.mLineOne.get().setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.text_size_large));
            paramView.setTag(holder);
        } else {
            holder = (MusicHolder) paramView.getTag();
        }
        MusicHolder.DataHolder dataHolder1 = this.mData[paramInt];
        holder.mLineOne.get().setText(dataHolder1.mLineOne);
        return paramView;
    }


    public int getViewTypeCount() {
        return 1;
    }


    public boolean hasStableIds() {
        return false;
    }


    public void unload() {
        clear();
        this.mData = null;
    }
}