package com.stc.radio.player.playback;/*
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.stc.radio.player.MusicService;
import com.stc.radio.player.contentmodel.Retro;
import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.model.MusicProviderSource;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.MediaIDHelper;
import com.stc.radio.player.utils.StreamLinkDecoder;

import timber.log.Timber;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;
import static com.google.android.exoplayer2.C.STREAM_TYPE_MUSIC;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
public class ExoPlayback implements Playback, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {

    private static final String TAG = "LocalPlayback";

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private final Context mContext;
    private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private final MusicProvider mMusicProvider;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile int mCurrentPosition;
    private volatile String mCurrentMediaId;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;
    private SimpleExoPlayer mMediaPlayer;

	BandwidthMeter bandwidthMeter;
	TrackSelection.Factory videoTrackSelectionFactory;
	MappingTrackSelector trackSelector;
	LoadControl loadControl;

	private DebugTextViewHelper debugViewHelper;
	private DefaultDataSourceFactory dataSourceFactory;
	private DefaultExtractorsFactory extractorsFactory;


	private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

	public ExoPlayback(Context context, MusicProvider musicProvider) {
        this.mContext = context;
        this.mMusicProvider = musicProvider;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.mWifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
        this.mState = PlaybackStateCompat.STATE_NONE;
    }

    @Override
    public void start() {
    }


	@Override
	public void pause() {
		Timber.w(" isPlaying? %b, state=%d", isPlaying(), mState);

		mState = PlaybackStateCompat.STATE_PAUSED;
		if(mMediaPlayer!=null && mMediaPlayer.getPlayWhenReady()){
			mMediaPlayer.setPlayWhenReady(false);
			//mMediaPlayer.stop();
		}
		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
		giveUpAudioFocus();
		unregisterAudioNoisyReceiver();
		relaxResources(false);
	}
    @Override
    public void stop(boolean notifyListeners) {
	    Timber.w(" isPlaying? %b, state=%d", isPlaying(), mState);
	    mState = PlaybackStateCompat.STATE_PAUSED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        relaxResources(true);
    }

    @Override
    public void setState(int state) {
        this.mState = state;
    }

    @Override
    public int getState() {
        return mState;
    }
    public int getExoState(){
	    if(mMediaPlayer==null)return -1;
	    return mMediaPlayer.getPlaybackState();
    }
	public boolean getExoPlayWhenReady(){
		if(mMediaPlayer==null)return false;

		return mMediaPlayer.getPlayWhenReady();
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.getPlayWhenReady();
    }

    @Override
    public int getCurrentStreamPosition() {
        return -1;
    }

    @Override
    public void updateLastKnownStreamPosition() {

    }
	@Override
	public void setPlayerView(SimpleExoPlayerView playerView) {
		playerView.setPlayer(mMediaPlayer);
	}

    @Override
    public void play(QueueItem item) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }
        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
	        mState = PlaybackStateCompat.STATE_BUFFERING;
	        mMediaPlayer.setPlayWhenReady(true);
	        configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_PAUSED;
            relaxResources(false);
            MediaMetadataCompat track = mMusicProvider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));
            //noinspection ResourceType
            String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
	        createMediaPlayerIfNeeded();
	        mState = PlaybackStateCompat.STATE_BUFFERING;
	        Timber.w("url: %s", source);
	        tryToPlayAsync(source);
	        mWifiLock.acquire();
	        if (mCallback != null) {
	            mCallback.onPlaybackStatusChanged(mState);
	        }


        }
    }
	private void tryToPlayAsync(String url)  {
		Log.d(TAG, "tryToPlayAsync: "+url);
		String token = Retro.getToken();
		if(url.contains("?") && !url.contains(token)){
			String oldToken = url.substring(url.indexOf("?")+1);
			url=url.replace(oldToken, token);
			Log.d(TAG, "tryToPlayAsync: newUrl="+url);
		}
		if (checkSuffix(url)) {
			decodeStremLink(url);
			return;
		}
		MediaSource audioSource = new ExtractorMediaSource(Uri.parse(url),
				dataSourceFactory, extractorsFactory, null, null);
		mMediaPlayer.prepare(audioSource);
		mMediaPlayer.setPlayWhenReady(true);
	}


	public boolean checkSuffix(String streamUrl) {
		String SUFFIX_PLS = ".pls";
		String SUFFIX_RAM = ".ram";
		String SUFFIX_WAX = ".wax";
		return streamUrl.contains(SUFFIX_PLS) ||
				streamUrl.contains(SUFFIX_RAM) ||
				streamUrl.contains(SUFFIX_WAX);
	}

	private void decodeStremLink(String streamLink) {
		new StreamLinkDecoder(streamLink) {
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);

				tryToPlayAsync(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	private void configMediaPlayerState() {
		LogHelper.d(TAG, "configMediaPlayerState. mAudioFocus=", mAudioFocus);
		if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
			// If we don't have audio focus and can't duck, we have to pause,
			if (mMediaPlayer!=null && mMediaPlayer.getPlayWhenReady()) {
				pause();
			}
		} else {  // we have audio focus:
			if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
				mMediaPlayer.setVolume(VOLUME_DUCK); // we'll be relatively quiet
			} else {
				if (mMediaPlayer != null) {
					mMediaPlayer.setVolume(VOLUME_NORMAL); // we can be loud again
				} // else do something for remote client.
			}
			// If we were playing when we lost focus, we need to resume playing.
			if (mPlayOnFocusGain) {
				if (mMediaPlayer != null && !mMediaPlayer.getPlayWhenReady()) {
					LogHelper.d(TAG,"configMediaPlayerState startMediaPlayer. seeking to ",
							mCurrentPosition);
					mState = PlaybackStateCompat.STATE_BUFFERING;
					mMediaPlayer.setPlayWhenReady(true);
				}
				mPlayOnFocusGain = false;
			}
		}
		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
	}

    @Override
    public void seekTo(int position) {
        LogHelper.d(TAG, "seekTo called with ", position);
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setCurrentStreamPosition(int pos) {
        this.mCurrentPosition = pos;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.mCurrentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMediaId;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        LogHelper.d(TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        LogHelper.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mMediaPlayer!=null && !mMediaPlayer.getPlayWhenReady() && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            LogHelper.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: ", focusChange);
        }
        configMediaPlayerState();
    }

    private void createMediaPlayerIfNeeded() {
        LogHelper.d(TAG, "createMediaPlayerIfNeeded. needed? ", (mMediaPlayer==null));
        if (mMediaPlayer == null) {
	        bandwidthMeter = new DefaultBandwidthMeter();
	        videoTrackSelectionFactory =
			        new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
	        trackSelector =
			        new DefaultTrackSelector(videoTrackSelectionFactory);
	        loadControl = new DefaultLoadControl();
	        mMediaPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector, loadControl);
	        mMediaPlayer.addListener(this);
	        mMediaPlayer.setAudioStreamType(STREAM_TYPE_MUSIC);
	        dataSourceFactory = new DefaultDataSourceFactory(mContext,
			        Util.getUserAgent(mContext, mContext.getPackageName()));
	        extractorsFactory = new DefaultExtractorsFactory();
        } else {
            mMediaPlayer.setPlayWhenReady(false);
        }

    }

    private void relaxResources(boolean releaseMediaPlayer) {
        LogHelper.d(TAG, "relaxResources. releaseMediaPlayer=", releaseMediaPlayer);
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
	        mPlayOnFocusGain = false;
        }
	    if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {
		Timber.w("onTimelineChanged");
	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		Timber.w("onTracksChanged: %b %d",mMediaPlayer.getPlayWhenReady(),mMediaPlayer.getPlaybackState());
		mState=PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
		mCallback.onPlaybackStatusChanged(mState);
	}

	@Override
	public void onLoadingChanged(boolean isLoading) {
		Timber.w("onLoadingChanged isLoading=%b",isLoading);
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		Timber.w("onPlayerStateChanged %b %d", playWhenReady, playbackState);
		mState=detectPlaybackStateCompact(playWhenReady, playbackState);
		configMediaPlayerState();
	}
	private int detectPlaybackStateCompact(boolean playWhenReady, int playbackState){
		if(playWhenReady && playbackState==ExoPlayer.STATE_IDLE){
			return PlaybackStateCompat.STATE_BUFFERING;
		}else if(playWhenReady && playbackState==ExoPlayer.STATE_BUFFERING){
			return PlaybackStateCompat.STATE_BUFFERING;
		}else if(playWhenReady && playbackState==ExoPlayer.STATE_READY){
			return PlaybackStateCompat.STATE_PLAYING;
		}else if(!playWhenReady && (playbackState==ExoPlayer.STATE_IDLE ||
				playbackState==ExoPlayer.STATE_ENDED  ||
				playbackState==ExoPlayer.STATE_READY)){
			return PlaybackStateCompat.STATE_PAUSED;
		}
		return PlaybackStateCompat.STATE_ERROR;
	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		int i = -1;
		boolean b = false;
		if (mMediaPlayer != null) {
			b = mMediaPlayer.getPlayWhenReady();
			i=mMediaPlayer.getPlaybackState();
		}
		Timber.w("onPlayerError %b %d", b, i);
		mState=PlaybackStateCompat.STATE_ERROR;
		if (mCallback != null) {
			//mCallback.onPlaybackStatusChanged(mState);
			mCallback.onError("MediaPlayer error " +  error.getMessage());
		}
	}

	@Override
	public void onPositionDiscontinuity() {
		LogHelper.d(TAG, "onCompletion from MediaPlayer");
		if (mCallback != null) {
			mCallback.onCompletion();
		}
	}

}
