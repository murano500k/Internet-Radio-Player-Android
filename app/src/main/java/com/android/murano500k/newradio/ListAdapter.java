package com.android.murano500k.newradio;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

import es.claucookie.miniequalizerlibrary.EqualizerView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> implements ListenerRadio {

	public static final String TAG = "ListAdapter";
	private ArrayList<String> mValues;
	public boolean shouldPlayAnim;
	public boolean isFavOnly;
	public Context context;
	public String artist;
	public String song;
	private PlaylistManager playlistManager;
	private String urlSelected;



	public class ViewHolder extends RecyclerView.ViewHolder {

		public final StationListItem mView;
		public final RelativeLayout mMainView;
		public final RelativeLayout mExtraView;
		public final LinearLayout mSelectView;
		public final EqualizerView playbackAnim;
		public final TextView mIdView;
		public final TextView mStationNameView;
		public final TextView mArtistView;
		public final TextView mSongView;
		public final ImageView mArtView;
		public final ImageView mFavView;
		public String URL;


		public ViewHolder(View view) {
			super(view);
			mView=(StationListItem)view;
			mMainView =(RelativeLayout)view.findViewById(R.id.layout_main);
			mExtraView =(RelativeLayout) view.findViewById(R.id.layout_extras);
			mSelectView = (LinearLayout) view.findViewById(R.id.layout_select);
			mFavView = (ImageView) view.findViewById(R.id.imgFav);
			mIdView = (TextView) view.findViewById(R.id.item_index);
			mStationNameView = (TextView) view.findViewById(R.id.station_name);
			mArtistView = (TextView) view.findViewById(R.id.artist);
			mSongView = (TextView) view.findViewById(R.id.song);
			playbackAnim = (EqualizerView) view.findViewById(R.id.playbackAnim);
			mArtView = (ImageView) view.findViewById(R.id.art);

		}

		@Override
		public String toString() {
			return super.toString() + " '" + mStationNameView.getText() + "'";
		}
	}

	public ListAdapter(Context context, HashSet<String> list, String urlS, boolean isFavOnly) {
		this.context=context;
		this.isFavOnly=isFavOnly;
		mValues=new ArrayList<>(list);
		urlSelected=urlS;
		artist=null;
		song=null;
		shouldPlayAnim=false;
		playlistManager=new PlaylistManager(context);
		setHasStableIds(false);
	}



	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.list_item, parent, false);

		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, int position) {

		holder.URL = mValues.get(position);
		if(position==mValues.indexOf(urlSelected)) {
			setViewSelected(holder, true, artist, song, PlaylistManager.getNameFromUrl(holder.URL));
		}
		else setViewSelected(holder, false, null, null, null);
		String numberText= position+".";
		if(numberText.length()>0)holder.mIdView.setText(numberText);
		holder.mStationNameView.setText(PlaylistManager.getNameFromUrl(holder.URL));


		if(isFavOnly){
			holder.mFavView.setVisibility(View.GONE);
		}else {
			holder.mFavView.setVisibility(View.VISIBLE);
			holder.mView.setFav(playlistManager.isStationFavorite(holder.URL));
		}
		holder.mSelectView.setOnClickListener(v -> {
			song=null;
			artist=null;
			updateAnimState(false);
			setPlayingInfo(song, artist);
			Intent intent= new Intent(context, ServiceRadio.class);
			intent.setAction(Constants.INTENT.PLAYBACK.RESUME);
			//else intent.setAction(Constants.PAUSE);
			intent.putExtra(Constants.DATA_CURRENT_STATION_URL, holder.URL);
			context.startService(intent);
		});
		holder.mFavView.setOnClickListener(v -> {
			holder.mView.setFav(!holder.mView.isFav());
			playlistManager.setStationFavorite(holder.URL, holder.mView.isFav());
		});
	}

	private void setViewSelected(final ViewHolder holder, boolean isSelected,
	                             String artist, String song, String stationName) {
		if(isSelected) {
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorSecondary));
			holder.mExtraView.setVisibility(View.VISIBLE);
			if(artist!=null) holder.mArtistView.setText(artist);
			if(song!=null) holder.mSongView.setText(song);
			if(stationName!=null) holder.mArtView.setImageResource(PlaylistManager.getArt(stationName,
					context));
			if(shouldPlayAnim) holder.playbackAnim.animateBars();
			else holder.playbackAnim.stopBars();
		}else {
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mExtraView.setVisibility(View.GONE);
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
		}
	}

	public void setPlayingInfo(String artist, String song){
		rx.Observable.just(mValues.indexOf(urlSelected))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(i -> {
			this.artist = artist;
			this.song = song;
			notifyItemChanged(i);
		});
	}

	public void updateAnimState(boolean isPlaying){
		rx.Observable.just(mValues.indexOf(urlSelected))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(i -> {

					this.shouldPlayAnim=isPlaying;
					notifyItemChanged(i);
				});

	}

	private void updateUrlSelected(String urlS){
		this.artist =null;
		this.song =null;
		rx.Observable.just(urlS)
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(url -> {
					if(!url.contains(urlSelected)) {
						int wasSelected=mValues.indexOf(urlSelected);
						urlSelected=url;
						notifyItemChanged(mValues.indexOf(urlSelected));
						notifyItemChanged(wasSelected);
					}
				});

	}

	public int getItemIndex(String url) {
		if(mValues.indexOf(url)>=0) return mValues.indexOf(url);
		else return -1;
	}

	@Override
	public int getItemCount() {
		return mValues.size();
	}

	public ArrayList<String>getItems(){
		return mValues;
	}

	@Override
	public void onFinish() {

	}

	@Override
	public void onLoadingStarted() {
		Log.d(TAG, "onLoadingStarted");
		setPlayingInfo(null,null);
		updateAnimState(false);
	}

	@Override
	public void onProgressUpdate(int p, int pMax, String s) {

	}

	@Override
	public void onRadioConnected() {

	}

	@Override
	public void onPlaybackStarted(String url) {
		Log.d(TAG, "onPlaybackStarted");
		updateUrlSelected(url);
		updateAnimState(true);
	}

	@Override
	public void onPlaybackStopped(boolean updateNotification) {
		Log.d(TAG, "onPlaybackStopped");
		if(updateNotification) {
			setPlayingInfo(null, null);
			updateAnimState(false);
		}
	}

	@Override
	public void onMetaDataReceived(String s, String s2) {

		if(s!=null && s.equals("StreamTitle")) {
			Log.d(TAG, "onMetaDataReceived");
			setPlayingInfo(PlaylistManager.getArtistFromString(s2),PlaylistManager
					.getTrackFromString(s2));
		}

	}

	@Override
	public void onPlaybackError(boolean updateNotification) {
		Log.d(TAG, "onPlaybackError");
		if(updateNotification) {
			setPlayingInfo(null, null);
			updateAnimState(false);
		}
	}


	@Override
	public void onStationSelected(String url) {
		Log.d(TAG, "onStationSelected");
		setPlayingInfo(null,null);
		updateUrlSelected(url);
	}

	@Override
	public void onSleepTimerStatusUpdate(String action, int seconds) {

	}

}
