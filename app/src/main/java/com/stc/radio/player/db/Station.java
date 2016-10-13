package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.stc.radio.player.contentmodel.PlaylistContent;
import com.stc.radio.player.utils.SettingsProvider;

import timber.log.Timber;

/**
 * Created by artem on 9/21/16.
 */
@Table(name = "Stations", id = "_id")
public class Station extends Model {
	@Column(name = "Name")
	private String name;/*
	@Column(name = "Url", unique = true, index = true, notNull = true)
	private String url;*/
	@Column(name = "Playlist")
	private String playlist;
	@Column(name = "Key")
	private String key;
	@Column(name = "Description")
	private String description;
	@Column(name = "ArtUrl")
	private String artUrl;
	@Column(name = "Favorite")
	private boolean favorite;
	@Column(name = "Active")
	private boolean active;
	@Column(name = "Position")
	private int position;

	public Station() {
	}
	public Station(PlaylistContent playlistContent, String playlist) {
		this.name = playlistContent.getName();
		this.key = playlistContent.getKey();
		this.description=playlistContent.getDescription();
		this.artUrl = trimArtUrl(playlistContent.getImages().getDefault());
		this.key = playlistContent.getKey();
		this.playlist = playlist;
		this.favorite = false;
		this.position = -1;
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
		String base="http://prem2.";
		if(playlist.equals("di") ) base+="di.fm";
		else base+=playlist+".com";
		return base+"/"+key+"_hi?"+ SettingsProvider.getToken();
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