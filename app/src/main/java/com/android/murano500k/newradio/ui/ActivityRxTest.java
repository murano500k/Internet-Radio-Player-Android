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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.murano500k.newradio.Constants;
import com.android.murano500k.newradio.PlaylistManager;
import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;
import com.wang.avi.AVLoadingIndicatorView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ActivityRxTest extends AppCompatActivity {
	public static final String TAG = "ActivityRadio";
	public static final String INTENT_UPDATE_STATIONS = "com.android.murano500k.newradio.INTENT_UPDATE_STATIONS";
	public static final String INTENT_OPEN_APP = "com.android.murano500k.newradio.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.android.murano500k.newradio.INTENT_CLOSE_APP";
	public static final String INTENT_SELECT_PLS = "com.android.murano500k.newradio.INTENT_SELECT_PLS";
	private static final int SELECT_PLS_REQUEST_CODE = 444;
	RecyclerView recyclerView;
	ListAdapterRx adapter;
	ImageButton btnPlay, btnPrev, btnNext;
	private AVLoadingIndicatorView spinner;
	private static ServiceRadioRx serviceRadioRx;
	boolean isListInitiated = false;
	boolean isFavOnly;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private PlaylistManager playlistManager;
	private ActivityRxTest activityRxTest;
	private LinearLayout layoutSelectPls;
	private Toast toast;

	private boolean isPlaying() {
		return serviceRadioRx != null && serviceRadioRx.isPlaying();
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			//log("Service Connected.");
			serviceRadioRx = ((ServiceRadioRx.LocalBinder) binder).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
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
		activityRxTest=this;
		EventBus.getDefault().register(this);
		playlistManager = new PlaylistManager(getApplicationContext());
		layoutSelectPls=(LinearLayout) findViewById(R.id.select_pls);
		initUI();
		ArrayList<String> list=null;
		if (savedInstanceState != null) list = savedInstanceState.getStringArrayList(Constants.KEY_LIST_URLS);
		if(list==null || list.size()<1) list = playlistManager.getStations();
		if(list==null || list.size()<1) {
			btnPlay.setVisibility(View.GONE);
			btnNext.setVisibility(View.GONE);
			btnPrev.setVisibility(View.GONE);
			layoutSelectPls.setVisibility(View.VISIBLE);
		} else {
			layoutSelectPls.setVisibility(View.GONE);
			btnPlay.setVisibility(View.VISIBLE);
			btnNext.setVisibility(View.VISIBLE);
			btnPrev.setVisibility(View.VISIBLE);
			playlistManager.getStations();
			if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) connect();
			initList(list, playlistManager.getSelectedUrl());
			updateUI();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putStringArrayList(Constants.KEY_LIST_URLS, playlistManager.getStations());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(intent.getAction().contains(INTENT_OPEN_APP)) updateUI();
		else if(intent.getAction().contains(INTENT_CLOSE_APP)){
			this.unbindService(mServiceConnection);
			finish();
		}
		else if(intent.getAction().contains(INTENT_UPDATE_STATIONS)){
			isListInitiated = false;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
				}
			});
		}
		super.onNewIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);

			if (item.getItemId() == R.id.action_shuffle) {
				item.setChecked(playlistManager.isShuffle());
			} else if (item.getItemId() == R.id.action_sleeptimer) {
				if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) item.setChecked(false);
				else item.setChecked(serviceRadioRx.isSleepTimerRunning());
			}
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_shuffle:
				if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) break;
				Log.d(TAG, "action_shuffle. was checked: " + item.isChecked());
				item.setChecked(!playlistManager.isShuffle());
				playlistManager.setShuffle(item.isChecked());
				break;
			/*case R.id.action_select_fav:
				Log.d(TAG, "action_select_fav. was checked: " + item.isChecked());
				item.setChecked(!playlistManager.isOnlyFavorites());
				playlistManager.setOnlyFavorites( item.isChecked());
				*//*Intent intent = new Intent(this, ServiceRadioRx.class);
				intent.setAction(Constants.INTENT.UPDATE_STATIONS);
				intent.putExtra(Constants.DATA_FAV_ONLY, isFavOnly);
				intent.putExtra(Constants.DATA_LIST_URLS, playlistManager.getStations());
				intent.putExtra(Constants.DATA_CURRENT_STATION_URL, playlistManager.getSelectedUrl());
				startService(intent);*//*
				isListInitiated = false;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
					}
				});
				break;*/
			case R.id.action_sleeptimer:
				Log.d(TAG, "action_sleeptimer. was checked: " + item.isChecked());
				if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) break;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (item.isChecked()) dialogShower.showCancelTimerDialog( item, activityRxTest);
						else dialogShower.showSetTimerDialog(item, activityRxTest);
					}
				});
				break;
			case R.id.action_reset:
				if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) break;
				Log.d(TAG, "action_reset. was clicked");
				Intent intentReset=new Intent(this,ServiceRadioRx.class);
				intentReset.setAction(ServiceRadioRx.INTENT_RESET);
				startService(intentReset);
				break;
			case R.id.action_buffer:
				if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) break;
				Log.d(TAG, "action_buffer");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialogShower.showDialogSetBufferSize(activityRxTest,getLayoutInflater());
					}
				});
				break;
			case R.id.action_select_pls:
				Log.d(TAG, "action_select_pls");
				selectPlsIntent();
				break;

		}

		return super.onOptionsItemSelected(item);
	}
	private void selectPlsIntent(){
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
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==RESULT_CANCELED) Toast.makeText(getApplicationContext(), "Station not added", Toast.LENGTH_SHORT).show();
		else if(resultCode==RESULT_OK){
			if(requestCode==SELECT_PLS_REQUEST_CODE) {
				String res=playlistManager.selectPls(data.getData());
				if(res!=null) {
					Log.e(TAG, "load playlist failed. "+ res);
					showToast("Error opening file. " +res);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							layoutSelectPls.setVisibility(View.VISIBLE);
							btnPlay.setVisibility(View.GONE);
							btnNext.setVisibility(View.GONE);
							btnPrev.setVisibility(View.GONE);
						}
					});

				}else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {

							layoutSelectPls.setVisibility(View.GONE);
							btnPlay.setVisibility(View.VISIBLE);
							btnNext.setVisibility(View.VISIBLE);
							btnPrev.setVisibility(View.VISIBLE);
							showToast("file loaded successfully");
							initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
						}
					});

				}
				/*if(Fpath==null) Toast.makeText(getApplicationContext(), "Empty path", Toast.LENGTH_SHORT).show();
				else {
					ArrayList<String> list=playlistManager.selectPls(Fpath);
					if(list==null || list.size()==0)Log.e(TAG,"list is Empty");
					else for (String s: list) Log.w(TAG, "item: " + s);
				}*/
			}


		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	private void btnControlClick(String extra){
		Log.i(TAG, "btnControlClick: "+extra);
		if(true) {
			EventBus.getDefault().post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, ServiceRadioRx.STATUS_LOADING, playlistManager.getSelectedUrl()));
			Intent intent = new Intent(getApplicationContext(), ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(extra, extra);
			startService(intent);
		}else Toast.makeText(getApplicationContext(),"Player is locked!", Toast.LENGTH_SHORT).show();
	}

	public void initUI() {
		progressBar=(ProgressBar) findViewById(R.id.progressBar);
		dialogShower =new DialogShower(playlistManager);

		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrev = (ImageButton) findViewById(R.id.btnPrev);
		spinner = (AVLoadingIndicatorView) findViewById(R.id.spinner);
		btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);

		btnPlay.setOnClickListener(v -> {
			btnControlClick(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED);
		});
		btnPrev.setOnClickListener(v -> {
			btnControlClick(ServiceRadioRx.EXTRA_PREV_PRESSED);
		});
		btnNext.setOnClickListener(v -> {
			btnControlClick(ServiceRadioRx.EXTRA_NEXT_PRESSED);
		});
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
		layoutSelectPls.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectPlsIntent();
			}
		});
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

	private void updateUI(){
		if(isPlaying())updateUI(Constants.UI_STATE.PLAYING, playlistManager.getNameFromUrl(playlistManager.getSelectedUrl()), 100);
		else updateUI(Constants.UI_STATE.IDLE, getResources().getString(R.string.app_name), 100);
	}
	private void updateUI(int state, String title, int progress) {
		//Log.d(TAG, "updatePlaybackViews state: " + state);
		switch (state) {
			case Constants.UI_STATE.LOADING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(true);
				spinner.setVisibility(View.VISIBLE);
				spinner.animate();
				//btnPlay.setVisibility(View.INVISIBLE);
				btnPlay.setActivated(true);
				break;
			case Constants.UI_STATE.PLAYING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(false);
				spinner.setVisibility(View.GONE);
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(true);
				break;
			case Constants.UI_STATE.IDLE:
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
		if(url!=null && !url.equals("")) name=playlistManager.getNameFromUrl(url);

		if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)) {
			Log.i(TAG, "onPlayerStartedEvent: " + name);
			updateUI(Constants.UI_STATE.PLAYING, name, 0);
			if (recyclerView != null && adapter != null && adapter.getItemIndex(eventUi.getExtras().url) != -1)
				recyclerView.smoothScrollToPosition(adapter.getItemIndex(eventUi.getExtras().url));
		}
		else if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)) {
			updateUI(Constants.UI_STATE.IDLE, getResources().getString(R.string.app_name), 0);
		}
		else if (action.equals(UiEvent.UI_ACTION.BUFFER_UPDATED)) {
			updateUI(Constants.UI_STATE.PLAYING, name, eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.LOADING_STARTED)) {
			updateUI(Constants.UI_STATE.LOADING, "loading...", eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.STATION_SELECTED)) {
			Log.i(TAG, "onStationSelected: "+ name);
			updateUI(Constants.UI_STATE.LOADING, name, 0);
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
			if(dialogShower.sleepTimerMenuItem!=null){
				dialogShower.sleepTimerMenuItem.setChecked(false);
				dialogShower.sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
				dialogShower.sleepTimerMenuItem.setTitle("Set sleep timer");
				dialogShower.sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}
			setTitle(R.string.app_name);
		} else if (action== SleepEvent.SLEEP_ACTION.UPDATE) {
			if (seconds > 1) {
				if(seconds>60) setTitle(seconds/60 + " minutes left");
				else setTitle(seconds+" seconds to sleep");
				if(dialogShower.sleepTimerMenuItem!=null) {
					dialogShower.sleepTimerMenuItem.setChecked(true);
					dialogShower.sleepTimerMenuItem.setIcon(R.drawable.ic_cancel_sleep);
					dialogShower.sleepTimerMenuItem.setTitle("Cancel sleep timer");
					dialogShower.sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}
		} else if (action== SleepEvent.SLEEP_ACTION.FINISH) {
			Toast.makeText(getApplicationContext(), "Sleep timer finished", Toast.LENGTH_SHORT).show();
			if(dialogShower.sleepTimerMenuItem!=null) {
				dialogShower.sleepTimerMenuItem.setChecked(false);
				dialogShower.sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
				dialogShower.sleepTimerMenuItem.setTitle("Set sleep timer");
				dialogShower.sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
}
