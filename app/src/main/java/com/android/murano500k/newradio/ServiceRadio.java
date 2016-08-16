package com.android.murano500k.newradio;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.Iterator;
import java.util.Random;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.telephony.PhoneStateListener.LISTEN_CALL_STATE;
import static android.telephony.PhoneStateListener.LISTEN_NONE;

/**
 * The type Service radio.
 */
public class ServiceRadio extends Service implements PlayerCallback  {


	private ConnectivityManager connectivityManager;

	//private final int AUDIO_BUFFER_CAPACITY_MS = 800;
	private int AUDIO_BUFFER_CAPACITY_MS = 800;
	private int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";

	private final String TAG= "ServiceRadio";
	private MultiPlayer mRadioPlayer;
	private boolean isSwitching;
	private boolean isClosedFromNotification = false;
	private boolean mLock;
	private boolean userStopped;
	/**
	 * The M local binder.
	 */
	public final IBinder mLocalBinder = new LocalBinder();
	private boolean audioFocusIntrerrupted;
	private MediaSession mediaSession;
	private SleepTimerTask sleepTimerTask;
	private AudioManager audioManager;
	private Initiator initiator;
	private int positionNotificationPeriod;
	private boolean positionSaved=false;
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
		isClosedFromNotification=false;
		userStopped=true;
		notifier.notifyRadioConnected();
		AUDIO_BUFFER_CAPACITY_MS=playlistManager.getBufferSize();
		AUDIO_DECODE_CAPACITY_MS=playlistManager.getDecodeSize();
		getPlayer();
		//initiator.initMediaSession();
		initiator.registerHeadsetPlugReceiver();
		initiator.startPhoneStateListener();
		initiator.registerMediaButtonsReciever();


	}
	@Override
	public boolean onUnbind(Intent intent) {
		if(initiator!=null){
			initiator.resetPhoneStateListener();
			initiator.resetConnectivityListener();
			initiator.resetAudioFocusChangeListener();
			initiator.unRegisterHeadsetPlugReceiver();
			initiator.unRegisterMediaButtonsReciever();
			//initiator.resetMediaSession();

		}
		if(isPlaying())stop();
		//if(notifier!=null && notificationProvider!=null) notifier.unRegisterListener(notificationProvider);

		stopSelf();
		return super.onUnbind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		Log.d(TAG,"onStartCommand " + action);
		Log.d(TAG, "INTENT " + action);
		if (action.equals(Constants.INTENT.PLAYBACK.PLAY_PAUSE)) {
			Log.d(TAG, "PLAY_NEXT shuffle: " +playlistManager.isShuffle());
			if(isPlaying()) intentActionPause(false);
			else	intentActionPlay(null);

		}else if (action.equals(Constants.INTENT.PLAYBACK.PLAY_NEXT)) {
			Log.d(TAG, "PLAY_NEXT shuffle: " +playlistManager.isShuffle());
			if(playlistManager.isShuffle()) playRandom();
			else	playNext();

		} else if (action.equals(Constants.INTENT.PLAYBACK.PLAY_PREV)) {
			Log.d(TAG, "PLAY_PREV");
			playPrev();

		} else if (action.equals(Constants.INTENT.PLAYBACK.PAUSE) || action.equals(Constants.INTENT.UPDATE_STATIONS)) {
			Log.d(TAG, "PAUSE");
			intentActionPause(false);

		} else if (action.contains(Constants.INTENT.PLAYBACK.RESUME) || action.equals(Constants.INTENT.HANDLE_CONNECTIVITY)) {
			intentActionPlay(intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL));

		} else if (action.contains(Constants.INTENT.SET_BUFFER_SIZE)) {
			Log.d(TAG, "INTENT_SET_BUFFER_SIZE");
			AUDIO_BUFFER_CAPACITY_MS = intent.getIntExtra(Constants.DATA_AUDIO_BUFFER_CAPACITY, 800);
			AUDIO_DECODE_CAPACITY_MS = intent.getIntExtra(Constants.DATA_AUDIO_DECODE_CAPACITY, 400);
			playlistManager.saveBufferSize(AUDIO_BUFFER_CAPACITY_MS);
			playlistManager.saveDecodeSize(AUDIO_DECODE_CAPACITY_MS);
			Toast.makeText(getApplicationContext(), "Audio buffer updated. AUDIO_BUFFER_CAPACITY " +
					"= "+
					AUDIO_BUFFER_CAPACITY_MS+ " ms, AUDIO_DECODE_CAPACITY = "+AUDIO_DECODE_CAPACITY_MS+ " ms",
					Toast.LENGTH_SHORT).show();
			Log.d(TAG, "AUDIO_BUFFER_CAPACITY_MS="+ AUDIO_BUFFER_CAPACITY_MS);
			Log.d(TAG, "AUDIO_DECODE_CAPACITY_MS="+ AUDIO_DECODE_CAPACITY_MS);
			getPlayer().setAudioBufferCapacityMs(AUDIO_BUFFER_CAPACITY_MS);
			getPlayer().setDecodeBufferCapacityMs(AUDIO_DECODE_CAPACITY_MS);

		} else if (action.equals(Constants.INTENT.SLEEP.SET)) {
			Log.d(TAG, "serv SET");
			int secs = intent.getIntExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, -254);
			if(secs==-254)             Log.d(TAG, "secs ERROR");
			Log.d(TAG, "secs" + secs);
			setSleepTimer(secs);
		}else if (action.equals(Constants.INTENT.SLEEP.CANCEL)) {
			Log.d(TAG, "serv CANCEL");
			cancelSleepTimer();
		} else if (action.equals(Constants.INTENT.CLOSE_NOTIFICATION)) {
			Log.d(TAG,"serv INTENT_CLOSE_NOTIFICATION");
			intentActionPause(true);
		}
		return START_NOT_STICKY;
	}

	private void playNext() {
		Iterator<String> iterator=playlistManager.getStations().iterator();
		String wasSelected=playlistManager.getSelectedUrl();
		while (iterator.hasNext()){
			if(wasSelected.contains(iterator.next()) && iterator.hasNext()) {
				intentActionPlay(iterator.next());
				break;
			}
		}
		String url=playlistManager.getSelectedUrl();
		notifier.notifyStationSelected(url);
		intentActionPlay(url);
	}
	private void playPrev() {
		Iterator<String> iterator=playlistManager.getStations().iterator();
		String wasSelected=playlistManager.getSelectedUrl();
		String prev=null;
		String next;
		while (iterator.hasNext()){
			next=iterator.next();
			if(wasSelected.contains(next) && prev!=null){
				playlistManager.setSelectedUrl(prev);
				intentActionPlay(prev);
				break;
			}
			prev=next;
		}

	}
	private void playRandom() {
		int currentIndex= new Random().nextInt(playlistManager.getStations().size()-1);
		intentActionPlay((String)playlistManager.getStations().toArray()[currentIndex]);
	}


	private void intentActionPlay(String url) {
		Log.d(TAG, "RESUME");
		if(url!=null) {
			playlistManager.setSelectedUrl(url);
		} else url = playlistManager.getSelectedUrl();
		notifier.notifyStationSelected(url);
		isClosedFromNotification=false;
		userStopped=false;
		play(url);
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
			if (checkSuffix(mRadioUrl))
				decodeStremLink(mRadioUrl);
			else {
				isSwitching = false;
				if (!mLock) {
					Log.d(TAG,"Play requested .getAudioBufferCapacityMs=" + getPlayer()
							.getAudioBufferCapacityMs()+ " getDecodeBufferCapacityMs="
							+getPlayer().getDecodeBufferCapacityMs());
					mLock = true;
					notifier.notifyLoadingStarted(mRadioUrl);
					getPlayer().playAsync(mRadioUrl);
				}
			}
		} else {
			Toast.makeText(getApplicationContext(), "focus not granted", Toast.LENGTH_SHORT).show();
			notifier.notifyPlaybackErrorOccured(true);
		}
	}
	@Override
	public void playerStarted() {
		PlaylistManager.addToLog("playerStarted");
		mRadioState = State.PLAYING;
		userStopped=false;
		mLock = false;
		isClosedFromNotification = false;
		notifier.notifyPlaybackStarted(playlistManager.getSelectedUrl());

	}


	private void intentActionPause(boolean isClosedFromNotification) {
		Log.d(TAG, "PAUSE");
		userStopped=true;
		this.isClosedFromNotification=isClosedFromNotification;
		initiator.resetConnectivityListener();
		if(isPlaying())stop();
	}
	public void stop() {
		Log.d(TAG, "stop() mLock= "+ mLock+ " mRadioState=" + mRadioState + " isSwitching=" +
				isSwitching);
		if (!mLock && mRadioState == State.PLAYING) {
			Log.d(TAG, "Stop requested.");
			mLock = true;
			mRadioState=State.IDLE;
			getPlayer().stop();
		}
	}
	@Override
	public void playerStopped(int i) {
		PlaylistManager.addToLog("Player stopped. perf = " + i+"%");

		mRadioState = State.IDLE;
		notifier.notifyMetaDataChanged(null,null);
		mLock = false;
		Log.d(TAG, "Player stopped. perf = " + i+"%");
		if (isSwitching)
			play(playlistManager.getSelectedUrl());
		else if (isClosedFromNotification) {
			notifier.notifyPlaybackStopped(false);
			return;
		} else  notifier.notifyPlaybackStopped(true);
		if(!userStopped) {

			Observable.just(i).observeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(integer -> {
						Toast.makeText(getApplicationContext(),"Player stopped. perf = " + integer+"%",Toast.LENGTH_SHORT ).show();
					});
			initiator.startConnectivityListener();
		}
	}

	@Override
	public void playerException(Throwable throwable) {
		PlaylistManager.addToLog("playerException: " + throwable.toString());

		mRadioState = State.STOPPED;
		mLock = false;
		Log.d(TAG, "playerException: " + throwable.getMessage()+ "\n\t"+ throwable.getStackTrace());
		String s=throwable.getMessage();
		if (isClosedFromNotification) {
			notifier.notifyPlaybackErrorOccured(false);
			return;
		}
		else  	notifier.notifyPlaybackErrorOccured(true);

		if(!userStopped) {
			Observable.just(s).observeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(s1 -> {
						Toast.makeText(getApplicationContext(),"playerException: " + s1,Toast.LENGTH_LONG).show();
					});
			initiator.startConnectivityListener();
		}
	}

	@Override
	public void playerMetadata(String s, String s1) {
		notifier.notifyMetaDataChanged(s,s1);
		//Log.d(TAG, "progressMetadata s="+ s+ " s1="+ s1);
	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {

		//audioTrack.getNotificationMarkerPosition();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(!positionSaved) {
				positionNotificationPeriod=audioTrack.getPositionNotificationPeriod();
				audioTrack.setPositionNotificationPeriod(positionNotificationPeriod/3);
				positionSaved=true;
			}
			else {
				audioTrack.setPositionNotificationPeriod(positionNotificationPeriod/3);
			}
		}
	}


	/**
	 * This method is called periodically by PCMFeed.
	 *
	 * @param isPlaying false means that the PCM data are being buffered,
	 *          but the audio is not playing yet
	 *
	 * @param audioBufferSizeMs the buffered audio data expressed in milliseconds of playing
	 * @param audioBufferCapacityMs the total capacity of audio buffer expressed in milliseconds of playing
	public void playerPCMFeedBuffer( boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs );

	 */
	@Override
	public void playerPCMFeedBuffer(boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs) {
		//Log.d(TAG, "progress audioBufferSizeMs="+ audioBufferSizeMs+" audioBufferCapacityMs="+ audioBufferCapacityMs);
		notifier.notifyProgressUpdated(audioBufferSizeMs, audioBufferCapacityMs, "");
	}






	public boolean isPlaying(){
		return State.PLAYING == mRadioState;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
			notifier.notifySleepTimerStatusUpdated(Constants.INTENT.SLEEP.CANCEL, -1);
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
				Log.d(TAG, "key [" + key + "]: " + val);
				if(key.contains("networkInfo")){
					NetworkInfo info=(NetworkInfo)val;
					assert info != null;
					String msg = "network state:" + info.getState()+" reason: "+info
							.getReason()+", userStopped="+userStopped + ", isPlaying()="+isPlaying();
					Log.d(TAG, msg);
					PlaylistManager.addToLog("handleConnectivityChange. "+msg);

					//Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();

					if(info.getState()==NetworkInfo.State.CONNECTED && !userStopped){
						PlaylistManager.addToLog("\thandleConnectivityChange. intentActionPlay");

						Log.d(TAG, "Resuming...");
						intentActionPlay(null);

					}

					if(info.getState()==NetworkInfo.State.DISCONNECTED || info.getState()==NetworkInfo.State.DISCONNECTING){
						//if(/*extras.getBoolean("noConnectivity")&& */isPlaying())

						if(isPlaying())userStopped=false;
					}
					break;
				}
			}
		}
		else {
			Log.d(TAG, "no extras");
		}

	}
	public boolean isOnline(){
		if(connectivityManager==null){
			connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		}
		NetworkInfo netInfo=connectivityManager.getActiveNetworkInfo();
		return (netInfo!=null && netInfo.isConnected());
	}
	public boolean requestFocus(){

		if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if (connectivityManager == null) connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		int result;
		if(audioManager.getMode()==AudioManager.MODE_IN_CALL) {
			Toast.makeText(getApplicationContext(),"AudioManager.MODE_IN_CALL. Radio will not start",Toast.LENGTH_SHORT).show();
			return false;
		}

		if(!isOnline()) {
			Toast.makeText(getApplicationContext(),"No connection. Radio will not start",Toast.LENGTH_SHORT).show();
			if(initiator.stateAudioDevice==Constants.STATE_AUDIO_DEVICE.PLUGGED){
				userStopped=false;
				if(initiator.connectedSubscription==null || initiator.connectedSubscription.isUnsubscribed()) initiator.startConnectivityListener();
			}
			return false;
		}
		result = audioManager.requestAudioFocus(initiator.getAudioFocusChangeListener(),
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {


		} else audioManager.abandonAudioFocus(initiator.getAudioFocusChangeListener());
		Log.d(TAG, "requested audio focus. result = "+ result);
		PlaylistManager.addToLog("requested audio focus. result = "+ result);
		return (result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
	}


	public class Initiator {

		private Subscription connectedSubscription=null;
		private AudioManager audioManager=null;
		private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener=null;
		private PhoneStateListener phoneStateListener=null;
		private TelephonyManager mTelephonyManager=null;
		private ComponentName mMediaButtonReceiverComponent;
		private int stateAudioDevice;
		private int lastStateAudioDevice;
		private HeadsetPlugReceiver headsetPlugReceiver;
		private Intent headsetPlugIntent;



		public Initiator() {
			connectedSubscription=null;
			audioFocusChangeListener=null;
			phoneStateListener=null;
			stateAudioDevice= Constants.STATE_AUDIO_DEVICE.UNKNOWN;
			lastStateAudioDevice= Constants.STATE_AUDIO_DEVICE.UNKNOWN;
		}


		private Subscription startConnectivityListener(){
			IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			Log.d(TAG, "initConnectivityListener");
			PlaylistManager.addToLog("initConnectivityListener");
			if(connectedSubscription==null || connectedSubscription.isUnsubscribed()) {
				connectedSubscription=RxBroadcast.fromBroadcast(getApplicationContext(), filter)
						.subscribe(ServiceRadio.this::handleConnectivityChange);
			}
			return connectedSubscription;
		}

		public void resetConnectivityListener(){
			Log.d(TAG, "resetConnectivityListener");
			PlaylistManager.addToLog("resetConnectivityListener");
			if(connectedSubscription!=null && !connectedSubscription.isUnsubscribed())
				connectedSubscription.unsubscribe();
			connectedSubscription=null;
		}
		public void registerMediaButtonsReciever(){
			Log.d(TAG, "registerMediaButtonsReciever");
			PlaylistManager.addToLog("registerMediaButtonsReciever");
			if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
			mMediaButtonReceiverComponent = new ComponentName(getApplicationContext().getPackageName(), MediaButtonsReceiver.class.getName());
			audioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
		}
		public void unRegisterMediaButtonsReciever(){
			Log.d(TAG, "unRegisterMediaButtonsReciever");
			PlaylistManager.addToLog("unRegisterMediaButtonsReciever");
			if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
			if(mMediaButtonReceiverComponent!=null) audioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
		}

		public PhoneStateListener startPhoneStateListener(){
			if(mTelephonyManager== null)mTelephonyManager=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			if(phoneStateListener==null) phoneStateListener= new PhoneStateListener() {
				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
						Log.d(TAG, "onCallStateChanged CALL_STATE_RINGING");
						if (isPlaying()) {
							if(isPlaying()) {
								Log.d(TAG, "onCallStateChanged will stop");
								audioFocusIntrerrupted = true;
								userStopped=false;
								lastStateAudioDevice=stateAudioDevice;
								audioManager.abandonAudioFocus(audioFocusChangeListener);
								intentActionPause(false);
							}
						}
					} else if (state == TelephonyManager.CALL_STATE_IDLE) {
						Log.d(TAG, "CALL_STATE_IDLE last="+lastStateAudioDevice+ " now="+stateAudioDevice+", interrupted="+audioFocusIntrerrupted);

						if(stateAudioDevice!=lastStateAudioDevice && stateAudioDevice==Constants.STATE_AUDIO_DEVICE.UNPLUGGED){
							Toast.makeText(getApplicationContext(),"call finished. Headset was unplugged during call, will not resume", Toast.LENGTH_SHORT).show();
							PlaylistManager.addToLog("call finished. Headset was unplugged during call, will not resume");
						}else if(audioFocusIntrerrupted && !isPlaying()){
							audioFocusIntrerrupted=false;
							userStopped=false;
							Log.d(TAG, "onCallStateChanged will play");
							PlaylistManager.addToLog("onCallStateChanged will play");
							intentActionPlay(null);
						}
					}
					super.onCallStateChanged(state, incomingNumber);
				}
			};
			Log.d(TAG, "startPhoneStateListener");
			PlaylistManager.addToLog("startPhoneStateListener");
			mTelephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE);
			return phoneStateListener;
		}

		public void resetPhoneStateListener(){
			Log.d(TAG, "resetPhoneStateListener");
			PlaylistManager.addToLog("resetPhoneStateListener");
			if(phoneStateListener!=null) {
				if (mTelephonyManager != null)
					mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
				phoneStateListener=null;
			}
		}


		public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener(){
			if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
				if(audioFocusChangeListener==null) {
					audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
						public void onAudioFocusChange(int focusChange) {
							if        (focusChange == AudioManager.AUDIOFOCUS_LOSS
									|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
									|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
								Log.d(TAG, "focusChange == AudioManager.AUDIOFOCUS_LOSS)");
								PlaylistManager.addToLog("AudioManager.AUDIOFOCUS_LOSS");

								if (isPlaying()) {
									Log.d(TAG, "focusChange will stop");
									audioFocusIntrerrupted = true;
									PlaylistManager.addToLog("\tAUDIOFOCUS intentActionPause");
									userStopped=false;
									intentActionPause(false);
								}
							} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
								Log.d(TAG, "focusChange AUDIOFOCUS_GAIN");
								PlaylistManager.addToLog("AudioManager.AUDIOFOCUS_GAIN");

								if (audioFocusIntrerrupted) {
									audioFocusIntrerrupted = false;
									Log.d(TAG, "focusChange will play");
									PlaylistManager.addToLog("\tAUDIOFOCUS intentActionPlay");

									if (!isPlaying()) intentActionPlay(null);
								}
							}

						}
					};
				}
				return audioFocusChangeListener;
		}
		public void resetAudioFocusChangeListener(){
			Log.d(TAG, "reset AudioFocusChangeListener");
			PlaylistManager.addToLog("reset AudioFocusChangeListener");

			if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
			if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);
			audioFocusChangeListener=null;
		}


		public void registerHeadsetPlugReceiver(){
			Log.d(TAG, "register HeadsetPlugReceiver");
			PlaylistManager.addToLog("register HeadsetPlugReceiver");

			if(headsetPlugReceiver==null) 		headsetPlugReceiver = new HeadsetPlugReceiver();
			headsetPlugIntent=getApplicationContext().registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		}
		public void unRegisterHeadsetPlugReceiver(){
			Log.d(TAG, "UNregister HeadsetPlugReceiver");
			PlaylistManager.addToLog("UNregister HeadsetPlugReceiver");

			if(headsetPlugIntent!=null && headsetPlugReceiver!=null) getApplicationContext().unregisterReceiver(headsetPlugReceiver);
			headsetPlugReceiver=null;
		}

		public class HeadsetPlugReceiver extends BroadcastReceiver{
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
					int state = intent.getIntExtra("state", -1);
					switch (state) {
						case 0:
							// headset unplugged
							Log.d(TAG, "headset unplugged");
							stateAudioDevice= Constants.STATE_AUDIO_DEVICE.UNPLUGGED;
							if(isPlaying()){
								Log.d(TAG, "will pause");
								intentActionPause(false);
							}
							break;
						case 1:
							// headset plugged
							stateAudioDevice= Constants.STATE_AUDIO_DEVICE.PLUGGED;
							Log.d(TAG, "headset plugged");
							if(!isPlaying()){
								Log.d(TAG, "will play");
								intentActionPlay(null);
							}
							break;
						default:
							// headset plugged
							stateAudioDevice= Constants.STATE_AUDIO_DEVICE.UNKNOWN;
							Log.d(TAG, "headset UNKNOWN");
							break;
					}
				}
			}
		}

		public void initMediaSession(){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				String TAG="MyMediaSession";
				mediaSession = new MediaSession(getApplicationContext(), TAG);
				mediaSession.setActive(true);
				mediaSession.setCallback(new MediaSession.Callback() {
					String TAG="MyMediaSession";
					@Override
					public void onSkipToNext() {
						super.onSkipToNext();
					}
					@Override
					public void onSkipToPrevious() {
						super.onSkipToPrevious();
					}
					public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
						Log.d(TAG, "onMediaButtonEvent");
						if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
							KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
							int keycode = event.getKeyCode();
							int action = event.getAction();
							long eventtime = event.getEventTime();
							switch (keycode) {
								case KeyEvent.KEYCODE_MEDIA_NEXT:
									if (action == KeyEvent.ACTION_DOWN) {
										Log.d(TAG, "Trigered KEYCODE_VOLUME_UP KEYCODE_MEDIA_NEXT");
										playNext();
									}
									break;
								case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
									if (action == KeyEvent.ACTION_DOWN) {
										Log.d(TAG, "Trigered KEYCODE_VOLUME_DOWN KEYCODE_MEDIA_PREVIOUS");
										playPrev();
									}
									break;
								case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
									if (action == KeyEvent.ACTION_DOWN) {
										Log.d(TAG, "Trigered PLAY_PAUSE KEYCODE_HEADSETHOOK");
										if(isPlaying()) intentActionPause(false);
										else intentActionPlay(null);
									}
								case KeyEvent.KEYCODE_MEDIA_PAUSE:
									if (action == KeyEvent.ACTION_DOWN) {
										Log.d(TAG, "Trigered PAUSE ");
										if(isPlaying()) intentActionPause(false);
									}
								case KeyEvent.KEYCODE_MEDIA_PLAY:
									if (action == KeyEvent.ACTION_DOWN) {
										Log.d(TAG, "Trigered PLAY");
										intentActionPlay(null);
									}
									break;
								default:
									break;
							}
						}
						Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
						return super.onMediaButtonEvent(mediaButtonIntent);
					}

					public void onPause() {
						Log.d(TAG, "onPause called (media button pressed)");
						super.onPause();
					}

					public void onPlay() {
						Log.d(TAG, "onPlay called (media button pressed)");
						super.onPlay();
					}

					public void onStop() {
						Log.d(TAG, "onStop called (media button pressed)");
						super.onStop();
					}
				});
				mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
			}
		}

		public void resetMediaSession(){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (mediaSession != null) mediaSession.release();
			}
		}
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
