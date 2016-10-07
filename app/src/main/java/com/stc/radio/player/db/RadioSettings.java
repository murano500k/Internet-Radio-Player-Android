package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by artem on 9/30/16.
 */
@Table(name = "RadioSettings", id = "_id")
public class RadioSettings extends Model {
	@Column(name = "Shuffle")
	public boolean shuffle;

	@Column(name = "BufferSize")
	public int bufferSize;
	@Column(name = "DecodeSize")
	public int decodeSize;

	public RadioSettings(boolean shuffle, int bufferSize, int decodeSize) {
		this.shuffle = shuffle;
		this.bufferSize = bufferSize;
		this.decodeSize = decodeSize;

	}
	public RadioSettings() {
	}


	public int getDecodeSize() {
		return decodeSize;
	}

	public void setDecodeSize(int decodeSize) {
		this.decodeSize = decodeSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public boolean isShuffle() {
		return shuffle;
	}

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}
}

