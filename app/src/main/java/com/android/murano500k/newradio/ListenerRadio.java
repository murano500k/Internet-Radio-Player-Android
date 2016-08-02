package com.android.murano500k.newradio;

import java.util.ArrayList;

/**
 * Created by mertsimsek on 01/07/15.
 */
public interface ListenerRadio {


	void onLoadingStarted(String url);

	void onRadioConnected();

	void onPlaybackStarted(String url);

	void onPlaybackStopped(boolean updateNotification);

	void onMetaDataReceived(String s, String s2);

	void onPlaybackError();

	void onListChanged(ArrayList<Station> newlist);

	void onStationSelected(String url);

	void onSleepTimerStatusUpdate(String action, int seconds);



}
