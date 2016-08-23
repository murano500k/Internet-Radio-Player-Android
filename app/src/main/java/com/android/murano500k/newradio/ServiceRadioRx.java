package com.android.murano500k.newradio;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.android.murano500k.newradio.events.EventBufferUpdate;
import com.android.murano500k.newradio.events.EventError;
import com.android.murano500k.newradio.events.EventStationSelected;
import com.android.murano500k.newradio.events.EventStopped;
import com.android.murano500k.newradio.events.EventUnlocked;
import com.spoledge.aacdecoder.MultiPlayer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import static com.android.murano500k.newradio.events.EventError.ERROR_FOCUS_NOT_GRANTED;
import static com.android.murano500k.newradio.events.EventError.ERROR_NO_URL;


public class ServiceRadioRx extends Service{
	private static final String INTENT_USER_ACTION = "com.android.murano500k.newradio.INTENT_USER_ACTION";
	private static final String EXTRA_PLAY_PAUSE_PRESSED = "com.android.murano500k.newradio.EXTRA_PLAY_PAUSE_PRESSED";
	private static final String EXTRA_NEXT_PRESSED = "com.android.murano500k.newradio.EXTRA_NEXT_PRESSED";
	private static final String EXTRA_PREV_PRESSED = "com.android.murano500k.newradio.EXTRA_PREV_PRESSED";
	private static final String EXTRA_PLAY_LIST_STATION_PRESSED = "com.android.murano500k.newradio.EXTRA_PLAY_LIST_STATION_PRESSED";
	private static final String EXTRA_URL = "com.android.murano500k.newradio.EXTRA_URL";

	private int AUDIO_BUFFER_CAPACITY_MS = 800;
	private int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";
	private final String TAG= "ServiceRadioRx";
	private MultiPlayer mRadioPlayer;
	public final IBinder mLocalBinder = new LocalBinder();
	private AudioManager audioManager;
	private NotificationProvider notificationProvider;
	private PlaylistManager playlistManager;
	private StatusPlayer statusPlayer;
	private CallBackPlayer callbackPlayer;
	private EventBus bus=EventBus.getDefault();

	public ServiceRadioRx() {
		playlistManager=new PlaylistManager(getApplicationContext());
	}
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

		if(callbackPlayer ==null) callbackPlayer =new CallBackPlayer();
		if(!bus.isRegistered(callbackPlayer))bus.register(callbackPlayer);
		if(statusPlayer==null) statusPlayer=new StatusPlayer();
		if(!bus.isRegistered(statusPlayer))bus.register(statusPlayer);
		if (mRadioPlayer == null) {
			mRadioPlayer = new MultiPlayer(callbackPlayer, AUDIO_BUFFER_CAPACITY_MS, AUDIO_DECODE_CAPACITY_MS);
			mRadioPlayer.setResponseCodeCheckEnabled(false);
		}

		return mRadioPlayer;
	}

	@Override
	public IBinder onBind(Intent intent) {
		getPlayer();
		return mLocalBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		EventBus.getDefault().register(this);

	}

	@Override
	public void onDestroy() {
		if(statusPlayer!=null && EventBus.getDefault().isRegistered(statusPlayer)) EventBus.getDefault().unregister(statusPlayer);
		if(callbackPlayer !=null && EventBus.getDefault().isRegistered(callbackPlayer)) EventBus.getDefault().unregister(callbackPlayer);
		EventBus.getDefault().unregister(this);
		super.onDestroy();
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
		String url=null;
		if(!statusPlayer.readyForAction())  EventBus.getDefault().post(new EventError(EventError.ERROR_LOCKED));

		if(intent.getExtras().containsKey(EXTRA_PLAY_PAUSE_PRESSED)) {
			if(statusPlayer.isPlaying()) stop();
			else url = getCurrentUrl();
		} else if (intent.getExtras().containsKey(EXTRA_NEXT_PRESSED)) {
			url = getNextUrl();
		} else if (intent.getExtras().containsKey(EXTRA_PREV_PRESSED)) {
			url = getPrevUrl();
		} else if (intent.getExtras().containsKey(EXTRA_PLAY_LIST_STATION_PRESSED)) {
			url = intent.getExtras().getString(EXTRA_URL);
		}

		if(url==null) EventBus.getDefault().post(new EventError(EventError.ERROR_NO_URL));
		else new PlayAsync(url);
	}

	private String getCurrentUrl() {
		return  null;
	}
	private String getNextUrl() {
		return  null;
	}
	private String getPrevUrl() {
		return  null;
	}

	private void stop(){
	statusPlayer.lock();
	getPlayer().stop();
}

	private class PlayAsync {
		final String url;
		private boolean needStart;

		PlayAsync(String url) {
			this.url = url;
			bus.post(new EventStationSelected(url));
			bus.register(this);
			needStart=false;
		}
		@Subscribe()
		public void onPlayerUnlockedEvent(EventUnlocked event) {
			if (url!=null) {
				needStart=statusPlayer.isPlaying();
				if(needStart) stop();
				else tryToStartPlayback(url);
			} else bus.post(new EventError(ERROR_NO_URL));
			if(!needStart && bus.isRegistered(this)) bus.unregister(this);
		}
		@Subscribe()
		public void onPlayerStoppedEvent(EventStopped event) {
			if(needStart) {
				tryToStartPlayback(url);
				if(bus.isRegistered(this)) bus.unregister(this);
			}
		}
	}

	private void tryToStartPlayback(String url)  {
		if (checkSuffix(url)) {
			decodeStremLink(url);
			statusPlayer.lock();
			return;
		}
		if(requestFocus()){
			bus.post(new EventBufferUpdate(0, true));
			getPlayer().playAsync(url);
		} else bus.post(new EventError(ERROR_FOCUS_NOT_GRANTED));
	}

	private boolean requestFocus() {
		return false;
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
				tryToStartPlayback(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	public class LocalBinder extends Binder {
		public ServiceRadioRx getService() {
			return ServiceRadioRx.this;
		}
	}


}
