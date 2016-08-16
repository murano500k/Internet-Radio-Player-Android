package com.android.murano500k.newradio;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.HashSet;

public class ActivityRadio extends AppCompatActivity implements ListenerRadio {
	private static final String TAG = "ActivityRadio";

	RecyclerView recyclerView;
	ListAdapter adapter;
	ImageButton btnPlay, btnPrev, btnNext;
	private AVLoadingIndicatorView spinner;
	private static ServiceRadio serviceRadio;
	ArrayList<ListenerRadio> mRadioListenerQueue = new ArrayList<>();
	boolean isServiceConnected = false;
	boolean isListInitiated = false;
	boolean isFavOnly;
	private boolean isLoading;
	private MenuItem sleepTimerMenuItem;
	private ProgressBar progressBar;
	private TextView progressText;
	private PlaylistManager playlistManager;
	private Dialog dialogSetBufferSize;
	private int uiPlaybackState;
	private boolean buttonsLocked;
	private int sizeBuffer, sizeDecode;
	private AudioManager audioManager;


	public void registerListener(ListenerRadio mRadioListener) {
		if (isServiceConnected)
			serviceRadio.notifier.registerListener(mRadioListener);
		else
			mRadioListenerQueue.add(mRadioListener);
	}

	public void unRegisterListener(ListenerRadio mRadioListener) {
		if (isServiceConnected && serviceRadio.notifier != null)
			serviceRadio.notifier.unRegisterListener(mRadioListener);
	}

	public void connect() {
		Log.d(TAG, "Requested to connect service.");
		Intent intent = new Intent(this, ServiceRadio.class);
		this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		registerListener(this);
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.d(TAG, "Service Connected.");
			serviceRadio = ((ServiceRadio.LocalBinder) binder).getService();
			isServiceConnected = true;
			if (!mRadioListenerQueue.isEmpty()) {
				for (ListenerRadio mRadioListener : mRadioListenerQueue) {
					registerListener(mRadioListener);
				}
			}
			updateTitle(null,false,0);
			if(serviceRadio.isPlaying()) updatePlaybackViews(Constants.UI_STATE.PLAYING);
			else updatePlaybackViews(Constants.UI_STATE.IDLE);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isServiceConnected = false;
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_radio);
		if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


		playlistManager = new PlaylistManager(getApplicationContext());
		connect();
		HashSet<String> list;
		ArrayList<String> arrayList = null;
		if (savedInstanceState != null) {
			arrayList = savedInstanceState.getStringArrayList(Constants.KEY_LIST_URLS);
			isFavOnly = savedInstanceState.getBoolean(Constants.KEY_FAV_ONLY);
		}else isFavOnly = playlistManager.isOnlyFavorites();
		if(arrayList==null || arrayList.size()<1) list = playlistManager.getStations();
		else list=new HashSet<>(arrayList);
		isListInitiated = false;
		initList(list, playlistManager.getSelectedUrl(), isFavOnly);
		initUI();
		if(isServiceConnected) {
			updateTitle(null,false,0);
			if(serviceRadio.isPlaying()) updatePlaybackViews(Constants.UI_STATE.PLAYING);
			else updatePlaybackViews(Constants.UI_STATE.IDLE);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(Constants.KEY_FAV_ONLY, playlistManager.isOnlyFavorites());
		outState.putString(Constants.KEY_SELECTED_URL, playlistManager.getSelectedUrl());
		outState.putStringArrayList(Constants.KEY_LIST_URLS, adapter.getItems());
		outState.putBoolean(Constants.KEY_IS_SHUFFLE, playlistManager.isShuffle());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(intent.getAction().contains(Constants.INTENT.OPEN_APP)){
			if(isServiceConnected){
				updateTitle(null,false,0);
				if(serviceRadio.isPlaying()) updatePlaybackViews(Constants.UI_STATE.PLAYING);
				else updatePlaybackViews(Constants.UI_STATE.IDLE);
			} else updatePlaybackViews(Constants.UI_STATE.IDLE);
			isFavOnly=playlistManager.isOnlyFavorites();

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

			} else if (item.getItemId() == R.id.action_select_fav) {
				item.setChecked(isFavOnly);

			} else if (item.getItemId() == R.id.action_sleeptimer) {
				item.setChecked(serviceRadio.isSleepTimerRunning());
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_shuffle:
				Log.d(TAG, "action_shuffle. was checked: " + item.isChecked());
				item.setChecked(!playlistManager.isShuffle());
				playlistManager.setShuffle(item.isChecked());
				break;
			case R.id.action_select_fav:
				Log.d(TAG, "action_select_fav. was checked: " + item.isChecked());
				isFavOnly = !item.isChecked();
				item.setChecked(isFavOnly);
				playlistManager.setOnlyFavorites(isFavOnly);
				Intent intent = new Intent(this, ServiceRadio.class);
				intent.setAction(Constants.INTENT.UPDATE_STATIONS);
				intent.putExtra(Constants.DATA_FAV_ONLY, isFavOnly);
				intent.putExtra(Constants.DATA_LIST_URLS, playlistManager.getStations());
				intent.putExtra(Constants.DATA_CURRENT_STATION_URL, playlistManager.getSelectedUrl());
				startService(intent);
				isListInitiated = false;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						initList(playlistManager.getStations(), playlistManager.getSelectedUrl(), isFavOnly);
					}
				});
				break;
			case R.id.action_sleeptimer:
				Log.d(TAG, "action_sleeptimer. was checked: " + item.isChecked());
				if (item.isChecked()) showCancelTimerDialog(item);
				else showSetTimerDialog(item);
				break;
			case R.id.action_buffer:
				Log.d(TAG, "action_buffer");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showDialogSetBufferSize();
					}
				});
				break;
			case R.id.action_log:
				Log.d(TAG, "action_log");
				Intent intent1=new Intent(getApplicationContext(),ActivityTemp.class);
				startActivity(intent1);
				break;
			case R.id.action_add:
				Log.d(TAG, "action_add");
				Intent intent2=new Intent(getApplicationContext(),ActivityCreateStation.class);
				startActivityForResult(intent2, Constants.REQUSET_ADD_STATION);
				break;

		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==RESULT_CANCELED) Toast.makeText(getApplicationContext(), "Station not added", Toast.LENGTH_SHORT).show();
		else if(resultCode==RESULT_OK){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "Station added successfully", Toast.LENGTH_SHORT).show();
					initList(playlistManager.getStations(), playlistManager.getSelectedUrl(), playlistManager.isOnlyFavorites());
				}
			});
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		if (serviceRadio != null && isServiceConnected) {
			Log.d(TAG, "Service Disconnected.");
			unbindService(mServiceConnection);
		}
		super.onDestroy();
	}


	public void initUI() {

		this.btnNext = (ImageButton) findViewById(R.id.btnNext);
		this.btnPrev = (ImageButton) findViewById(R.id.btnPrev);
		this.spinner = (AVLoadingIndicatorView) findViewById(R.id.spinner);
		btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);

		btnPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!buttonsLocked){
					if (uiPlaybackState == Constants.UI_STATE.LOADING || uiPlaybackState == Constants.UI_STATE.PLAYING) {
						updatePlaybackViews(Constants.UI_STATE.IDLE);

						Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
						intent.setAction(Constants.INTENT.PLAYBACK.PAUSE);
						startService(intent);
					} else {
						updatePlaybackViews(Constants.UI_STATE.LOADING);
						Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
						intent.setAction(Constants.INTENT.PLAYBACK.RESUME);
						startService(intent);
					}
					lockButtons();
				} else {
					Toast.makeText(getApplicationContext(), "Buttons locked", Toast.LENGTH_SHORT).show();
				}
			}
		});
		btnPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!buttonsLocked){
					updatePlaybackViews(Constants.UI_STATE.LOADING);

					Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
					intent.setAction(Constants.INTENT.PLAYBACK.PLAY_PREV);
					startService(intent);
					lockButtons();
				}else 	Toast.makeText(getApplicationContext(), "Buttons locked", Toast.LENGTH_SHORT).show();
			}
		});
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if(!buttonsLocked){
					updatePlaybackViews(Constants.UI_STATE.LOADING);

					Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
					intent.setAction(Constants.INTENT.PLAYBACK.PLAY_NEXT);
					startService(intent);
					lockButtons();
				}else 	Toast.makeText(getApplicationContext(), "Buttons locked", Toast.LENGTH_SHORT).show();
			}
		});
		btnPlay.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
				else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
				return false;
			}
		});
		btnPrev.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
				else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
				return false;
			}
		});
		btnNext.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
				else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
				return false;
			}
		});
		unLockButtons();
	}

	private void lockButtons(){
		buttonsLocked=true;
	}
	private void unLockButtons(){
		buttonsLocked=false;
	}

	public void initList(HashSet<String> list, String urlS, boolean isFavOnly) {
		isListInitiated = false;
		adapter = new ListAdapter(getApplicationContext(), list, urlS, isFavOnly);
		if (recyclerView == null) {
			recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));
		}
		recyclerView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		registerListener(adapter);
		isListInitiated = true;
	}

	@Override
	public void onFinish() {
		Log.d(TAG, "onFinish");
		unRegisterListener(this);
		if (adapter != null) unRegisterListener(adapter);
		finish();
	}

	@Override
	public void onLoadingStarted(String url) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updatePlaybackViews(Constants.UI_STATE.LOADING);
				updateTitle(null, false, -1);
			}
		});
	}

	@Override
	public void onProgressUpdate(int p, int pMax, String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateProgress(p, pMax, false);
			}
		});
	}

	@Override
	public void onRadioConnected() {
		Log.d(TAG, "onRadioConnected");
	}

	@Override
	public void onPlaybackStarted(final String url) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updatePlaybackViews(Constants.UI_STATE.PLAYING);
				updateTitle(null, false, -1);
			}
		});
	}

	@Override
	public void onPlaybackStopped(boolean updateNotification) {
		Log.d(TAG, "onPlaybackStopped "+ updateNotification);
		if(!updateNotification) {
			finish();
			return;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updatePlaybackViews(Constants.UI_STATE.IDLE);
				updateTitle(null, false, -1);

			}
		});


	}

	@Override
	public void onMetaDataReceived(String s, String s2) {

	}

	@Override
	public void onPlaybackError(boolean updateNotification) {
		Log.d(TAG, "onPlaybackStopped "+ updateNotification);
		if(!updateNotification) {
			finish();
			return;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updatePlaybackViews(Constants.UI_STATE.IDLE);
				updateTitle("Can't play", false, -1);
			}
		});

	}

	@Override
	public void onStationSelected(final String url) {
		if (isListInitiated) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setTitle(PlaylistManager.getNameFromUrl(url));
					if(recyclerView!=null && adapter.getItemIndex(url)!=-1)
						recyclerView.smoothScrollToPosition(adapter.getItemIndex(url));
				}
			});
		}
	}

	private void updatePlaybackViews(int UiPlaybackState) {
		Log.d(TAG, "updatePlaybackViews state: " + UiPlaybackState);
		uiPlaybackState=UiPlaybackState;
		unLockButtons();
		switch (UiPlaybackState) {
			case Constants.UI_STATE.LOADING:
				if (spinner != null && spinner.getVisibility() != View.VISIBLE) {
					spinner.setVisibility(View.VISIBLE);
					spinner.animate();
				}
				btnPlay.setVisibility(View.INVISIBLE);
				btnPlay.setActivated(true);
				updateProgress(0, -1, true);
				break;
			case Constants.UI_STATE.PLAYING:
				if (spinner != null && spinner.getVisibility() != View.GONE) {
					spinner.setVisibility(View.GONE);
				}
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(true);
				break;
			case Constants.UI_STATE.IDLE:
				if (spinner != null && spinner.getVisibility() != View.GONE) {
					spinner.setVisibility(View.GONE);
				}
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(false);
				updateProgress(0, -1, false);
				break;
		}
	}

	private void updateProgress(int audioBufferSizeMs, int audioBufferCapacityMs, boolean intermediate) {
		if (progressBar == null) {
			progressBar = (ProgressBar) findViewById(R.id.progressBar);
		}
		if (progressText == null) {
			progressText = (TextView) findViewById(R.id.progressText);
		}
		progressBar.setIndeterminate(intermediate);

		String s="";
		if(intermediate) {
			s="...";
		}else if(audioBufferCapacityMs==0){
			progressBar.setProgress(0);
			s="0%";
		}
		else {
			progressBar.setProgress( audioBufferSizeMs * progressBar.getMax() / audioBufferCapacityMs );
			s= audioBufferSizeMs * progressBar.getMax() / audioBufferCapacityMs+"%";
		}
		progressText.setText(s);
	}

	public void updateTitle(String text, boolean sleepActive, int seconds) {
		if (sleepActive) {
			int minutes = seconds / 60;
			if (minutes == 0) setTitle("<1 minute left");
			else setTitle(minutes + " minutes left");
		} else if (text != null && text.length() > 0) setTitle(text);
		else if (serviceRadio.isPlaying())
			setTitle(PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl()));
		else if (isLoading) setTitle("Loading...");
		else setTitle(R.string.app_name);
	}


	@Override
	public void onSleepTimerStatusUpdate(String action, int seconds) {
		Log.d(TAG, "onSleepTimerStatusUpdate " + action + " " + seconds);
		if (action.contains(Constants.ACTION_SLEEP_CANCEL)) {
			Toast.makeText(getApplicationContext(), "Sleep time cancelled", Toast.LENGTH_SHORT).show();
			if(sleepTimerMenuItem!=null){
				sleepTimerMenuItem.setChecked(false);
				sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
				sleepTimerMenuItem.setTitle("Set sleep timer");
				sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}
			updateTitle(null, false, -1);
		} else if (action.contains(Constants.ACTION_SLEEP_UPDATE)) {
			if (seconds > 1) {
				updateTitle(null, true, seconds);
				if(sleepTimerMenuItem!=null) {
					sleepTimerMenuItem.setChecked(true);
					sleepTimerMenuItem.setIcon(R.drawable.ic_cancel_sleep);
					sleepTimerMenuItem.setTitle("Cancel sleep timer");
					sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}
		} else if (action.contains(Constants.ACTION_SLEEP_FINISH)) {
			Toast.makeText(getApplicationContext(), "Sleep time finished", Toast.LENGTH_SHORT).show();
			if(sleepTimerMenuItem!=null) {
				sleepTimerMenuItem.setChecked(false);
				sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
				sleepTimerMenuItem.setTitle("Set sleep timer");
				sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}
		}
	}

	private void showDialogSetBufferSize() {
		if (!isServiceConnected) {
			Log.d(TAG, "isServiceConnected=false");
		} else {

			View dialogView=getLayoutInflater().inflate(R.layout.dialog_buffer, null);
				Button b1 = (Button) dialogView.findViewById(R.id.buttonOk);
				Button b2 = (Button) dialogView.findViewById(R.id.buttonCancel);
				EditText editBuffer = (EditText) dialogView.findViewById(R.id.editTextBuffer);
				EditText editDecode = (EditText) dialogView.findViewById(R.id.editTextDecode);

				editBuffer.setText(String.valueOf(playlistManager.getBufferSize()));
				editDecode.setText(String.valueOf(playlistManager.getDecodeSize()));
				b1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						sizeBuffer = Integer.parseInt(editBuffer.getText().toString());
						sizeDecode = Integer.parseInt(editDecode.getText().toString());
						Log.d(TAG, "onBufferOptionClick sizeBuffer=" + sizeBuffer + " sizeDecode=" + sizeDecode);
						Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
						intent.setAction(Constants.INTENT.SET_BUFFER_SIZE);
						intent.putExtra(Constants.DATA_AUDIO_BUFFER_CAPACITY, sizeBuffer);
						intent.putExtra(Constants.DATA_AUDIO_DECODE_CAPACITY, sizeDecode);
						startService(intent);
						if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing())
							dialogSetBufferSize.dismiss();
					}
				});

				b2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing()) {
							Toast.makeText(getApplicationContext(), "Set buffer size cancelled", Toast.LENGTH_SHORT).show();
							dialogSetBufferSize.dismiss();
						}
					}
				});
			dialogSetBufferSize = new AlertDialog.Builder(this)
					.setTitle("Set buffer size in ms")
					.setView(dialogView)
					.setCancelable(true)
					.create();
			dialogSetBufferSize.show();


		}
	}

	private void showCancelTimerDialog(MenuItem menuItem) {
		sleepTimerMenuItem=menuItem;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog = builder.setTitle("Cancel current timer?")
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "try to cancel timer");

								if (isServiceConnected) {
									Intent intent = new Intent();
									intent.setAction(Constants.INTENT.SLEEP.CANCEL);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									menuItem.setChecked(false);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										e.printStackTrace();
										menuItem.setChecked(true);
									}

								}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "cancelled");
							}
						}).create();
		dialog.show();

	}

	public int selected;

	private void showSetTimerDialog(MenuItem menuItem) {
		sleepTimerMenuItem=menuItem;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final CharSequence[] array = {"1", "15", "30", "60", "90", "120", "240"};
		AlertDialog dialog = builder.setTitle("Set sleep timer in minutes")
				.setSingleChoiceItems(array, 0, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selected = which;

					}
				})
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "try to set timer " + array[selected] + " minutes");

								int mins = Integer.parseInt(array[selected].toString());
								int secondsBeforeSleep = mins * 60;
								if (isServiceConnected) {
									Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
									intent.setAction(Constants.INTENT.SLEEP.SET);
									Log.d(TAG, "secondsBeforeSleep putExtra : " + secondsBeforeSleep);
									intent.putExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, secondsBeforeSleep);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									sleepTimerMenuItem.setChecked(true);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										e.printStackTrace();
										sleepTimerMenuItem.setChecked(false);
									}
								}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "cancelled");
							}
						}).create();
		dialog.show();
	}

}
