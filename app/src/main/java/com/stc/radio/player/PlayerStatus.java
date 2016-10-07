package com.stc.radio.player;

public class PlayerStatus {
	public int playbackStatus;
	public ServiceRadioRx.LOADING_TYPE loadingType;
	public ServiceRadioRx.FOCUS_STATUS focusStatus;
	private final String name;


	@Override
	public String toString() {
		return name;
	}

	PlayerStatus(String name, int playbackStatus, ServiceRadioRx.LOADING_TYPE loadingType, ServiceRadioRx.FOCUS_STATUS focusStatus) {
		this.name=name;
		this.playbackStatus = playbackStatus;
		this.loadingType = loadingType;
		this.focusStatus = focusStatus;
	}
}
