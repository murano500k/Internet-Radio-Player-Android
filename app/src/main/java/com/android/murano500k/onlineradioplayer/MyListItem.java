package com.android.murano500k.onlineradioplayer;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.TextView;


/**
 * Created by artem on 6/8/16.
 */
public class MyListItem extends CardView implements Checkable {

    public MyListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyListItem(Context context) {
        super(context);
    }

    private boolean checked = false;

    private TextView name;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView)findViewById(R.id.text);
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

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}
