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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.like.LikeButton;
import com.stc.radio.player.AlbumArtCache;
import com.stc.radio.player.MusicService;
import com.stc.radio.player.R;
import com.stc.radio.player.model.MyMetadata;
import com.stc.radio.player.playback.PlaybackManager;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.OnSwipeListener;

import timber.log.Timber;

import static com.stc.radio.player.playback.PlaybackManager.CUSTOM_ACTION_THUMBS_UP;
import static com.stc.radio.player.playback.PlaybackManager.IS_FAVORITE;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment {
	private String musicId;

	private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);
	private ImageButton mPlayPrev, mPlayPause, mPlayNext;
	private TextView mSong;
	LikeButton favButton;
	private boolean isFav;

	private TextView mArtist;
	private TextView mStation;
	public ImageView mAlbumArt;
	private View rootView;

	String artUrl;
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
					" title=", metadata.getDescription().getTitle());
			PlaybackControlsFragment.this.onMetadataChanged(metadata);
		}
	};

	public void onMyMetadataUpdate(MyMetadata myMetadata){
		CharSequence s = "" + myMetadata;
		if(mArtist!=null) mArtist.setText(s);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		setRetainInstance(true);
		rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
		mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
		mPlayPause.setEnabled(true);
		mPlayPrev = (ImageButton) rootView.findViewById(R.id.play_prev);
		mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);

		mPlayNext = (ImageButton) rootView.findViewById(R.id.play_next);
		mPlayPause.setOnClickListener(mButtonListener);
		mPlayNext.setEnabled(true);
		mPlayPrev.setEnabled(true);
		mPlayNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				nextMedia();
			}
		});
		mPlayPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				prevMedia();
			}
		});
		favButton = (LikeButton) rootView.findViewById(R.id.fav_button_control);
		mSong = (TextView) rootView.findViewById(R.id.title);
		mArtist = (TextView) rootView.findViewById(R.id.artist);
		mStation = (TextView) rootView.findViewById(R.id.station);
		favButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				customAction();
			}
		});
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		LogHelper.d(TAG, "fragment.onStart");
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
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
		else if(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)!=null)
			setExtraInfo( metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
		String artUrl = null;
		if (metadata.getDescription().getIconUri() != null) {
			artUrl = metadata.getDescription().getIconUri().toString();
		}

		if (!TextUtils.equals(artUrl, this.artUrl)) {
			this.artUrl = artUrl;
			Bitmap art = metadata.getDescription().getIconBitmap();
			com.stc.radio.player.AlbumArtCache cache = AlbumArtCache.getInstance();
			if (art == null) {
				art = cache.getIconImage(this.artUrl);
			}
			if (art != null) {
				animateAlbumArt(art);
			} else {
				cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
							@Override
							public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {

								if (icon != null) {
									LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
											" h=", icon.getHeight());
									if (isAdded()) {
										animateAlbumArt(icon);
									}
								}
							}
						}
				);
			}
		}
	}

	private void animateAlbumArt(Bitmap art) {
             animate(mAlbumArt, art);
	}
    private void animate(final ImageView imageView,Bitmap endImage) {


        int fadeInDuration = 500; // Configure time values here
        int timeBetween = 3000;
        int fadeOutDuration = 1000;

        imageView.setVisibility(View.INVISIBLE);    //Visible or invisible by default - this will apply when the animation ends
        imageView.setImageBitmap(endImage);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
        fadeIn.setDuration(fadeInDuration);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); // and this
        fadeOut.setStartOffset(fadeInDuration + timeBetween);
        fadeOut.setDuration(fadeOutDuration);

        AnimationSet animation = new AnimationSet(false); // change to false
        animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        animation.setRepeatCount(1);
        imageView.setAnimation(animation);

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
		updateButtonState(state.getState());
		for(PlaybackStateCompat.CustomAction action: state.getCustomActions())
		if(action.getAction().contains(CUSTOM_ACTION_THUMBS_UP)) {
			isFav=action.getExtras().containsKey(IS_FAVORITE) && action.getExtras().getBoolean(IS_FAVORITE);
			favButton.setLiked(isFav);
			break;
		}
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		String extraInfo="";
		if (controller != null && controller.getExtras() != null) {
			String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
			if (castName != null) {
				extraInfo = getResources().getString(R.string.casting_to_device, castName);
				setExtraInfo(extraInfo);
			}

		}
	}

	public void updateButtonState(int state){
		mPlayPause.setImageDrawable(
				ContextCompat.getDrawable(getActivity(), PlaybackManager.getPlayPauseIcon(state)));
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
					LogHelper.d(TAG, "Play button pressed, in state " + state);
					if (state == PlaybackStateCompat.STATE_PAUSED ||
							state == PlaybackStateCompat.STATE_STOPPED ||
							state == PlaybackStateCompat.STATE_NONE) {
						playMedia();
					} else if (state == PlaybackStateCompat.STATE_PLAYING ||
							state == PlaybackStateCompat.STATE_BUFFERING ||
							state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT) {
						pauseMedia();
					}else if( state == PlaybackStateCompat.STATE_ERROR){
						stopMedia();
					}
					break;
				default:
					Timber.w("Unknown button clicked: %d", v.getId());
			}
		}

		;

	};

	private void customAction() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			Bundle customActionExtras=new Bundle();
			//favButton.setLiked(!favButton.isActivated());
			customActionExtras.putBoolean(IS_FAVORITE, !favButton.isActivated());

			controller.getTransportControls().sendCustomAction(CUSTOM_ACTION_THUMBS_UP,customActionExtras);
		}
	}


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

	public void playMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().play();
		}
	}

	public void pauseMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			if(controller.getPlaybackState().getState()==PlaybackStateCompat.STATE_ERROR){
				controller.getTransportControls().stop();
			}else {
				controller.getTransportControls().pause();
			}
		}
	}
	public void stopMedia() {
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.getTransportControls().stop();
		}
	}

}
