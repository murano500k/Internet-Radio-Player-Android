package com.stc.radio.player;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Custom RecyclerView that doesn't intercept the scrolling from its parent
 * and touch events to it's children. This is used for the trailers and reviews list.
 */
public class CustomTouchRecyclerView extends RecyclerView {

    public CustomTouchRecyclerView(Context context) {
        super(context);
    }

    public CustomTouchRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTouchRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }
}
