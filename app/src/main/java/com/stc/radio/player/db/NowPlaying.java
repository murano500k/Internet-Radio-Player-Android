package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by artem on 10/6/16.
 */
@Table(name = "NowPlaying", id = "_id")
public class NowPlaying extends Model {
	@Column(name = "Artist")
	public String artist;

	@Column(name = "Song")
	public String song;

	@Column(name = "url")
	public String url;

	@Column(name = "ActivePlaylistId")
	public long activePlaylistId;

	@Column(name = "UiState")
	public int uiState;


	public long getActivePlaylistId() {
		return activePlaylistId;
	}

	public NowPlaying withActivePlaylistId(long activePlaylistId) {
		this.activePlaylistId = activePlaylistId;
		return this;
	}

	public NowPlaying withArtist(String artist) {
		this.artist=artist;
		return this;
	}
	public NowPlaying withSong(String song) {
		this.song=song;
		return this;
	}
	public NowPlaying withUrl(String url) {
		/*if(this.url!=null && this.url.length()>0) {
			artist=song=null;
		}*/
		this.url=url;
		return this;
	}



	public String getArtist() {
		return artist;
	}

	public String getSong() {
		return song;
	}


	public String getUrl() {
		return url;
	}

	public int getUiState() {
		return uiState;
	}

	public NowPlaying() {
	}

	public NowPlaying withUiState(int uiState) {
		this.uiState=uiState;
		return this;
	}
}