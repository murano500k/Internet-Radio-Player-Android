
package com.stc.radio.player.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import com.stc.radio.player.MusicService;
import com.stc.radio.player.db.Metadata;
import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.model.MusicProviderSource;
import com.stc.radio.player.model.MyMetadata;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.MediaIDHelper;
import com.stc.radio.player.utils.RatingHelper;
import com.stc.radio.player.utils.StreamLinkDecoder;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;


public class MyLocalPlayback implements Playback, AudioManager.OnAudioFocusChangeListener, PlayerCallback {

	private static final String TAG = LogHelper.makeLogTag(MyLocalPlayback.class);

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
	private static final int AUDIO_FOCUSED = 2;

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
	private MultiPlayer mMediaPlayer;
	private boolean isPlaying;
	private String mCurrentSource;
	private int audioSessionId;

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

	public MyLocalPlayback(Context context, MusicProvider musicProvider) {
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
	public void setState(int state) {
		this.mState = state;
	}

	@Override
	public int getState() {
		return mState;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public boolean isPlaying() {
		return mPlayOnFocusGain || (mMediaPlayer != null && isPlaying);
	}

	@Override
	public int getCurrentStreamPosition() {
		return -1;
	}

	@Override
	public void updateLastKnownStreamPosition() {
			mCurrentPosition = -1;

	}

	@Override
	public void play(MediaSessionCompat.QueueItem item) {
		mPlayOnFocusGain = true;
		tryToGetAudioFocus();
		registerAudioNoisyReceiver();
		String mediaId = item.getDescription().getMediaId();
		boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
		if (mediaHasChanged) {
			mCurrentPosition = 0;
			mCurrentMediaId = mediaId;
		}
			mState = PlaybackStateCompat.STATE_STOPPED;
			relaxResources(false); // release everything except MediaPlayer
		String musicId=MediaIDHelper.extractMusicIDFromMediaID(mCurrentMediaId);
		assertNotNull(musicId);
			MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
		RatingHelper.incrementPlayedTimes(musicId);

			//noinspection ResourceType
		mCurrentSource = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
			Timber.w("NOW WILL PLAY %s", mCurrentSource);
		try {
			if(isPlaying){
				mState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
				mMediaPlayer.stop();
			}else {
				createMediaPlayerIfNeeded();
				mState = PlaybackStateCompat.STATE_PLAYING;
				tryToPlayAsync(mCurrentSource);
				mWifiLock.acquire();
				if (mCallback != null) {
					mCallback.onPlaybackStatusChanged(mState);
				}
			}
			} catch (Exception ex) {
				Timber.e("Exception playing song. %s", ex.getMessage());
				if (mCallback != null) {
					mCallback.onError(ex.getMessage());
				}
			}
		}


	@Override
	public void stop(boolean notifyListeners) {
		mState = PlaybackStateCompat.STATE_STOPPED;
		if (notifyListeners && mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
		mCurrentPosition = getCurrentStreamPosition();
		// Give up Audio focus
		giveUpAudioFocus();
		unregisterAudioNoisyReceiver();
		// Relax all resources
		relaxResources(true);
	}

	@Override
	public void pause() {

		if (mState == PlaybackStateCompat.STATE_PLAYING) {
			// Pause media player and cancel the 'foreground service' state.
			if (mMediaPlayer != null && isPlaying) {
				mMediaPlayer.stop();
				mMediaPlayer=null;
			}
			// while paused, retain the MediaPlayer but give up audio focus
			relaxResources(true);
			giveUpAudioFocus();
		}
		mState = PlaybackStateCompat.STATE_PAUSED;
		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
		unregisterAudioNoisyReceiver();
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

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and
	 * starts/restarts it. This method starts/restarts the MediaPlayer
	 * respecting the current audio focus state. So if we have focus, it will
	 * play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is
	 * allowed by the current focus settings. This method assumes mPlayer !=
	 * null, so if you are calling it, you have to do so from a context where
	 * you are sure this is the case.
	 */
	private void configMediaPlayerState() {
		Timber.w("check");

		LogHelper.d(TAG, "configMediaPlayerState. mAudioFocus=", mAudioFocus);
		if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
			// If we don't have audio focus and can't duck, we have to pause,
			if (mMediaPlayer != null && isPlaying) {
				pause();
			}
		} else if (mPlayOnFocusGain) {
				if (mMediaPlayer != null && !isPlaying) {
					mState = PlaybackStateCompat.STATE_PLAYING;
					createMediaPlayerIfNeeded();
					//tryToGetAudioFocus();
					if(mCurrentSource!=null)tryToPlayAsync(mCurrentSource);
				}
				mPlayOnFocusGain = false;
			}
		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
	}

	/**
	 * Called by AudioManager on audio focus changes.
	 * Implementation of {@link AudioManager.OnAudioFocusChangeListener}
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
				focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
				focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
			// We have lost focus. If we can duck (low playback volume), we can keep playing.
			// Otherwise, we need to pause the playback.
			if (isPlaying()) {
				mPlayOnFocusGain = true;
			}
		} else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
				|| focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
				|| focusChange == AudioManager.AUDIOFOCUS_GAIN
				|| focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE){
			LogHelper.w(TAG, "onAudioFocusChange: ", focusChange);
			mAudioFocus = AUDIO_FOCUSED;
		}
		configMediaPlayerState();
	}




	@Override
	public void playerAudioTrackCreated(AudioTrack audiotrack) {
		audioSessionId = audiotrack.getAudioSessionId();
	}

	@Override
	public void playerStopped(int i) {
		Timber.w("callback");
		isPlaying=false;
		if(mState==PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && mCurrentSource!=null) tryToPlayAsync(mCurrentSource);
		mState=PlaybackStateCompat.STATE_PAUSED;

		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
		//EventBus.getDefault().post(new MyMetadata(""));

	}

	@Override
	public void playerStarted() {
		Timber.w("callback");
		isPlaying=true;
		mState=PlaybackStateCompat.STATE_PLAYING;
		if (mCallback != null) {
			mCallback.onPlaybackStatusChanged(mState);
		}
		//configMediaPlayerState();
	}

	@Override
	public void playerException(Throwable throwable) {
		Timber.e(throwable.getMessage(), "TEST callback playerException");
		isPlaying=false;
		//mState = PlaybackStateCompat.STATE_PAUSED;
		//configMediaPlayerState();
		/*if (mCallback != null) {
			mCallback.onError("MediaPlayer error " + throwable.getMessage() + " (" + throwable.toString() + ")");
		}*/
	}

	@Override
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs){
		/*if(mState!=PlaybackStateCompat.STATE_PLAYING) {
			mState = PlaybackStateCompat.STATE_PLAYING;
			configMediaPlayerState();
		}
*/
		//bus.post(new BufferUpdate(audioBufferSizeMs, audioBufferCapacityMs, nowPlaying.isPlaying()));
	}

	@Override
	public void playerMetadata(String s1, String s2) {
		String a="";
		String s="";
		boolean post=false;
		Metadata metadata;
		if((s1!=null && s1.equals("StreamTitle")) || (s2!=null && s2.contains("kbps"))) {
			a = getArtistFromString(s2);
			s = getTrackFromString(s2);
		}else if(s1!=null && s1.contains("StreamTitle")) {
			a = s1.replace("StreamTitle=", "");
			s = "";
		}
		EventBus.getDefault().post(new MyMetadata(a+"  "+ s));
	}

	static String getArtistFromString(String data) {
		String artistName = "";
		if (data != null && data.contains(" - ")) {
			artistName = data.substring(0, data.indexOf(" - "));
		}
		return artistName;
	}

	static String getTrackFromString(String data) {
		String trackName = "";
		if (data != null && data.contains(" - ")) {
			trackName = data.substring(data.indexOf(" - ") + 3);
		}
		return trackName;
	}
	private void tryToPlayAsync(String url)  {

		if (checkSuffix(url)) {
			decodeStremLink(url);
			return;
		}
		Timber.w("BEFORE PLAY: %s", url);

		mMediaPlayer.playAsync(url);
	}


	public boolean checkSuffix(String streamUrl) {
		String SUFFIX_PLS = ".name";
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
	private void createMediaPlayerIfNeeded() {
		LogHelper.d(TAG, "createMediaPlayerIfNeeded. needed? ", (mMediaPlayer==null));
		try {
			java.net.URL.setURLStreamHandlerFactory( protocol -> {
				//Timber.w("Asking for stream handler for protocol: '%s'", protocol);
				if ("icy".equals(protocol))
					return new com.spoledge.aacdecoder.IcyURLStreamHandler();
				return null;
			});
		} catch (Throwable t) {
			Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
		}
		if (mMediaPlayer == null) {
			mMediaPlayer = new MultiPlayer(this);
			//mMediaPlayer.setResponseCodeCheckEnabled(true);
		} else {
			mMediaPlayer.stop();
		}
	}
	private void relaxResources(boolean releaseMediaPlayer) {
		LogHelper.d(TAG, "relaxResources. releaseMediaPlayer=", releaseMediaPlayer);

		// stop and release the Media Player, if it's available
		if (releaseMediaPlayer && mMediaPlayer != null) {
			mMediaPlayer = null;
		}

		// we can also release the Wifi lock, if we're holding it
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


}