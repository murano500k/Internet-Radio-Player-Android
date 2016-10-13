package com.stc.radio.player.ui;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by artem on 9/21/16.
 */
@Table(name = "Playlists", id = "_id")

public class Playlist extends Model {
	@Column(name = "Name")
	public String name;
	@Column(name = "Url")
	public String url;
	@Column(name = "Art")
	public int art;
	@Column(name = "Position")
	public int position;


	public Playlist() {
	}

	public Playlist(String name, String url, int position) {
		this.name = name;
		this.url = url;
		this.position = position;
	}
}
