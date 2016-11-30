package com.example.android.uamp.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by artem on 11/30/16.
 */
@Table(name = "DBUserPrefsItem", id = "_id")
public class DBUserPrefsItem extends Model {

	@Column(name = "MediaId")
	String mediaId;

	@Column(name = "Favorite")
	boolean favorite;

	public DBUserPrefsItem(String mediaId, boolean favorite, long priority) {
		this.favorite = favorite;
		this.mediaId = mediaId;
		this.priority = priority;
	}

	public String getMediaId() {

		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

	@Column(name = "Priority")
	long priority;

	public long getPriority() {
		return priority;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

}
