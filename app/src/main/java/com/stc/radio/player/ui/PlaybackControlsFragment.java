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
package com.stc.radio.player.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.stc.radio.player.OnSwipeListener;
import com.stc.radio.player.R;
import com.stc.radio.player.RequestArt;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

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


    private ImageButton mPlayPause;
    private TextView mSong;
    private TextView mArtist;
    private TextView mStation;
    private ImageView mAlbumArt;
	private View rootView;
	private EventBus bus=EventBus.getDefault();
	String stationName;
	String song;
	String artist;
	String url;
	int uiState;
	private OnControlsFragmentInteractionListener mListener;
	Bitmap art;

	public String getUrl() {
		return url;
	}

	public PlaybackControlsFragment() {
	}

	public static PlaybackControlsFragment newInstance(){
		return new PlaybackControlsFragment();
	}
	public static PlaybackControlsFragment newInstance(PlaybackControlsFragment oldF,String url) {
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


		/*if(savedInstanceState!=null){
			this.stationName=savedInstanceState.getString(ARG_STATION);
			this.url=savedInstanceState.getString(ARG_URL);
			this.artist=savedInstanceState.getString(ARG_ARTIST);
			this.song=savedInstanceState.getString(ARG_SONG);
			this.uiState=savedInstanceState.getInt(ARG_UI_STATE);
			onPlaybackStateChanged(stationName, url, artist,song,uiState);
		}*/
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		NowPlaying nowPlaying= DbHelper.getNowPlaying();
		nowPlaying.withArtist(artist)
				.withSong(song)
				.save();
		super.onSaveInstanceState(outState);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		Timber.w("check");
		if(!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

		mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
	    mPlayPause.setEnabled(true);

        mPlayPause.setOnClickListener(mButtonListener);
        mSong = (TextView) rootView.findViewById(R.id.title);
	    mArtist = (TextView) rootView.findViewById(R.id.artist);
	    mStation = (TextView) rootView.findViewById(R.id.extra_info);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
		art= getArguments().getParcelable(ARG_ART_BITMAP);
		Timber.w("art %s", art);
		//if(art!=null) mAlbumArt.setImageBitmap(art);
		//else bus.post(new RequestArt());
		bus.post(new RequestArt());
	    rootView.setOnTouchListener(getOnSwipeListener(rootView));

		updateStation(DbHelper.getCurrentStation().name, DbHelper.getCurrentStation().url);
		updateMetadata(DbHelper.getNowPlaying().artist, DbHelper.getNowPlaying().song);
		updateButtons(DbHelper.getNowPlaying().getUiState());
        return rootView;
    }
	public OnSwipeListener getOnSwipeListener(View rootView){
		return new OnSwipeListener(rootView) {
		@Override
		public void onLeftToRightSwipe() {
			super.onLeftToRightSwipe();
			if (getActivity() == null) {
				Timber.w("onLeftToRightSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
				return;
			}
			Timber.w("onLeftToRightSwipe");
			updateButtons(MainActivity.UI_STATE.LOADING);
			mListener.onControlsFragmentInteraction(-1);
		}

		@Override
		public void onRightToLeftSwipe() {
			super.onRightToLeftSwipe();
			if (getActivity() == null) {
				Timber.w("onRightToLeftSwipe called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
				return;
			}
			Timber.w("onRightToLeftSwipe");
			updateButtons(MainActivity.UI_STATE.LOADING);
			mListener.onControlsFragmentInteraction(1);
		}
	};
	}


    @Override
    public void onStart() {
        super.onStart();
	    Timber.w("check");
    }

    @Override
    public void onStop() {
        super.onStop();
	    Timber.w("check");

    }
	/*public void onMetadataChanged(Metadata metadata) {
        Timber.d("onMetadataChanged %s %s %s", metadata.getArtist(), metadata.getSong(), metadata.getUrl());
        if (getActivity() == null) {
            Timber.w("onMetadataChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

		if (metadata.getArtist() == null)mArtist.setText("");
		else mArtist.setText(metadata.getArtist());
		if (metadata.getSong() == null)mSong.setText("");
		else mSong.setText(metadata.getSong());
    }
*/
	public void updateButtons(int state){
		if(state!=uiState){
			uiState=state;
			if(uiState == MainActivity.UI_STATE.IDLE){
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_play));
				updateMetadata(null,null);
			}else if (uiState == MainActivity.UI_STATE.LOADING) {
				updateMetadata("loading...",null);
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
			} else {
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
			}
		}
	}
	public void updateStation(String station, String url){
		stationName="";
		if(station==null) {
			rootView.setVisibility(View.GONE);
		}else {
			rootView.setVisibility(View.VISIBLE);
			stationName = station;
			mStation.setText(stationName);
		}

	}



	public void onPlaybackStateChanged(int state) {
		Timber.d("onPlaybackStateChanged %s", state);
		if (getActivity() == null) {
			Timber.w("onPlaybackStateChanged called when getActivity null," +
					"this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		updateButtons(state);
	}
	public void updateMetadata(String currentArtist, String currentSong) {
		if(currentArtist!=null) {
			artist=currentArtist;
			mArtist.setText(artist);
		}
		if(currentSong!=null) {
			song=currentSong;
			mSong.setText(song);
		}
	}
	public void updateMetadata(Metadata metadata) {

		if(metadata.getArtist()!=null) artist=metadata.getArtist();
		else artist="";
		mArtist.setText(artist);

		if(metadata.getSong()!=null) song=metadata.getSong();
		else song="";
		mSong.setText(song);
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Timber.w("check");
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
		Timber.w("check");
		if(EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
		mListener = null;
	}



	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNewArt(Bitmap bitmap){
		Timber.d("onNewArt %s", bitmap.toString());
		assertNotNull(bitmap);
		if(art==null || !art.equals(bitmap)) {
			art=bitmap;
			mAlbumArt.setImageBitmap(art);
		}
	}

	public Bitmap getArtBitmap() {
		return art;
	}

	public final View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
	        Toast.makeText(getActivity(), "Playpause pressed", Toast.LENGTH_SHORT).show();
	        mListener.onControlsFragmentInteraction(0);
        }
    };

	public interface OnControlsFragmentInteractionListener {
		void onControlsFragmentInteraction(int value);
	}
	public void onPlaybackStateChanged(String stationName, String url, String artist, String song, int state) {
		/*Timber.d("onPlaybackStateChanged %s", state);
		if (getActivity() == null) {
			Timber.w("onPlaybackStateChanged called when getActivity null," +
					"this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}

		this.stationName=stationName;
		this.artist=artist;
		this.song=song;
		this.url=url;
		this.uiState=state;
		updateUi();

	    if (!TextUtils.equals(artUrl, mArtUrl)) {
		    mArtUrl = artUrl;
		    Bitmap art = null;*//*metadata.getDescription().getIconBitmap();*//*
		    AlbumArtCache cache = AlbumArtCache.getInstance();
		    if (art == null) {
			    art = cache.getIconImage(mArtUrl);
		    }
		    if (art != null) {
			    mAlbumArt.setImageBitmap(art);
		    } else {
			    cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
						    @Override
						    public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
							    if (icon != null) {
								    Timber.d("album art icon of w=%d h=%d", icon.getWidth(), icon.getHeight());
								    if (isAdded()) {
									    mAlbumArt.setImageBitmap(icon);
								    }
							    }
						    }
					    }
			    );
		    }
	    }
*/
	}
}

