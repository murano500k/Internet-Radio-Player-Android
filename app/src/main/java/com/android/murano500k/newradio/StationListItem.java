package com.android.murano500k.newradio;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Created by artem on 6/8/16.
 */
public class StationListItem extends CardView implements Checkable {

    public StationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StationListItem(Context context) {
        super(context);
    }

    private boolean checked = false;
    private boolean fav = false;

    private TextView name;
    private ImageView favStar;


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView)findViewById(R.id.text);
        favStar = (ImageView) findViewById(R.id.imgFav);
    }
    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        if(checked) {
            this.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            name.setTextColor(getResources().getColor(R.color.colorTextAccent));
        }
        else {
            this.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            name.setTextColor(getResources().getColor(R.color.colorText));
        }

    }

    public boolean isFav() {
        return fav;
    }

    public void setFav(boolean fav) {
        this.fav = fav;
        if(fav) {
            favStar.setImageResource(R.drawable.ic_fav_enabled);
        }
        else {
            favStar.setImageResource(R.drawable.ic_fav_disabled);
        }
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}
