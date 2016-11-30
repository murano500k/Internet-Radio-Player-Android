package com.stc.radio.player.model;

/**
 * Created by artem on 10/23/16.
 */
public class MyMetadata {
	private String text;

	public MyMetadata(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text+"";
	}
}
