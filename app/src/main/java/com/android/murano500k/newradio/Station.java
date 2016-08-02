package com.android.murano500k.newradio;

/**
 * Created by artem on 8/1/16.
 */

public class Station {
	public final int id;
	public boolean fav;
	public String name;
	public String url;
	public int image;


	public Station(int id, String name, String url) {
		this.id = id;
		this.name = name;
		this.url = url;
		fav=false;
	}


}
