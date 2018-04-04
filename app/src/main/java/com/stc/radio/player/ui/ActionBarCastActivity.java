/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stc.radio.player.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.stc.radio.player.service.MusicService;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.utils.ListColumnsCounter;
import com.stc.radio.player.utils.LogHelper;

import timber.log.Timber;

import static com.stc.radio.player.service.MusicService.ACTION_CMD;
import static com.stc.radio.player.service.MusicService.CMD_NAME;
import static com.stc.radio.player.service.MusicService.CMD_SLEEP_CANCEL;
import static com.stc.radio.player.service.MusicService.CMD_SLEEP_START;
import static com.stc.radio.player.service.MusicService.EXTRA_TIME_TO_SLEEP;
import static com.stc.radio.player.service.MusicService.SleepCountdownTimer.NOTIFICATION_ID_SLEEP;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 *
 * The requirements for a subclass is to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link Toolbar} with id 'toolbar',
 * a {@link DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(ActionBarCastActivity.class);

    private static final int DELAY_MILLIS = 1000;

    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private boolean mToolbarInitialized;

	private NavigationView navigationView;

    private int mItemToOpenWhenDrawerCloses = -1;
	protected boolean isRoot;


    private final DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerClosed(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerClosed(drawerView);
            if (mItemToOpenWhenDrawerCloses >= 0) {
                Bundle extras = ActivityOptions.makeCustomAnimation(
                    ActionBarCastActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();

 	            startActivity(new Intent(ActionBarCastActivity.this, MusicPlayerActivity.class), extras);
                    finish();

            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerStateChanged(newState);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerOpened(drawerView);
            if (getSupportActionBar() != null) getSupportActionBar()
                    .setTitle(R.string.app_name);
	        if(navigationView==null){
		        Log.e(TAG, "onDrawerOpened: navView == null");
	        }else {
		        initNavMenuItems(navigationView);
	        }
        }
    };

    private final FragmentManager.OnBackStackChangedListener mBackStackChangedListener = this::updateDrawerToggle;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                "the end of your onCreate method");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

	    if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
		    return true;
	    }

	    Timber.w("after toggle");
	    if(item!=null){
		    switch (item.getItemId()) {
			    case android.R.id.home: {
				    onBackPressed();
				    break;
			    }
		    }

    }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
	        finish();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                "'toolbar'");
        }
        mToolbar.inflateMenu(R.menu.menu);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView " +
                        "with id 'nav_view'");
            }
	        initNavMenuItems(navigationView);
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
            mDrawerLayout.setDrawerListener(mDrawerListener);
            populateDrawerItems(navigationView);
            setSupportActionBar(mToolbar);
            updateDrawerToggle();
        } else {
            setSupportActionBar(mToolbar);
        }

        mToolbarInitialized = true;
    }
	public void initNavMenuItems(NavigationView navigationView){

        MenuItem itemShuffle = navigationView.getMenu().findItem(R.id.shuffle);
        itemShuffle.setActionView(new Switch(this));
        ((Switch)itemShuffle.getActionView()).setChecked(DbHelper.isShuffle());
        itemShuffle.getActionView().setOnClickListener(view -> {
            boolean isShuffle = !DbHelper.isShuffle();
            Timber.w("new is shuffle=%b", isShuffle);
            DbHelper.setShuffle(isShuffle);

        });

        MenuItem itemLargeColumns = navigationView.getMenu().findItem(R.id.large_columns);
        itemLargeColumns.setActionView(new Switch(this));
        ((Switch) itemLargeColumns.getActionView()).setChecked(ListColumnsCounter.isUsingLargeColumns(this));
        itemLargeColumns.getActionView().setOnClickListener(view -> {
            boolean isUsingLargeColumns = ListColumnsCounter.isUsingLargeColumns(getApplicationContext());
            Log.d(TAG, "onClick: isUsingLargeColumns=" + isUsingLargeColumns);
            ListColumnsCounter.setUseLargeColumns(getApplicationContext(), !isUsingLargeColumns);
            recreate();
        });

        MenuItem itemSleep = navigationView.getMenu().findItem(R.id.sleep);
        itemSleep.setActionView(new Switch(this));
        boolean isRunning = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isRunning = isSleepTimerRunning();
        }
        ((Switch) itemSleep.getActionView()).setChecked(isRunning);
        itemSleep.getActionView().setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "onClick: isRunning=" + isSleepTimerRunning());
                if (isSleepTimerRunning()) {
                    Intent intentSleep = new Intent(getApplicationContext(), MusicService.class);
                    intentSleep.setAction(ACTION_CMD);
                    intentSleep.putExtra(CMD_NAME, CMD_SLEEP_CANCEL);
                    startService(intentSleep);
                    if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawers();
                        return;
                    }
                } else {
                    showDialogSetSleepTimer();
                }
            } else {
                Toast.makeText(view.getContext(), "This feature require minimum Android version 6.0 Marshmallow", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showDialogSetSleepTimer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = ((Activity) this).getLayoutInflater();
		View v = inflater.inflate(R.layout.sleep_time_selector_dialog, null);
		SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
		TextView textView = (TextView) v.findViewById(R.id.textMinutes);
		seekBar.setMax(100);
		seekBar.setProgress(0);
		textView.setText("20 minutes");
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
			                              boolean fromUser) {
				int val = seekBar.getProgress() + 20;
				String text = val + " minutes";
				textView.setText(text);
			}
		});
		builder.setView(v)
				.setPositiveButton("OK", (dialogInterface, i) -> {
                    String stringVal = textView.getText().toString();
                    int intVal = Integer.parseInt(stringVal.substring(
                            0,
                            stringVal.indexOf(" ")
                    ));
                    if (intVal < 20)
                        Toast.makeText(getApplicationContext(), "Incorrect value", Toast.LENGTH_SHORT).show();
                    else {
                        Intent intentSleep = new Intent(getApplicationContext(), MusicService.class);
                        intentSleep.setAction(ACTION_CMD);
                        intentSleep.putExtra(CMD_NAME, CMD_SLEEP_START);
                        intentSleep.putExtra(EXTRA_TIME_TO_SLEEP, intVal*60000);
                        startService(intentSleep);
                        dialogInterface.dismiss();
                        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                            mDrawerLayout.closeDrawers();
                            return;
                        }
                    }
                })
				.setCancelable(true)
				.setTitle("Select minutes before sleep");
		builder.create();
		builder.show();
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	boolean isSleepTimerRunning(){
		NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		boolean isRunning=false;
			StatusBarNotification[] notifications=nm.getActiveNotifications();
			if(notifications !=null){
				for(StatusBarNotification notification: notifications){
					if(notification.getId()==NOTIFICATION_ID_SLEEP) {
						isRunning=true;
						break;
					}
				}
			}

		return isRunning;
	}
    private void populateDrawerItems(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    Timber.w("test");
                    return true;
                });

    }

    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }

        boolean isRoot = getFragmentManager().getBackStackEntryCount() <= 1;
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }

}
