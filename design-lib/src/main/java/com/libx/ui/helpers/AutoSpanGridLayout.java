package com.libx.ui.helpers;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AutoSpanGridLayout extends GridLayoutManager {

    private final float viewWidth;

    public AutoSpanGridLayout(Context context, float viewWidth) {
        super(context, 2);
        this.viewWidth = viewWidth;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        float total = getWidth() - getPaddingRight() - getPaddingLeft();
        int spanCount = Math.max(1, (int) (total / viewWidth));
        setSpanCount(spanCount);
        super.onLayoutChildren(recycler, state);
    }
}