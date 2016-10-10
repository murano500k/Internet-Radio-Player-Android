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

import com.stc.radio.player.BitmapManager;
import com.stc.radio.player.OnSwipeListener;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;


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

	public int getUiState() {
		return uiState;
	}

	public String getSong() {
		return song;
	}

	public String getArtist() {
		return artist;
	}

	public String getUrl() {
		return url;
	}

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

		super.onSaveInstanceState(outState);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

		//if(!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
		mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
	    mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);
        mSong = (TextView) rootView.findViewById(R.id.title);
	    mArtist = (TextView) rootView.findViewById(R.id.artist);
	    mStation = (TextView) rootView.findViewById(R.id.extra_info);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
		rootView.setOnTouchListener(getOnSwipeListener(rootView));
		Station station = DbHelper.getActiveStation();

		if(station!=null) {
			File artFile = ((BitmapManager) getActivity()).getArtFile(station.artPath);
			mStation.setText(station.name);
			if (artFile.exists()) {
				mAlbumArt.setImageBitmap(((BitmapManager) getActivity()).getArtBitmap(artFile));
			}else fail();
			Timber.w("init %d %s %s %s", DbHelper.getNowPlaying().getUiState(), station.name,DbHelper.getNowPlaying().artist, DbHelper.getNowPlaying().song );
			updateButtons(DbHelper.getNowPlaying().getUiState());
			if(DbHelper.getNowPlaying().getUiState()== MainActivity.UI_STATE.LOADING) updateMetadata("loading...", null);
			else if (DbHelper.getNowPlaying().getUiState()== MainActivity.UI_STATE.IDLE) updateMetadata(null, null);
			else updateMetadata(DbHelper.getNowPlaying().artist, DbHelper.getNowPlaying().song);
		}else {
			rootView.setVisibility(View.GONE);
			Timber.e("ERROR NO ACTIVE STATION");
		}
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
	    //Timber.w("check");
    }

    @Override
    public void onStop() {
        super.onStop();
	    //Timber.w("check");

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
		rootView.setVisibility(View.VISIBLE);
		Timber.d("state %d", state);
			uiState=state;
			if(uiState == MainActivity.UI_STATE.IDLE){
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_play));
				updateMetadata(null,null);
			}else if (uiState == MainActivity.UI_STATE.LOADING) {
				//updateMetadata("loading...",null);
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_loading));
			} else {
				mPlayPause.setImageDrawable(
						ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
			}

	}




	public void updateMetadata(String currentArtist, String currentSong) {
		//Timber.d("onMetadataChanged %s", currentArtist);
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
		//updateButtons(MainActivity.UI_STATE.PLAYING);
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

