/*
package com.android.murano500k.newradio;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import rx.Subscription;


public class MyService extends Service implements PlayerCallback{
	private static final String INTENT_PLAYER_UNLOCKED = "com.android.murano500k.newradio.INTENT_PLAYER_UNLOCKED";
	private static final String INTENT_PLAYER_LOCKED = "com.android.murano500k.newradio.INTENT_PLAYER_LOCKED";
	private final String TAG= "ServiceRadio";
	public static final String INTENT_PLAYER_ACTION= "com.android.murano500k.newradio.INTENT_PLAYER_ACTION";
	public static final String INTENT_PLAYER_LOCK_STATE_CHANGED= "com.android.murano500k.newradio.INTENT_PLAYER_LOCK_STATE_CHANGED";
	public static final String INTENT_USER_ACTION= "com.android.murano500k.newradio.INTENT_USER_ACTION";

	public static final String UNLOCKED_ACTION_START= "com.android.murano500k.newradio.UNLOCKED_ACTION_START";
	public static final String UNLOCKED_ACTION_STOP= "com.android.murano500k.newradio.UNLOCKED_ACTION_START";


	public static final String EXTRA_LOCK_STATE= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_STARTED= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_STOPPED= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_ERROR= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_METADATA= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_AUDIOTRACK= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";
	public static final String EXTRA_PLAYER_PCM= "com.android.murano500k.newradio.EXTRA_PLAYER_STOPPED";



	public static final String EXTRA_PLAY_PAUSE_PRESSED= "com.android.murano500k.newradio.EXTRA_PLAY_PAUSE_PRESSED";
	public static final String EXTRA_PLAY_LIST_STATION_PRESSED= "com.android.murano500k.newradio.EXTRA_PLAY_LIST_STATION_PRESSED";
	public static final String EXTRA_NEXT_PRESSED= "com.android.murano500k.newradio.EXTRA_NEXT_PRESSED";
	public static final String EXTRA_PREV_PRESSED= "com.android.murano500k.newradio.EXTRA_PREV_PRESSED";
	public static final String EXTRA_URL= "com.android.murano500k.newradio.EXTRA_URL";

	public static final String ERROR_EMPTY_URL= "Empty url";

	private int AUDIO_BUFFER_CAPACITY_MS = 800;
	private int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";

	private boolean isSwitching;
	private boolean isClosedFromNotification = false;
	private boolean mLock;
	private boolean userStopped;
	private boolean audioFocusIntrerrupted;
	private MediaSession mediaSession;
	private AudioManager audioManager;
	private int positionNotificationPeriod;
	private boolean positionSaved=false;
	private Subscription playerStoppedSubscription;
	private Subscription playerUnlockedSubscription;
	private boolean playerLocked;
	private Subscription lockStateChangedSubscription;

	public enum State {
		IDLE,
		PLAYING,
		STOPPED,
		LOADING;
	}
	private State mRadioState;
	private MultiPlayer mRadioPlayer;
	public final IBinder mLocalBinder = new LocalBinder();
	public NotifierRadio notifier;
	private NotificationProvider notificationProvider;
	private PlaylistManager playlistManager;
	private ServiceRadio.Initiator initiator;


	public MultiPlayer getPlayer() {
		try {
			java.net.URL.setURLStreamHandlerFactory(new java.net.URLStreamHandlerFactory() {

				public java.net.URLStreamHandler createURLStreamHandler(String protocol) {
					Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
					if ("icy".equals(protocol))
						return new com.spoledge.aacdecoder.IcyURLStreamHandler();
					return null;
				}
			});
		} catch (Throwable t) {
			Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
		}


		if (mRadioPlayer == null) {
			mRadioPlayer = new MultiPlayer(this, AUDIO_BUFFER_CAPACITY_MS, AUDIO_DECODE_CAPACITY_MS);
			mRadioPlayer.setResponseCodeCheckEnabled(false);
			mRadioPlayer.setPlayerCallback(this);
		}

		return mRadioPlayer;
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		return mLocalBinder;
	}

	*/
/**
	 * The type Local binder.
	 *//*

	public class LocalBinder extends Binder {
		*/
/**
		 * Gets service.
		 *
		 * @return the service
		 *//*

		public MyService getService() {
			return MyService.this;
		}
	}


	public MyService() {
		playlistManager=new PlaylistManager(getApplicationContext());

	}



	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if(action!=null && action.contains(INTENT_USER_ACTION)){
			handleUserAction(intent);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void handleUserAction(Intent intent) {
*/
/*
		String url=null;
		if(intent.getExtras().containsKey(EXTRA_PLAY_PAUSE_PRESSED)) {
				if(mRadioState== State.LOADING || mRadioState == State.PLAYING) {
					tryToStopPlayback();
					return;
				}else url = getCurrentUrl();
			} else if (intent.getExtras().containsKey(EXTRA_NEXT_PRESSED)) {
					url = getNextUrl();
			} else if (intent.getExtras().containsKey(EXTRA_PREV_PRESSED)) {
					url = getPrevUrl();
			} else if (intent.getExtras().containsKey(EXTRA_PLAY_LIST_STATION_PRESSED)) {
					url = intent.getExtras().getString(EXTRA_URL);
			}
			if(url==null) notifyErrorOccured("Empty url");
			else {
				notifier.notifyStationSelected(url);
				prepareToPlayAsync(url);
			}*//*

		}
	private void tryToStopPlayback(){
		if(!playerLocked) {
			notifyLockStateChanged(true);
			getPlayer().stop();
		}else {
			playerUnlockedSubscription = RxBroadcast.fromBroadcast(getApplicationContext(), new IntentFilter(INTENT_PLAYER_UNLOCKED))
					.subscribe(intent1 -> stopWhenUnlocked());
		}
	}

	private void prepareToPlayAsync(String url) {
		*/
/*notifier.notifyLoadingStarted();
		switch (mRadioState) {
			case PLAYING:
			case LOADING:
				IntentFilter filter = new IntentFilter();
				notifyLockStateChanged(true);
				playerStoppedSubscription = RxBroadcast.fromBroadcast(getApplicationContext(), filter)
						.subscribe(intent -> {
							if(intent.getAction().contains(INTENT_PLAYER_ACTION) && intent.getExtras().containsKey(EXTRA_PLAYER_STOPPED)){
								if(playerStoppedSubscription!=null && !playerStoppedSubscription.isUnsubscribed()) playerStoppedSubscription.unsubscribe();
								notifyLockStateChanged(false);
								tryToStartPlayback(url);
							}
						});
				getPlayer().stop();
				break;
			case STOPPED:
			case IDLE:
				tryToStartPlayback(url);
				break;
		}return false;*//*

		
	}



	private void notifyLockStateChanged(boolean state){
		playerLocked=state;
		String action;
		if(state) action=INTENT_PLAYER_LOCKED;
		else action=INTENT_PLAYER_UNLOCKED;
		Intent intent=new Intent(action);
		sendBroadcast(intent);
	}
	private void startWhenUnlocked(String url){
			tryToStartPlayback(url);
	}

	private void tryToStartPlayback(String url) {
	}

	private void stopWhenUnlocked(){
		notifyLockStateChanged(false);
		getPlayer().stop();
	}





	public boolean checkSuffix(String streamUrl) {
		return streamUrl.contains(SUFFIX_PLS) ||
				streamUrl.contains(SUFFIX_RAM) ||
				streamUrl.contains(SUFFIX_WAX);
	}


	private void decodeStremLink(String streamLink) {
		new StreamLinkDecoder(streamLink) {
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
				notifyLockStateChanged(false);
				tryToStartPlayback(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	@Override
	public void playerStarted() {


	}

	@Override
	public void playerPCMFeedBuffer(boolean b, int i, int i1) {

	}

	@Override
	public void playerStopped(int i) {

	}

	@Override
	public void playerException(Throwable throwable) {

	}

	@Override
	public void playerMetadata(String s, String s1) {

	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {

	}
	private void startLockStateChangedSubscription(){
		IntentFilter lockStateChangedFilter= new IntentFilter(INTENT_PLAYER_LOCKED );
		lockStateChangedFilter.addAction(INTENT_PLAYER_UNLOCKED);
		if(lockStateChangedSubscription==null || lockStateChangedSubscription.isUnsubscribed()) {
			lockStateChangedSubscription = RxBroadcast.fromBroadcast(getApplicationContext(),lockStateChangedFilter )
					.subscribe(intent -> {
						if (intent.getAction().contains(INTENT_PLAYER_LOCKED)) playerLocked = true;
						else if (intent.getAction().contains(INTENT_PLAYER_UNLOCKED)) playerLocked = false;
					});
		}
	}
	private void stopLockStateChangedSubscription() {
		if (lockStateChangedSubscription != null && !lockStateChangedSubscription.isUnsubscribed())
			lockStateChangedSubscription.unsubscribe();
	}
}
*/
