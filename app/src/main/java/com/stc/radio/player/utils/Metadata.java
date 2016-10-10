package com.stc.radio.player.utils;

/**
 * Created by artem on 9/29/16.
 */
public class Metadata {
	private String artist, song;

	public Metadata(String artist, String song) {
		this.artist = artist;
		this.song = song;
	}
	public boolean equals(Metadata metadata){
		boolean res=true;
		if(metadata.getSong()==null) {
			if (this.song == null) return false;
			if (metadata.getArtist() == null) {
				if (this.artist != null) return false;
				else return true;
			}
		}
		if(song!=null && metadata.getSong().contains(song)){
			if(this.artist!=null && metadata.getArtist().contains(artist)) return true;
			else return false;
		}else return false;
	}


	public String getSong() {
		return song;
	}

	public String getArtist() {
		return artist;
	}


}
