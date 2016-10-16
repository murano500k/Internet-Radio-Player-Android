/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stc.radio.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.stc.radio.player.db.Metadata;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.OnSwipeListener;
import com.stc.radio.player.utils.PabloPicasso;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

import static com.stc.radio.player.db.NowPlaying.STATUS_IDLE;
import static com.stc.radio.player.db.NowPlaying.STATUS_PAUSING;
import static com.stc.radio.player.db.NowPlaying.STATUS_PLAYING;
import static com.stc.radio.player.db.NowPlaying.STATUS_STARTING;
import static com.stc.radio.player.db.NowPlaying.STATUS_SWITCHING;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_CONNECTIVITY;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_FOCUS;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_UNMUTE;
import static junit.framework.Assert.assertNotNull;


/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment {

    private static final String TAG = "PlaybackControlsFragment";
    private static final String ARG_STATION = "com.stc.radio.player.ui.ARG_STATION";
    private static final String ARG_ARTIST = "com.stc.radio.player.ui.ARG_ARTIST";
	private static final String ARG_SONG = "com.stc.radio.player.ui.ARG_SONG";
	private static final String ARG_URL = "com.stc.radio.player.ui.ARG_URL";
	private static final String ARG_ART_BITMAP = "com.stc.radio.player.ui.ARG_URL";
	private static final String ARG_UI_STATE = "com.stc.radio.player.ui.ARG_UI_STATE";


    private ImageButton mPlayPrev, mPlayPause, mPlayNext;
    private TextView mSong;
    private TextView mArtist;
    private TextView mStation;
    private ImageView mAlbumArt;
	private View rootView;
	private EventBus bus=EventBus.getDefault();
	String song;
	String artist;
	private OnControlsFragmentInteractionListener mListener;
	Bitmap art;
	private Metadata metadata;
	private int status;
	private Station station;
	private FrameLayout pacmanIndicator;


	public PlaybackControlsFragment() {
	}

	public static PlaybackControlsFragment newInstance(){
		return new PlaybackControlsFragment();
	}
	public static PlaybackControlsFragment newInstance(String url, String artist, String song, int uiState) {
		Bundle args = new Bundle();
		//if(oldF!=null && url.contains(oldF.getUrl()) && oldF.getArtBitmap()!=null) {
		//	args.putParcelable("BitmapImage",oldF.getArtBitmap());
		//}
		PlaybackControlsFragment fragment = new PlaybackControlsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*NowPlaying nowPlaying=NowPlaying.getInstance();
		this.station=nowPlaying.getStation();
		this.status=nowPlaying.getStatus();
		this.metadata =nowPlaying.getMetadata();
		this.art=nowPlaying.getArtBitmap();
		assertNotNull(station);
		assertNotNull(status);
		assertNotNull(art);*/
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		super.onSaveInstanceState(outState);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		setRetainInstance(true);

		//if(!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
		mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
		mPlayPause.setEnabled(true);
        mPlayPrev = (ImageButton) rootView.findViewById(R.id.play_prev);
		pacmanIndicator = (FrameLayout) rootView.findViewById(R.id.pacman_layout);
		pacmanIndicator.setVisibility(View.GONE);

		mPlayNext= (ImageButton) rootView.findViewById(R.id.play_next);
		mPlayPause.setOnClickListener(v ->  onButtonClicked(0));
		pacmanIndicator.setOnClickListener(	v -> onButtonClicked(0));
		mPlayNext.setOnClickListener(v -> onButtonClicked(1));
		mPlayPrev.setOnClickListener(v -> onButtonClicked(-1));


		mSong = (TextView) rootView.findViewById(R.id.title);
	    mArtist = (TextView) rootView.findViewById(R.id.artist);
	    mStation = (TextView) rootView.findViewById(R.id.station);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
		rootView.setOnTouchListener(getOnSwipeListener(rootView));
		rootView.setVisibility(View.GONE);
		station= NowPlaying.getInstance().getStation();
		metadata=NowPlaying.getInstance().getMetadata();
		status=NowPlaying.getInstance().getStatus();
		assertNotNull(station);
		Timber.w("init %d %s %s %s", status, station.getName(), metadata==null ? null : metadata.getArtist(),metadata==null ? null : metadata.getSong() );
		updateButtons(status);
		updateMetadata(metadata);
		updateStation(station);
		return rootView;
    }

	public void updateStation(Station station) {
		if (getActivity() == null) {
			Timber.w("onLeftToRightSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		//if(this.station==null && station==null) return;
		//if(this.station!=null && station!=null && this.station.equals(station)) return;
		assertNotNull(station);
		this.station=station;
		if(mStation!=null) mStation.setText(station.getName());
		PabloPicasso.with(getContext()).load(station.getArtUrl()).error(R.drawable.default_art).fit().into(mAlbumArt);
	}


	public void updateButtons(int state){
		if (getActivity() == null) {
			Timber.w("onLeftToRightSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		//if(status!=state){
			status=state;
			rootView.setVisibility(View.VISIBLE);
			Timber.d("state %d", state);
			switch (state){
				case STATUS_IDLE:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_play));
					updateMetadata(null);
					break;
				case STATUS_PLAYING:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
					break;
				case STATUS_PAUSING:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mArtist.setText("PAUSING");
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
					break;
				case STATUS_STARTING:
					pacmanIndicator.setVisibility(View.VISIBLE);
					break;
				case STATUS_SWITCHING:
					pacmanIndicator.setVisibility(View.VISIBLE);
					mArtist.setText("SWITCHING");

					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
					break;
				case STATUS_WAITING_CONNECTIVITY:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mArtist.setText("WAITING_CONNECTIVITY");
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
					break;
				case STATUS_WAITING_FOCUS:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mArtist.setText("WAITING_FOCUS");
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
					break;
				case STATUS_WAITING_UNMUTE:
					if(pacmanIndicator.isShown()) pacmanIndicator.setVisibility(View.GONE);
					mPlayPause.setImageDrawable(
							ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
					break;
			}
		//}
	}

	public void updateMetadata(Metadata metadata) {
		//if(this.metadata==null && metadata==null) return;
		if (getActivity() == null) {
			Timber.w("onLeftToRightSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		if(metadata!=null) {
			this.metadata=metadata;
		}else {
			this.metadata=null;
		}
		String currentArtist=null;
		String currentSong=null;
		if(this.metadata!=null){
			currentArtist=this.metadata.getArtist();
			currentSong=this.metadata.getSong();
		}
		if (getActivity() == null) {
			Timber.w("onPlaybackStateChanged called when getActivity null," +
					"this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		rootView.setVisibility(View.VISIBLE);
		if(currentArtist!=null) {
			artist=currentArtist;
		}else artist="";
		if(currentSong!=null) {
			song=currentSong;
		}else song="";

		mArtist.setText(artist);
		mSong.setText(song);
	}




	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		//Timber.w("check");
		if (context instanceof PlaybackControlsFragment.OnControlsFragmentInteractionListener) {
			mListener = (PlaybackControlsFragment.OnControlsFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnListFragmentInteractionListener");
		}
	}





	@Override
	public void onDetach() {
		super.onDetach();
//		Timber.w("check");
		//if(EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
		mListener = null;
	}
public void onButtonClicked(int which){
	if (getActivity() == null) {
		Timber.w("onLeftToRightSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
		return;
	}
	Timber.w("<");
	if(status==STATUS_PLAYING || status==STATUS_STARTING || status==STATUS_SWITCHING) {
		if (which == 0) updateButtons(STATUS_PAUSING);
		else updateButtons(STATUS_SWITCHING);
	}else {
		if (which == 0) updateButtons(STATUS_STARTING);
		else updateButtons(STATUS_STARTING);
	}
	mListener.onControlsFragmentInteraction(which);
}

	public OnSwipeListener getOnSwipeListener(View rootView){
		return new OnSwipeListener(rootView) {
			@Override
			public void onLeftToRightSwipe() {
				super.onLeftToRightSwipe();
				onButtonClicked(-1);
			}

			@Override
			public void onRightToLeftSwipe() {
				super.onRightToLeftSwipe();

				onButtonClicked(1);
			}
		};
	}

	public void hide() {
		if (getActivity() == null) {
			Timber.w("hide called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		if(rootView!=null)rootView.setVisibility(View.GONE);
	}
	public boolean isShown(){
		if(rootView==null) return false;
		else return rootView.isShown();
	}
	public void show(NowPlaying nowPlaying){
		if (getActivity() == null) {
			Timber.w("hide called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		if(nowPlaying!=null){
			if(nowPlaying.getStation()!=null) {
				this.station=nowPlaying.getStation();
				updateStation(nowPlaying.getStation());
			}
			this.status=nowPlaying.getStatus();
			this.metadata=nowPlaying.getMetadata();
			if(status!=STATUS_IDLE || status!=STATUS_PLAYING) updateButtons(status);
			else updateButtons(STATUS_IDLE);
			updateMetadata(metadata);
		}
		if(rootView!=null)rootView.setVisibility(View.VISIBLE);
	}


	public interface OnControlsFragmentInteractionListener {
		void onControlsFragmentInteraction(int value);
	}
}

