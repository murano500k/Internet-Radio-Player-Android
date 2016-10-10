package com.stc.radio.player.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.utils.Metadata;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
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





	EventBus bus = EventBus.getDefault();

	public static final int STATUS_PLAYING = 1;
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_STARTING = -1;
	public static final int STATUS_SWITCHING = -2;
	public static final int STATUS_PAUSING = -3;
	public static final int STATUS_WAITING_FOCUS = -4;
	public static final int STATUS_WAITING_CONNECTIVITY = -5;
	public static final int STATUS_WAITING_UNMUTE = -6;
	public static NowPlaying getInstance(){
		From from = new Select().from(NowPlaying.class);
		if(from.exists()) {
			return from.executeSingle();
		}else {
			return null;
		}
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
		if(artist==null || song==null) {
			From from = new Select().from(NowPlaying.class);
			if(from.exists()) {
				NowPlaying nowPlaying= from.executeSingle();
				artist=nowPlaying.getMetadata().getArtist();
				song=nowPlaying.getMetadata().getSong();
			}else return null;
			if(artist==null || song==null) return null;
		}
		return new Metadata(artist, song);
	}

	public Station getStation() {
		if(stationId<0) {
			From from = new Select().from(NowPlaying.class);
			if(from.exists()) {
				NowPlaying nowPlaying= from.executeSingle();
				stationId=nowPlaying.stationId;
			}else stationId= -1;
		}
		if(stationId<0) return null;
		else {
			From from = new Select().from(Station.class).where("_id = ?", stationId);
			if(from.exists()) return from.executeSingle();
			else return null;
		}
	}
	public List<Station> getActiveList() {
		Station station=getStation();
		if(stationId<0 || station==null) return null;
		else {
			From from = new Select().from(Station.class).where("PlaylistId = ?", station.playlistId);
			if(from.exists()) {
				return from.execute();
			}else return null;
		}
	}

	public Playlist getPlaylist() {
		if(stationId<0) {
			From from = new Select().from(NowPlaying.class);
			if(from.exists()) {
				NowPlaying nowPlaying= from.executeSingle();
				stationId=nowPlaying.stationId;
			}else stationId= -1;
		}
		if(stationId<0) return null;
		else {

			From from = new Select().from(Playlist.class).where("_id = ?", getStation().playlistId);
			if(from.exists()) return from.executeSingle();
			else return null;
		}
	}
	public NowPlaying withMetadata(Metadata metadata) {
		boolean post=false;
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
		this.save();
		if(post) {
			setMetadata(null);
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
	public Bitmap getArtBitmap(){
		File file = new File(getStation().artPath);
		if(file.exists()) return  BitmapFactory.decodeFile(file.getAbsolutePath(), new BitmapFactory.Options());
		else return null;
	}
	public static Bitmap getStationArtBitmap(Station s){
		File file = new File(s.artPath);
		if(file.exists()) return  BitmapFactory.decodeFile(file.getAbsolutePath(), new BitmapFactory.Options());
		else return null;
	}
}