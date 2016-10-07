package com.stc.radio.player.ui;

/**
 * Created by artem on 9/29/16.
 */
public class Metadata {
	private String artist, song, url;

	public Metadata(String artist, String song, String url) {
		this.artist = artist;
		this.song = song;
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public String getSong() {
		return song;
	}

	public String getArtist() {
		return artist;
	}


}
