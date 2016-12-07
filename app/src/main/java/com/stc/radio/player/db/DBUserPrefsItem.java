package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by artem on 11/30/16.
 */
@Table(name = "DBUserPrefsItem")
public class DBUserPrefsItem extends Model {
	public DBUserPrefsItem() {
	}

	@Column(name = "MaxPlayedTimes")
	int maxPlayedTimes;
	@Column(name = "Shuffle")
	boolean shuffle;

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}

	public boolean isShuffle() {
		return shuffle;
	}

	public DBUserPrefsItem(int maxPlayedTimes) {
		this.maxPlayedTimes = maxPlayedTimes;
	}

	public int getMaxPlayedTimes() {
		return maxPlayedTimes;
	}

	public void setMaxPlayedTimes(int maxPlayedTimes) {
		this.maxPlayedTimes = maxPlayedTimes;
	}
}
