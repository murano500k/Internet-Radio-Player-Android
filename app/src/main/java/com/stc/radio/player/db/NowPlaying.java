package com.stc.radio.player.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;

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

	@Column(name = "StationIdentifier")
	private long stationId=-100;



	@Column(name = "Status")
	private int status=0;

	@Column(name = "Shuffle")
	private boolean shuffle=false;

	static NowPlaying instance;

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
		if(station==null) {
			if (stationId < 0) {
				From from = new Select().from(NowPlaying.class);
				if (from.exists()) {
					NowPlaying nowPlaying = from.executeSingle();
					stationId = nowPlaying.stationId;
				} else stationId = -1;
			}
			if (stationId < 0) return null;
			else {
				From from = new Select().from(Station.class).where("_id = ?", stationId);
				if (from.exists()) station =  from.executeSingle();
			}
		}
		return station;

	}
	public List<Station> getActiveList() {
		Station station=getStation();
		if(station==null) return null;
		else {
			From from = new Select().from(Station.class).where("Playlist = ?", station.getPlaylist());
			if(from.exists()) {
				return from.execute();
			}else return null;
		}
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
		if(s==null) stationId=-100;
		else this.stationId = s.getId();
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
		if(stationId!=s.getId()) post=true;
		this.stationId = s.getId();
		station=s;
		setMetadata(null);

		this.save();
		if(post) {
			setMetadata(null);
			bus.post(this);
		}
	}
	public void setStation(Station s, boolean fireEvent) {
		boolean post=fireEvent;
		this.stationId = s.getId();
		station=s;
		setMetadata(null);

		this.save();
		if(post) {
			setMetadata(null);

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
			setMetadata(null);
			bus.post(this);
		}
	}

	public List<Station>getStations(){
		if(list!=null && list.size()>1) return list;
		else {
			From from= new Select().from(Station.class);
			if(from.exists()) list=from.execute();
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
}