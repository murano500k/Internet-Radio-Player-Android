package com.android.murano500k.newradio.ui;

/**
 * Created by artem on 9/5/16.
 */

public class SleepEvent {
	public enum SLEEP_ACTION {
		CANCEL,
		UPDATE,
		FINISH
	}
	private int seconds;
	private SLEEP_ACTION sleepAction;

	public SleepEvent( SLEEP_ACTION sleepAction,int seconds) {
		this.seconds = seconds;
		this.sleepAction = sleepAction;
	}

	public SLEEP_ACTION getSleepAction() {
		return sleepAction;
	}

	public void setSleepAction(SLEEP_ACTION sleepAction) {
		this.sleepAction = sleepAction;
	}

	public int getSeconds() {
		return seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}
}
