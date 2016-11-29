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
package com.example.android.uamp.ui;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.MusicService;
import com.example.android.uamp.model.MyMetadata;
import com.example.android.uamp.utils.LogHelper;
import com.stc.radio.player.R;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.OnSwipeListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment {

	private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);
	private ImageButton mPlayPrev, mPlayPause, mPlayNext;
	private TextView mSong;
	private TextView mArtist;
	private TextView mStation;
	private ImageView mAlbumArt;
	private View rootView;
	String song;
	String artist;
	String artUrl;
	private int status;
	private Station station;
	private FrameLayout pacmanIndicator;
	EventBus bus=EventBus.getDefault();
	// Receive callbacks from the MediaController. Here we update our state such as which queue
	// is being shown, the current title and description and the PlaybackState.
	private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
		@Override
		public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
			LogHelper.d(TAG, "Received playback state change to state ", state.getState());
			PlaybackControlsFragment.this.onPlaybackStateChanged(state);
		}

		@Override
		public void onMetadataChanged(MediaMetadataCompat metadata) {
			if (metadata == null) {
				return;
			}
			LogHelper.d(TAG, "Received metadata state change to mediaId=",
					metadata.getDescription().getMediaId(),
					" artist=", metadata.getDescription().getTitle());
			PlaybackControlsFragment.this.onMetadataChanged(metadata);
		}
	};
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMyMetadataUpdate(MyMetadata myMetadata){
		CharSequence s = "" + myMetadata;
		if(mArtist!=null) mArtist.setText(s);
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
		mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);

		mPlayNext = (ImageButton) rootView.findViewById(R.id.play_next);
		mPlayPause.setOnClickListener(mButtonListener);
		pacmanIndicator.setOnClickListener(mButtonListener);
		//mAlbumArt.setOnClickListener(	v -> onButtonClicked(0));
		mPlayNext.setOnClickListener(mButtonListener);
		mPlayPrev.setOnClickListener(mButtonListener);


		mSong = (TextView) rootView.findViewById(R.id.title);
		mArtist = (TextView) rootView.findViewById(R.id.artist);
		mStation = (TextView) rootView.findViewById(R.id.station);
		rootView.setOnTouchListener(getOnSwipeListener(rootView));

	    /*
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MediaControllerCompat controller = ((FragmentActivity) getActivity())
                        .getSupportMediaController();
                MediaMetadataCompat metadata = controller.getMetadata();
                if (metadata != null) {
                    intent.putExtra(MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
                        metadata.getDescription());
                }
                startActivity(intent);
            }
        });
        */
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		LogHelper.d(TAG, "fragment.onStart");
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if(!bus.isRegistered(this))bus.register(this);

		if (controller != null) {
			onConnected();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		LogHelper.d(TAG, "fragment.onStop");
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.unregisterCallback(mCallback);
		}
		if(bus.isRegistered(this))bus.unregister(this);

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void onConnected() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		LogHelper.d(TAG, "onConnected, mediaController==null? ", controller == null);
		if (controller != null) {
			onMetadataChanged(controller.getMetadata());
			onPlaybackStateChanged(controller.getPlaybackState());
			controller.registerCallback(mCallback);
		}
		if(!bus.isRegistered(this))bus.register(this);
	}

	private void onMetadataChanged(MediaMetadataCompat metadata) {
		LogHelper.d(TAG, "onMetadataChanged ", metadata);
		if (getActivity() == null) {
			LogHelper.w(TAG, "onMetadataChanged called when getActivity null," +
					"this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		if (metadata == null  || metadata.getDescription()==null) {
			return;
		}
		if(metadata.getDescription().getTitle()!=null)
			setExtraInfo((String) metadata.getDescription().getTitle());
		if(metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE)!=null)
			mStation.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE));
		else if(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)!=null)
			setExtraInfo( metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
		String artUrl = null;
		if (metadata.getDescription().getIconUri() != null) {
			artUrl = metadata.getDescription().getIconUri().toString();
		}

		if (true || !TextUtils.equals(artUrl, this.artUrl)) {


			this.artUrl = artUrl;
			Bitmap art = metadata.getDescription().getIconBitmap();
			AlbumArtCache cache = AlbumArtCache.getInstance();
			if (art == null) {
				art = cache.getIconImage(this.artUrl);
			}
			if (art != null) {
				mAlbumArt.setImageBitmap(art);
			} else {
				cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
							@Override
							public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {

								if (icon != null) {
									LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
											" h=", icon.getHeight());
									if (isAdded()) {

										mAlbumArt.setImageBitmap(icon);
										//onMyMetadataUpdate(new MyMetadata(""));
									}
								}
							}
						}
				);
			}
		}
	}

	public void setExtraInfo(String extraInfo) {
		if (extraInfo!= null) {
			mSong.setVisibility(View.VISIBLE);
			mSong.setText(extraInfo);
		}
	}

	private void onPlaybackStateChanged(PlaybackStateCompat state) {
		LogHelper.d(TAG, "onPlaybackStateChanged ", state);
		if (getActivity() == null) {
			LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
					"this should not happen if the callback was properly unregistered. Ignoring.");
			return;
		}
		if (state == null) {
			return;
		}
		boolean enablePlay = false;
		switch (state.getState()) {
			case PlaybackStateCompat.STATE_PAUSED:
			case PlaybackStateCompat.STATE_STOPPED:
				enablePlay = true;
				break;
			case PlaybackStateCompat.STATE_ERROR:
				LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
				Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
				break;
		}

		if (enablePlay) {
			mPlayPause.setImageDrawable(
					ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
		} else {
			mPlayPause.setImageDrawable(
					ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_pause));
		}

		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		String extraInfo = null;
		if (controller != null && controller.getExtras() != null) {
			String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
			if (castName != null) {
				extraInfo = getResources().getString(R.string.casting_to_device, castName);
			}
		}
		setExtraInfo(extraInfo);
	}

	public OnSwipeListener getOnSwipeListener(View rootView) {
		return new OnSwipeListener(rootView) {

			@Override
			public void onTopToBottomSwipe() {
				mButtonListener.onClick(mPlayPause);
			}

			@Override
			public void onBottomToTopSwipe() {

			}

			@Override
			public void onLeftToRightSwipe() {
			}

			@Override
			public void onRightToLeftSwipe() {

			}
		};
	}


	private final View.OnClickListener mButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			MediaControllerCompat controller = ((FragmentActivity) getActivity())
					.getSupportMediaController();
			PlaybackStateCompat stateObj = controller.getPlaybackState();
			final int state = stateObj == null ?
					PlaybackStateCompat.STATE_NONE : stateObj.getState();
			LogHelper.d(TAG, "Button pressed, in state " + state);
			switch (v.getId()) {
				case R.id.play_pause:
				case R.id.pacman_layout:
					LogHelper.d(TAG, "Play button pressed, in state " + state);
					if (state == PlaybackStateCompat.STATE_PAUSED ||
							state == PlaybackStateCompat.STATE_STOPPED ||
							state == PlaybackStateCompat.STATE_NONE) {
						playMedia();
					} else if (state == PlaybackStateCompat.STATE_PLAYING ||
							state == PlaybackStateCompat.STATE_BUFFERING ||
							state == PlaybackStateCompat.STATE_CONNECTING) {
						pauseMedia();
					}
					break;
				case R.id.play_next:
					nextMedia();
					break;
				case R.id.play_prev:
					prevMedia();
					break;
			}
		}

		;

	};


	public void nextMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().skipToNext();
		}
	}

	public void prevMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().skipToPrevious();
		}
	}

	private void playMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().play();
		}
	}

	private void pauseMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().pause();
		}
	}
}
