package com.android.murano500k.newradio.events;

/**
 * Created by artem on 8/18/16.
 */

public class EventError {
		public static final String ERROR_FOCUS_NOT_GRANTED = "ERROR_FOCUS_NOT_GRANTED";
		public static final String ERROR_LOCKED = "ERROR_LOCKED";
		public static final String ERROR_NO_URL = "ERROR_NO_URL";
	public final String reason;



	public EventError(String reason) {
		this.reason = reason;
	}
}

