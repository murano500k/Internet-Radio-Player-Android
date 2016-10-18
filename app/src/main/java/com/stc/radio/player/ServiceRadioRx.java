package com.stc.radio.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import com.stc.radio.player.db.Metadata;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.SleepEvent;
import com.stc.radio.player.utils.MediaButtonsReceiver;
import com.stc.radio.player.utils.StreamLinkDecoder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.AsyncEmitter;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static com.stc.radio.player.db.NowPlaying.STATUS_IDLE;
import static com.stc.radio.player.db.NowPlaying.STATUS_PAUSING;
import static com.stc.radio.player.db.NowPlaying.STATUS_PLAYING;
import static com.stc.radio.player.db.NowPlaying.STATUS_STARTING;
import static com.stc.radio.player.db.NowPlaying.STATUS_SWITCHING;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_CONNECTIVITY;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_FOCUS;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_UNMUTE;
import static java.lang.Boolean.TRUE;
import static junit.framework.Assert.assertTrue;


public class ServiceRadioRx extends Service implements PlayerCallback{
	public static final String INTENT_RESET = "com.stc.radio.player.INTENT_RESET";
	public static final String INTENT_SLEEP_SET = "com.stc.radio.player.INTENT_SLEEP_SET";
	public static final String INTENT_SLEEP_CANCEL = "com.stc.radio.player.INTENT_SLEEP_CANCEL";
	public static final String INTENT_SET_BUFFER_SIZE = "com.stc.radio.player.INTENT_SET_BUFFER_SIZE";
	public static final String EXTRA_AUDIO_BUFFER_CAPACITY = "com.android.murano500k" +
			".EXTRA_AUDIO_BUFFER_CAPACITY";
	public static final String EXTRA_AUDIO_DECODE_CAPACITY = "com.android.murano500k" +
			".EXTRA_AUDIO_DECODE_CAPACITY";
	public static final String EXTRA_SLEEP_SECONDS= "com.stc.radio.player.INTENT_SLEEP_CANCEL";

	public static final String INTENT_USER_ACTION = "com.stc.radio.player.INTENT_USER_ACTION";
	public static final String EXTRA_WHICH = "com.stc.radio.player.EXTRA_WHICH";
	public static final String EXTRA_PLAY_PAUSE_PRESSED = "com.stc.radio.player.EXTRA_PLAY_PAUSE_PRESSED";
	public static final String EXTRA_NEXT_PRESSED = "com.stc.radio.player.EXTRA_NEXT_PRESSED";
	public static final String EXTRA_PREV_PRESSED = "com.stc.radio.player.EXTRA_PREV_PRESSED";
	public static final String EXTRA_PLAY_LIST_STATION_PRESSED = "com.stc.radio.player.EXTRA_PLAY_LIST_STATION_PRESSED";
	public static final String EXTRA_URL = "com.stc.radio.player.EXTRA_URL";

	public static final int EXTRA_WHICH_PREVIOUS = -1;
	public static final int EXTRA_WHICH_PLAY_PAUSE= 0;
	public static final int EXTRA_WHICH_NEXT= 1;


	private Subscription loadingFailedListener;


	private ComponentName mMediaButtonReceiverComponent;
	private SleepTimerTask sleepTimerTask;

	private final String TAG= "ServiceRadioRx";
	public final IBinder mLocalBinder = new LocalBinder();
	private AudioManager audioManager;
	private EventBus bus=EventBus.getDefault();
	private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
	private HeadsetPlugReceiver headsetPlugReceiver;
	private Intent headsetPlugIntent;
	private NotificationProviderRx notificationProviderRx;
	public boolean isServiceConnected;
	private Toast toast;
	private Subscription internetListener;
	private MultiPlayer mRadioPlayer;
	private NowPlaying nowPlaying;
	private Subscription checkDbSubscription;
	private Subscriber<Boolean> startPlayingSubscriber;
	private Subscriber playbackInterruptedSubscriber;

	public NowPlaying getNowPlaying() {
		return nowPlaying;
	}


	public class LocalBinder extends Binder {
		public ServiceRadioRx getService() {
			return ServiceRadioRx.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		isServiceConnected=true;
		if(!bus.isRegistered(this))bus.register(this);

		return mLocalBinder;
	}


	private ServiceRadioRx getService() {
		return this;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		isServiceConnected=false;


		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		isServiceConnected=false;
		if(!bus.isRegistered(this)) bus.register(this);
		notificationProviderRx =new NotificationProviderRx(getService(), getService());
		if(!bus.isRegistered(notificationProviderRx)) bus.register(notificationProviderRx);
		nowPlaying=NowPlaying.getInstance();
		if(nowPlaying!=null)nowPlaying.withStatus(STATUS_IDLE);
		if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		registerHeadsetPlugReceiver();
		registerMediaButtonsReciever();
	}
	public boolean isPlaying(){
		if(nowPlaying!=null) return nowPlaying.isPlaying();
		else return false;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		isServiceConnected=false;
		if(bus.isRegistered(this)) bus.unregister(this);
		if(notificationProviderRx!=null){
			if(bus.isRegistered(notificationProviderRx)) bus.unregister(notificationProviderRx);
			notificationProviderRx.cancelNotification();
			notificationProviderRx=null;
		}
		if(nowPlaying.getStatus()!=STATUS_IDLE) {
			if(nowPlaying.getStatus()!=STATUS_WAITING_CONNECTIVITY) abandonFocus();
			else dontlistenConnectivity();
			nowPlaying.setStatus(STATUS_IDLE);
			getPlayer().stop();
		}

		dontlistenConnectivity();
		unRegisterHeadsetPlugReceiver();
		unRegisterMediaButtonsReciever();
		nowPlaying.save();
	}

	@Subscribe()
	public void onNowPlayingUpdate(NowPlaying nowPlaying) {
		if (nowPlaying.getStation() == null) Timber.e("NULL STATION");
		else if (!nowPlaying.getStation().equals(this.nowPlaying.getStation())
				|| nowPlaying.getStatus() != this.nowPlaying.getStatus()) {
			this.nowPlaying = nowPlaying;
		}
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!bus.isRegistered(this))bus.register(this);
		if(intent!=null && intent.getAction()!=null){
			Timber.w("INTENT %s", intent.getAction());
			String action = intent.getAction();
			if(action!=null && action.contains(INTENT_USER_ACTION)){
				int which = intent.getIntExtra(EXTRA_WHICH, 0);
				Timber.w("INTENT_USER_ACTION: %d", which);
				onControlsInteraction(which);

			}else if(action!=null && action.contains(INTENT_SET_BUFFER_SIZE)) {
				//DbHelper.setBufferSize(intent.getIntExtra(EXTRA_AUDIO_BUFFER_CAPACITY, 800));
				//DbHelper.setDecodeSize(intent.getIntExtra(EXTRA_AUDIO_DECODE_CAPACITY, 400));
			}else if(action!=null && action.contains(INTENT_RESET)){
				resetPlayer();
			} else if (action!=null && action.equals(INTENT_SLEEP_SET)) {
				Log.d(TAG, "serv INTENT_SLEEP_SET");
				int secs = intent.getIntExtra(EXTRA_SLEEP_SECONDS, -254);
				if(secs==-254)             Log.d(TAG, "secs ERROR");
				Log.d(TAG, "secs" + secs);
				setSleepTimer(secs);
			}else if (action!=null && action.equals(INTENT_SLEEP_CANCEL)) {
				Log.d(TAG, "serv CANCEL");
				cancelSleepTimer();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Subscribe()
	public void onNewStationToPlay(Station station){
		assertTrue(station!=null);
		Timber.w("onNewItemToPlay item %s, status %d", station.getName(), nowPlaying.getStatus());
		/*if(nowPlaying.getStation().equals(station)){
			onPlayPause();
		}else {*/
		nowPlaying.setStation(station, true);
			switch (nowPlaying.getStatus()){
				case STATUS_IDLE:
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					if (requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
					else nowPlaying.setStatus(STATUS_IDLE);
					break;
				case STATUS_PLAYING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					getPlayer().stop();
					break;
				case STATUS_PAUSING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					break;
				case STATUS_STARTING:
				case STATUS_SWITCHING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					getPlayer().stop();
					//TODO wait 1 sec and play
					break;
				case STATUS_WAITING_CONNECTIVITY:

					dontlistenConnectivity();
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					if (requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
					else nowPlaying.setStatus(STATUS_IDLE);
					break;
				case STATUS_WAITING_UNMUTE:
					showToast("STATUS_WAITING_UNMUTE");
					Timber.e("STATUS_WAITING_UNMUTE");
				case STATUS_WAITING_FOCUS:
					abandonFocus();
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					if (requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
					else nowPlaying.setStatus(STATUS_IDLE);
					break;

			}
		}

	public void onPlayPause() {
		Timber.w("onPlayPause");
		if(startPlayingSubscriber!=null && !startPlayingSubscriber.isUnsubscribed()) startPlayingSubscriber.unsubscribe();
		else if(playbackInterruptedSubscriber!=null && !playbackInterruptedSubscriber.isUnsubscribed()) {
			playbackInterruptedSubscriber.unsubscribe();
		}else {
			startPlayingSubscriber = getStartPlayingSubscriber();
			PublishSubject<Integer> publishSubject=PublishSubject.create();



			AsyncEmitter<Boolean> emitter;
		}

		switch(nowPlaying.getStatus()){
			case STATUS_IDLE:
				if(nowPlaying.getStation()==null) {
					showToast("No station selected");
					Timber.e("No station selected");
				}else {
					nowPlaying.setStatus(STATUS_STARTING);
					if (requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
					else nowPlaying.setStatus(STATUS_IDLE);
				}
				break;
			case STATUS_PLAYING:
				nowPlaying.setStatus(STATUS_PAUSING);
				getPlayer().stop();
				break;
			case STATUS_PAUSING:
				break;
			case STATUS_STARTING:
			case STATUS_SWITCHING:
				nowPlaying.setStatus(STATUS_IDLE);
				getPlayer().stop();
				break;
			case STATUS_WAITING_UNMUTE:
				nowPlaying.setStatus(STATUS_IDLE);
				getPlayer().stop();
				showToast("STATUS_WAITING_UNMUTE");
				Timber.e("STATUS_WAITING_UNMUTE");
				break;
			case STATUS_WAITING_CONNECTIVITY:
				nowPlaying.setStatus(STATUS_IDLE);
				dontlistenConnectivity();
				break;
			case STATUS_WAITING_FOCUS:
				nowPlaying.setStatus(STATUS_IDLE);
				abandonFocus();
				break;
			default:
				showToast("onPlayPause");
				Timber.e("onPlayPause");
		}
	}



	private void resetPlayer() {
		showToast("resetPlayer");
		Timber.e("resetPlayer %d", nowPlaying.getStatus());
		if(nowPlaying.getStatus()!=STATUS_IDLE){
			abandonFocus();
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE);
			getPlayer().stop();
			mRadioPlayer=null;
			nowPlaying.setStatus(STATUS_IDLE);
		}
	}

	@Subscribe()
	private void onControlsInteraction(int which) {
		Timber.w("new command %d",which);
		if(which==0) onPlayPause();
		else {
			List<Station> list = nowPlaying.getStations();
			Iterator<Station> iterator = list.iterator();
			Station lastStation = null;
			Station station = null;
			while (iterator.hasNext()) {
				lastStation = station;
				station = iterator.next();
				if (station.getKey().contains(nowPlaying.getStation().getKey())) {
					if (which < 0 && lastStation != null) onNewStationToPlay(lastStation);
					else if(which < 0) onNewStationToPlay(list.get(list.size()-1));
					else if (which > 0 && !iterator.hasNext())
						onNewStationToPlay(list.get(0));
					else if (which > 0 && iterator.hasNext())
						onNewStationToPlay(iterator.next());
					else if (station != null)
						onNewStationToPlay(station);
					else {
						showToast("null list");
						Timber.e("null list, request was: %d", which);
					}
					return;
				}
			}
			showToast("newPos notFound in list");Timber.e(" ");

		}
	}



	public Subscription getInternetListener(String url){
		String resUrl = url.substring(url.indexOf("//")+2);
		if(resUrl.contains(":")) resUrl=resUrl.substring(0,resUrl.indexOf(":"));
		else if(resUrl.contains("/")) resUrl=resUrl.substring(0,resUrl.indexOf("/"));
		Log.d("getInternetListener", url);
		return ReactiveNetwork.observeInternetConnectivity(3000, url,8000, 30000)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(aBoolean -> {if(aBoolean) {
					if(nowPlaying.getStatus()==STATUS_WAITING_CONNECTIVITY){
						nowPlaying.setMetadata(new Metadata("Connection established", "Starting"));
						nowPlaying.setStatus(STATUS_STARTING,true);
						if(requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
						else nowPlaying.withMetadata(null).setStatus(STATUS_IDLE, true);
					}
				}});
	}
	public Subscription getLoadingFailedListener(String url){
		return ReactiveNetwork.observeInternetConnectivity(500, url,8000, 5000)
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(aBoolean -> {
					if(!aBoolean) {
						Timber.w("ISplaying %b",nowPlaying.isPlaying());
						Timber.w("status %d",nowPlaying.getStatus());

						if(nowPlaying.getStatus()!=STATUS_SWITCHING){
							showToast("Connection failed retry");
							nowPlaying.setStatus(STATUS_SWITCHING);
							getPlayer().stop();
							tryToPlayAsync(nowPlaying.getStation().getUrl());
						}else nowPlaying.setStatus(STATUS_IDLE);

					}
				});
	}
	public void listenConnectivity(String url){
		if(internetListener==null || internetListener.isUnsubscribed()) {
			Log.w("Connectivity","start listening");
			internetListener=getInternetListener(url);
		}else Log.w("Connectivity","continue listening");
	}
	public void dontlistenConnectivity(){
		Log.i(TAG,"dontlistenConnectivity");
		if(internetListener!=null && !internetListener.isUnsubscribed()) {
			Log.w("Connectivity","stop listening");
			internetListener.unsubscribe();
		}else Log.w("Connectivity","was not listening");
	}
	public void abandonFocus(){
		if(nowPlaying.getStatus()!=STATUS_IDLE) {
			audioManager.abandonAudioFocus(audioFocusChangeListener);
		}
	}
	private boolean requestFocus() {
			int r = audioManager.requestAudioFocus(
					getAudioFocusChangeListener(),
					AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
		return (r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
	}



	@Override
	public void playerException(Throwable throwable) {
		Timber.w(throwable.getMessage(), "TEST callback playerException");
		mRadioPlayer=null;
		//nowPlaying.setMetadata(new Metadata(throwable.getMessage(),"isPlaying: "+nowPlaying.isPlaying()));
		nowPlaying.setStatus(STATUS_IDLE, true);
		showToast("ERROR: "+throwable.getMessage());
	}
	@Override
	public void playerStarted() {
		Timber.w("callback playerStarted when status was %d", nowPlaying.getStatus());
		if(nowPlaying.getStatus()==STATUS_WAITING_CONNECTIVITY) {
			dontlistenConnectivity();
		}
		if(loadingFailedListener != null && !loadingFailedListener.isUnsubscribed()) loadingFailedListener.unsubscribe();
		nowPlaying.setStatus(STATUS_PLAYING, TRUE);
	}


	@Override
	public void playerStopped(int i) {

		Timber.w("TEST callback playerStopped %d", i);
		Timber.w("base_status : %b",nowPlaying.isPlaying());
		nowPlaying.setBaseStatus(false);
		nowPlaying.setMetadata(null, false);

		if(nowPlaying.getStatus()==STATUS_WAITING_UNMUTE){
			nowPlaying.setStatus(STATUS_IDLE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			}else {
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			}
			abandonFocus();
		}else if(nowPlaying.getStatus()==STATUS_SWITCHING) {
			nowPlaying.setStatus(STATUS_STARTING);
			tryToPlayAsync(nowPlaying.getStation().getUrl());
		}
		else if(nowPlaying.getStatus()==STATUS_PLAYING){
			nowPlaying.setStatus(STATUS_WAITING_CONNECTIVITY, true);
			abandonFocus();
			listenConnectivity(nowPlaying.getStation().getUrl());

		} else if(nowPlaying.getStatus()!=STATUS_WAITING_FOCUS &&
				nowPlaying.getStatus()!=STATUS_WAITING_CONNECTIVITY){
			nowPlaying.setStatus(STATUS_IDLE);
			abandonFocus();
		}
	}
	/**
	 * This method is called periodically by PCMFeed.
	 *
	 * isPlaying false means that the PCM data are being buffered,
	 *          but the audio is not playing yet
	 *
	 *  audioBufferSizeMs the buffered audio data expressed in milliseconds of playing
	 *  audioBufferCapacityMs the total capacity of audio buffer expressed in milliseconds of playing
	 */
/*public void playerPCMFeedBuffer( final boolean isPlaying,
                                     final int audioBufferSizeMs, final int audioBufferCapacityMs ) {

        uiHandler.post( new Runnable() {
            public void run() {
                progress.setProgress( audioBufferSizeMs * progress.getMax() / audioBufferCapacityMs );
                if (isPlaying) txtStatus.setText( R.string.text_playing );
            }
        });
    }*/
	@Override
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs){
		bus.post(new BufferUpdate(audioBufferSizeMs, audioBufferCapacityMs, nowPlaying.isPlaying()));
	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {
		Timber.d("audiostate %d, ", audioTrack.getState());
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
			post=true;
		}else if(s1!=null && s1.contains("StreamTitle")) {
			a = s1.replace("StreamTitle=", "");
			s = "";
			post=true;
		}
		if(post){
			metadata = new Metadata(a, s);
			nowPlaying.setMetadata(metadata, true);

		}
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
		//Assuming player is not locked, playing and device is in correct state
		if (checkSuffix(url)) {
			decodeStremLink(url);
			return;
		}
		//loadingFailedListener=getLoadingFailedListener(url);

		getPlayer().playAsync(url, 128);
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
	public MultiPlayer getPlayer() {
			if (mRadioPlayer == null) {
				try {

					java.net.URL.setURLStreamHandlerFactory( protocol -> {
						Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
						if ("icy".equals(protocol))
							return new com.spoledge.aacdecoder.IcyURLStreamHandler();
						return null;
					});
				} catch (Throwable t) {
					Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
				}

				mRadioPlayer = new MultiPlayer(this, 2000, 1000);
			mRadioPlayer.setResponseCodeCheckEnabled(true);
		}

		return mRadioPlayer;
	}


	private void setSleepTimer(int seconds){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		sleepTimerTask=null;
		sleepTimerTask=new SleepTimerTask();
		sleepTimerTask.execute(seconds);
		bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.UPDATE, seconds));
		Log.d(TAG, "sleep timer created");
	}
	private void cancelSleepTimer(){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		Log.d(TAG, "sleep timer cancelled");
	}
	private class SleepTimerTask extends AsyncTask<Integer, Integer, Void> {
		@Override
		protected void onProgressUpdate(Integer... values) {
			int secondsLeft = values[0];
			Log.d(TAG, "onProgressUpdate sleep timer left "+ secondsLeft +" seconds");
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.UPDATE, secondsLeft));
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Log.d(TAG, "sleep timer finished");
			if(nowPlaying.getStatus()!=STATUS_IDLE){
				nowPlaying.setStatus(STATUS_IDLE);
				abandonFocus();
				getPlayer().stop();
			}
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.FINISH, -1));
		}

		@Override
		protected void onCancelled(Void aVoid) {
			cancelSleepTimer();
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.CANCEL, -1));
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

	public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener() {
		if (audioManager == null)
			audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if (audioFocusChangeListener == null) {
			audioFocusChangeListener = focusChange -> {
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
						|| focusChange == AudioManager.AUDIOFOCUS_LOSS
						|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
					Timber.w("focusChange == AudioManager.AUDIOFOCUS_LOSS)");
					if (nowPlaying.isPlaying() || nowPlaying.getStatus()==STATUS_PLAYING) {
						nowPlaying.setStatus(STATUS_WAITING_FOCUS);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
							audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_VIBRATE);
						} else {
							audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE);
						}
						getPlayer().stop();
					}
				} else {
					Log.d(TAG, "focusChange AUDIOFOCUS_GAIN");
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
							audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_VIBRATE);
						} else {
							audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE);
						}
						nowPlaying.setStatus(STATUS_STARTING);
						if (requestFocus()) tryToPlayAsync(nowPlaying.getStation().getUrl());
						else nowPlaying.setStatus(STATUS_IDLE);
					}
			};
		}
		return audioFocusChangeListener;
	}
	public void registerMediaButtonsReciever(){
		Log.d(TAG, "registerMediaButtonsReciever");
		if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		mMediaButtonReceiverComponent = new ComponentName(getApplicationContext().getPackageName(), MediaButtonsReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
	}

	public void unRegisterMediaButtonsReciever(){
		Log.d(TAG, "unRegisterMediaButtonsReciever");
		if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(mMediaButtonReceiverComponent!=null) audioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
	}

	public void registerHeadsetPlugReceiver(){
		Log.d(TAG, "register HeadsetPlugReceiver");
		if(nowPlaying==null )nowPlaying=new NowPlaying().withStatus(STATUS_IDLE);

		if(headsetPlugReceiver==null) 		headsetPlugReceiver = new HeadsetPlugReceiver();
		headsetPlugIntent=getApplicationContext().registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}

	public void unRegisterHeadsetPlugReceiver(){
		Log.d(TAG, "UNregister HeadsetPlugReceiver");

		if(headsetPlugIntent!=null && headsetPlugReceiver!=null) getApplicationContext().unregisterReceiver(headsetPlugReceiver);
		headsetPlugReceiver=null;
	}

	public class HeadsetPlugReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				if (isServiceConnected) {
					int state = intent.getIntExtra("state", -1);
					switch (state) {
						case 0:
							// headset unplugged

								if(nowPlaying.getStatus()==STATUS_PLAYING){
									nowPlaying.setStatus(STATUS_WAITING_UNMUTE);
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
										audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
												AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
									}else {
										audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
												AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
									}
									getPlayer().stop();
								}

							break;
						case 1:
							if (nowPlaying.getStatus()==STATUS_IDLE) {

								nowPlaying.setStatus(STATUS_STARTING);
								if (requestFocus())	tryToPlayAsync(nowPlaying.getStation().getUrl());
								else nowPlaying.setStatus(STATUS_IDLE);

							}
							break;
					}
				}
			}
		}
	}


	public void showToast(String text){
		Observable.just(1)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(integer -> {
					if(toast!=null){
						toast.cancel();
						toast=null;
					}
					toast=Toast.makeText(getApplicationContext(), text,Toast.LENGTH_SHORT);
					toast.show();
				});
	}


/*

	public void updateArt() {
		if (currentArt != null) bus.post(currentArt);
		else if(currentUrl!=null){
			String artUrl = DbHelper.getArtUrl(currentUrl);
			Timber.w("station artUrl=%s", artUrl);
			PabloPicasso.with(getContext()).load(Uri.parse(artUrl))
					.placeholder(R.drawable.ic_default_art)
					.error(android.R.drawable.stat_notify_error)
					//.resizeDimen(R.dimen.list_item_art_size, R.dimen.list_item_art_size)
					.into(new Target() {
						@Override
						public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
							bus.post(bitmap);
						}

						@Override
						public void onBitmapFailed(Drawable errorDrawable) {
							bus.post(drawableToBitmap(errorDrawable));
						}

						@Override
						public void onPrepareLoad(Drawable placeHolderDrawable) {
							//bus.post(drawableToBitmap(placeHolderDrawable));
						}
					});
		}else Timber.e("NO URL");
	}
*/
	public void observeMetadata(){
		Observer observer= new Observer() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Object o) {
				Timber.w("new o = %s", o.toString());
			}
			};



		Observable<Metadata>metadataObservable=Observable.from(new Future<Metadata>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {

				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return false;
			}

			@Override
			public Metadata get() throws InterruptedException, ExecutionException {
				return null;
			}

			@Override
			public Metadata get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return null;
			}
		});
		metadataObservable.subscribe(new Observer<Metadata>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Metadata metadata) {

			}
		});

	}

	public Subscriber<Boolean>getStartPlayingSubscriber(){
		return new Subscriber<Boolean>() {
			@Override
			public void onStart() {

				nowPlaying.setStatus(STATUS_STARTING, true);
				super.onStart();
			}

			@Override
			public void onCompleted() {
				Timber.w("callback playerStarted when status was %d", nowPlaying.getStatus());
				if(nowPlaying.getStatus()==STATUS_WAITING_CONNECTIVITY) dontlistenConnectivity();
				nowPlaying.setStatus(STATUS_PLAYING, true);
			}

			@Override
			public void onError(Throwable e) {
				showToast("ERROR "+e.getMessage());
				nowPlaying.setStatus(STATUS_IDLE, true);
				getPlayer().stop();
			}

			@Override
			public void onNext(Boolean aBoolean) {

			}
		};
	}

}


