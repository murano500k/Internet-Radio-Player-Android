package com.stc.radio.player.ui;

import com.stc.radio.player.PlayerStatus;

/**
 * Created by artem on 8/30/16.
 */


public class UiEvent {

	private final UI_ACTION uiAction;
	private final Extras extras;
	public enum UI_ACTION{
		STATION_SELECTED,
		LOADING_STARTED,
		PLAYBACK_STARTED,
		PLAYBACK_STOPPED,
		BUFFER_UPDATED,
		METADATA_UPDATED,
		SLEEP_TIMER_UPDATED,
		SLEEP_TIMER_CANCELLED
	}
	public class Extras{
		public final PlayerStatus playerStatus;
		public final String url;
		public final String artist;
		public final String song;
		public final int progress;


		public Extras(PlayerStatus playerStatus) {
			this.playerStatus=playerStatus;
			url="";
			artist="";
			song="";
			progress=0;
		}

		public Extras(PlayerStatus playerStatus, String url) {
			this.playerStatus=playerStatus;
			this.url = url;
			artist="";
			song="";
			progress=0;
		}

		public Extras(PlayerStatus playerStatus, String url, String artist, String song) {
			this.playerStatus=playerStatus;
			this.url = url;
			this.artist = artist;
			this.song = song;
			progress=0;
		}

		public Extras(PlayerStatus playerStatus, String url, int progress) {
			this.playerStatus=playerStatus;
			this.url = url;
			this.progress = progress;
			artist="";
			song="";
		}
	}



	public UiEvent(UI_ACTION ui_action, PlayerStatus playerStatus, String url, String artist, String song){
		this.uiAction = ui_action;
		extras=new Extras(playerStatus,url,artist,song);
	}

	public UiEvent(UI_ACTION ui_action, PlayerStatus playerStatus, String url) {
		this.uiAction = ui_action;
		extras=new Extras(playerStatus, url);
	}

	public UiEvent(UI_ACTION ui_action, PlayerStatus playerStatus) {
		this.uiAction = ui_action;
		extras=new Extras(playerStatus);
	}
	public UiEvent(UI_ACTION uiAction, PlayerStatus playerStatus, String url, int progress) {
		this.uiAction = uiAction;
		extras=new Extras(playerStatus, url, progress);
	}


	public UI_ACTION getUiAction() {
		return uiAction;
	}

	public Extras getExtras() {
		return extras;
	}

}
