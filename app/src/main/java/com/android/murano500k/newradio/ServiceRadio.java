package com.android.murano500k.newradio;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.Random;

import rx.Subscription;

import static android.telephony.PhoneStateListener.LISTEN_NONE;

/**
 * The type Service radio.
 */
public class ServiceRadio extends Service implements PlayerCallback{
//TODO: Widget, notification, sleeptimer, design (theme, colors)
	//TODO:Interruptions: connectivity, phone, headphones plug, audiofocus


	//private final int AUDIO_BUFFER_CAPACITY_MS = 800;
	private final int AUDIO_BUFFER_CAPACITY_MS = 1600;
	private final int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";

	private final String TAG= "ServiceRadio";
	private MultiPlayer mRadioPlayer;
	private boolean isSwitching;
	private boolean isClosedFromNotification = false;
	private boolean mLock;
	private boolean wasDisconnectedWhenPlaying;
	/**
	 * The M local binder.
	 */
	public final IBinder mLocalBinder = new LocalBinder();
	private boolean isPlaying;
	private MediaSession mediaSession;
	private Subscription stationsSubscription;
	private SleepTimerTask sleepTimerTask;
	private int audioDeviceState;
	private AudioManager audioManager;
	private Initiator initiator;
	private ConnectivityManager cm;
	private int positionNotificationPeriod =17640/4;
	private NotificationProvider notificationProvider;
	private PlaylistManager playlistManager;

	public enum State {
		IDLE,
		PLAYING,
		 STOPPED,
	}
	private State mRadioState;

	public NotifierRadio notifier;

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

	/**
	 * The type Local binder.
	 */
	public class LocalBinder extends Binder {
		/**
		 * Gets service.
		 *
		 * @return the service
		 */
		public ServiceRadio getService() {
			return ServiceRadio.this;
		}
	}

	@Override
	public void playerStarted() {
		mRadioState = State.PLAYING;
		mLock = false;
		isClosedFromNotification = false;
		notifier.notifyStationSelected(playlistManager.getSelectedUrl());
		notifier.notifyPlaybackStarted(playlistManager.getSelectedUrl());
		initiator.startConnectivityListener();
	}

	@Override
	public void playerPCMFeedBuffer(boolean b, int i, int i1) {
		//Log.d(TAG, "progressplayerPCMFeedBuffer b="+ b+ " i="+ i+" i1="+ i1);
		notifier.notifyProgressUpdated(i, i1, "b="+ b+ " i="+ i+" i1="+ i1);
	}

	@Override
	public void playerStopped(int i) {
		mRadioState = State.IDLE;
		//Log.d(TAG, "playerStopped(int i)= " +i);

		if (isClosedFromNotification) {
			//buildNotification();
			notifier.notifyPlaybackStopped(true);
			isClosedFromNotification = false;
		} else{
			notifier.notifyPlaybackStopped(false);
		}
		mLock = false;
		Log.d(TAG, "Player stopped. State : " + mRadioState);
		if (isSwitching)
			play(playlistManager.getSelectedUrl());

	}

	@Override
	public void playerException(Throwable throwable) {
		mRadioState = State.STOPPED;
		mLock = false;
		Log.d(TAG, "playerException: " + throwable.getMessage());
		notifier.notifyPlaybackErrorOccured();
	}

	@Override
	public void playerMetadata(String s, String s1) {
		notifier.notifyMetaDataChanged(s,s1);
		//Log.d(TAG, "progressMetadata s="+ s+ " s1="+ s1);


	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {
			audioTrack.setPositionNotificationPeriod(positionNotificationPeriod);
	}


	/**
	 * Instantiates a new Service radio.
	 */
	public ServiceRadio() {
	}
	@Override
	public void onCreate() {
		super.onCreate();
		playlistManager =new PlaylistManager(getApplicationContext());
		mRadioState = State.IDLE;
		notifier=new NotifierRadio();
		notificationProvider=new NotificationProvider(getApplicationContext(), this);
		notifier.registerListener(notificationProvider);
		initiator=new Initiator();
		isSwitching = false;
		mLock = false;
		notifier.notifyRadioConnected();
		getPlayer();
		initMediaSession();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		Log.d(TAG,"onStartCommand " + action);
		Log.d(TAG, "INTENT " + action);
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent event= intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			int keycode = event.getKeyCode();
			int keyAction = event.getAction();
			switch (keycode) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "KEYCODE_MEDIA_NEXT");
						isClosedFromNotification=false;
						playNext();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "KEYCODE_MEDIA_PREVIOUS");
						isClosedFromNotification=false;
						playPrev();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY_PAUSE");
						intentActionPlayPause(intent);
					}
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PAUSE ");
						intentActionPause(intent);
					}
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY");
						intentActionPlay(intent);
					}
					break;
				default:
					break;
			}
		} else if (action.equals(Constants.INTENT_SLEEP_TIMER_SET)) {
			Log.d(TAG, "serv INTENT_SLEEP_TIMER_SET");
			int secs = intent.getIntExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, -254);
			if(secs==-254)             Log.d(TAG, "secs ERROR");
			Log.d(TAG, "secs" + secs);
			setSleepTimer(secs);
		}else if (action.equals(Constants.INTENT_SLEEP_TIMER_CANCEL)) {
			Log.d(TAG, "serv INTENT_SLEEP_TIMER_CANCEL");
			cancelSleepTimer(); //TODO uncomment sleep
		} else if (action.equals(Constants.INTENT_CLOSE_NOTIFICATION)) {
			Log.d(TAG,"serv INTENT_CLOSE_NOTIFICATION");
			isClosedFromNotification=true;
			/*if(mNotificationManager != null) {
				mNotificationManager.cancel(Constants.NOTIFICATION_ID);
			}//TODO isClosedFromNotification*/
			if(isPlaying())stop();
			stopSelf();
		} else if (action.equals(Constants.INTENT_PLAY_PAUSE)) {
			intentActionPlayPause(intent);
		} else if (action.equals(Constants.INTENT_PLAY_NEXT)) {
			Log.d(TAG, "INTENT_PLAY_NEXT");
			isClosedFromNotification=false;
			playNext();
		} else if (action.equals(Constants.INTENT_PLAY_RANDOM)) {
			Log.d(TAG, "INTENT_PLAY_RANDOM");
			isClosedFromNotification=false;
			playRandom();
		} else if (action.equals(Constants.INTENT_PLAY_PREV)) {
			Log.d(TAG, "INTENT_PLAY_PREV");
			isClosedFromNotification=false;
			playPrev();
		} else if (action.equals(Constants.INTENT_PAUSE_PLAYBACK)) {
			Log.d(TAG, "INTENT_PAUSE_PLAYBACK");
			intentActionPause(intent);
		} else if (action.contains(Constants.INTENT_RESUME_PLAYBACK)) {
			intentActionPlay(intent);
		} else if (action.contains(Constants.INTENT_HANDLE_CONNECTIVITY)) {
			if (mRadioState==State.STOPPED){
				intentActionPlay(intent);
			}
		} /*else if (action.contains(Constants.INTENT_UPDATE_FAVORITE_STATION)) {
			int indexToUpdate;
			boolean favToUpdate;
			indexToUpdate= intent.getIntExtra(Constants.DATA_STATION_INDEX, -1);
			favToUpdate = intent.getBooleanExtra(Constants.DATA_STATION_FAVORITE, false);
			Log.d(TAG, "updating station indexToUpdate " + indexToUpdate + ", fav="+favToUpdate);

			if(indexToUpdate!=-1) for(Station station: stations) if(station.id==indexToUpdate) {
				station.fav=favToUpdate;
				Log.d(TAG, "station " + station.url + " was set fav="+favToUpdate);
			}

		} else if (action.contains(Constants.INTENT_UPDATE_STATIONS)) {
			favOnly = intent.getBooleanExtra(Constants.DATA_FAV_ONLY, isFavOnly());
			stationsSubscription = stationsObservable()
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.doOnCompleted(() -> {
						//stop();
						if(currentStation==null) currentStation=getStations().get(0);
						else currentStation=getStationByUrl(currentStation.url);
						notifier.notifyListChanged(getStations());
						notifier.notifyStationSelected(currentStation.url);
						savePrefsToStorage(false);
					})
					.subscribe(stations1 -> {
						if(isFavOnly()) stationsFav=stations1;
						else stations=stations1;
						return;
					});
		}*/
		return START_NOT_STICKY;
	}

	/*public rx.Observable<ArrayList<Station>> stationsObservable() {
		return Observable.create(new Observable.OnSubscribe<ArrayList<Station>>() {
			@Override
			public void call(Subscriber<? super ArrayList<Station>> subscriber) {
				if(!isFavOnly()) subscriber.onNext(stations);
				else{
					ArrayList<Station> favStations = new ArrayList<Station>();
					for(Station station: stations){
						if(station.fav) favStations.add(station);
					}
					if(favStations.size()==0){
						Log.d(TAG,"No favorite stations. All stations selected");
						//Toast.makeText(getApplicationContext(), "No favorite stations. All stations selected", Toast.LENGTH_SHORT).show();
						subscriber.onNext(stations);
					}
					else subscriber.onNext(favStations);
				}
				subscriber.onCompleted();

			}
		});
	}*/

	private void intentActionPause(Intent intent) {
		Log.d(TAG, "INTENT_PAUSE_PLAYBACK");
		isClosedFromNotification=false;
		if(isPlaying())stop();
	}
	private void intentActionPlay(Intent intent) {
		Log.d(TAG, "INTENT_RESUME_PLAYBACK");
		isClosedFromNotification=false;
		String stationUrl;
		if(intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL)!=null) {
			stationUrl = intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL);
			play(stationUrl);
		}
		else play(playlistManager.getSelectedUrl());
		//else Toast.makeText(getApplicationContext(), "No station selected", Toast.LENGTH_SHORT).show();

	}

	private void intentActionPlayPause(Intent intent) {
		Log.d(TAG, "INTENT_PLAY_PAUSE");
		String stationUrl;
		isClosedFromNotification=false;
		if(isPlaying())stop();
		else {
			if(intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL)!=null) {
				stationUrl = intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL);
				//currentStation=getStationByUrl(stationUrl);
				play(stationUrl);
			}
			else play(playlistManager.getSelectedUrl());
		}

	}
	private void playNext() {
		int selectedIndex= playlistManager.getStations().indexOf(playlistManager.getSelectedUrl());
		if(selectedIndex>=playlistManager.getStations().size()-1) {
			playlistManager.setSelectedUrl(playlistManager.getStations().get(selectedIndex));
		}else{
			playlistManager.setSelectedUrl(playlistManager.getStations().get(selectedIndex+1));
		}
		play(playlistManager.getSelectedUrl());
	}
	private void playPrev() {
		int selectedIndex= playlistManager.getStations().indexOf(playlistManager.getSelectedUrl());
		if(selectedIndex==0) {
			playlistManager.setSelectedUrl(playlistManager.getStations().get(selectedIndex));
		}else{
			playlistManager.setSelectedUrl(playlistManager.getStations().get(selectedIndex-1));
		}
		play(playlistManager.getSelectedUrl());
	}
	private void playRandom() {
		int currentIndex= new Random().nextInt(playlistManager.getStations().size()-1);
		playlistManager.setSelectedUrl(playlistManager.getStations().get(currentIndex));
		play(playlistManager.getSelectedUrl());
	}

	/**
	 * Init media session.
	 */
	public void initMediaSession() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			String TAG = "MyMediaSession";
			mediaSession = new MediaSession(getApplicationContext(), TAG);
			mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
			mediaSession.setActive(true);
			Intent intentMediaButton = new Intent(getApplicationContext(), ServiceRadio.class);
			intentMediaButton.setAction(Constants.INTENT_MEDIA_BUTTON);
			PendingIntent pendingIntent=PendingIntent.getService(getApplicationContext(), 0, intentMediaButton, 0);
			mediaSession.setMediaButtonReceiver(pendingIntent);
		}
	}

	public void play(String mRadioUrl) {
		Log.d(TAG, "play() mLock= "+ mLock+ " mRadioState=" + mRadioState + " isSwitching=" +
				isSwitching);

		if (isPlaying()) {
			Log.d(TAG, "Switching Radio");
			isSwitching = true;
			stop();
		}else if(requestFocus()) {
			sendBroadcast(new Intent(Constants.ACTION_MEDIAPLAYER_STOP));
			notifier.notifyLoadingStarted(mRadioUrl);
			if (checkSuffix(mRadioUrl))
				decodeStremLink(mRadioUrl);
			else {
				isSwitching = false;
				if (!mLock) {
					Log.d(TAG,"Play requested.");
					mLock = true;
					getPlayer().playAsync(mRadioUrl);
				}
			}
		} else {
			Log.d(TAG, "focus not granted");
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		//savePrefsToStorage(true);
		return super.onUnbind(intent);
	}

	/**
	 * Stop.
	 */
	public void stop() {
		Log.d(TAG, "stop() mLock= "+ mLock+ " mRadioState=" + mRadioState + " isSwitching=" +
				isSwitching);

		if (!mLock && mRadioState == State.PLAYING) {
			Log.d(TAG, "Stop requested.");
			mLock = true;
			mRadioState=State.IDLE;
			wasDisconnectedWhenPlaying=false;
			getPlayer().stop();
		}
	}


	/**
	 * Is playing boolean.
	 *
	 * @return the boolean
	 */
	public boolean isPlaying(){
		if (State.PLAYING == mRadioState)
			return true;
		return false;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if(notifier!=null) notifier.unregisterAll();
	}


	private void setSleepTimer(int seconds){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		sleepTimerTask=null;
		sleepTimerTask=new SleepTimerTask();
		sleepTimerTask.execute(seconds);
		notifier.notifySleepTimerStatusUpdated(Constants.ACTION_SLEEP_UPDATE, seconds);
		Log.d(TAG, "sleep timer created");
	}
	private void cancelSleepTimer(){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		notifier.notifySleepTimerStatusUpdated(Constants.ACTION_SLEEP_CANCEL, -1);
		Log.d(TAG, "sleep timer cancelled");
	}
	private class SleepTimerTask extends AsyncTask<Integer, Integer, Void> {
		@Override
		protected void onProgressUpdate(Integer... values) {
			int secondsLeft = values[0];
			Log.d(TAG, "onProgressUpdate sleep timer left "+ secondsLeft +" seconds");
			notifier.notifySleepTimerStatusUpdated(Constants.ACTION_SLEEP_UPDATE, secondsLeft);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Log.d(TAG, "sleep timer finished");
			notifier.notifySleepTimerStatusUpdated(Constants.ACTION_SLEEP_FINISH, -1);
			if(isPlaying()) stop();
		}

		@Override
		protected void onCancelled(Void aVoid) {
			notifier.notifySleepTimerStatusUpdated(Constants.INTENT_SLEEP_TIMER_CANCEL, -1);
			Log.d(TAG, "sleep timer cancelled");
		}

		@Override
		protected Void doInBackground(Integer... params) {
			int secondsLeft = params[0];
			while(secondsLeft>=0) {
				if(secondsLeft%30==0)publishProgress(secondsLeft);
				SystemClock.sleep(1000);
				secondsLeft--;
			}
			return null;
		}
	}



	public boolean isSleepTimerRunning(){
		return(sleepTimerTask!=null && !sleepTimerTask.isCancelled());
	}
	private void debugIntent(Intent intent, String tag) {
		Log.v(tag, "action: " + intent.getAction());
		Log.v(tag, "component: " + intent.getComponent());
		Bundle extras = intent.getExtras();
		if (extras != null) {
			for (String key: extras.keySet()) {
				Log.v(tag, "key [" + key + "]: " +
						extras.get(key));
			}
		}
		else {
			Log.v(tag, "no extras");
		}
	}

	void handleConnectivityChange(final Intent intent) {

		//debugIntent(intent, "handleConnectivityChange");
		Bundle extras = intent.getExtras();
		if (extras != null) {
			for (String key: extras.keySet()) {
				Object val=extras.get(key);
				//Log.v(TAG, "key [" + key + "]: " + val);
				if(key.contains("networkInfo")){
					NetworkInfo info=(NetworkInfo)val;
					assert info != null;
					String msg = "network state:" + info.getState()+" reason: "+info
							.getReason()+", wasDisconnectedWhenPlaying="+wasDisconnectedWhenPlaying;
					Log.d(TAG, msg);
					//Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();

					if(info.getState()==NetworkInfo.State.CONNECTED && !mLock && !isSwitching &&
							!extras.getBoolean("noConnectivity") && !isPlaying() && wasDisconnectedWhenPlaying){
						Log.d(TAG, "Resuming...");
						wasDisconnectedWhenPlaying=false;
						play(playlistManager.getSelectedUrl());

					}
					if(info.getState()==NetworkInfo.State.DISCONNECTED && extras.getBoolean
							("noConnectivity")&& mRadioState==State.PLAYING) {
						wasDisconnectedWhenPlaying=true;
					}
					break;
				}
			}
		}
		else {
			Log.v(TAG, "no extras");
		}

	}
	/*boolean isOnline(Context context) {
		if (cm == null) cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if(netInfo != null && netInfo.isConnected()){
			return true;
		}else {
			return false;
		}
	}*/

	public boolean requestFocus(){
		return true;
		/*if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		boolean granted=false;
		int result = audioManager.requestAudioFocus(initiator.getAudioFocusChangeListener(),
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);

		granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
		Log.d(TAG, "requested audio focus. result = "+ granted);
		return granted;*/
	}

	private void activateInterruptionListeners(){
		initiator.startConnectivityListener();
		initiator.startPhoneStateListener();
		initiator.startAudioDeviceCallback();
	}

	public class Initiator {

		private Subscription connectedSubscription;
		private ConnectivityManager cm;
		private AudioManager audioManager;
		private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
		private AudioDeviceCallback audioDeviceCallback;
		private PhoneStateListener phoneStateListener;
		private TelephonyManager mTelephonyManager;


		public PhoneStateListener startPhoneStateListener(){
			if(phoneStateListener==null) phoneStateListener= new PhoneStateListener() {
				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
						if (isPlaying()) {
							boolean isInterrupted = true;
                        /*if(networkChangeReceiver!=null && networkChangeReceiver.isListening){
                            context.unregisterReceiver(networkChangeReceiver);
                            networkChangeReceiver.isListening=false;
                        }*/

						}
					} else if (state == TelephonyManager.CALL_STATE_IDLE) {
						if (mRadioState==State.STOPPED) {

						}
					}
					super.onCallStateChanged(state, incomingNumber);
				}
			};
			return phoneStateListener;
		}

		public void resetPhoneStateListener(){
			Log.d(TAG, "resetPhoneStateListener");
			if(phoneStateListener!=null) {
				if (mTelephonyManager != null && phoneStateListener != null)
					mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
			}
		}


		public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener(){
			if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if(audioFocusChangeListener!=null) {
					audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
						public void onAudioFocusChange(int focusChange) {
							if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
									focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
							{
								if (isPlaying()) {
									audioManager.abandonAudioFocus(audioFocusChangeListener);


								}
							} else if(focusChange == AudioManager.AUDIOFOCUS_GAIN ||
									focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
							{


							}
						}
					};
				}
				return audioFocusChangeListener;
			}
			else return null;
		}
		public void resetAudioFocusChangeListener(){
			Log.d(TAG, "resetAudioFocusChangeListener");
			audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
			if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);
		}


		public AudioDeviceCallback startAudioDeviceCallback(){
			if(audioManager==null) 	mTelephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if(audioDeviceCallback!=null) audioDeviceCallback= new AudioDeviceCallback() {
					@Override
					public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
						super.onAudioDevicesAdded(addedDevices);
					}
					@Override
					public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
						for (AudioDeviceInfo info :
								removedDevices) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
								if (AudioDeviceInfo.TYPE_WIRED_HEADSET == info.getType()
										|| AudioDeviceInfo.TYPE_WIRED_HEADPHONES == info.getType()) {
									if(connectedSubscription != null && !connectedSubscription.isUnsubscribed()) resetConnectivityListener();
									if (mTelephonyManager != null && phoneStateListener!=null)
										mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
									if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);
									super.onAudioDevicesRemoved(removedDevices);
									return;
								}
							}
						}
						super.onAudioDevicesRemoved(removedDevices);
					}
				};
				audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
			}

			return audioDeviceCallback;
		}
		public void resetAudioDeviceCallback(){
			Log.d(TAG, "resetAudioDeviceCallback");
			audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
			if(audioDeviceCallback!=null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
				}
			}
		}
		private Subscription startConnectivityListener(){
			IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			Log.d(TAG, "initConnectivityListener");
			wasDisconnectedWhenPlaying=false;

			if(connectedSubscription==null || connectedSubscription.isUnsubscribed()) RxBroadcast.fromBroadcast(getApplicationContext(), filter)
					.subscribe(ServiceRadio.this::handleConnectivityChange);
			return connectedSubscription;
		}

		public void resetConnectivityListener(){
			Log.d(TAG, "resetPhoneStateListener");
			if(connectedSubscription!=null && !connectedSubscription.isUnsubscribed())
				connectedSubscription.unsubscribe();

		}
	}
	/**
	 * Save prefs to storage.
	 */

/*
	public void getStationsListChangedNotification(){
		if(isFavOnly()) {
			notifier.notifyListChanged(stationsFav);
		}
		else notifier.notifyListChanged(stations);

	}*/


	public void setClosedFromNotification(boolean isClosedFromNotification){
		this.isClosedFromNotification=isClosedFromNotification;

	}

	public boolean isClosedFromNotification(){
		return isClosedFromNotification;
	}



	public boolean checkSuffix(String streamUrl) {
		if (streamUrl.contains(SUFFIX_PLS) ||
				streamUrl.contains(SUFFIX_RAM) ||
				streamUrl.contains(SUFFIX_WAX))
			return true;
		else
			return false;
	}


	private void decodeStremLink(String streamLink) {
		new StreamLinkDecoder(streamLink) {
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
				mLock=false;
				play(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	/*private void updateNotification() {
		if(notificationProvider==null)
		if(stationName!=null)this.stationName = stationName;
		else this.stationName="";

		if(singerName!=null)this.singerName = singerName;
		else this.singerName="";

		if(songName!=null)this.songName = songName;
		else this.songName = "";

		if(smallImage!=-1)this.smallImage = smallImage;
		if(artImage!=-1)this.artImage = BitmapFactory.decodeResource(context.getResources(),
				artImage);
		if(!isClosedFromNotification) buildNotification();
	}*/

/*

    private void updateWidget()  {//TODO UPDATE WIDGET CODE
        Intent intent = new Intent(this, SimpleWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), SimpleWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        intent.putExtra(SimpleWidgetProvider.SELECTED_STATION_TITLE, radioManager.getCurrentStation().name);
        intent.putExtra(SimpleWidgetProvider.IS_PLAYING, radioManager.isPlaying());
        sendBroadcast(intent);
    }
    */






}
