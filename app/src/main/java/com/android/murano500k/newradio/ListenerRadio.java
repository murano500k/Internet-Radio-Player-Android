package com.android.murano500k.newradio;

/**
 * Created by mertsimsek on 01/07/15.
 */
public interface ListenerRadio {


	void onFinish();
	void onLoadingStarted(String url);

	void onProgressUpdate(int p, int pMax, String s);


	void onRadioConnected();

	void onPlaybackStarted(String url);

	void onPlaybackStopped(boolean updateNotification);

	void onMetaDataReceived(String s, String s2);

	void onPlaybackError(boolean updateNotification);

	//void onListChanged(ArrayList<String>newList, String selected, boolean favOnly);

	void onStationSelected(String url);

	void onSleepTimerStatusUpdate(String action, int seconds);



}
