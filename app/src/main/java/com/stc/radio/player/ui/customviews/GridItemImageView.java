package com.stc.radio.player.ui.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Custom ImageView so that it is the optimal height for a movie poster
 */
public class GridItemImageView extends ImageView {
    //private static final float POSTER_WIDTH_RATIO_TO_HEIGHT = 2f/3;
    private static final float POSTER_WIDTH_RATIO_TO_HEIGHT = 1;
    public GridItemImageView(Context context) {
        super(context);
    }

    public GridItemImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridItemImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GridItemImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //The height should be 33% longer than the width.
        float newMeasuredHeight = getMeasuredWidth() / POSTER_WIDTH_RATIO_TO_HEIGHT;
        setMeasuredDimension(getMeasuredWidth(), (int)newMeasuredHeight);
    }
}
