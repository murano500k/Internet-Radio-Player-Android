package com.android.murano500k.newradio.events;

/**
 * Created by artem on 8/18/16.
 */

public class EventBufferUpdate {
	public final int progressValue;
	public final boolean intermediate;


	public EventBufferUpdate(int progressValue, boolean intermediate) {
		this.progressValue = progressValue;
		this.intermediate = intermediate;
	}
}
