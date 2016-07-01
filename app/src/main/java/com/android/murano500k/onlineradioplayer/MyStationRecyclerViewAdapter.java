package com.android.murano500k.onlineradioplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class MyStationRecyclerViewAdapter extends RecyclerView.Adapter<MyStationRecyclerViewAdapter.ViewHolder>  {

    private final List<StationContent.Station> mValues;
    private final ListSelectListener mListener;
    public int selectedIndex;
    public boolean shouldPlayAnim;
    public Context context;
    public String artist;
    public String song;
    private RecyclerView recyclerView;

    public MyStationRecyclerViewAdapter(List<StationContent.Station> items, ListSelectListener listener, Context context) {
        mValues = items;
        this.context=context;

        mListener = listener;
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
            setViewSelected(holder, true, artist, song);

        }
        else setViewSelected(holder, false, null, null);
        holder.mIdView.setText(mValues.get(position).id);
        holder.mStationNameView.setText(mValues.get(position).name);
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.stationSelected(holder.mItem);
                }
            }
        });
    }
    private void setViewSelected(final ViewHolder holder, boolean isSelected, String artist, String song){

        if(isSelected) {
            holder.mIdView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
            holder.mStationNameView.setTextColor(context.getResources().getColor(R.color.colorTextAccent));
            holder.mMainView.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
            holder.mExtraContentView.setVisibility(View.VISIBLE);
            holder.mMainContentView.setBackgroundColor(context.getResources().getColor(R.color.colorSecondary));
            if(artist!=null) holder.mArtistView.setText(artist);
            if(song!=null) holder.mSongView.setText(song);

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
        if(recyclerView!=null) {
            recyclerView.smoothScrollToPosition(index);
        }
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

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout mMainView;
        public final RelativeLayout mExtraContentView;
        public final RelativeLayout mMainContentView;
        public final View mView;
        public final EqualizerView playbackAnim;

        public final TextView mIdView;
        public final TextView mStationNameView;
        public final TextView mArtistView;
        public final TextView mSongView;
        public StationContent.Station mItem;

        public ViewHolder(View view) {
            super(view);
            mView=view;
            mMainView=(LinearLayout)view.findViewById(R.id.main_layout);
            mMainContentView = (RelativeLayout) view.findViewById(R.id.main_content);
            mExtraContentView =(RelativeLayout) view.findViewById(R.id.extras_layout);
            mIdView = (TextView) view.findViewById(R.id.item_index);
            mStationNameView = (TextView) view.findViewById(R.id.station_name);
            mArtistView = (TextView) view.findViewById(R.id.artist);
            mSongView = (TextView) view.findViewById(R.id.song);
            playbackAnim = (EqualizerView) view.findViewById(R.id.playbackAnim);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mStationNameView.getText() + "'";
        }
    }
}
