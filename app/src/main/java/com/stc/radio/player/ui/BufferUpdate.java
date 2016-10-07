package com.stc.radio.player.ui;

/**
 * Created by artem on 9/29/16.
 */

public class BufferUpdate {
	private int audioBufferCapacityMs;
	private int audioBufferSizeMs;
	private boolean isPlaying;

	public int getAudioBufferCapacityMs() {
		return audioBufferCapacityMs;
	}

	public int getAudioBufferSizeMs() {
		return audioBufferSizeMs;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public BufferUpdate(int audioBufferSizeMs, int audioBufferCapacityMs, boolean isPlaying) {
		this.audioBufferCapacityMs = audioBufferCapacityMs;
		this.audioBufferSizeMs = audioBufferSizeMs;
		this.isPlaying=isPlaying;
	}
}
