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

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.R;
import com.stc.radio.player.utils.LogHelper;

import timber.log.Timber;

import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;


public class MusicPlayerActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String SAVED_MEDIA_ID="com.stc.radio.player.MEDIA_ID";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.stc.radio.player.EXTRA_START_FULLSCREEN";

    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.stc.radio.player.CURRENT_MEDIA_DESCRIPTION";


    private Bundle mVoiceSearchParams;
	  private SearchView searchView;
    private ProgressBar progress;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_player);
        LogHelper.d(TAG, "Activity onCreate");
        progress=(ProgressBar)findViewById(R.id.progressBar);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        initializeFromParams(savedInstanceState, getIntent());
	      initializeToolbar();
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    private void logStationSelected(MediaBrowser.MediaItem item){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.getDescription().getMediaId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.getDescription().getTitle().toString());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item) {
	    KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
	    LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            logStationSelected(item);
            getMediaController().getTransportControls()
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
    }


    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
        if (intent.getAction() != null
            && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                mVoiceSearchParams.getString(SearchManager.QUERY));
	        String query=mVoiceSearchParams.getString(SearchManager.QUERY);
			getMediaController().getTransportControls().playFromSearch(query,new Bundle());
        } else if (savedInstanceState != null) {
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
		searchView.setOnCloseListener(() -> {
            Timber.w("");
            return false;
        });
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d(TAG, "onQueryTextSubmit: ");
				Bundle extras=new Bundle();
				extras.putString(SearchManager.QUERY,query);
				KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
				getBrowseFragment().onScrollToItem(query);
				getMediaController().getTransportControls()
						.playFromSearch(query, null);
				return true;
			}
			@Override
			public boolean onQueryTextChange(String s) {
				Log.d(TAG, "onQueryTextChange: "+s);
				if( s==null || s.length()==0){
					KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
                    menu.findItem(R.id.search).collapseActionView();
					return true;
				}else if(s.length()>2){
					getBrowseFragment().onScrollToItem(s);
					return true;
				}else {
					return true;
				}
			}
		});
		searchView.setOnCloseListener(() -> {
            Log.d(TAG, "onClose: ");
            KeyboardUtil.hideKeyboard(MusicPlayerActivity.this);
            menu.findItem(R.id.search).collapseActionView();
            return false;
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
            Slide slideTransition = new Slide(Gravity.LEFT);
            slideTransition.setDuration(getResources().getInteger(R.integer.anim_duration_long));

            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);

            fragment.setReenterTransition(slideTransition);
            fragment.setExitTransition(slideTransition);
            fragment.setSharedElementEnterTransition(new ChangeBounds());


            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            transaction.addToBackStack(null);
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
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            getMediaController().getTransportControls()
                    .playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }
        getBrowseFragment().onConnected();
    }
}
