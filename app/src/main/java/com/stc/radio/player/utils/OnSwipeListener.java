package com.stc.radio.player.utils;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by artem on 10/3/16.
 */

public class OnSwipeListener implements View.OnTouchListener {
	private int min_distance = 100;
	private float downX, downY, upX, upY;
	View v;

	public OnSwipeListener(View v) {
		this.v = v;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		this.v = v;
		switch(event.getAction()) { // Check vertical and horizontal touches
			case MotionEvent.ACTION_DOWN: {
				downX = event.getX();
				downY = event.getY();
				return true;
			}
			case MotionEvent.ACTION_UP: {
				upX = event.getX();
				upY = event.getY();

				float deltaX = downX - upX;
				float deltaY = downY - upY;

				//HORIZONTAL SCROLL
				if (Math.abs(deltaX) > Math.abs(deltaY)) {
					if (Math.abs(deltaX) > min_distance) {
						// left or right
						if (deltaX < 0) {
							this.onLeftToRightSwipe();
							return true;
						}
						if (deltaX > 0) {
							this.onRightToLeftSwipe();
							return true;
						}
					} else {
						//not long enough swipe...
						return false;
					}
				}
				//VERTICAL SCROLL
				else {
					if (Math.abs(deltaY) > min_distance) {
						// top or down
						if (deltaY < 0) {
							this.onTopToBottomSwipe();
							return true;
						}
						if (deltaY > 0) {
							this.onBottomToTopSwipe();
							return true;
						}
					} else {
						//not long enough swipe...
						return false;
					}
				}
				return false;
			}
		}
		return false;
	}

	public void onLeftToRightSwipe(){
		Toast.makeText(v.getContext(),"left to right",
				Toast.LENGTH_SHORT).show();
	}

	public void onRightToLeftSwipe() {
		Toast.makeText(v.getContext(),"right to left",
				Toast.LENGTH_SHORT).show();
	}

	public void onTopToBottomSwipe() {
		Toast.makeText(v.getContext(),"top to bottom",
				Toast.LENGTH_SHORT).show();
	}

	public void onBottomToTopSwipe() {
		Toast.makeText(v.getContext(),"bottom to top",
				Toast.LENGTH_SHORT).show();
	}
}