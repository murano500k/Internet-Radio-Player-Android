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

package com.stc.radio.player.service;

 import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.stc.radio.player.ErrorHandler;
import com.stc.radio.player.R;
import com.stc.radio.player.playback.ExoPlayback;
import com.stc.radio.player.playback.PlaybackManager;
import com.stc.radio.player.playback.QueueManager;
import com.stc.radio.player.source.MusicProvider;
import com.stc.radio.player.ui.MusicPlayerActivity;
import com.stc.radio.player.utils.CarHelper;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.PackageValidator;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;


 public class MusicService extends MediaBrowserService implements
         PlaybackManager.PlaybackServiceCallback {

     private static final String TAG = LogHelper.makeLogTag(MusicService.class);

     // Extra on MediaSession that contains the Cast device name currently connected to
     public static final String EXTRA_CONNECTED_CAST = "com.stc.radio.player.CAST_NAME";
     // The action of the incoming Intent indicating that it contains a command
     // to be executed (see {@link #onStartCommand})
     public static final String ACTION_CMD = "com.stc.radio.player.ACTION_CMD";
     // The key in the extras of the incoming Intent indicating the command that
     // should be executed (see {@link #onStartCommand})
     public static final String CMD_NAME = "com.stc.radio.player.CMD_NAME";
     // A value of a CMD_NAME key in the extras of the incoming Intent that
     // indicates that the music playback should be paused (see {@link #onStartCommand})
     public static final String CMD_PAUSE = "com.stc.radio.player.CMD_PAUSE";
	 public static final String CMD_SLEEP_CANCEL = "com.stc.radio.player.CMD_SLEEP_CANCEL";
	 public static final String CMD_SLEEP_START = "com.stc.radio.player.CMD_SLEEP_START";
	 public static final String EXTRA_TIME_TO_SLEEP = "com.stc.radio.player.EXTRA_TIME_TO_SLEEP";

	 // A value of a CMD_NAME key that indicates that the music playback should switch
     // to local playback from cast playback.
     public static final String CMD_STOP_CASTING = "com.stc.radio.player.CMD_STOP_CASTING";
     // Delay stopSelf by using a handler.
     private static final int STOP_DELAY = 30000;

	 private MusicProvider mMusicProvider;
     private PlaybackManager mPlaybackManager;

     private MediaSession mSession;
     private MediaNotificationManager mMediaNotificationManager;
     private Bundle mSessionExtras;
     private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
     private MediaRouter mMediaRouter;
     private PackageValidator mPackageValidator;
     private boolean mIsConnectedToCar;
     private BroadcastReceiver mCarConnectionReceiver;
	 private SleepCountdownTimer sleepTimer;
	 private ErrorHandler mErrorHandler;


     @Override
     public void onCreate() {
         super.onCreate();
         LogHelper.d(TAG, "onCreate");
         mErrorHandler=new ErrorHandler(this);

         mMusicProvider = new MusicProvider();

         mMusicProvider.retrieveMediaAsync(null /* Callback */);

         mPackageValidator = new PackageValidator(this);

         QueueManager queueManager = new QueueManager(mMusicProvider, getResources(),
                 new QueueManager.MetadataUpdateListener() {
                     @Override
                     public void onMetadataChanged(MediaMetadata metadata) {
                         mSession.setMetadata(metadata);
                     }

                     @Override
                     public void onMetadataRetrieveError() {
                         mPlaybackManager.updatePlaybackState(
                                 getString(R.string.error_no_metadata));
                     }

                     @Override
                     public void onCurrentQueueIndexUpdated(int queueIndex) {
                         mPlaybackManager.handlePlayRequest();
                     }

                     @Override
                     public void onQueueUpdated(String title,
                                                List<MediaSession.QueueItem> newQueue) {
                         mSession.setQueue(newQueue);
                         mSession.setQueueTitle(title);
                     }
                 });



	     ExoPlayback playback = new ExoPlayback(this, mMusicProvider);
         mPlaybackManager = new PlaybackManager(this, getResources(), mMusicProvider, queueManager,
                 playback);

         // Start a new MediaSession
         mSession = new MediaSession(this, "MusicService");
         setSessionToken(mSession.getSessionToken());
         mSession.setCallback(mPlaybackManager.getMediaSessionCallback());
         mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                 MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

         Context context = getApplicationContext();
         Intent intent = new Intent(context, MusicPlayerActivity.class);
         PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                 intent, PendingIntent.FLAG_UPDATE_CURRENT);
         mSession.setSessionActivity(pi);

         mSessionExtras = new Bundle();
         CarHelper.setSlotReservationFlags(mSessionExtras, true, true, true);
         //WearHelper.setSlotReservationFlags(mSessionExtras, true, true);
         //WearHelper.setUseBackgroundFromTheme(mSessionExtras, true);
         mSession.setExtras(mSessionExtras);

         mPlaybackManager.updatePlaybackState(null);

         try {
             mMediaNotificationManager = new MediaNotificationManager(this);
         } catch (RemoteException e) {
             throw new IllegalStateException("Could not create a MediaNotificationManager", e);
         }



         mMediaRouter = MediaRouter.getInstance(getApplicationContext());

         registerCarConnectionReceiver();
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onStartCommand(Intent, int, int)
      */
     @Override
     public int onStartCommand(Intent startIntent, int flags, int startId) {
	     if (startIntent != null) {


		     String action = startIntent.getAction();
             String command = startIntent.getStringExtra(CMD_NAME);
		     Log.d(TAG, "onStartCommand: action="+action);
		     Log.d(TAG, "onStartCommand: cmd="+command);
             if (ACTION_CMD.equals(action)) {
                 if (CMD_PAUSE.equals(command)) {
                     mPlaybackManager.handlePauseRequest();
                 }else if(CMD_SLEEP_START.equals(command)){
	                startSleepTimer(startIntent);
                 }else if(CMD_SLEEP_CANCEL.equals(command)){
					cancelSleepTimer();
                 }
             } else {
                 // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                 Log.w(TAG, "MediaButtonReceiver.handleIntent: "+startIntent );
                 //MediaButtonReceiver.handleIntent(mSession, startIntent);
             }
         }
         // Reset the delay handler to enqueue a message to stop the service if
         // nothing is playing.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         return START_STICKY;
     }

	 private void cancelSleepTimer() {
		 if(sleepTimer !=null) {
			 Toast.makeText(this, "Sleep timer cancelled", Toast.LENGTH_SHORT).show();
			 sleepTimer.cancelTimer();
			 sleepTimer=null;
		 }
	 }

	 public void startSleepTimer(Intent startIntent) {
		 cancelSleepTimer();
		 int timeToSleep=1200000;
		 if(startIntent.getExtras().containsKey(EXTRA_TIME_TO_SLEEP)){
			 timeToSleep=startIntent.getExtras().getInt(EXTRA_TIME_TO_SLEEP);
		 }
		 if(timeToSleep<30000 || timeToSleep>90000000){
			 timeToSleep=1200000;
		 }
		 Log.d(TAG, "startSleepTimer: "+timeToSleep);
		 sleepTimer=new SleepCountdownTimer(timeToSleep,this);
		 sleepTimer.start();

	 }

	 /**
      * (non-Javadoc)
      * @see android.app.Service#onDestroy()
      */
     @Override
     public void onDestroy() {
         LogHelper.d(TAG, "onDestroy");
         unregisterCarConnectionReceiver();
         // Service is being killed, so make sure we release our resources
         mPlaybackManager.handleStopRequest(null);
         mMediaNotificationManager.stopNotification();



         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mSession.release();
         try {
             mErrorHandler.writeErrorsToFile();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }


     @Override
     public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                  Bundle rootHints) {
         LogHelper.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                 "; clientUid=" + clientUid + " ; rootHints=", rootHints);
         if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
             // If the request comes from an untrusted package, return null. No further calls will
             // be made to other media browsing methods.
             LogHelper.w(TAG, "OnGetRoot: IGNORING request from untrusted package "
                     + clientPackageName);
             return null;
         }

         return new BrowserRoot(MEDIA_ID_ROOT, null);
     }

     @Override
     public void onLoadChildren(@NonNull final String parentMediaId,
                                @NonNull final Result<List<MediaBrowser.MediaItem>> result) {
         LogHelper.d(TAG, "OnLoadChildren: parentMediaId=", parentMediaId);
         if (mMusicProvider.isInitialized()) {
             // if music library is ready, return immediately
             result.sendResult(mMusicProvider.getChildren(parentMediaId));
         } else {
             result.detach();

             mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                 @Override
                 public void onMusicCatalogReady(boolean success) {
                     result.sendResult(mMusicProvider.getChildren(parentMediaId));
                 }
             });
         }
     }

	 @Override
	 public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowser.MediaItem>> result, @NonNull Bundle options) {

		 super.onLoadChildren(parentId, result, options);
	 }

	 @Override
	 public void onLoadItem(String itemId, Result<MediaBrowser.MediaItem> result) {


	 }

	 @Override
     public void onPlaybackStart() {
         if (!mSession.isActive()) {
             mSession.setActive(true);
         }

         mDelayedStopHandler.removeCallbacksAndMessages(null);

         // The service needs to continue running even after the bound client (usually a
         // MediaController) disconnects, otherwise the music playback will stop.
         // Calling startService(Intent) will keep the service running until it is explicitly killed.
         startService(new Intent(getApplicationContext(), MusicService.class));
     }


     /**
      * Callback method called from PlaybackManager whenever the music stops playing.
      */
     @Override
     public void onPlaybackStop() {
         Log.w(TAG, "onPlaybackStop: ");
         // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
         // potentially stopping the service.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         stopForeground(true);
     }

     @Override
     public void onNotificationRequired() {
         mMediaNotificationManager.startNotification();
     }

     @Override
     public void onPlaybackStateUpdated(PlaybackState newState) {
         mSession.setPlaybackState(newState);
     }

     @Override
     public void onError(ExoPlaybackException e) {
        mErrorHandler.addError(e);

     }

     private void registerCarConnectionReceiver() {
         IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
         mCarConnectionReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                 mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                 LogHelper.i(TAG, "Connection event to Android Auto: ", connectionEvent,
                         " isConnectedToCar=", mIsConnectedToCar);
             }
         };
         registerReceiver(mCarConnectionReceiver, filter);
     }

     private void unregisterCarConnectionReceiver() {
         unregisterReceiver(mCarConnectionReceiver);
     }

     /**
      * A simple handler that stops the service if playback is not active (playing)
      */
     private static class DelayedStopHandler extends Handler {
         private final WeakReference<MusicService> mWeakReference;

         private DelayedStopHandler(MusicService service) {
             mWeakReference = new WeakReference<>(service);
         }

         @Override
         public void handleMessage(Message msg) {
             MusicService service = mWeakReference.get();
             if (service != null && service.mPlaybackManager.getPlayback() != null) {
                 if (service.mPlaybackManager.getPlayback().isPlaying()) {
                     LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                     return;
                 }
                 LogHelper.d(TAG, "Stopping service with delay handler.");
                 service.stopSelf();
             }
         }
     }




	 public class SleepCountdownTimer extends CountDownTimer
	 {
		 public static final int NOTIFICATION_ID_SLEEP = 125;
		 private static final String TAG = "SleepCountdownTimer";
		 private final Context mContext;
		 NotificationManager notificationManager;
		 Notification notification;
		 private static final int TIMER_UPDATE_INTERVAL=1000;


		 public SleepCountdownTimer(long startTime, Context c)
		 {
			 super(startTime, TIMER_UPDATE_INTERVAL);

			 notificationManager=(NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
			 this.mContext =c;
			 if(notification!=null){
				 notificationManager.cancel(NOTIFICATION_ID_SLEEP);
			 }

			 String string = startTime/60000+" minutes left";
			 notification=createNotification(string);
			 notificationManager.notify(NOTIFICATION_ID_SLEEP, notification);

		 }
		 private Intent getCmdIntent(String cmd ){
			 Intent i = new Intent(mContext, MusicService.class);
			 i.setAction(MusicService.ACTION_CMD);
			 i.putExtra(MusicService.CMD_NAME, cmd);
			 return i;
		 }

		 @Override
		 public void onFinish()
		 {
			 notificationManager.cancel(NOTIFICATION_ID_SLEEP);
			 Toast.makeText(mContext, "Sleep timer finished", Toast.LENGTH_SHORT).show();
			 mContext.startService(getCmdIntent(CMD_PAUSE));
		 }

		 @Override
		 public void onTick(long millisUntilFinished)
		 {
			 String string;
			 long secondsUntilFinished=millisUntilFinished/1000;
			 long minsLft=secondsUntilFinished/60;
			 long secsLeft=secondsUntilFinished%60;

			 if(minsLft == 0) string="less than a minute remaining";
			 else string =  minsLft+" minutes "+secsLeft+" seconds left";

			 /*if(notification!=null){
				 notificationManager.cancel(NOTIFICATION_ID_SLEEP);
			 }*/
			 notification=createNotification(string);
			 notificationManager.notify(NOTIFICATION_ID_SLEEP, notification);
		 }

		 private Notification createNotification(String string) {
			 PendingIntent pendingIntent=PendingIntent.getService(mContext, 0,
					 getCmdIntent(CMD_SLEEP_CANCEL),
					 PendingIntent.FLAG_CANCEL_CURRENT);
			 return new Notification.Builder(mContext)
					 .setContentIntent(pendingIntent)
					 .setSmallIcon(R.drawable.ic_timer)
					 .setContentTitle("Sleep timer is running. Click to cancel" )
					 .setContentText(string)
					 .build();
		 }
		 public void cancelTimer(){
				notificationManager.cancel(NOTIFICATION_ID_SLEEP);
			 this.cancel();
		 }
	 }


 }
