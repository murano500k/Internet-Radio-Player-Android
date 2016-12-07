package com.stc.radio.player.db;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.stc.radio.player.model.MusicProviderSource;

/**
 * Created by artem on 10/22/16.
 */
@Table(name = "DBMediaItem", id = "_id")
public class DBMediaItem extends Model {

	@Column(name = "MediaId")
	String mediaId;

	@Column(name = "Source")
	String source;

	@Column(name = "Title")
	String title;

	@Column(name = "IconUri")
	String iconUri;

	@Column(name = "PlayedTimes")
	int playedTimes;

	@Column(name = "Favorite")
	boolean favorite;






	public String getMediaId() {
		return mediaId;
	}

	public String getSource() {
		return source;
	}

	public String getTitle() {
		return title;
	}

	public String getIconUri() {
		return iconUri;
	}

	public int getPlayedTimes() {
		return playedTimes;
	}

	public void setPlayedTimes(int playedTimes) {
		this.playedTimes = playedTimes;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

	public DBMediaItem(MediaMetadataCompat metadata) {
		this.mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
		this.source = metadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
		this.title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
		this.iconUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ART_URI);

		playedTimes=0;

		favorite=false;
	}


	public DBMediaItem() {
	}

	public DBMediaItem(String mediaId, String title, String source, String iconUri){
		this.mediaId = mediaId;
		this.title = title;
		this.source = source;
		this.iconUri = iconUri;
	}
}
