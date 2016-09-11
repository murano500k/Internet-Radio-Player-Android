package com.android.murano500k.newradio.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.murano500k.newradio.PlaylistDownloader;
import com.android.murano500k.newradio.PlaylistManager;
import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;
import com.android.murano500k.newradio.UrlManager;
import com.wang.avi.AVLoadingIndicatorView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.android.murano500k.newradio.UrlManager.INDEX_CUSTOM;
import static com.android.murano500k.newradio.UrlManager.INDEX_DI;

public class ActivityRxTest extends AppCompatActivity {
	public static final String TAG = "ActivityRadio";
	public static final String INTENT_TOKEN_UPDATED = "com.android.murano500k.newradio.INTENT_TOKEN_UPDATED";
	public static final String INTENT_UPDATE_STATIONS = "com.android.murano500k.newradio.INTENT_UPDATE_STATIONS";
	public static final String EXTRA_UPDATE_STATIONS_RESULT_STRING = "com.android.murano500k.newradio.EXTRA_UPDATE_STATIONS_RESULT_STRING";
	public static final String EXTRA_UPDATE_STATIONS_INDEX = "com.android.murano500k.newradio.EXTRA_UPDATE_STATIONS_INDEX";
	public static final String INTENT_OPEN_APP = "com.android.murano500k.newradio.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.android.murano500k.newradio.INTENT_CLOSE_APP";
	private static final int SELECT_PLS_REQUEST_CODE = 444;

	final public static class UI_STATE{
		static final int LOADING= 0;
		static final int PLAYING= 1;
		static final int IDLE= -1;
	}

	RecyclerView recyclerView;
	ListAdapterRx adapter;
	ImageButton btnPlay, btnPrev, btnNext;
	private AVLoadingIndicatorView spinner;
	private static ServiceRadioRx serviceRadioRx;
	boolean isListInitiated = false;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private UrlManager urlManager;


	public PlaylistManager getPlaylistManager() {
		return playlistManager;
	}

	public DialogShower getDialogShower() {
		return dialogShower;
	}

	private PlaylistManager playlistManager;
	private LinearLayout loadingLayout;
	private Toast toast;
	private PlaylistDownloader playlistDownloader;
	private DrawerManager drawerManager;

	private boolean isPlaying() {
		return serviceRadioRx != null && serviceRadioRx.isPlaying();
	}
	public ServiceRadioRx getService() {
		return serviceRadioRx;
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.i(TAG, "Service Connected.");
			serviceRadioRx = ((ServiceRadioRx.LocalBinder) binder).getService();
			updateLoadingVisibility(1);

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceRadioRx.isServiceConnected=false;
		}
	};
	public void connect() {
		Intent intent = new Intent(this, ServiceRadioRx.class);
		this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rx_test);
		EventBus.getDefault().register(this);
		if(serviceRadioRx!=null) serviceRadioRx.isServiceConnected=false;
		playlistDownloader=new PlaylistDownloader(getApplicationContext());
		urlManager =new UrlManager(this);
		playlistManager = new PlaylistManager(getApplicationContext());
		drawerManager=new DrawerManager(this);
		initUI();
		updateLoadingVisibility();
		if(initActivePlaylist()) connect();
	}

	private boolean initActivePlaylist(){
		if(playlistManager.getStations()==null){
			int i = playlistManager.getActivePlaylistIndex();
			String url=null;
			if(i==INDEX_CUSTOM) {
				String res = urlManager.getSavedCustomPlaylistString();
				if (res != null) {
					Intent intent = new Intent(this, ActivityRxTest.class);
					intent.setAction(ActivityRxTest.INTENT_UPDATE_STATIONS);
					intent.putExtra(ActivityRxTest.EXTRA_UPDATE_STATIONS_RESULT_STRING, res);
					intent.putExtra(ActivityRxTest.EXTRA_UPDATE_STATIONS_INDEX, i);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					return false;
				}
			} else url = urlManager.getPlaylistUrl(i);
			if (url == null || url.length() == 0 || url.contains("undefined")) {
				i = INDEX_DI;
				playlistManager.saveActivePlaylistIndex(i);
				url = urlManager.getPlaylistUrl(i);
			}
			playlistDownloader.downloadPlaylist(url,i);
			return false;
		}else {
			initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
			return true;
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		playlistManager.saveActivePlaylistIndex(drawerManager.getDrawer().getCurrentSelectedPosition());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(intent.getAction().contains(INTENT_OPEN_APP)) updateUI();
		else if(intent.getAction().contains(INTENT_CLOSE_APP)){
			this.unbindService(mServiceConnection);
			playlistManager.saveActivePlaylistIndex(drawerManager.getDrawer().getCurrentSelectedPosition());
			finish();
		}
		else if(intent.getAction().contains(INTENT_UPDATE_STATIONS)){
			int i = intent.getIntExtra(EXTRA_UPDATE_STATIONS_INDEX, -1);
			String s=intent.getStringExtra(EXTRA_UPDATE_STATIONS_RESULT_STRING);
			Log.w("onUpdateStations", "index="+i);
			Log.w("onUpdateStations", "str_result="+s);
			playlistManager.saveActivePlaylistIndex(i);
			if(i==INDEX_CUSTOM) urlManager.saveCustomPlaylistString(s);
			playlistManager.selectPls(s);
			initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
			if(getService()!=null && getService().isServiceConnected) {
				Intent intentReset=new Intent(this,ServiceRadioRx.class);
				intentReset.setAction(ServiceRadioRx.INTENT_RESET);
				startService(intentReset);
				updateLoadingVisibility(-1);
			}else connect();
		}
		super.onNewIntent(intent);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==RESULT_CANCELED) {
			drawerManager.getDrawer().setSelection(INDEX_DI);
			Toast.makeText(getApplicationContext(), "Playlist not selected", Toast.LENGTH_SHORT).show();
		}
		else if(resultCode==RESULT_OK){
			if(requestCode==SELECT_PLS_REQUEST_CODE) {
				playlistDownloader.downloadPlaylist(data.getData().toString(), INDEX_CUSTOM);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(serviceRadioRx != null && serviceRadioRx.isServiceConnected) {
			unbindService(mServiceConnection);
			serviceRadioRx=null;
		}

	}
	public void selectPlsIntent(){
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/mpeg-url");
		intent.setType("audio/x-mpegurl");
		intent.setType("audio/mpegurl");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), SELECT_PLS_REQUEST_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(getApplicationContext(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
		}
	}
	private void updateLoadingVisibility(int drawerOpenClosed){
			serviceRadioRx.isServiceConnected=true;
			drawerManager.initDrawer(
					playlistManager.isShuffle(),
					serviceRadioRx.isSleepTimerRunning(),
					playlistManager.getActivePlaylistIndex());
		if(drawerOpenClosed>0) drawerManager.getDrawer().openDrawer();
		if(drawerOpenClosed<0) drawerManager.getDrawer().closeDrawer();
		loadingLayout.setVisibility(View.GONE);
		updateUI(UI_STATE.IDLE, playlistManager.getActivePlaylistName(),0);
	}
	public void updateLoadingVisibility(){
		loadingLayout.setVisibility(View.VISIBLE);
		updateUI(UI_STATE.LOADING, "Loading playlist", 0);
	}

	/*private void onNewPlaylistSelected(int i){
		hideToast();
		Assert.assertTrue(i>0);
		(false);
			if(drawerManager.getDrawer().isDrawerOpen()) drawerManager.getDrawer().closeDrawer();
			//showToast("file loaded successfully");
			playlistManager.saveActivePlaylistIndex(i);
			if(getService()==null || !getService().isServiceConnected) connect();
			drawerManager.onDrawerPlaylistTypeChanged(i);
			initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
		}
		updateUI();
		*//*if(getService()!=null && getService().isServiceConnected) {
			Intent intentReset=new Intent(this,ServiceRadioRx.class);
			intentReset.setAction(ServiceRadioRx.INTENT_RESET);
			startService(intentReset);
		}*//*
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

				}});

	}*/


	private void btnControlClick(String extra){
		Log.i(TAG, "btnControlClick: "+extra);
		if(getService().isServiceConnected) {
			EventBus.getDefault().post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, ServiceRadioRx.STATUS_LOADING, playlistManager.getSelectedUrl()));
			Intent intent = new Intent(getApplicationContext(), ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(extra, extra);
			startService(intent);
		}else Toast.makeText(getApplicationContext(),"Player is locked!", Toast.LENGTH_SHORT).show();
	}
	public void initUI() {
		loadingLayout =(LinearLayout) findViewById(R.id.select_pls);

		progressBar=(ProgressBar) findViewById(R.id.progressBar);
		dialogShower =new DialogShower(playlistManager);

		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrev = (ImageButton) findViewById(R.id.btnPrev);
		spinner = (AVLoadingIndicatorView) findViewById(R.id.spinner);
		btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);

		btnPlay.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED));
		btnPrev.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_PREV_PRESSED));
		btnNext.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_NEXT_PRESSED));
		btnPlay.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		btnPrev.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		btnNext.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		/*loadingLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectPlsIntent();
			}
		});
		*/
	}

	public void initList(ArrayList<String> list, String urlS) {
		isListInitiated = false;
		adapter = new ListAdapterRx(getApplicationContext(), list, urlS);
		if (recyclerView == null) {
			recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));
		}
		recyclerView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		isListInitiated = true;
		if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) connect();
	}

	public void updateUI(){
		if(isPlaying())updateUI(UI_STATE.PLAYING, playlistManager.getNameFromUrl(playlistManager.getSelectedUrl()), 100);
		else updateUI(UI_STATE.IDLE, playlistManager.getActivePlaylistName(), 100);
	}
	private void updateUI(int state, String title, int progress) {
		switch (state) {
			case UI_STATE.LOADING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(true);
				spinner.setVisibility(View.VISIBLE);
				spinner.animate();
				//btnPlay.setVisibility(View.INVISIBLE);
				btnPlay.setActivated(true);
				break;
			case UI_STATE.PLAYING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(false);
				spinner.setVisibility(View.GONE);
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(true);
				break;
			case UI_STATE.IDLE:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(false);
				spinner.setVisibility(View.GONE);
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(false);
				break;
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiEvent(UiEvent eventUi) {
		UiEvent.UI_ACTION action = eventUi.getUiAction();
		String url = eventUi.getExtras().url;
		String name = "";
		if(url!=null && !url.equals("")) name=playlistManager.getActivePlaylistName();

		if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)) {
			Log.i(TAG, "onPlayerStartedEvent: " + name);
			updateUI(UI_STATE.PLAYING, name, 0);
			if (recyclerView != null && adapter != null && adapter.getItemIndex(eventUi.getExtras().url) != -1)
				recyclerView.smoothScrollToPosition(adapter.getItemIndex(eventUi.getExtras().url));
		}
		else if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)) {
			updateUI(UI_STATE.IDLE, playlistManager.getActivePlaylistName(), 0);
		}
		else if (action.equals(UiEvent.UI_ACTION.BUFFER_UPDATED)) {
			updateUI(UI_STATE.PLAYING, name, eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.LOADING_STARTED)) {
			updateUI(UI_STATE.LOADING, "loading...", eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.STATION_SELECTED)) {
			Log.i(TAG, "onStationSelected: "+ name);
			updateUI(UI_STATE.LOADING, name, 0);
			if(recyclerView!=null && adapter!=null && adapter.getItemIndex(eventUi.getExtras().url)!=-1)
				recyclerView.smoothScrollToPosition(adapter.getItemIndex(eventUi.getExtras().url));
		}
	}


	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onSleepTimerStatusUpdate(SleepEvent sleepEvent) {
		SleepEvent.SLEEP_ACTION action=sleepEvent.getSleepAction();
		int seconds=sleepEvent.getSeconds();
		Log.d(TAG, "onSleepTimerStatusUpdate " + action + " " + seconds);
		if (action== SleepEvent.SLEEP_ACTION.CANCEL) {
			Toast.makeText(getApplicationContext(), "Sleep time cancelled", Toast.LENGTH_SHORT).show();
			if(drawerManager.getDrawer()!=null)
				drawerManager.getDrawer().updateItem(drawerManager.getDrawerItemSleep(false,DrawerManager.INDEX_SLEEP));
			setTitle(R.string.app_name);
		} else if (action== SleepEvent.SLEEP_ACTION.UPDATE) {
			if (seconds > 1) {
				if(seconds>60) setTitle(seconds/60 + " minutes left");
				else setTitle(seconds+" seconds to sleep");
				drawerManager.getDrawer().updateItem(
						drawerManager.getDrawerItemSleep(true,DrawerManager.INDEX_SLEEP));

			}
		} else if (action== SleepEvent.SLEEP_ACTION.FINISH) {
			Toast.makeText(getApplicationContext(), "Sleep timer finished", Toast.LENGTH_SHORT).show();
			drawerManager.getDrawer().updateItem(
					drawerManager.getDrawerItemSleep(false,DrawerManager.INDEX_SLEEP));
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
	public void hideToast(){
		Observable.just(1)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(integer -> {
					if(toast!=null){
						toast.cancel();
						toast=null;
					}
				});
	}
	public PlaylistDownloader getPlaylistDownloader() {
		return playlistDownloader;
	}
}
