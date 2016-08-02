package com.android.murano500k.newradio;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by artem on 7/22/16.
 */
public class NotifierRadio {
	private final String TAG= "NotifierRadio";

	private ArrayList<ListenerRadio> mListenerList;

	public NotifierRadio() {
		mListenerList=new ArrayList<ListenerRadio>() ;
	}

	public void registerListener(ListenerRadio mListener) {
		mListenerList.add(mListener);
	}

	public void unregisterAll() {
		if(mListenerList!=null)
			for (ListenerRadio listenerRadio: mListenerList)
				mListenerList.remove(listenerRadio);
	}

	public void notifyPlaybackStarted(String url) {
		for (ListenerRadio listener : mListenerList) {
			listener.onPlaybackStarted(url);
		}
	}
	public void notifyRadioConnected() {
		for (ListenerRadio listener : mListenerList) {
			listener.onRadioConnected();
		}
	}
	public void notifyListChanged(ArrayList<Station> newlist) {
		Log.d(TAG, "notifyListChanged "+mListenerList.toString());
		for (ListenerRadio listener : mListenerList) {
			listener.onListChanged(newlist);
		}
	}
	public void notifyPlaybackStopped(boolean updateNotification) {
		for (ListenerRadio listener : mListenerList)
			listener.onPlaybackStopped(updateNotification);
	}

	public void notifyMetaDataChanged(String s, String s2) {
		for (ListenerRadio listener : mListenerList)
			listener.onMetaDataReceived(s, s2);
	}

	public void notifyLoadingStarted(String url) {
		for (ListenerRadio listener  : mListenerList) {
			listener.onLoadingStarted(url);
		}
	}

	public void notifyPlaybackErrorOccured(){
		for (ListenerRadio listener : mListenerList) {
			listener.onPlaybackError();
		}
	}
	public void notifyStationSelected(String url){
		for (ListenerRadio listener : mListenerList) {
			listener.onStationSelected(url);
		}
	}
	public void notifySleepTimerStatusUpdated(String action, int seconds){
		for (ListenerRadio listener : mListenerList) {
			listener.onSleepTimerStatusUpdate(action, seconds);
		}
	}
}
