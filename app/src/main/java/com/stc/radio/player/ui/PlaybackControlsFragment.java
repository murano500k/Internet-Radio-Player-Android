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
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
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

import com.google.firebase.analytics.FirebaseAnalytics;
import com.like.LikeButton;
import com.stc.radio.player.R;
import com.stc.radio.player.playback.PlaybackManager;
import com.stc.radio.player.service.MusicService;
import com.stc.radio.player.utils.AlbumArtCache;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.OnSwipeListener;

import timber.log.Timber;

import static com.stc.radio.player.playback.PlaybackManager.CUSTOM_ACTION_THUMBS_UP;
import static com.stc.radio.player.playback.PlaybackManager.IS_FAVORITE;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment {

	private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);
	private ImageButton mPlayPrev, mPlayPause, mPlayNext;
	private TextView mNowPlayingTitle;
	LikeButton favButton;
	private boolean isFav;

	public ImageView mAlbumArt;
	private View rootView;
	private FirebaseAnalytics mFirebaseAnalytics;
	String artUrl;
	private final MediaController.Callback mCallback = new MediaController.Callback() {
		@Override
		public void onPlaybackStateChanged(@NonNull PlaybackState state) {
			LogHelper.d(TAG, "Received playback state change to state ", state.getState());
			PlaybackControlsFragment.this.onPlaybackStateChanged(state);
		}

		@Override
		public void onMetadataChanged(MediaMetadata metadata) {
			if (metadata == null) {
				return;
			}
			LogHelper.d(TAG, "Received metadata state change to mediaId=",
					metadata.getDescription().getMediaId(),
					" title=", metadata.getDescription().getTitle());
			PlaybackControlsFragment.this.onMetadataChanged(metadata);
		}
	};



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		mFirebaseAnalytics=FirebaseAnalytics.getInstance(getActivity());
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
		mNowPlayingTitle = (TextView) rootView.findViewById(R.id.title);
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
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			onConnected();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		LogHelper.d(TAG, "fragment.onStop");
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			controller.unregisterCallback(mCallback);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void onConnected() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		LogHelper.d(TAG, "onConnected, mediaController==null? ", controller == null);
		if (controller != null) {
			onMetadataChanged(controller.getMetadata());
			onPlaybackStateChanged(controller.getPlaybackState());
			controller.registerCallback(mCallback);
		}
	}

	private void onMetadataChanged(MediaMetadata metadata) {
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
		else if(metadata.getString(MediaMetadata.METADATA_KEY_TITLE)!=null)
			setExtraInfo( metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
		String artUrl = null;
		if (metadata.getDescription().getIconUri() != null) {
			artUrl = metadata.getDescription().getIconUri().toString();
		}

		if (!TextUtils.equals(artUrl, this.artUrl)) {
			this.artUrl = artUrl;
			Bitmap art = metadata.getDescription().getIconBitmap();
			AlbumArtCache cache = AlbumArtCache.getInstance();
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


        int fadeInDuration = 1000; // Configure time values here
        int timeBetween = 3000;
        int fadeOutDuration = 1000;


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
			mNowPlayingTitle.setVisibility(View.VISIBLE);
			mNowPlayingTitle.setText(extraInfo);
		}
	}

	private void onPlaybackStateChanged(PlaybackState state) {
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
		for(PlaybackState.CustomAction action: state.getCustomActions())
		if(action.getAction().contains(CUSTOM_ACTION_THUMBS_UP)) {
			isFav=action.getExtras().containsKey(IS_FAVORITE) && action.getExtras().getBoolean(IS_FAVORITE);
			favButton.setLiked(isFav);
			break;
		}
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
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
				getActivity().getDrawable( PlaybackManager.getPlayPauseIcon(state)));
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
			MediaController controller = ((FragmentActivity) getActivity())
					.getMediaController();
			PlaybackState stateObj = controller.getPlaybackState();
			final int state = stateObj == null ?
					PlaybackState.STATE_NONE : stateObj.getState();
			LogHelper.d(TAG, "Button pressed, in state " + state);
			switch (v.getId()) {
				case R.id.play_pause:
					LogHelper.d(TAG, "Play button pressed, in state " + state);
					if (state == PlaybackState.STATE_PAUSED ||
							state == PlaybackState.STATE_STOPPED ||
							state == PlaybackState.STATE_NONE) {
						playMedia();
					} else if (state == PlaybackState.STATE_PLAYING ||
							state == PlaybackState.STATE_BUFFERING ||
							state == PlaybackState.STATE_SKIPPING_TO_NEXT) {
						pauseMedia();
					}else if( state == PlaybackState.STATE_ERROR){
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
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			Bundle customActionExtras=new Bundle();
			customActionExtras.putBoolean(IS_FAVORITE, !favButton.isActivated());
			logStationSelected(controller.getMetadata().getDescription().getTitle(), !favButton.isActivated());
			controller.getTransportControls().sendCustomAction(CUSTOM_ACTION_THUMBS_UP,customActionExtras);
		}
	}

	private void logStationSelected(CharSequence title, boolean state){
		Bundle bundle = new Bundle();
		bundle.putBoolean(FirebaseAnalytics.Param.VALUE, state);
		bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, title.toString());
		mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, bundle);
	}


	public void nextMedia() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			controller.getTransportControls().skipToNext();
		}
	}

	public void prevMedia() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			controller.getTransportControls().skipToPrevious();
		}
	}

	public void playMedia() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			controller.getTransportControls().play();
		}
	}

	public void pauseMedia() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			if(controller.getPlaybackState().getState()== PlaybackState.STATE_ERROR){
				controller.getTransportControls().stop();
			}else {
				controller.getTransportControls().pause();
			}
		}
	}
	public void stopMedia() {
		MediaController controller = ((FragmentActivity) getActivity())
				.getMediaController();
		if (controller != null) {
			controller.getTransportControls().stop();
		}
	}

}
