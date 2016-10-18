package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.utils.SettingsProvider;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by artem on 10/6/16.
 */
@Table(name = "NowPlaying")
public class NowPlaying extends Model {
	@Column(name = "Artist")
	private String artist=null;

	@Column(name = "Song")
	private String song=null;

	@Column(name = "StationKey")
	private String stationKey=null;



	@Column(name = "Status")
	private int status=0;

	@Column(name = "Shuffle")
	private boolean shuffle=false;

	static NowPlaying instance;
	private String playlist;
	private Station station;
	private List<Station>list;




	EventBus bus = EventBus.getDefault();

	public static final int STATUS_PLAYING = 1;
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_STARTING = -1;
	public static final int STATUS_SWITCHING = -2;
	public static final int STATUS_PAUSING = -3;
	public static final int STATUS_WAITING_FOCUS = -4;
	public static final int STATUS_WAITING_CONNECTIVITY = -5;
	public static final int STATUS_WAITING_UNMUTE = -6;
	private boolean baseStatus;

	public static NowPlaying getInstance(){
		if(instance==null) {
			From from = new Select().from(NowPlaying.class);
			if(from.exists()) {
				instance= from.executeSingle();
			}else {
				return null;
			}
		}

		return instance;
	}

	public int getStatus() {
		if(status<-100) {
			From from = new Select().from(NowPlaying.class);
			if(from.exists()) {
				NowPlaying nowPlaying= from.executeSingle();
				status=nowPlaying.status;
			}else status= -100;
		}
		return status;
	}
	public boolean getShuffle() {
		return shuffle;
	}
	public boolean equals(NowPlaying nowPlaying){
		boolean metaEquals=false;
		if(nowPlaying.getMetadata()==null){
			if(this.getMetadata()==null) metaEquals=true;
		}else {
			if(this.getMetadata()!=null) metaEquals=this.getMetadata().equals(nowPlaying.getMetadata());
		}

		boolean stationEquals=false;
		if(nowPlaying.getStation()==null){
			if(this.getStation()==null) stationEquals=true;
		}else {
			if(this.getStation()!=null) stationEquals=this.getStation().equals(nowPlaying.getStation());
		}

		return (metaEquals && stationEquals && this.getStatus()==nowPlaying.getStatus());
	}


	public Metadata getMetadata() {

		if(artist==null || song==null) return null;

		return new Metadata(artist, song);
	}

	public Station getStation() {
		if (station == null) {
			if (stationKey == null) {
				From from = new Select().from(NowPlaying.class);
				if (from.exists() && from.executeSingle() != null) {
					NowPlaying nowPlaying = from.executeSingle();
					stationKey = nowPlaying.getStationKey();
				} else stationKey = null;
			}
			if (stationKey == null) return null;
			else {
				From from = new Select().from(Station.class).where("Key = ?", stationKey);
				if (from.exists()) station = from.executeSingle();
			}
		}
		return station;

	}
	public String getPlaylist() {
		 if(getStation()!=null) return getStation().getPlaylist();
		return null;
	}
	public NowPlaying withMetadata(Metadata metadata) {
		if(metadata==null){
			this.song = null;
			this.artist = null;
		}else {
			this.song = metadata.getSong();
			this.artist = metadata.getArtist();
		}
		this.save();
		return this;
	}


	public NowPlaying withStation(Station s) {
		if(s==null) stationKey=null;
		else this.stationKey =  s.getKey();
		station=getStation();

		this.save();
		return this;
	}
	public NowPlaying withStatus(int s) {
		this.status = s;
		this.save();
		return this;
	}

	public NowPlaying withShuffle(boolean s) {
		this.shuffle = s;
		this.save();
		return this;
	}
	public void setMetadata(Metadata metadata, boolean fireEvent) {
		boolean post=fireEvent;
		if(metadata==null){
			//if(artist!=null || song!=null) post=true;
			this.song = null;
			this.artist = null;
		}else {
			//if(!new Metadata(artist,song).equals(metadata)) post=true;
			this.song = metadata.getSong();
			this.artist = metadata.getArtist();
		}
		this.save();
		if(post) bus.post(this);
	}

	public void setMetadata(Metadata metadata) {
		boolean post=false;
		if(metadata==null){
			if(artist!=null || song!=null) post=true;
			this.song = null;
			this.artist = null;
		}else {
			if(!new Metadata(artist,song).equals(metadata)) post=true;
			this.song = metadata.getSong();
			this.artist = metadata.getArtist();
		}
		this.save();
		if(post) bus.post(this);
	}

	public void setStation(Station s) {
		boolean post=false;
		if(s.getKey()==null )this.stationKey=null;
		else this.stationKey = s.getKey();
		station=s;
		this.save();
		if(post) {
			bus.post(this);
		}
	}
	public void setStation(Station s, boolean fireEvent) {
		boolean post=fireEvent;
		this.stationKey = s.getKey();
		station=s;
		this.save();
		if(post) {
			bus.post(this);
		}
	}

	public void setStatus(int status, boolean fireEvent) {
		boolean post=fireEvent;
		this.status = status;
		this.save();
		if(post) {

			bus.post(this);
		}
	}
	public void setShuffle(boolean shuffle) {
		boolean post=false;
		if(this.shuffle != shuffle) post=true;
		this.shuffle = shuffle;
		this.save();
		if(post) bus.post(this);
	}

	public void setStatus(int status) {
		boolean post=false;
		if(this.status != status) post=true;
		this.status = status;
		this.save();
		if(post) {
			bus.post(this);
		}
	}

	public List<Station>getStations(){
		if(list!=null && list.size()>1) return list;
		else{
			From from = new Select().from(Station.class).where("Playlist = ?", SettingsProvider.getPlaylist());
			if (from.exists()) list = from.execute();
		}
		return list;
	}

	public void setBaseStatus(boolean baseStatus) {
		this.baseStatus = baseStatus;

	}

	public boolean isPlaying() {
		return baseStatus;
	}

	public void setStations(List<Station> stations) {
		this.list=stations;
	}

	public String getStationKey() {
		return stationKey;
	}

	public void setPlaylist(String pls) {
		this.playlist=pls;
	}
}