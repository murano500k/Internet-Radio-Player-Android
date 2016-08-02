package com.android.murano500k.newradio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
	        serviceRadio.getStationsListChangedNotification();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceConnected=false;
        }
    };
    private boolean isLoading;
    private Subscription loadingTimer;
	private RelativeLayout loadingLayout;
	private AVLoadingIndicatorView loadingInd;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
		return true;
	}

    public void initUI(){
	    this.loadingLayout = (RelativeLayout) findViewById(R.id.loading_layout);
	    loadingInd=(AVLoadingIndicatorView) findViewById(R.id.loading);
	    this.controlLayout = (FrameLayout) findViewById(R.id.controlLayout);
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
                    intent.putExtra(Constants.DATA_CURRENT_STATION_URL,
		                    serviceRadio.getStations().get(adapter.selectedIndex).url);
                    startService(intent);
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
	    loadingLayout.setVisibility(View.GONE);
    }
	public void initList(ServiceRadio serviceRadio){
		this.recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter=new ListAdapter(serviceRadio.getStations(), getApplicationContext());
		recyclerView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}
	public void updateLoadingVisibility(boolean v){
		if(v) loadingLayout.setVisibility(View.VISIBLE);
		else loadingLayout.setVisibility(View.GONE);
	}

    @Override
    public void onLoadingStarted(String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
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
                if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();
                adapter.updateAnimState(true);
                btnPlay.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
                btnPlay.setActivated(true);
                adapter.setSelectedIndex(serviceRadio.getStationByUrl(url).id);
	            setTitle(serviceRadio.getStationByUrl(url).name);
            }
        });
    }

    @Override
    public void onPlaybackStopped(boolean updateNotification) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();
                    btnPlay.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.GONE);
                    btnPlay.setActivated(false);
                    adapter.updateAnimState(false);
	                setTitle(getResources().getString(R.string.app_name));
                }
            });



    }

    @Override
    public void onMetaDataReceived(String s, String s2) {
        final String data=s2;
        //TODO: UPDATE NOTIF and ALBUMART

        if(s!=null && s.equals("StreamTitle")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setPlayingInfo(StationContent.getArtistFromString(data),StationContent.getTrackFromString(data));
                    /*radioManager.updateNotification(radioManager.getCurrentStation().name,getArtistFromString(data),getTrackFromString(data)
                            ,RadioPlayerService.getArt(radioManager.getCurrentStation().name, getApplicationContext())
                            ,RadioPlayerService.getArt(radioManager.getCurrentStation().name, getApplicationContext()));
                    updateWidget();*/
                }
            });
        }    }

    @Override
    public void onPlaybackError() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(loadingTimer!=null && !loadingTimer.isUnsubscribed()) loadingTimer.unsubscribe();

                    spinner.setVisibility(View.GONE);
                    btnPlay.setActivated(false);
                }
            });

    }

	@Override
	public void onListChanged(ArrayList<Station> newlist) {
		Log.d(TAG, "onListChanged list size = "+ newlist.size());

		runOnUiThread(() -> {
			//loadingLayout.setVisibility(View.GONE);
			adapter = new ListAdapter(newlist, getApplicationContext());
			if(recyclerView==null) {
				recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
				recyclerView.setLayoutManager(new LinearLayoutManager(this));
				recyclerView.setAdapter(adapter);
			} else recyclerView.swapAdapter(adapter, true);

			isListInitiated=true;
		});
	}

	@Override
	public void onStationSelected(final String url) {
		if(isListInitiated) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Station s = serviceRadio.getStationByUrl(url);
					if (recyclerView != null) {
						if (s != null) {
							adapter.setSelectedIndex(s.id);
							recyclerView.smoothScrollToPosition(s.id);
						}
					}
				}
			});
		}

	}

	@Override
	public void onSleepTimerStatusUpdate(String action, int seconds) {

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

}
