package com.android.murano500k.newradio.events;

import com.android.murano500k.newradio.PlaylistManager;

/**
 * Created by artem on 8/18/16.
 */

public class EventStationSelected {
	public final String url,name;



	public EventStationSelected(String url) {
		this.url = url;
		this.name = PlaylistManager.getNameFromUrl(url);
	}
}
