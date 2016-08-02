package com.android.murano500k.newradio;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder>  {

	private static final String TAG = "ListAdapter";
	//private final ArrayList<Station> mValues;
	public int selectedIndex;
	public boolean shouldPlayAnim;
	public Context context;
	public String artist;
	public String song;
	private RecyclerView recyclerView;
	private ArrayList<Station> mValues;

	public ListAdapter(ArrayList<Station>  list, Context context) {
		//mValues = serviceRadio.getStations();
		this.context=context;
		this.mValues=list;
	}



	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.list_item, parent, false);

		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, int position) {
		holder.mItem = mValues.get(position);
		if(position==selectedIndex) {
			setViewSelected(holder, true, artist, song, mValues.get(position).name);
		}
		else setViewSelected(holder, false, null, null, null);
		holder.mIdView.setText(String.valueOf(mValues.get(position).id));
		holder.mStationNameView.setText(mValues.get(position).name);
		holder.mView.setOnClickListener(v -> {
			Intent intent= new Intent(context, ServiceRadio.class);
			intent.setAction(Constants.INTENT_RESUME_PLAYBACK);
			intent.putExtra(Constants.DATA_CURRENT_STATION_URL, holder.mItem.url);
			context.startService(intent);
		});
		holder.mView.setFav(mValues.get(position).fav);
		holder.mFavView.setOnClickListener(v -> {
			holder.mView.setFav(!holder.mItem.fav);
			holder.mItem.fav=holder.mView.isFav();
			Intent intent = new Intent(context, ServiceRadio.class);
			intent.setAction(Constants.INTENT_UPDATE_FAVORITE_STATION);
			//Log.d(TAG, "updating station indexToUpdate " + mValues.get(position).id + ", fav="+holder.mItem.fav);

			intent.putExtra(Constants.DATA_STATION_INDEX, mValues.get(position).id);
			intent.putExtra(Constants.DATA_STATION_FAVORITE, holder.mItem.fav);
			context.startService(intent);
			//serviceRadio.updateStation(holder.mItem, holder.mItem.fav);
			notifyItemChanged(position);
		});
		if(/*serviceRadio.isFavOnly()*/false){//TODO isFavOnly
			holder.mFavView.setVisibility(View.GONE);
		}else {
			holder.mFavView.setVisibility(View.VISIBLE);
		}
	}

	private void setViewSelected(final ViewHolder holder, boolean isSelected, String artist, String song, String stationName){
		if(isSelected) {
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
			holder.mExtraContentView.setVisibility(View.VISIBLE);
			holder.mMainContentView.setBackgroundColor(context.getResources().getColor(R.color.colorSecondary));
			if(artist!=null) holder.mArtistView.setText(artist);
			if(song!=null) holder.mSongView.setText(song);
			if(stationName!=null) holder.mArtView.setImageResource(StationContent.getArt(stationName, context));
			if(shouldPlayAnim) holder.playbackAnim.animateBars();
			else holder.playbackAnim.stopBars();
		}else {
			holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorText));
			holder.mExtraContentView.setVisibility(View.GONE);
			holder.mMainContentView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
			holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));

		}
	}
	public void setSelectedIndex(int index){
		selectedIndex=index;
		this.artist =null;
		this.song =null;
		notifyDataSetChanged();
	}

	public void setPlayingInfo(String artist, String song){
		this.artist = artist;
		this.song = song;
		notifyItemChanged(selectedIndex);
	}

	public void updateAnimState(boolean isPlaying){
		this.shouldPlayAnim=isPlaying;
		notifyItemChanged(selectedIndex);
	}


	@Override
	public int getItemCount() {
		return mValues.size();
	}


	public class ViewHolder extends RecyclerView.ViewHolder {
		public final LinearLayout mMainView;
		public final RelativeLayout mExtraContentView;
		public final RelativeLayout mMainContentView;
		public final StationListItem mView;
		public final EqualizerView playbackAnim;
		public final TextView mIdView;
		public final TextView mStationNameView;
		public final TextView mArtistView;
		public final TextView mSongView;
		public final ImageView mArtView;
		public final ImageView mFavView;
		public Station mItem;


		public ViewHolder(View view) {
			super(view);
			mView=(StationListItem)view;

			mMainView=(LinearLayout)view.findViewById(R.id.main_layout);
			mMainContentView = (RelativeLayout) view.findViewById(R.id.main_content);
			mExtraContentView =(RelativeLayout) view.findViewById(R.id.extras_layout);
			mIdView = (TextView) view.findViewById(R.id.item_index);
			mStationNameView = (TextView) view.findViewById(R.id.station_name);
			mArtistView = (TextView) view.findViewById(R.id.artist);
			mSongView = (TextView) view.findViewById(R.id.song);
			playbackAnim = (EqualizerView) view.findViewById(R.id.playbackAnim);
			mArtView = (ImageView) view.findViewById(R.id.art);
			mFavView = (ImageView) view.findViewById(R.id.imgFav);
		}

		@Override
		public String toString() {
			return super.toString() + " '" + mStationNameView.getText() + "'";
		}
	}
}
