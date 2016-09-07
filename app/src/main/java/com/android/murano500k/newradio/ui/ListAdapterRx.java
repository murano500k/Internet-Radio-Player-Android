package com.android.murano500k.newradio.ui;

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

import com.android.murano500k.newradio.Constants;
import com.android.murano500k.newradio.PlaylistManager;
import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;
import com.android.murano500k.newradio.StationListItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class ListAdapterRx extends RecyclerView.Adapter<ListAdapterRx.ViewHolder> {

	public static final String TAG = "ListAdapter";
	private ArrayList<String> mValues;
	boolean shouldPlayAnim;
	boolean isFavOnly;
	public Context context;
	String artist;
	String song;
	private PlaylistManager playlistManager;
	private String urlSelected;

	 class ViewHolder extends RecyclerView.ViewHolder {
		final StationListItem mView;
		final RelativeLayout mMainView;
		final RelativeLayout mExtraView;
		final LinearLayout mSelectView;
		final EqualizerView playbackAnim;
		final TextView mIdView;
		final TextView mStationNameView;
		final TextView mArtistView;
		final TextView mSongView;
		final ImageView mArtView;
		final ImageView mFavView;
		String URL;


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
	ListAdapterRx(Context context, ArrayList<String> list, String urlS) {
		this.context=context;
		mValues=list;
		urlSelected=urlS;
		artist=null;
		song=null;
		shouldPlayAnim=false;
		playlistManager=new PlaylistManager(context);
		setHasStableIds(false);
		EventBus.getDefault().register(this);
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
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorSecondary));
			holder.mExtraView.setVisibility(View.VISIBLE);
			holder.mArtistView.setText(artist);
			holder.mSongView.setText(song);
			holder.mArtView.setImageResource(playlistManager.getArt(urlSelected, context));
			if(shouldPlayAnim) holder.playbackAnim.animateBars();
			else holder.playbackAnim.stopBars();
		} else {
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mExtraView.setVisibility(View.GONE);
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
		}
		String numberText= position+".";
		if(numberText.length()>1)holder.mIdView.setText(numberText);
		holder.mStationNameView.setText(playlistManager.getNameFromUrl(holder.URL));
		holder.mFavView.setVisibility(View.GONE);
/*

		if(isFavOnly){
			holder.mFavView.setVisibility(View.GONE);
		}else {
			holder.mFavView.setVisibility(View.VISIBLE);
			holder.mView.setFav(playlistManager.isStationFavorite(holder.URL));
		}
*/
		holder.mSelectView.setOnClickListener(v -> {
			EventBus.getDefault().post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, ServiceRadioRx.STATUS_LOADING, holder.URL));
			Intent intent= new Intent(context, ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(ServiceRadioRx.EXTRA_PLAY_LIST_STATION_PRESSED, ServiceRadioRx.EXTRA_PLAY_LIST_STATION_PRESSED);
			intent.putExtra(ServiceRadioRx.EXTRA_URL, holder.URL);
			context.startService(intent);
		});
		holder.mFavView.setOnClickListener(v -> {
			holder.mView.setFav(!holder.mView.isFav());
			playlistManager.setStationFavorite(holder.URL, holder.mView.isFav());
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

	private void updateUI(int state, String url, String artist, String song){
		this.artist=artist;
		this.song=song;
		shouldPlayAnim=(state==Constants.UI_STATE.PLAYING);
		if(url!=null) {
			String lastUrl=urlSelected;
			urlSelected=url;
			notifyItemChanged(mValues.indexOf(lastUrl));
			notifyItemChanged(mValues.indexOf(urlSelected));
		}
	}


	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiEvent(UiEvent eventUi) {
		UiEvent.UI_ACTION action = eventUi.getUiAction();
		if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)) {
			Log.i(TAG, "onPlayerStartedEvent");
			shouldPlayAnim=true;
			updateUI(Constants.UI_STATE.PLAYING, eventUi.getExtras().url, null, null);
		}
		else if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)) {
			shouldPlayAnim=false;
			updateUI(Constants.UI_STATE.IDLE, urlSelected, null, null);
		}
		else if (action.equals(UiEvent.UI_ACTION.METADATA_UPDATED)) {
			shouldPlayAnim=true;
			updateUI(Constants.UI_STATE.PLAYING, urlSelected, eventUi.getExtras().artist, eventUi.getExtras().song);
		}
		else if (action.equals(UiEvent.UI_ACTION.LOADING_STARTED)) {
			shouldPlayAnim=false;
			updateUI(Constants.UI_STATE.LOADING, eventUi.getExtras().url, null, null);
		}
		else if (action.equals(UiEvent.UI_ACTION.STATION_SELECTED)) {
			Log.i(TAG, "onStationSelected");
			if(eventUi.getExtras().playerStatus==ServiceRadioRx.STATUS_PLAYING){
				shouldPlayAnim=true;
				updateUI(Constants.UI_STATE.PLAYING, eventUi.getExtras().url, eventUi.getExtras().artist, eventUi.getExtras().song);
			}else {
				shouldPlayAnim=false;
				updateUI(Constants.UI_STATE.LOADING,eventUi.getExtras().url, null, null);
			}
		}
	}


}
