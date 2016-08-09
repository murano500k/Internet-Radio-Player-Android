package com.android.murano500k.newradio;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.speech.levelmeter.LevelMeterActivity;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;

import rx.Subscription;

public class ActivityRadio extends AppCompatActivity implements ListenerRadio {
    private static final String TAG = "ActivityRadio";

    RecyclerView recyclerView;
    ListAdapter adapter;
    ImageButton btnPlay, btnPrev, btnNext, btnShuffle, btnSleep;
    private AVLoadingIndicatorView spinner;

    private static ServiceRadio serviceRadio;
    private android.widget.ImageView networkStatus;
    private android.widget.TextView sleepMinutes;
    private android.widget.TextView textView;
    private android.widget.RelativeLayout sleepTimerLayout;
    private android.widget.FrameLayout controlLayout;
    ArrayList<ListenerRadio>mRadioListenerQueue = new ArrayList<>();
    boolean isServiceConnected = false;
	boolean isListInitiated = false;


    private boolean isLoading;
    private Subscription loadingTimer;
	private AVLoadingIndicatorView loadingInd;
	private Menu menu;
	private MenuItem sleepTimerMenuItem;
	private ProgressBar progressBar;
	private int maxProgress;
	private PlaylistManager playlistManager;

	public void registerListener(ListenerRadio mRadioListener) {
		if (isServiceConnected)
			serviceRadio.notifier.registerListener(mRadioListener);
		else
			mRadioListenerQueue.add(mRadioListener);
    }
	public void registerListener() {
		serviceRadio.notifier.registerListener(this);
	}
    public void connect() {
        Log.d(TAG, "Requested to connect service.");
        Intent intent = new Intent(this, ServiceRadio.class);
        this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.d(TAG, "Service Connected.");
			serviceRadio = ((ServiceRadio.LocalBinder) binder).getService();
			isServiceConnected = true;
			registerListener();

			if (!mRadioListenerQueue.isEmpty()) {
				for (ListenerRadio mRadioListener : mRadioListenerQueue) {
					registerListener(mRadioListener);
				}
			}
			//serviceRadio.getStationsListChangedNotification();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isServiceConnected=false;
		}
	};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
		playlistManager=new PlaylistManager(getApplicationContext());
		initList();
	    initUI();
        connect();
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Intent intent=new Intent(this, ServiceRadio.class);
		intent.setAction(Constants.INTENT_UPDATE_STATIONS);
		//intent.putExtra(Constants.DATA_FAV_ONLY, favOnly);
		startService(intent);


		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onDestroy() {
		if(serviceRadio != null && isServiceConnected){
			Log.d(TAG, "Service Disconnected.");
			unbindService(mServiceConnection);
		}
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean favOnly;

		switch (item.getItemId()) {
			case R.id.action_select_fav:
				favOnly = true;
				break;
			case R.id.action_select_all:
				favOnly = false;
				break;
			default:
				favOnly = false;
				break;
		}
		Log.d(TAG, "onOptionsItemSelected favOnly"+favOnly);
		Log.d(TAG, "action_select_fav selected");
		if(isServiceConnected){
			Intent intent=new Intent(this, ServiceRadio.class);
			intent.setAction(Constants.INTENT_UPDATE_STATIONS);
			intent.putExtra(Constants.DATA_FAV_ONLY, favOnly);
			startService(intent);
		} else {
			Toast.makeText(getApplicationContext(), "Service not connected",Toast.LENGTH_SHORT ).show();
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		this.menu=menu;

		return true;
	}

    public void initUI(){
	    this.sleepTimerLayout = (RelativeLayout) findViewById(R.id.sleepTimerLayout);
	    this.textView = (TextView) findViewById(R.id.textView);
	    this.sleepMinutes = (TextView) findViewById(R.id.sleepMinutes);
	    this.btnSleep = (ImageButton) findViewById(R.id.btnSleep);
	    this.btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
	    this.btnNext = (ImageButton) findViewById(R.id.btnNext);
	    this.btnPrev = (ImageButton) findViewById(R.id.btnPrev);
	    this.spinner = (AVLoadingIndicatorView) findViewById(R.id.spinner);
	    this.btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);
	    this.networkStatus = (ImageView) findViewById(R.id.networkStatus);

        networkStatus = (ImageView) findViewById(R.id.networkStatus);
        btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isActivated()) {
                    Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
                    intent.setAction(Constants.INTENT_PAUSE_PLAYBACK);
                    startService(intent);
                } else {
                    Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
                    intent.setAction(Constants.INTENT_RESUME_PLAYBACK);
					startService(intent);
					/*String url="";
					if(serviceRadio.getStations().get(adapter.selectedIndex)==null)
						url=serviceRadio.getStations().get(0).url;
					else url=serviceRadio.getStations().get(adapter.selectedIndex).url;
                    intent.putExtra(Constants.DATA_CURRENT_STATION_URL,url);*/
                }
            }
        });
        spinner=(AVLoadingIndicatorView) findViewById(R.id.spinner);
        btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        btnShuffle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_BUTTON_PRESS){
                    v.setPressed(true);
                } else if(event.getAction()==MotionEvent.ACTION_BUTTON_RELEASE){
                    v.setPressed(false);
                }
                return false;
            }
        });
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isActivated()) {
                    v.setActivated(false);
                }else {
                    v.setActivated(true);
                }
            }
        });
        btnPrev= (ImageButton) findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
                intent.setAction(Constants.INTENT_PLAY_PREV);
                startService(intent);
            }
        });
        btnNext= (ImageButton) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
                if(btnShuffle.isActivated()){
                    intent.setAction(Constants.INTENT_PLAY_RANDOM);
                } else intent.setAction(Constants.INTENT_PLAY_NEXT);
                startService(intent);
            }
        });
    }
	public void initList(){

		adapter=new ListAdapter(getApplicationContext());
		if(recyclerView==null){
			recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));
		}
		if(recyclerView.getAdapter()!=null) recyclerView.swapAdapter(adapter, true);
		else recyclerView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		isListInitiated=true;
	}

    @Override
    public void onLoadingStarted(String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
				updatePlaybackViews(Constants.UI_STATE_LOADING);
	            isLoading=true;
	            updateTitle(null, false, -1);
                //if(loadingTimer==null || loadingTimer.isUnsubscribed())startLoadingWatchdog();
            }
        });
    }
    @Override
    public void onProgressUpdate(int p, int pMax, String s ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
				updateProgress(p, pMax, false);
                //if(loadingTimer==null || loadingTimer.isUnsubscribed())startLoadingWatchdog();
            }
        });
    }

    @Override
    public void onRadioConnected() {
	    Log.d(TAG, "onRadioConnected");
/*

	    runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
			    initList(serviceRadio);
			    updateLoadingVisibility(false);
		    }
	    });

*/

    }

    @Override
    public void onPlaybackStarted(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
	            if(isLoading) isLoading=false;
                if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();
				updatePlaybackViews(Constants.UI_STATE_PLAYING);
				//adapter.setSelectedIndex(serviceRadio.getStationByUrl(url).id);
	            adapter.notifyDataSetChanged();
				adapter.updateAnimState(true);
				updateTitle(null, false, -1);
            }
        });
    }

    @Override
    public void onPlaybackStopped(boolean updateNotification) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();
					updatePlaybackViews(Constants.UI_STATE_IDLE);
					adapter.updateAnimState(false);
					updateTitle(null, false, -1);
                }
            });
		if(updateNotification) this.finish();

	}

    @Override
    public void onMetaDataReceived(String s, String s2) {
        final String data=s2;
        //TODO: UPDATE NOTIF and ALBUMART

        if(s!=null && s.equals("StreamTitle")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setPlayingInfo(PlaylistManager.getArtistFromString(data),PlaylistManager
							.getTrackFromString(data));
                }
            });
        }    }

    @Override
    public void onPlaybackError() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();

					updatePlaybackViews(Constants.UI_STATE_IDLE);
					adapter.updateAnimState(false);
					updateTitle("Can't play", false, -1);
                }
            });

    }

	@Override
	public void onListChanged() {
		runOnUiThread(() -> initList());
	}

	@Override
	public void onStationSelected(final String url) {
		if(isListInitiated) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.notifyItemChanged(playlistManager.getIndex(url));
					recyclerView.smoothScrollToPosition(playlistManager.getIndex(url));
				}
			});
		}
	}

	@Override
	public void onSleepTimerStatusUpdate(String action, int seconds) {
		Log.d(TAG, "onSleepTimerStatusUpdate "+ action + " "+ seconds);
		if(action.contains(Constants.ACTION_SLEEP_CANCEL)){
			Toast.makeText(getApplicationContext(), "Sleep time cancelled" ,Toast.LENGTH_SHORT).show();
			sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
			sleepTimerMenuItem.setTitle("Set sleep timer");
			sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			updateTitle(null, false,-1);
		} else if(action.contains(Constants.ACTION_SLEEP_UPDATE)){
			if(seconds>1) {
				updateTitle(null, true, seconds);
				sleepTimerMenuItem.setIcon(R.drawable.ic_cancel_sleep);
				sleepTimerMenuItem.setTitle("Cancel sleep timer");
				sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
		} else if(action.contains(Constants.ACTION_SLEEP_FINISH)){
			Toast.makeText(getApplicationContext(), "Sleep time finished" ,Toast.LENGTH_SHORT).show();
			sleepTimerMenuItem.setIcon(R.drawable.ic_sleep);
			sleepTimerMenuItem.setTitle("Set sleep timer");
			sleepTimerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		}
	}
	public void updateTitle(String text, boolean sleepActive, int seconds) {
		if(sleepActive) {
			int minutes = seconds/60;
			if(minutes==0) setTitle("<1 minute left");
			else setTitle( minutes +" minutes left");
		}
		else if (text!=null && text.length()>0)	setTitle(text);
		else if(serviceRadio.isPlaying()) setTitle(PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl()));
		else if(isLoading) setTitle("Loading...");
		else setTitle(R.string.app_name);
	}

		public void onSleepTimerOptionClick(MenuItem item){
		sleepTimerMenuItem = item;
		if(!serviceRadio.isSleepTimerRunning()) {
			showSetTimerDialog();
		} else {
			showCancelTimerDialog();
		}

	}


	private void showCancelTimerDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog =builder.setTitle("Cancel current timer?")
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG,"try to cancel timer");

								if(isServiceConnected) {
									Intent intent = new Intent();
									intent.setAction(Constants.INTENT_SLEEP_TIMER_CANCEL);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: "+ e.getMessage() );
										e.printStackTrace();
									}
								}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG,"cancelled");
							}
						}).create();
		dialog.show();

	}
	public int selected;
	private void showSetTimerDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final CharSequence[] array = {"1", "15", "30", "60", "90", "120", "240"};
		AlertDialog dialog =builder.setTitle("Set sleep timer in minutes")
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
								Log.d(TAG,"try to set timer "+ array[selected]+" minutes");

								int mins=Integer.parseInt(array[selected].toString());
								int secondsBeforeSleep=mins*60;
								if(isServiceConnected) {
									Intent intent = new Intent(getApplicationContext(),ServiceRadio.class);
									intent.setAction(Constants.INTENT_SLEEP_TIMER_SET);
									Log.d(TAG,"secondsBeforeSleep putExtra : " + secondsBeforeSleep);
									intent.putExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, secondsBeforeSleep);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: "+ e.getMessage() );
										e.printStackTrace();
									}

								}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG,"cancelled");
							}
						}).create();
		dialog.show();
	}
	public void startLoadingWatchdog() {
        System.out.println("WatchDog is scheduled.");
	    /*loadingTimer =Observable.just(0)
			    .delay(5, TimeUnit.SECONDS)
			    .doOnCompleted(() -> {
				    Log.d(TAG, "WatchDog timeout. still loading: "+ isLoading);
				    btnPlay.setActivated(false);
				    spinner.setVisibility(View.GONE);
				    Toast.makeText(getApplicationContext(), "Loading error", Toast.LENGTH_SHORT).show();
				    Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
				    intent.setAction(Constants.INTENT_PAUSE_PLAYBACK);
				    startService(intent);
			    })

	    loadingTimer =Observable.timer(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())

                .subscribe( aLong ->  {
                    Log.d(TAG, "WatchDog timeout. still loading: "+ isLoading);
                        btnPlay.setActivated(false);
                        spinner.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Loading error", Toast.LENGTH_SHORT).show();
                        Intent intent= new Intent(getApplicationContext(), ServiceRadio.class);
                        intent.setAction(Constants.INTENT_PAUSE_PLAYBACK);
                        startService(intent);
                });
	    */
    }
	private void updatePlaybackViews(String UiPlaybackState){
		Log.d(TAG, "updatePlaybackViews state: "  + UiPlaybackState);
		switch (UiPlaybackState){
			case Constants.UI_STATE_LOADING:
				if(spinner!=null && spinner.getVisibility()!=View.VISIBLE) {
					spinner.setVisibility(View.VISIBLE);
					spinner.animate();
				}
				btnPlay.setActivated(true);
				updateProgress(0,-1, true);
				break;
			case Constants.UI_STATE_PLAYING:
				if(spinner!=null && spinner.getVisibility()!=View.GONE) {
					spinner.setVisibility(View.GONE);
				}
				btnPlay.setActivated(true);
				updateProgress(maxProgress,maxProgress, false);

				break;
			case Constants.UI_STATE_IDLE:
				if(spinner!=null && spinner.getVisibility()!=View.GONE) {
					spinner.setVisibility(View.GONE);
				}
				btnPlay.setActivated(false);
				updateProgress(0,-1, false);
				break;
		}
	}
	private void updateProgress(int currentProgress,int maxProgress, boolean intermediate){
		if(progressBar==null){
			progressBar=(ProgressBar) findViewById(R.id.progressBar);
			this.maxProgress=800;
		}
		if(maxProgress<=0) maxProgress=this.maxProgress;
		else this.maxProgress=maxProgress;

		progressBar.setIndeterminate(intermediate);
		progressBar.setMax(maxProgress);
		progressBar.incrementProgressBy(1);
		//Log.d(TAG, "currentProgress/maxProgress=" +currentProgress/maxProgress);
		progressBar.setProgress(currentProgress);
		//progressBar.setT(currentText);


		//waveProgressbar.setCurrent(currentProgress,currentText); // 77, "788M/1024M"
		//waveProgressbar.setMaxProgress(maxProgress);
		//waveProgressbar.setText(String mTextColor,int mTextSize);//"#FFFF00", 41
		//waveProgressbar.setWaveColor("#5b9ef4"); //"#5b9ef4"

		//waveProgressbar.setWave(float mWaveHight,float mWaveWidth);
		//waveProgressbar.setmWaveSpeed(int mWaveSpeed);//The larger the value, the slower the
		// vibration
	}
public void onTempOptionClick(MenuItem item)
{
	Log.d(TAG, "onTempOptionClick");
	if (item.getItemId()==R.id.action_temp) {
		Intent intent= new Intent(getApplicationContext(), LevelMeterActivity.class);
		startActivity(intent);
	}
}
}
