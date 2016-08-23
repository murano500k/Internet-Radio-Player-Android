package com.android.murano500k.newradio;

import com.android.murano500k.newradio.events.EventError;
import com.android.murano500k.newradio.events.EventLocked;
import com.android.murano500k.newradio.events.EventStarted;
import com.android.murano500k.newradio.events.EventStopped;
import com.android.murano500k.newradio.events.EventUnlocked;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by artem on 8/18/16.
 */

public class StatusPlayer {
	private boolean isPlaying, isLocked, isInitiated;

	public StatusPlayer() {
		isPlaying=isLocked=false;
		isInitiated=true;
	}

	public boolean isPlaying(){
		return isPlaying;
	}

	public boolean canPlay(){
		return(isInitiated && !isLocked && !isPlaying);
	}
	public boolean readyForAction(){
		return(isInitiated && !isLocked);
	}
	public boolean canStop(){
		return(isInitiated && !isLocked && isPlaying);
	}
	public boolean lock(){
		if(!isLocked && isInitiated) {
			isLocked=true;
			EventBus.getDefault().post(new EventLocked());
		}
		return isLocked;
	}
	@Subscribe(priority = 10)
	public void onPlayerStartedEvent(EventStarted event) {
		isPlaying=true;
		isLocked=false;
		EventBus.getDefault().post(new EventUnlocked());
	}
	@Subscribe(priority = 10)
	public void onPlayerStoppedEvent(EventStopped event) {
		isPlaying=false;
		isLocked=false;
		EventBus.getDefault().post(new EventUnlocked());
	}
	@Subscribe(priority = 10)
	public void onPlayerErrorEvent(EventError event) {
		isPlaying=false;
		isLocked=false;
		EventBus.getDefault().post(new EventUnlocked());
	}


}
