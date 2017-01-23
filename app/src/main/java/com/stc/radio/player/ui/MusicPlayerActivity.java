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
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.R;
import com.stc.radio.player.utils.LogHelper;

import timber.log.Timber;

import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;


/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String SAVED_MEDIA_ID="com.stc.radio.player.MEDIA_ID";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.stc.radio.player.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenPlayerActivity}, speeding up the screen rendering
     * while the {@link android.support.v4.media.session.MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.stc.radio.player.CURRENT_MEDIA_DESCRIPTION";


    private Bundle mVoiceSearchParams;
	private SearchView searchView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    if(isTablet()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
	    else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);


        initializeFromParams(savedInstanceState, getIntent());
	    initializeToolbar();
        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
	    KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);

	    LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            getSupportMediaController().getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }

    @Override
    public void isItemFavorite(String musicId) {

    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenPlayerActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
		                String.valueOf(intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION)));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null
            && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                mVoiceSearchParams.getString(SearchManager.QUERY));
	        String query=mVoiceSearchParams.getString(SearchManager.QUERY);
			getSupportMediaController().getTransportControls().playFromSearch(query,new Bundle());
	        //getBrowseFragment().onScrollToItem(query);
        } else if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
        }else {
	        mediaId = MEDIA_ID_ROOT;
        }
        navigateToBrowser(mediaId);
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search, menu);
		menu.findItem(R.id.search).setIcon(getDrawable(android.R.drawable.ic_menu_search));
		searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {
				Timber.w("");
				return false;
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d(TAG, "onQueryTextSubmit: ");
				Bundle extras=new Bundle();
				extras.putString(SearchManager.QUERY,query);
				KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
				getBrowseFragment().onScrollToItem(query);
				getSupportMediaController().getTransportControls()
						.playFromSearch(query, null);
				return true;
			}
			@Override
			public boolean onQueryTextChange(String s) {
				Log.d(TAG, "onQueryTextChange: "+s);
				if( s==null || s.length()==0){
					KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
					menu.findItem(R.id.search).collapseActionView();
					return false;
				}else if(s.length()>2){
					getBrowseFragment().onScrollToItem(s);
					return true;
				}else {
					return false;
				}
			}
		});
		searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				Log.d(TAG, "onFocusChange: "+b);
			}
		});
		searchView.setOnSearchClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				Log.d(TAG, "onClick: ");
			}
		});
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {
				Log.d(TAG, "onClose: ");
				KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);

				menu.findItem(R.id.search).collapseActionView();
				return true;
			}
		});
		return true;

	}
    public void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
	    if(mediaId.equals(MEDIA_ID_ROOT)) isRoot=true;
	    else isRoot=false;
	    isRoot=false;
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != null) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }



	public String getMediaId() {
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected void onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            getSupportMediaController().getTransportControls()
                    .playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }
        getBrowseFragment().onConnected();
    }
	public boolean isTablet() {
		return (checkDimension(this)>=7);
	}
	private static double checkDimension(Context context) {

		WindowManager windowManager = ((Activity)context).getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);

		// since SDK_INT = 1;
		int mWidthPixels = displayMetrics.widthPixels;
		int mHeightPixels = displayMetrics.heightPixels;

		// includes window decorations (statusbar bar/menu bar)
		try
		{
			Point realSize = new Point();
			Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
			mWidthPixels = realSize.x;
			mHeightPixels = realSize.y;
		}
		catch (Exception ignored) {}

		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(mWidthPixels/dm.xdpi,2);
		double y = Math.pow(mHeightPixels/dm.ydpi,2);
		double screenInches = Math.sqrt(x+y);
		//Timber.d("Screen inches : %d", screenInches);
		return screenInches;
	}

}
