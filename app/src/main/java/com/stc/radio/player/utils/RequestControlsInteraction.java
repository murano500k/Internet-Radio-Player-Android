package com.stc.radio.player.utils;

/**
 * Created by artem on 10/11/16.
 */
public class RequestControlsInteraction {
	private int which;

	public RequestControlsInteraction(int which) {
		this.which = which;
	}

	public int which() {
		return which;
	}
}
