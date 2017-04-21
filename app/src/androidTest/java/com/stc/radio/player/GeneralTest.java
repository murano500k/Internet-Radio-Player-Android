package com.stc.radio.player;

import android.app.Instrumentation;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.stc.radio.player.service.MusicService;
import com.stc.radio.player.source.MusicProvider;
import com.stc.radio.player.source.MusicProviderSource;
import com.stc.radio.player.source.RemoteSource;
import com.stc.radio.player.playback.ExoPlayback;
import com.stc.radio.player.playback.Playback;
import com.stc.radio.player.playback.QueueManager;
import com.stc.radio.player.utils.QueueHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.stc.radio.player.service.MusicService.ACTION_CMD;
import static com.stc.radio.player.service.MusicService.CMD_NAME;
import static com.stc.radio.player.service.MusicService.CMD_SLEEP_CANCEL;
import static com.stc.radio.player.service.MusicService.CMD_SLEEP_START;
import static com.stc.radio.player.service.MusicService.EXTRA_TIME_TO_SLEEP;
import static com.stc.radio.player.service.MusicService.SleepCountdownTimer.NOTIFICATION_ID_SLEEP;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by artem on 1/18/17.
 */
@RunWith(AndroidJUnit4.class)
public class GeneralTest {
	private static final String TAG = "GeneralTest";
	ExoPlayback exoPlayback;
	Instrumentation instrumentation;
	Context context;
	CountingIdlingResource idlingResource;
	private AudioManager audioManager;
	private QueueManager queueManager;
	MusicProvider musicProvider;
	private List<MediaBrowserCompat.MediaItem> mediaItemList;
	private List<MediaSessionCompat.QueueItem> queue;

	@Before
	public void before(){
		instrumentation= InstrumentationRegistry.getInstrumentation();
		context=instrumentation.getTargetContext();
		audioManager=(AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		idlingResource= new CountingIdlingResource("test");
		Espresso.registerIdlingResources(idlingResource);
	}

	public void initTest(){
		MusicProviderSource source= new RemoteSource();
		MusicProvider musicProvider=new MusicProvider(source);
		exoPlayback = new ExoPlayback(context,musicProvider);
		exoPlayback.setCallback(new Playback.Callback() {
			@Override
			public void onCompletion() {
				Log.d(TAG, "onCompletion: ");
			}

			@Override
			public void onPlaybackStatusChanged(int state) {
				Log.d(TAG, "onPlaybackStatusChanged: "+state);
			}

			@Override
			public void onError(String error) {
				Log.e(TAG, "onError: "+error);
			}

		});

		queueManager=new QueueManager(musicProvider, context.getResources(), new QueueManager.MetadataUpdateListener() {
			@Override
			public void onMetadataChanged(MediaMetadataCompat metadata) {
				Log.d(TAG, "onMetadataChanged: "+metadata);
			}

			@Override
			public void onMetadataRetrieveError() {
				Log.e(TAG, "onMetadataRetrieveError: ");

			}

			@Override
			public void onCurrentQueueIndexUpdated(int queueIndex) {
				Log.d(TAG, "onCurrentQueueIndexUpdated: "+queueIndex);
			}

			@Override
			public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue) {
				Log.d(TAG, "onQueueUpdated: "+title);
				Log.d(TAG, "onQueueUpdated: "+newQueue);
			}
		});
		idlingResource= new CountingIdlingResource("test");
		Espresso.registerIdlingResources(idlingResource);
		idlingResource.increment();

		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				musicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
					@Override
					public void onMusicCatalogReady(boolean success) {
						assertTrue(success);
						mediaItemList=musicProvider.getChildren(MEDIA_ID_ROOT);
						idlingResource.decrement();


					}
				});
			}
		});
		SystemClock.sleep(5000);
		assertNotNull(mediaItemList);
		assertNotNull(musicProvider);
		queue= QueueHelper.getPlayingQueue(
				mediaItemList.get(0).getMediaId(),
				musicProvider
		);
		Log.d(TAG, "queue size: "+queue.size());

	}
	private void playNoWait(int i, String msg) {
		MediaSessionCompat.QueueItem item=queue.get(i);
		assertNotNull(item);
		Log.d(TAG, "playWait: "+item);
		idlingResource.increment();
		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "before "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				exoPlayback.play(item);
				Log.d(TAG, "after "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				//while(!audioManager.isMusicActive())SystemClock.sleep(1000);
			}
		});
		//SystemClock.sleep(1000);
		idlingResource.decrement();
	}

	private void playWait(int i, String msg) {
		MediaSessionCompat.QueueItem item=queue.get(i);
		assertNotNull(item);
		Log.d(TAG, "playWait: "+item);
		idlingResource.increment();
		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "before "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				exoPlayback.play(item);
				Log.d(TAG, "after "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				while(!audioManager.isMusicActive())SystemClock.sleep(1000);
			}
		});
		SystemClock.sleep(1000);
		idlingResource.decrement();
	}
	private void pauseWait(String msg) {
		idlingResource.increment();
		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "before "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				//exoPlayback.stop(true);
				exoPlayback.pause();
				Log.d(TAG, "after "+msg+": "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
				while(!audioManager.isMusicActive())SystemClock.sleep(1000);
			}
		});
		SystemClock.sleep(1000);
		idlingResource.decrement();
	}
	@Test
	public void testPlayer(){
		initTest();
		Log.d(TAG, "before start: "+exoPlayback.getExoPlayWhenReady()+" "+exoPlayback.getExoState());
		playNoWait(1, "1play no wait pressed");
		Log.d(TAG, "1playing: "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
		playWait(345, "2failplay wait pressed");

		//Log.d(TAG, "stopped: "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());

		SystemClock.sleep(2000);
		Log.d(TAG, "2playing after 2 sec: "+exoPlayback.getExoState()+" "+exoPlayback.getExoPlayWhenReady());
		playWait(11, "3play wait pressed");
		SystemClock.sleep(2000);

		pauseWait("pause pressed");
		//playWait(8, "skiptonext pressed");
		SystemClock.sleep(2000);
		assertTrue(!audioManager.isMusicActive());
		//playNoWait(0);


	}

	@Test
	@RequiresApi(api = Build.VERSION_CODES.M)
	public void testSleepTimer(){
		Intent intentSleep = new Intent(instrumentation.getTargetContext(), MusicService.class);
		intentSleep.setAction(ACTION_CMD);
		intentSleep.putExtra(CMD_NAME, CMD_SLEEP_START);
		intentSleep.putExtra(EXTRA_TIME_TO_SLEEP, 60000);
		idlingResource.increment();

		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "Start service");

				instrumentation.getTargetContext().startService(intentSleep);
				SystemClock.sleep(5000);
				NotificationManager nm=(NotificationManager)instrumentation.getTargetContext()
				.getSystemService(NOTIFICATION_SERVICE);
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
				assertTrue(isRunning);
				idlingResource.decrement();
			}
		});

		instrumentation.waitForIdleSync();

		Intent intentSleepCancel = new Intent(instrumentation.getTargetContext(), MusicService.class);
		intentSleep.setAction(ACTION_CMD);
		intentSleep.putExtra(CMD_NAME, CMD_SLEEP_CANCEL);
		idlingResource.increment();
		instrumentation.runOnMainSync(new Runnable() {
			@Override
			public void run() {
				instrumentation.getTargetContext().startService(intentSleepCancel);
				NotificationManager nm=(NotificationManager)instrumentation.getTargetContext()
						.getSystemService(NOTIFICATION_SERVICE);
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
				assertTrue(!isRunning);
				idlingResource.decrement();
			}
		});
		instrumentation.waitForIdleSync();

	}
}
