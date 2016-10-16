package com.stc.radio.player.contentmodel;

import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.SettingsProvider;

import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.DI;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.RADIOTUNES;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.ROCK;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.SOMA;

/**
 * Created by artem on 10/15/16.
 */

public class StationsManager {
	public static String getArtUrl(String s1) {
		return "http://somafm.com/img3/" + s1+ "-400.jpg";
	}
	public  static String getUrl(String playlist, String key) {
		int num=1;//new Random().nextInt(1)+1;
		if(playlist.equals(SOMA))
			return "http://ice" + num + ".somafm.com/" + key + "-128-aac";

		String base="";
		if(playlist.equals(DI) ) base+=playlist+"/"+key+"_hi";
		else if (playlist.equals(RADIOTUNES) || playlist.equals(ROCK))base+=playlist+".com"+"/"+key+"_hi";
		else base+=playlist+".com"+"/"+key;
		return "http://prem"+num+"."+base+"?"+ SettingsProvider.getToken();
	}
	/**
	 * Created by artem on 10/13/16.
	 */

	public static class PLAYLISTS {
		public static final String DI="di.fm";
		public static final String RADIOTUNES="radiotunes";
		public static final String JAZZ="jazzradio";
		public static final String ROCK="rockradio";
		public static final String CLASSIC="classicalradio";
		public static final String FAV="favorite";
		public static final String SOMA="somafm";
	}

	/**
	 * Created by artem on 10/13/16.
	 */

	public static class Soma extends Station {


		public static final String[] somaStations = {
				"groovesalad",
				"dronezone",
				"spacestation",
				"secretagent",
				"lush",
				"u80s",
				"deepspaceone",
				"beatblender",
				"defcon",
				"seventies",
				"folkfwd",
				"bootliquor",
				"suburbsofgoa",
				"poptron",
				"thistle",
				"fluid",
				"digitalis",
				"illstreet",
				"7soul",
				"brfm",
				"cliqhop",
				"doomed",
				"earwaves",
		};
	}
}
