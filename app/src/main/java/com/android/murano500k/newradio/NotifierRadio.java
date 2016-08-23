package com.android.murano500k.newradio;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by artem on 7/22/16.
 */
public class NotifierRadio {
	public static final String TAG= "NotifierRadio";

	private ArrayList<ListenerRadio> mListenerList;
	private PlaylistManager playlistManager;

	public NotifierRadio(Context context) {
		mListenerList=new ArrayList<ListenerRadio>() ;
		playlistManager=new PlaylistManager(context);
	}

	public void registerListener(ListenerRadio mListener) {
		Log.d(TAG, "registerListener. size= " +mListenerList.size());
		mListenerList.add(mListener);
	}
	public void unRegisterListener(ListenerRadio mListener) {
		if(mListenerList!=null){
			for(Iterator<ListenerRadio> it = mListenerList.iterator(); it.hasNext();) {
				ListenerRadio listenerRadio = it.next();
				if(listenerRadio == mListener) {
					it.remove();
				}
			}
		}
	}

	public void unregisterAll() {
		if(mListenerList!=null){
			for(Iterator<ListenerRadio> it = mListenerList.iterator(); it.hasNext();) {
			ListenerRadio listenerRadio = it.next();
			if(listenerRadio != null) {
				it.remove();
			}
		}
		}
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
	/*public void notifyListChanged(ArrayList<String> newList) {
		Log.d(TAG, "notifyListChanged "+mListenerList.toString());
		for (ListenerRadio listener : mListenerList) {
			listener.onListChanged(newList);
		}
	}*/
	public void notifyPlaybackStopped(boolean updateNotification) {
		Log.d(TAG, "notifyPlaybackStopped "+ updateNotification);
		if(mListenerList!=null){
			ListenerRadio listenerRadio;
			Iterator<ListenerRadio> it = mListenerList.iterator();
			while(it.hasNext()) {
				listenerRadio=it.next();
				if(listenerRadio !=null) {
					listenerRadio.onPlaybackStopped(updateNotification);
				}
			}
		}
		if(!updateNotification) mListenerList=null;
	}

	public void notifyMetaDataChanged(String s, String s2) {
		for (ListenerRadio listener : mListenerList)
			listener.onMetaDataReceived(s, s2);
	}

	public void notifyLoadingStarted() {
		for (ListenerRadio listener  : mListenerList) {
			listener.onLoadingStarted();
		}
	}

	public void notifyFinish() {
		if(mListenerList!=null){
			for(Iterator<ListenerRadio> it = mListenerList.iterator(); it.hasNext();) {
				ListenerRadio listenerRadio = it.next();
				if(listenerRadio !=null) {
					listenerRadio.onFinish();
				}
			}
		}
	}

	public void notifyProgressUpdated(int p, int pMax, String s) {
		for (ListenerRadio listener  : mListenerList) {
			listener.onProgressUpdate(p, pMax, s);
		}
	}

	public void notifyPlaybackErrorOccured(boolean updateNotification) {
		Log.d(TAG, "notifyPlaybackErrorOccured "+ updateNotification);
		if(mListenerList!=null) {
			ListenerRadio listenerRadio;
			for (Iterator<ListenerRadio> it = mListenerList.iterator(); it.hasNext(); ) {
				listenerRadio = it.next();
				if (listenerRadio != null) {
					listenerRadio.onPlaybackError(updateNotification);
				}
			}
		}
		if(!updateNotification) mListenerList=null;

	}
	public void notifyStationSelected(String url){
		playlistManager.setSelectedUrl(url);
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
