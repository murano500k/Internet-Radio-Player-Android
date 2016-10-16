package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;
import com.stc.radio.player.contentmodel.StationsManager;

import timber.log.Timber;

/**
 * Created by artem on 9/21/16.
 */
@Table(name = "Stations", id = "_id")
public class Station extends Model {
	@Column(name = "Name")
	protected String name;/*
	@Column(name = "Url", unique = true, index = true, notNull = true)
	protected String url;*/
	@Column(name = "Playlist")
	protected String playlist;
	@Column(name = "Key")
	protected String key;
	@Column(name = "Description")
	protected String description;
	@Column(name = "ArtUrl")
	protected String artUrl;
	@Column(name = "Favorite")
	protected boolean favorite;
	@Column(name = "Active")
	protected boolean active;
	@Column(name = "Position")
	protected int position;

	protected static int SERVERS_NUM=4;

	public Station() {
	}
	public Station(ParsedPlaylistItem parsedPlaylistItem, String playlist, int position, boolean active) {
		this.name = parsedPlaylistItem.getName();
		this.key = parsedPlaylistItem.getKey();
		this.description= parsedPlaylistItem.getDescription();
		this.artUrl = trimArtUrl(parsedPlaylistItem.getImages().getDefault());
		this.playlist = playlist;
		this.favorite = false;
		this.position = position;
	}

	public Station(String name, String key,String description, String artUrl, String playlist, int position, boolean active) {
		this.name = name;
		this.key = key;
		this.description=description;
		this.artUrl = artUrl;
		this.playlist = playlist;
		this.favorite = false;
		this.position = position;
	}
	@Override
	public String toString() {
		String res="";
		res+="name="+name+",\n";
		res+="key="+key+",\n";
		res+="url="+getUrl()+",\n";
		res+="desc="+description+",\n";
		res+="artUrl="+artUrl+",\n";
		res+="playlist="+playlist+",\n";
		res+="favorite="+favorite+",\n";
		res+="position="+position+",\n";
		res+="active="+active+",\n";
		return res;
	}

	public boolean equals(Station station){
		if(station==null) return false;
		return (station.getId()==this.getId());
	}

	public Station(String url, String name, long playlistId, String artUrl) {
		this.name = name;
		this.active = false;
		this.favorite = false;
		this.position = -1;
		this.artUrl = artUrl;
	}

	private String trimArtUrl(String s){
		if(s==null) Timber.e("ERROR null");
		else return "http:"+s.replace("{?size,height,width,quality,pad}", "?size=200x200");
return "null";
}
	public String getUrl() {
	    return StationsManager.getUrl(playlist,key);
	}
	public String getArtUrl(){
		return artUrl;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return key;
	}

	public String getPlaylist() {
		return playlist;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}


}